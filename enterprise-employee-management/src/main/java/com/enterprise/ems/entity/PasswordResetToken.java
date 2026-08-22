package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: One-time, time-limited token issued for the "forgot password" flow.
 * TABLE: password_reset_tokens
 *
 * WHY A SEPARATE TABLE INSTEAD OF A COLUMN ON User:
 * - A user can request a reset more than once before using a link; keeping a
 *   history (and marking old ones used/expired) is simpler as its own table.
 * - Token lookups (findByToken) are cheap with an index and don't touch the
 *   User row at all until the token is validated.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Random, unguessable, unique - this is what goes in the emailed link.
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    // Set true the moment the token is successfully used, so it can never be replayed.
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
