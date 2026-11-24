package com.enterprise.ems.service.impl;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.FaceEnrollment;
import com.enterprise.ems.entity.FaceEnrollmentHistory;
import com.enterprise.ems.dto.FaceEnrollmentHistoryDTO;
import com.enterprise.ems.dto.FaceVerifyResultDTO;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.FaceEnrollmentHistoryRepository;
import com.enterprise.ems.repository.FaceEnrollmentRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.FaceRecognitionService;
import com.enterprise.ems.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/*
 * PURPOSE: Real implementation of face verification, running entirely on
 * this server - no cloud API, no per-call cost, no API key.
 *
 * HOW IT WORKS (see the class-level comment in FaceRecognitionService for the
 * feature-flag behaviour):
 *   1. A photo comes in (enrollment or a punch-in/out attempt).
 *   2. A free, pre-trained, open-source face-embedding model (downloaded once
 *      by DJL from https://resources.djl.ai the first time this feature runs,
 *      then cached locally - see attendance.face-verification.model-cache-dir)
 *      turns the face into a list of ~512 numbers (an "embedding").
 *   3. Two embeddings from the same person end up close together
 *      numerically; different people end up far apart. "Close" is measured
 *      with cosine similarity and compared against
 *      attendance.face-verification.match-threshold.
 *   4. Only step 3's numbers are stored (FaceEnrollment.embedding) - not a
 *      second copy of the photo - and the comparison always happens here on
 *      the server, never trusting anything computed in the browser.
 *
 * NOTE FOR WHOEVER BUILDS THIS NEXT: this class was written and reviewed
 * carefully but could not be compiled inside the sandbox that produced it (no
 * Maven Central access there). Run `mvn clean compile` locally the first time
 * - Maven Central and resources.djl.ai are both normal public endpoints your
 * machine can reach - and if DJL's exact API differs slightly from what's
 * used below (translator signatures do shift a little between DJL versions),
 * the fix is almost always confined to the FaceFeatureTranslator inner class
 * at the bottom of this file.
 */
