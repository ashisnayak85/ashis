package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    boolean existsByName(String name);
}
