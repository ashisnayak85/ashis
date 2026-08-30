package com.enterprise.ca.repository;

import com.enterprise.ca.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
