package com.enterprise.ems.repository;

import com.enterprise.ems.entity.ResignationAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResignationAssetRepository extends JpaRepository<ResignationAsset, Long> {

    List<ResignationAsset> findByResignationId(Long resignationId);

    long countByResignationIdAndReturnedFalse(Long resignationId);
}
