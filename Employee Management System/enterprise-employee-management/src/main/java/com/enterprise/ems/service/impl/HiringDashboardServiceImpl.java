package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.*;
import com.enterprise.ems.dto.HiringViews;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.repository.*;
import com.enterprise.ems.service.HiringDashboardService;
import com.enterprise.ems.service.hiring.HiringAccessService;
import com.enterprise.ems.service.hiring.HiringActor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/*
 * PURPOSE: Read-only hiring numbers: dashboard KPIs (hiring staff only), the signed-in
 * person's capabilities (sidebar / buttons), and the lookup lists the forms need.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HiringDashboardServiceImpl implements HiringDashboardService {

    private final JobRequisitionRepository requisitionRepository;
    private final JobApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final InterviewRepository interviewRepository;
    private final OfferRepository offerRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final LocationRepository locationRepository;
    private final EmployeeTypeRepository employeeTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeWorkflowRoleRepository employeeWorkflowRoleRepository;
    private final HiringAccessService access;

    @Override
    public HiringViews.Dashboard getDashboard(HiringActor actor) {
        access.requireStaff(actor);
        LocalDateTime now = LocalDateTime.now();

        List<RequisitionStatus> working = List.of(RequisitionStatus.OPEN, RequisitionStatus.ON_HOLD);

        Map<String, Long> pipeline = new LinkedHashMap<>();
        for (ApplicationStage s : ApplicationStage.values()) pipeline.put(s.name(), 0L);
        for (Object[] row : applicationRepository.countByStageForRequisitionStatuses(working)) {
            pipeline.put(((ApplicationStage) row[0]).name(), (Long) row[1]);
        }

        Map<String, Long> offers = new LinkedHashMap<>();
        for (OfferStatus s : OfferStatus.values()) offers.put(s.name(), 0L);
        for (Object[] row : offerRepository.countByStatus()) {
            offers.put(((OfferStatus) row[0]).name(), (Long) row[1]);
        }

        Map<String, Long> sources = new LinkedHashMap<>();
        for (CandidateSource s : CandidateSource.values()) sources.put(s.name(), 0L);
        for (Object[] row : candidateRepository.countBySource()) {
            sources.put(((CandidateSource) row[0]).name(), (Long) row[1]);
        }

        // Time-to-hire = application date -> date it reached HIRED, over the most recent hires.
        List<JobApplication> hires = applicationRepository.findTop500ByStageOrderByStageChangedAtDesc(ApplicationStage.HIRED);
        LocalDateTime ninetyDaysAgo = now.minusDays(90);
        long hiresLast90 = hires.stream().filter(a -> a.getStageChangedAt().isAfter(ninetyDaysAgo)).count();
        Double avgDays = hires.isEmpty() ? null : hires.stream()
                .mapToLong(a -> Math.max(0, ChronoUnit.DAYS.between(a.getAppliedOn(), a.getStageChangedAt().toLocalDate())))
                .average().orElse(0);

        return new HiringViews.Dashboard(
                requisitionRepository.countByStatus(RequisitionStatus.OPEN),
                requisitionRepository.sumUnfilledPositions(List.of(RequisitionStatus.OPEN)),
                requisitionRepository.countAwaitingApproval(),
                candidateRepository.countByActiveTrue(),
                interviewRepository.countByStatusAndScheduledAtBetween(InterviewStatus.SCHEDULED, now, now.plusDays(7)),
                hiresLast90,
                avgDays == null ? null : Math.round(avgDays * 10.0) / 10.0,
                pipeline, offers, sources);
    }

    @Override
    public HiringViews.Me me(HiringActor actor) {
        long pending = requisitionRepository.findPendingApprovalFor(actor.id()).size()
                + offerRepository.findPendingApprovalFor(actor.id()).size();
        LocalDateTime now = LocalDateTime.now();
        long upcoming = interviewRepository.findForInterviewer(actor.id()).stream()
                .filter(i -> i.getStatus() == InterviewStatus.SCHEDULED && !i.getScheduledAt().isBefore(now))
                .count();
        return new HiringViews.Me(actor.id(), actor.employee().getFullName(), actor.admin(), actor.recruiter(),
                actor.staff(), actor.canRaiseRequisition(), pending, upcoming);
    }

    @Override
    public HiringViews.Lookups lookups(HiringActor actor) {
        access.requireCanRaiseRequisition(actor);

        List<HiringViews.LookupItem> departments = departmentRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER))
                .map(d -> new HiringViews.LookupItem(d.getId(), d.getName())).toList();
        List<HiringViews.LookupItem> designations = designationRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .sorted(Comparator.comparing(Designation::getName, String.CASE_INSENSITIVE_ORDER))
                .map(d -> new HiringViews.LookupItem(d.getId(), d.getName())).toList();
        List<HiringViews.LookupItem> locations = locationRepository.findAll().stream()
                .filter(l -> Boolean.TRUE.equals(l.getActive()))
                .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
                .map(l -> new HiringViews.LookupItem(l.getId(), l.getName())).toList();
        List<HiringViews.LookupItem> employeeTypes = employeeTypeRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(EmployeeType::getName, String.CASE_INSENSITIVE_ORDER))
                .map(t -> new HiringViews.LookupItem(t.getId(), t.getName())).toList();

        // Recruiters (holders of the Recruiter role) plus the current user when they are an admin,
        // so a fresh install with nobody assigned yet can still assign someone.
        Map<Long, Employee> recruiters = new LinkedHashMap<>();
        for (EmployeeWorkflowRole ewr : employeeWorkflowRoleRepository
                .findByWorkflowRole_NameIgnoreCaseAndActiveTrueOrderByIdAsc(AppConstants.WORKFLOW_ROLE_RECRUITER)) {
            Employee e = ewr.getEmployee();
            if (Boolean.TRUE.equals(e.getActive())) recruiters.putIfAbsent(e.getId(), e);
        }
        if (actor.admin()) {
            // actor.employee() was loaded OUTSIDE this transaction (spring.jpa.open-in-view=false), so its lazy
            // department / designation cannot be read from it. Re-load the admin inside this transaction instead.
            Employee self = employeeRepository.findById(actor.id()).orElse(actor.employee());
            recruiters.putIfAbsent(self.getId(), self);
        }

        return new HiringViews.Lookups(departments, designations, locations, employeeTypes,
                recruiters.values().stream().map(this::toLookup).toList());
    }

    @Override
    public List<HiringViews.EmployeeLookup> searchEmployees(String keyword, HiringActor actor) {
        access.requireCanRaiseRequisition(actor);
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }
        return employeeRepository.searchEmployees(keyword.trim(), PageRequest.of(0, 20, Sort.by("firstName")))
                .getContent().stream().map(this::toLookup).toList();
    }

    private HiringViews.EmployeeLookup toLookup(Employee e) {
        return new HiringViews.EmployeeLookup(e.getId(), e.getEmployeeCode(), e.getFullName(),
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDesignation() != null ? e.getDesignation().getName() : null);
    }
}