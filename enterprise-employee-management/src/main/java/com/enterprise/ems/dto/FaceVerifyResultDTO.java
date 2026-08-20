package com.enterprise.ems.dto;

import lombok.*;

/*
 * PURPOSE: Lets an admin "observe" a face match instead of only seeing a
 * pass/fail. Unlike the real punch-in/out flow (which only ever needs a
 * boolean), this returns the actual cosine-similarity number alongside the
 * configured threshold - exactly what's needed to tell "this employee is a
 * borderline near-miss, the threshold may be too strict" apart from
 * "this is nowhere close, something is actually broken" without digging
 * through server logs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceVerifyResultDTO {
    private boolean enrolled;
    private boolean matched;
    private Double similarity;   // null when not enrolled - nothing to compare against
    private double threshold;
    private String message;
}
