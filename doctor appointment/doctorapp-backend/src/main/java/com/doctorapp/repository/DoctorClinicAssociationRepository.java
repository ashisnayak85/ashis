package com.doctorapp.repository;

import com.doctorapp.entity.DoctorClinicAssociation;
import com.doctorapp.entity.DoctorClinicAssociation.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorClinicAssociationRepository extends JpaRepository<DoctorClinicAssociation, Long> {

    Optional<DoctorClinicAssociation> findByDoctorIdAndClinicId(Long doctorId, Long clinicId);

    List<DoctorClinicAssociation> findByDoctorIdAndStatus(Long doctorId, Status status);

    List<DoctorClinicAssociation> findByDoctorId(Long doctorId);

    List<DoctorClinicAssociation> findByClinicIdAndStatus(Long clinicId, Status status);

    List<DoctorClinicAssociation> findByClinicIdInAndStatus(List<Long> clinicIds, Status status);

    List<DoctorClinicAssociation> findByClinicIdIn(List<Long> clinicIds);

    boolean existsByDoctorIdAndClinicIdAndStatus(Long doctorId, Long clinicId, Status status);

    long countByClinicIdAndStatus(Long clinicId, Status status);
}
