package com.orgsite.repository;

import com.orgsite.entity.ContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {
    List<ContentBlock> findByOrganizationIdOrderBySortOrderAsc(Long organizationId);
    List<ContentBlock> findByOrganizationIdAndTypeAndVisibleTrueOrderBySortOrderAsc(Long organizationId, ContentBlock.BlockType type);
    List<ContentBlock> findByOrganizationIdAndVisibleTrueOrderBySortOrderAsc(Long organizationId);
}
