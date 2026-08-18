package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO {

    private Long id;

    // No @NotNull here on purpose: the /self endpoint deliberately omits this from
    // the request body (an employee can't choose to mark someone else) and it's
    // filled in server-side from the session instead. The /admin (mark) and
    // /biometric paths supply it directly. Enforced in AttendanceServiceImpl.
    private Long employeeId;

    private String employeeName;

    // Same reasoning as employeeId: /self always forces this to "today" server-side,
    // so the request body never carries it.
    private LocalDate attendanceDate;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    @NotBlank(message = "Status is required")
    private String status;

    // SELF / ADMIN / BIOMETRIC - who recorded this punch. Read-only from the
    // client's point of view: the server always sets it based on which endpoint
    // was called, never trusts a value sent in the request body.
    private String source;

    private String remarks;
}
