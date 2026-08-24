package com.doctorapp.repository;

import com.doctorapp.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findByVerified(boolean verified);

    long countByVerified(boolean verified);

    @Query("""
        select d from Doctor d
        join d.specializations s
        where d.verified = true and d.active = true
        and (:specialization is null or lower(s.name) = lower(:specialization))
        """)
    List<Doctor> searchBySpecialization(String specialization);
}
