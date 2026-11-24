package com.enterprise.ems.dto;

import lombok.*;

/*
 * PURPOSE: Body for POST /api/attendance/self/punch-in and /punch-out.
 * Deliberately carries nothing but an optional remark - no employeeId, no date,
 * no time. Who/when is always resolved server-side (session + server clock),
 * so there's nothing here for a client to spoof.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfPunchRequest {

    private String remarks;
}
