package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.CandidateSource;
import com.enterprise.ems.dto.CandidateDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.entity.Candidate;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.JobApplication;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.HiringMapper;
import com.enterprise.ems.repository.CandidateRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.JobApplicationRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.CandidateService;
import com.enterprise.ems.service.hiring.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;

/*
 * PURPOSE: Candidate profiles. Hiring staff manage them; hiring managers and
 * interviewers can only read the candidates they are involved with (HiringAccessService).
 * Resumes are stored privately (HiringFileStorage) and every download is audited.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final EmployeeRepository employeeRepository;
    private final JobApplicationRepository applicationRepository;
    private final HiringAccessService access;
    private final HiringMapper mapper;
    private final HiringFileStorage fileStorage;
    private final AuditService auditService;

    @Override
    public CandidateDTO create(CandidateDTO dto, HiringActor actor) {
        access.requireStaff(actor);
        if (!Boolean.TRUE.equals(dto.getConsentGiven())) {
            throw new BusinessException("Confirm that the candidate has consented to their personal data being processed for hiring");
        }
        String email = normaliseEmail(dto.getEmail());
        String mobile = blankToNull(dto.getMobile());
        if (candidateRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A candidate with email " + email + " already exists");
        }
        if (mobile != null && candidateRepository.existsByMobile(mobile)) {
            throw new DuplicateResourceException("A candidate with mobile number " + mobile + " already exists");
        }

        Candidate c = Candidate.builder()
                .createdBy(actor.employee())
                .consentGivenAt(LocalDateTime.now())
                .active(true)
                .build();
        applyFields(c, dto, email, mobile);
        c = candidateRepository.save(c);

        auditService.log("CREATE", "Candidate", c.getId(), "Created candidate " + c.getFullName());
        return mapper.toDTO(c, true);
    }

    @Override
    public CandidateDTO update(Long id, CandidateDTO dto, HiringActor actor) {
        access.requireStaff(actor);
        Candidate c = getOrThrow(id);
        requireActive(c);

        String email = normaliseEmail(dto.getEmail());
        String mobile = blankToNull(dto.getMobile());
        candidateRepository.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new DuplicateResourceException("A candidate with email " + email + " already exists"); });
        if (mobile != null) {
            candidateRepository.findFirstByMobile(mobile).filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new DuplicateResourceException("A candidate with mobile number " + mobile + " already exists"); });
        }

        applyFields(c, dto, email, mobile);
        candidateRepository.save(c);
        auditService.log("UPDATE", "Candidate", c.getId(), "Updated candidate " + c.getFullName());
        return mapper.toDTO(c, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateDTO getById(Long id, HiringActor actor) {
        Candidate c = getOrThrow(id);
        access.requireCanViewCandidate(actor, id);
        return mapper.toDTO(c, actor.staff());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CandidateDTO> search(String keyword, CandidateSource source, Pageable pageable, HiringActor actor) {
        access.requireStaff(actor);
        String kw = keyword == null ? "" : keyword.trim();
        return Pages.map(candidateRepository.search(kw, source, pageable), c -> mapper.toDTO(c, true));
    }

    @Override
    public CandidateDTO uploadResume(Long id, MultipartFile file, HiringActor actor) {
        access.requireStaff(actor);
        Candidate c = getOrThrow(id);
        requireActive(c);

        HiringFileStorage.StoredFile stored = fileStorage.storeResume(file);
        String previous = c.getResumeStoredName();
        c.setResumeStoredName(stored.storedName());
        c.setResumeOriginalName(stored.originalName());
        c.setResumeContentType(stored.contentType());
        c.setResumeUploadedAt(LocalDateTime.now());
        candidateRepository.save(c);
        fileStorage.delete(previous); // replace, don't accumulate

        auditService.log("UPLOAD_RESUME", "Candidate", c.getId(), "Resume uploaded for " + c.getFullName());
        return mapper.toDTO(c, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeDownload downloadResume(Long id, HiringActor actor) {
        Candidate c = getOrThrow(id);
        access.requireCanViewCandidate(actor, id);
        if (c.getResumeStoredName() == null) {
            throw new ResourceNotFoundException("No resume has been uploaded for this candidate");
        }
        auditService.log("DOWNLOAD_RESUME", "Candidate", c.getId(),
                "Resume of " + c.getFullName() + " opened by " + actor.employee().getFullName());
        return new ResumeDownload(fileStorage.load(c.getResumeStoredName()), c.getResumeOriginalName(), c.getResumeContentType());
    }

    @Override
    public CandidateDTO anonymise(Long id, HiringActor actor) {
        access.requireAdmin(actor);
        Candidate c = getOrThrow(id);
        if (!Boolean.TRUE.equals(c.getActive())) {
            throw new BusinessException("This candidate has already been anonymised");
        }
        boolean hasOpenApplication = applicationRepository.findByCandidateIdOrderByAppliedOnDesc(id).stream()
                .map(JobApplication::getStage)
                .anyMatch(stage -> !stage.isTerminal());
        if (hasOpenApplication) {
            throw new BusinessException("This candidate still has open applications - reject or withdraw them before anonymising");
        }

        fileStorage.delete(c.getResumeStoredName());
        c.setFirstName("Anonymised");
        c.setLastName(null);
        c.setEmail("anonymised-" + c.getId() + "@deleted.invalid");
        c.setMobile(null);
        c.setCurrentCompany(null);
        c.setCurrentDesignation(null);
        c.setCurrentLocation(null);
        c.setTotalExperience(null);
        c.setCurrentCtc(null);
        c.setExpectedCtc(null);
        c.setNoticePeriodDays(null);
        c.setSkills(null);
        c.setVendorName(null);
        c.setReferredBy(null);
        c.setResumeStoredName(null);
        c.setResumeOriginalName(null);
        c.setResumeContentType(null);
        c.setResumeUploadedAt(null);
        c.setActive(false);
        c.setAnonymisedAt(LocalDateTime.now());
        candidateRepository.save(c);

        auditService.log("ANONYMISE", "Candidate", c.getId(), "Candidate data anonymised by " + actor.employee().getFullName());
        return mapper.toDTO(c, true);
    }

    // ============================================================ helpers

    private Candidate getOrThrow(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
    }

    private void requireActive(Candidate c) {
        if (!Boolean.TRUE.equals(c.getActive())) {
            throw new BusinessException("This candidate's data has been anonymised and can no longer be changed");
        }
    }

    private void applyFields(Candidate c, CandidateDTO dto, String email, String mobile) {
        CandidateSource source = dto.getSource() != null ? dto.getSource() : CandidateSource.OTHER;
        c.setFirstName(dto.getFirstName().trim());
        c.setLastName(blankToNull(dto.getLastName()));
        c.setEmail(email);
        c.setMobile(mobile);
        c.setCurrentCompany(blankToNull(dto.getCurrentCompany()));
        c.setCurrentDesignation(blankToNull(dto.getCurrentDesignation()));
        c.setCurrentLocation(blankToNull(dto.getCurrentLocation()));
        c.setTotalExperience(dto.getTotalExperience());
        c.setCurrentCtc(dto.getCurrentCtc());
        c.setExpectedCtc(dto.getExpectedCtc());
        c.setNoticePeriodDays(dto.getNoticePeriodDays());
        c.setSkills(blankToNull(dto.getSkills()));
        c.setSource(source);
        c.setVendorName(source == CandidateSource.AGENCY ? blankToNull(dto.getVendorName()) : null);

        Employee referrer = null;
        if (source == CandidateSource.REFERRAL && dto.getReferredById() != null) {
            referrer = employeeRepository.findById(dto.getReferredById())
                    .orElseThrow(() -> new ResourceNotFoundException("Referring employee not found: " + dto.getReferredById()));
        }
        c.setReferredBy(referrer);
    }

    private String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
