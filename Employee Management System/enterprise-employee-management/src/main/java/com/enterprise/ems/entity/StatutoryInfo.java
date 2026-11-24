package com.enterprise.ems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/*
 * PURPOSE: Statutory / payroll-compliance details (PF and ESI) required for
 *          Indian payroll processing. Embedded into Employee - kept as its
 *          own class since these ~9 fields form one logical "statutory"
 *          section on the form and are only ever read/written together.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatutoryInfo {

    // --- Provident Fund ---
    @Column(name = "pf_applicable")
    private Boolean pfApplicable;

    @Column(name = "pf_number", length = 30)
    private String pfNumber;

    @Column(name = "uan_number", length = 20)
    private String uanNumber;

    // "Restrict PF" - caps employer PF contribution at the statutory wage
    // ceiling instead of the employee's actual full salary.
    @Column(name = "restrict_pf")
    private Boolean restrictPf;

    // Employee opted out of the Pension Scheme portion of PF.
    @Column(name = "zero_pension")
    private Boolean zeroPension;

    // Employee exempt from Professional Tax.
    @Column(name = "zero_pt")
    private Boolean zeroPt;

    // --- Employee State Insurance ---
    @Column(name = "esi_applicable")
    private Boolean esiApplicable;

    @Column(name = "esi_number", length = 30)
    private String esiNumber;

    // Employee covered under an exempted/dispensary-free ESI arrangement.
    @Column(name = "esi_dispensation")
    private Boolean esiDispensation;
}
