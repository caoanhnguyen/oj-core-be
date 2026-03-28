package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "contest_participations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestParticipation extends BaseEntity {

    @Column(nullable = false)
    Boolean isRegistered;

    @Column(name = "is_disqualified", nullable = false)
    @Builder.Default
    boolean isDisqualified = false; // Mặc định là trong sạch

    // -- Relationships -- //

    // ContestParticipation - Contest //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    Contest contest;

    // ContestParticipation - User //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}