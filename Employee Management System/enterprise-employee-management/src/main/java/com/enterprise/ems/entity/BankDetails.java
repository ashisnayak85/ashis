package com.enterprise.ems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/*
 * PURPOSE: Bank account details for salary disbursement, embedded into Employee.
 *          Kept as a separate class (rather than flat fields on Employee) so it
 *          reads as one logical unit and can be reused if payroll ever needs
 *          bank details on another entity (e.g. Vendor) later.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDetails {

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 30)
    private String accountNumber;

    @Column(name = "bank_ifsc_code", length = 11)
    private String ifscCode;
}
