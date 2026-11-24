package com.enterprise.ems.dto;

import lombok.*;

/*
 * PURPOSE: Tells the frontend whether to show the camera step at all, and
 * whether the current employee still needs to enroll their face first.
 * This is the piece that lets the whole feature be switched off with a
 * single config value (attendance.face-verification.enabled) - when
 * "enabled" comes back false, the React attendance page never renders the
 * webcam UI and never calls the enroll/verify endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceStatusDTO {
    private boolean enabled;
    private boolean enrolled;
}
