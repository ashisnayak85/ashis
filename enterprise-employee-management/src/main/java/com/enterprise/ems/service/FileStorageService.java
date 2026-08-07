package com.enterprise.ems.service;

import com.enterprise.ems.entity.FileUpload;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    FileUpload storeFile(MultipartFile file, String entityType, Long entityId, String uploadedBy);

    List<FileUpload> getFiles(String entityType, Long entityId);
}
