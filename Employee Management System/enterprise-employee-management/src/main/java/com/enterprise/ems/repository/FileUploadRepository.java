package com.enterprise.ems.repository;

import com.enterprise.ems.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    List<FileUpload> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
