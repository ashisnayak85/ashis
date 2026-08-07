package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;

/*
 * PURPOSE: Role entity - defines user permissions (ADMIN, MANAGER, USER)
 * TABLE: roles
 * RELATIONSHIP: Many-to-Many with User via user_roles join table
 *
 * JPA ANNOTATION: @Entity - marks this class as a JPA entity mapped to DB table
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // ROLE_ADMIN, ROLE_MANAGER, ROLE_USER

    @Column(name = "description", length = 255)
    private String description;
}
