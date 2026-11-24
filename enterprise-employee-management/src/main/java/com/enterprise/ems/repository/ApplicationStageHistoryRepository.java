package com.enterprise.ems.repository;

import com.enterprise.ems.entity.ApplicationStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, Long> {

    List<ApplicationStageHistory> findByApplicationIdOrderByChangedAtAscIdAsc(Long applicationId);
}
