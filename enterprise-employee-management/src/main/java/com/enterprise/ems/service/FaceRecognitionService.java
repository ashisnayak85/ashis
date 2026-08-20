package com.enterprise.ems.service;

import com.enterprise.ems.dto.FaceEnrollmentHistoryDTO;
import com.enterprise.ems.dto.FaceVerifyResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
 * PURPOSE: Stops buddy-punching (Employee A's login being used by someone
 * else to mark Employee A present) by matching a live photo taken at
 * punch-in/out time against a reference photo captured once at enrollment.
 *
 * THE FEATURE FLAG: everything here is gated by isEnabled(), backed by
 * attendance.face-verification.enabled in application.properties. When that
 * is false, callers (AttendanceApiController) skip this service entirely -
 * no model is ever loaded, no camera is ever required, punch-in/out behaves
 * exactly as it did before this feature existed. Flip the property back to
 * true to turn it on again - no code changes either way.
 */
public interface FaceRecognitionService {

    // Whether the feature is switched on at all, per application.properties.
    boolean isEnabled();

    // Whether this employee has completed the one-time enrollment capture.
    boolean hasEnrollment(Long employeeId);

    // Captures (or re-captures) this employee's reference face from a photo.
    // If an enrollment already exists for this employee, the PREVIOUS
    // embedding + photo are backed up to FaceEnrollmentHistory before being
    // overwritten - nothing is silently lost on a re-enroll. actorUsername
    // is who triggered this (their own username for self-enroll, or the
    // admin's username for an admin-triggered re-enroll) - stored on the
    // backup record for audit. Throws if the feature is disabled or no face
    // is clearly detected.
    void enroll(Long employeeId, MultipartFile image, String actorUsername);

    // Compares a freshly captured photo against the employee's enrolled
    // reference face. Returns true only on a confident match. Throws if the
    // employee has no enrollment yet, or if no face is detected in the photo.
    boolean verify(Long employeeId, MultipartFile image);

    // Convenience used by the punch-in/punch-out endpoints: does nothing and
    // returns false when the feature is disabled (so a normal punch proceeds
    // unmodified); when enabled, requires a photo, requires prior enrollment,
    // and throws a clear BusinessException on a failed match rather than
    // silently letting the punch through.
    boolean verifyIfRequired(Long employeeId, MultipartFile image);

    // Admin diagnostic: runs the same comparison as verify(), but returns the
    // actual similarity score and threshold instead of just throwing on a
    // mismatch - lets an admin SEE why a punch-in is being rejected (a
    // borderline near-miss vs. a wildly-off result mean very different
    // things) without needing to read server logs. Never touches attendance.
    FaceVerifyResultDTO testVerify(Long employeeId, MultipartFile image);

    // Backup history for this employee's enrollment, most recent first.
    List<FaceEnrollmentHistoryDTO> getHistory(Long employeeId);
}
