package com.enterprise.ca.repository;

import com.enterprise.ca.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {
    boolean existsByGstinAndGstinIsNotNull(String gstin);
    boolean existsByPanAndPanIsNotNull(String pan);
    long countByActiveTrue();
}
