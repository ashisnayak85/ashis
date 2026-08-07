package com.enterprise.ems.service.impl;

import com.enterprise.ems.entity.FileUpload;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.repository.FileUploadRepository;
import com.enterprise.ems.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/*
 * PURPOSE: File Upload Module (Phase 9)
 * Stores files on local filesystem and metadata in database
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    private final FileUploadRepository fileUploadRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public FileUpload storeFile(MultipartFile file, String entityType, Long entityId, String uploadedBy) {
        if (file.isEmpty()) {
            throw new BusinessException("Cannot upload empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String storedFilename = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileUpload fileUpload = FileUpload.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .filePath(targetLocation.toString())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .entityType(entityType)
                    .entityId(entityId)
                    .uploadedBy(uploadedBy)
                    .build();

            log.info("File stored: {} for {} {}", storedFilename, entityType, entityId);
            return fileUploadRepository.save(fileUpload);
        } catch (IOException ex) {
            log.error("File upload failed", ex);
            throw new BusinessException("Failed to store file: " + originalFilename);
        }
    }

    @Override
    public List<FileUpload> getFiles(String entityType, Long entityId) {
        return fileUploadRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
