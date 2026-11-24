package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.HiringViews;
import com.enterprise.ems.service.HiringDashboardService;
import com.enterprise.ems.service.hiring.HiringAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* PURPOSE: Hiring helper endpoints - "what can I do" (me), dashboard KPIs, form lookups, employee picker. */
@RestController
@RequestMapping("/api/hiring")
@RequiredArgsConstructor
public class HiringApiController {

    private final HiringDashboardService dashboardService;
    private final HiringAccessService access;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<HiringViews.Me>> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.me(access.actor(principal))));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<HiringViews.Dashboard>> dashboard(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(access.actor(principal))));
    }

    @GetMapping("/lookups")
    public ResponseEntity<ApiResponse<HiringViews.Lookups>> lookups(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.lookups(access.actor(principal))));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<HiringViews.EmployeeLookup>>> employees(
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.searchEmployees(keyword, access.actor(principal))));
    }
}
