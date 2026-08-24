package com.doctorapp.repository;

import com.doctorapp.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicRepository extends JpaRepository<Clinic, Long>, ClinicRepositoryCustom {
    List<Clinic> findByClinicAdminId(Long clinicAdminId);
    List<Clinic> findByVerified(boolean verified);
    List<Clinic> findByIdIn(List<Long> ids);
    long countByVerified(boolean verified);
    boolean existsByClinicName(String clinicName);
}
