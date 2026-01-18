package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken extends BaseEntity {

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    String token;

    @Column(name = "expiry_date", nullable = false)
    Instant expiryDate;

    // Đánh dấu token này đã bị revoke/thu hồi chưa
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    Boolean revoked = false;

    // -- Relationships -- //

    // User - RefreshToken //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}