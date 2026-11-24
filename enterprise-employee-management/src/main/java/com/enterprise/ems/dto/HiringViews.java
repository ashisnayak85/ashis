package com.enterprise.ems.dto;

import java.util.List;
import java.util.Map;

/* PURPOSE: Read-only response records for the hiring lookups, "me" and dashboard endpoints. */
public final class HiringViews {

    private HiringViews() {}

    public record LookupItem(Long id, String name) {}

    public record EmployeeLookup(Long id, String employeeCode, String fullName, String departmentName, String designationName) {}

    // Everything the hiring forms need in ONE call, so the screens don't depend on the
    // permissions of the separate department / designation / location endpoints.
    public record Lookups(List<LookupItem> departments,
                          List<LookupItem> designations,
                          List<LookupItem> locations,
                          List<LookupItem> employeeTypes,
                          List<EmployeeLookup> recruiters) {}

    // What the signed-in person can do in hiring - drives the sidebar and buttons.
    public record Me(Long employeeId,
                     String employeeName,
                     boolean admin,
                     boolean recruiter,
                     boolean staff,
                     boolean canRaiseRequisition,
                     long pendingApprovals,
                     long upcomingInterviews) {}

    public record Dashboard(long openRequisitions,
                            long unfilledPositions,
                            long requisitionsAwaitingApproval,
                            long activeCandidates,
                            long interviewsNext7Days,
                            long hiresLast90Days,
                            Double averageTimeToHireDays,
                            Map<String, Long> pipelineByStage,
                            Map<String, Long> offersByStatus,
                            Map<String, Long> candidatesBySource) {}
}
