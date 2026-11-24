package com.onehealth.repository;

import com.onehealth.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);

    /**
     * Dashboard feed: every BILLED line for the org within a date range (all
     * branches). Filters on the item's own createdAt (i.e. when it was
     * billed/paid), not the prescription's createdAt (when the doctor wrote
     * it) - a prescription written one day and billed the next should count
     * as that later day's revenue.
     */
    @Query("select pi from PrescriptionItem pi " +
           "join fetch pi.prescription p " +
           "join fetch pi.medicine m " +
           "where p.organizationId = :organizationId " +
           "and function('date', pi.createdAt) between :from and :to")
    List<PrescriptionItem> findForOrganizationInRange(
            @Param("organizationId") Long organizationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Same, scoped to one branch - used by a clinic admin's own dashboard view. */
    @Query("select pi from PrescriptionItem pi " +
           "join fetch pi.prescription p " +
           "join fetch pi.medicine m " +
           "where p.organizationId = :organizationId and p.clinicId = :clinicId " +
           "and function('date', pi.createdAt) between :from and :to")
    List<PrescriptionItem> findForClinicInRange(
            @Param("organizationId") Long organizationId,
            @Param("clinicId") Long clinicId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
