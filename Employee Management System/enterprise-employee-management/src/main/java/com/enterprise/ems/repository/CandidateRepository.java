package com.enterprise.ems.repository;

import com.enterprise.ems.constant.CandidateSource;
import com.enterprise.ems.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Candidate> findByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);

    Optional<Candidate> findFirstByMobile(String mobile);

    // Active (non-anonymised) candidates only. Empty keyword matches all; null source = any source.
    @Query("SELECT c FROM Candidate c WHERE c.active = true AND (:source IS NULL OR c.source = :source) AND " +
           "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(c.lastName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(c.mobile, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(c.skills, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(c.currentCompany, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Candidate> search(@Param("keyword") String keyword,
                           @Param("source") CandidateSource source,
                           Pageable pageable);

    long countByActiveTrue();

    @Query("SELECT c.source, COUNT(c) FROM Candidate c WHERE c.active = true GROUP BY c.source")
    List<Object[]> countBySource();
}
