package com.enterprise.ems.service.hiring;

import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/*
 * PURPOSE: Stores candidate resumes - SEPARATELY from FileStorageServiceImpl on purpose.
 *
 * WHY NOT REUSE FileStorageService?
 *  - It writes into app.upload.dir, which WebMvcConfig exposes at /uploads/** and
 *    SecurityConfig marks permitAll: anyone with the URL could download a resume.
 *  - It keeps whatever extension the client sent and does no content checks.
 *
 * THIS CLASS:
 *  - writes to app.hiring.storage-dir (default "hiring-files"), which is NOT served
 *    statically. Files are only reachable through CandidateApiController, which
 *    checks access first.
 *  - allows only pdf / doc / docx, verifies the real file signature (magic bytes),
 *    and never uses the client's file name on disk (random UUID name).
 */
@Component
public class HiringFileStorage {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    @Value("${app.hiring.storage-dir:hiring-files}")
    private String storageDir;

    public record StoredFile(String storedName, String originalName, String contentType) {}

    public StoredFile storeResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please choose a resume file to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("Resume must not be larger than 5 MB");
        }

        String originalName = sanitizeName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!CONTENT_TYPES.containsKey(extension)) {
            throw new BusinessException("Only PDF, DOC and DOCX resumes are allowed");
        }

        try {
            byte[] head = new byte[8];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.readNBytes(head, 0, head.length);
            }
            if (!signatureMatches(extension, head, read)) {
                throw new BusinessException("The file content does not match its type - upload a real PDF, DOC or DOCX file");
            }

            Path base = baseDir();
            Files.createDirectories(base);
            String storedName = UUID.randomUUID() + "." + extension;
            Path target = base.resolve(storedName).normalize();
            if (!target.startsWith(base)) {
                throw new BusinessException("Invalid file name");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(storedName, originalName, CONTENT_TYPES.get(extension));
        } catch (IOException e) {
            throw new BusinessException("Could not store the resume, please try again");
        }
    }

    public Resource load(String storedName) {
        try {
            Path base = baseDir();
            Path path = base.resolve(storedName).normalize();
            if (!path.startsWith(base) || !Files.isRegularFile(path)) {
                throw new ResourceNotFoundException("Resume file not found");
            }
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Resume file not found");
        }
    }

    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) return;
        try {
            Path base = baseDir();
            Path path = base.resolve(storedName).normalize();
            if (path.startsWith(base)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // best effort - the DB row is what matters for access
        }
    }

    private Path baseDir() {
        return Paths.get(storageDir).toAbsolutePath().normalize();
    }

    private String sanitizeName(String name) {
        String cleaned = name == null ? "resume" : name.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).trim();
        if (cleaned.isEmpty()) cleaned = "resume";
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean signatureMatches(String extension, byte[] head, int read) {
        if (read < 4) return false;
        return switch (extension) {
            case "pdf" -> head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            // .docx is a zip container: "PK\3\4"
            case "docx" -> head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4;
            // legacy .doc is an OLE2 compound file: D0 CF 11 E0
            case "doc" -> (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                    && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0;
            default -> false;
        };
    }
}