@Service
@RequiredArgsConstructor
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(FaceRecognitionServiceImpl.class);

    // Free, open-source model DJL ships in its public demo model zoo -
    // downloaded once, cached locally, never billed. No account/API key needed.
    private static final String FACE_FEATURE_MODEL_URL =
            "https://resources.djl.ai/test-models/pytorch/face_feature.zip";
    private static final int MODEL_INPUT_SIZE = 112;

    private final FaceEnrollmentRepository faceEnrollmentRepository;
    private final FaceEnrollmentHistoryRepository faceEnrollmentHistoryRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    @Value("${attendance.face-verification.enabled:false}")
    private boolean enabled;

    @Value("${attendance.face-verification.match-threshold:0.75}")
    private double matchThreshold;

    // Loaded lazily, once, on first actual use - not at application startup.
    // This is what keeps the feature genuinely "free and inert" while
    // disabled: with the flag off, this field simply never gets touched.
    private volatile ZooModel<Image, float[]> featureModel;
    private final Object modelLock = new Object();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean hasEnrollment(Long employeeId) {
        return faceEnrollmentRepository.existsByEmployeeId(employeeId);
    }

    @Override
    @Transactional
    public void enroll(Long employeeId, MultipartFile image, String actorUsername) {
        if (!enabled) {
            throw new BusinessException("Face verification is currently disabled for this system");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        float[] embedding = extractEmbedding(image);

        var existing = faceEnrollmentRepository.findByEmployeeId(employeeId);

        // Re-enrollment: back up what's about to be overwritten before touching it.
        // Nothing about a previous enrollment is ever silently discarded.
        existing.ifPresent(current -> {
            FaceEnrollmentHistory backup = FaceEnrollmentHistory.builder()
                    .employeeId(employeeId)
                    .embedding(current.getEmbedding())
                    .referencePhotoPath(current.getReferencePhotoPath())
                    .originallyCapturedAt(current.getUpdatedAt() != null ? current.getUpdatedAt() : current.getCreatedAt())
                    .replacedBy(actorUsername)
                    .build();
            faceEnrollmentHistoryRepository.save(backup);
        });

        FaceEnrollment enrollment = existing.orElseGet(() -> FaceEnrollment.builder().employee(employee).build());
        enrollment.setEmbedding(toCsv(embedding));

        // Actually persist the captured photo (previously this field was left
        // null - nothing was ever saved to look at) so re-enrollment history
        // and admin review have a real image to fall back on, not just numbers.
        var stored = fileStorageService.storeFile(image, "FaceEnrollment", employeeId, actorUsername);
        enrollment.setReferencePhotoPath(stored.getFilePath());

        faceEnrollmentRepository.save(enrollment);

        boolean wasReEnrollment = existing.isPresent();
        auditService.log(wasReEnrollment ? "UPDATE" : "CREATE", "FaceEnrollment", employeeId,
                (wasReEnrollment ? "Face re-enrolled" : "Face enrolled") + " for employee: "
                        + employee.getEmployeeCode() + " by " + actorUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verify(Long employeeId, MultipartFile image) {
        FaceEnrollment enrollment = faceEnrollmentRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessException(
                        "Face not enrolled yet - please complete face enrollment before punching in"));

        float[] incoming = extractEmbedding(image);
        float[] stored = fromCsv(enrollment.getEmbedding());

        double similarity = cosineSimilarity(incoming, stored);
        log.info("Face verification for employee {}: similarity={} (threshold={})",
                employeeId, similarity, matchThreshold);
        return similarity >= matchThreshold;
    }

    @Override
    public boolean verifyIfRequired(Long employeeId, MultipartFile image) {
        // THE OFF SWITCH: with the feature disabled, do nothing and let the
        // punch proceed exactly as it always has - this is the only line that
        // matters for "disable without touching any other code".
        if (!enabled) {
            return false;
        }
        if (!hasEnrollment(employeeId)) {
            throw new BusinessException(
                    "Face verification is enabled for this organization, but you haven't enrolled your face yet. " +
                            "Please complete face enrollment first, then try again.");
        }
        if (image == null || image.isEmpty()) {
            throw new BusinessException("Please capture your photo to punch in/out - face verification is required.");
        }
        boolean matched = verify(employeeId, image);
        if (!matched) {
            throw new BusinessException(
                    "Face verification failed - this doesn't look like the enrolled photo. " +
                            "Please retake the photo in good lighting, or contact HR if this keeps happening.");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public FaceVerifyResultDTO testVerify(Long employeeId, MultipartFile image) {
        var enrollment = faceEnrollmentRepository.findByEmployeeId(employeeId);
        if (enrollment.isEmpty()) {
            return FaceVerifyResultDTO.builder()
                    .enrolled(false)
                    .matched(false)
                    .similarity(null)
                    .threshold(matchThreshold)
                    .message("This employee has not enrolled their face yet.")
                    .build();
        }

        float[] incoming = extractEmbedding(image);
        float[] stored = fromCsv(enrollment.get().getEmbedding());
        double similarity = cosineSimilarity(incoming, stored);
        boolean matched = similarity >= matchThreshold;

        log.info("[ADMIN TEST-VERIFY] employee {}: similarity={} (threshold={}) -> {}",
                employeeId, similarity, matchThreshold, matched ? "MATCH" : "NO MATCH");

        String message = matched
                ? "Match - this photo is close enough to the enrolled face."
                : String.format(
                        "No match - similarity %.3f is below the threshold %.3f. If this is genuinely the right " +
                        "person, either the enrollment photo was poor (re-enroll with better lighting/framing) or " +
                        "the threshold may be set too strict for this model - see Section 'Tuning' in " +
                        "FACE_VERIFICATION_NOTES.md.",
                        similarity, matchThreshold);

        return FaceVerifyResultDTO.builder()
                .enrolled(true)
                .matched(matched)
                .similarity(similarity)
                .threshold(matchThreshold)
                .message(message)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaceEnrollmentHistoryDTO> getHistory(Long employeeId) {
        return faceEnrollmentHistoryRepository.findByEmployeeIdOrderByReplacedAtDesc(employeeId).stream()
                .map(h -> FaceEnrollmentHistoryDTO.builder()
                        .id(h.getId())
                        .originallyCapturedAt(h.getOriginallyCapturedAt())
                        .replacedAt(h.getReplacedAt())
                        .replacedBy(h.getReplacedBy())
                        .hasPhoto(h.getReferencePhotoPath() != null)
                        .build())
                .toList();
    }

    private float[] extractEmbedding(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("No photo was captured - please try again");
        }
        try (InputStream in = image.getInputStream()) {
            Image img = ImageFactory.getInstance().fromInputStream(in);
            try (Predictor<Image, float[]> predictor = getFeatureModel().newPredictor()) {
                return predictor.predict(img);
            }
        } catch (IOException | TranslateException e) {
            log.error("Face embedding extraction failed", e);
            throw new BusinessException(
                    "Couldn't process that photo - please retake it with your face clearly visible, facing the camera, in good light");
        }
    }

    private ZooModel<Image, float[]> getFeatureModel() {
        ZooModel<Image, float[]> result = featureModel;
        if (result == null) {
            synchronized (modelLock) {
                result = featureModel;
                if (result == null) {
                    try {
                        Criteria<Image, float[]> criteria = Criteria.builder()
                                .setTypes(Image.class, float[].class)
                                .optModelUrls(FACE_FEATURE_MODEL_URL)
                                .optTranslator(new FaceFeatureTranslator())
                                .optEngine("PyTorch")
                                .optProgress(new ProgressBar())
                                .build();
                        result = criteria.loadModel();
                        featureModel = result;
                        log.info("Face recognition model loaded successfully");
                    } catch (IOException | ModelNotFoundException | MalformedModelException e) {
                        log.error("Failed to load face recognition model - is there internet access " +
                                "for the one-time model download from resources.djl.ai?", e);
                        throw new BusinessException(
                                "Face verification is temporarily unavailable. Please contact IT/admin.");
                    }
                }
            }
        }
        return result;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new BusinessException("Face data mismatch - please re-enroll your face");
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }

    private String toCsv(float[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private float[] fromCsv(String csv) {
        String[] parts = csv.split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Float.parseFloat(parts[i]);
        }
        return arr;
    }

    // Turns a raw photo into a normalized 512-number face embedding. The
    // resize-to-112x112 and [-1, 1] normalization match how this particular
    // pre-trained model was trained - changing them would silently break
    // match accuracy, so leave this translator alone unless swapping models.
    private static class FaceFeatureTranslator implements Translator<Image, float[]> {

        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            array = NDImageUtils.resize(array, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);
            array = array.transpose(2, 0, 1)
                    .toType(DataType.FLOAT32, false)
                    .div(255f)
                    .sub(0.5f)
                    .div(0.5f);
            return new NDList(array.expandDims(0));
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            NDArray embedding = list.singletonOrThrow().squeeze(0);
            NDArray norm = embedding.norm();
            embedding = embedding.div(norm);
            return embedding.toFloatArray();
        }

        @Override
        public Batchifier getBatchifier() {
            return null;
        }
    }
}
