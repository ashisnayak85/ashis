package com.enterprise.ca.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: A business or individual whose books/compliance the firm manages.
 * Every ledger entry, invoice and (optionally) compliance task hangs off a
 * Client - equivalent of "Employee" in a staff-management system, but here
 * it's the firm's own customer.
 */
@Entity
@Table(name = "client")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 30)
    private ClientType clientType;

    // GSTIN is optional - not every client is GST-registered (e.g. small
    // proprietorships under the threshold), so this is intentionally nullable.
    @Column(length = 15)
    private String gstin;

    @Column(length = 10)
    private String pan;

    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ClientType {
        INDIVIDUAL, PROPRIETORSHIP, PARTNERSHIP, LLP, PRIVATE_LIMITED, PUBLIC_LIMITED, TRUST
    }
}
