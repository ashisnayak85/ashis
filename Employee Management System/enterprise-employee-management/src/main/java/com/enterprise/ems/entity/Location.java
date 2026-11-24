package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * PURPOSE: Location entity - physical work sites (offices, branches)
 * TABLE: location
 * WHY EXISTS: An organization may operate from multiple sites; every employee
 * works out of exactly one. Kept as an INDEPENDENT axis from Department -
 * "IT" is the same department whether the employee sits in Bangalore or Pune.
 * Department and Location are two separate ManyToOne relationships on
 * Employee, never nested inside each other.
 *
 * FUTURE EXTENSION POINT: `timezone` is captured now (even though nothing
 * uses it yet) because it is expensive to retrofit later - any future
 * per-location holiday calendar, working-hours, or attendance-timestamp
 * feature will need it, and adding it after data already exists means a
 * painful backfill. A future HolidayCalendar/WorkingHours entity should
 * attach via a `location_id` FK pointing here, the same way Employee does -
 * do not embed calendar/schedule fields directly on this entity.
 *
 * RELATIONSHIP: OneToMany with Employee
 */
@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "pincode", length = 10)
    private String pincode;

    // Required - an office needs a reachable contact number. Kept as free-form
    // text (not a number type) since numbers can have a leading '+' and vary
    // in length across countries - the same reasoning as pincode.
    @Column(name = "office_contact", nullable = false, length = 20)
    private String officeContact;

    // Captured now for future holiday-calendar/working-hours features - see class note above.
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
