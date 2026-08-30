package com.orgsite.service;

import com.orgsite.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    /** Saves an uploaded image to local disk under a per-org subfolder and returns the public URL path. */
    public String store(MultipartFile file, Long organizationId) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalName.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Only image files are allowed (jpg, png, gif, webp)");
        }

        try {
            Path orgDir = Paths.get(uploadDir, "org-" + organizationId);
            Files.createDirectories(orgDir);

            String filename = UUID.randomUUID() + "." + extension;
            Path target = orgDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/org-" + organizationId + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store uploaded file", e);
            throw new BadRequestException("Failed to save uploaded file");
        }
    }
}
