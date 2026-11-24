package com.enterprise.ems.repository;

import com.enterprise.ems.entity.PrivilegeModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeModuleRepository extends JpaRepository<PrivilegeModule, Long> {

    List<PrivilegeModule> findByActiveTrueOrderByNameAsc();

    Optional<PrivilegeModule> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
