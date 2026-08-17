package com.enterprise.ems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/*
 * PURPOSE: Reusable address block, embedded twice into Employee
 *          (once for presentAddress, once for permanentAddress) via
 *          @AttributeOverrides - so both share this one definition
 *          instead of duplicating four fields twice.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Column(length = 255)
    private String addressLine;

    @Column(length = 100)
    private String cityOrDistrict;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;
}
