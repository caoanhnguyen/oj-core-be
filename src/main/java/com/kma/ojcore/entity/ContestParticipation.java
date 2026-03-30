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

    @Column(name = "score")
    @Builder.Default
    Double score = 0.0;

    @Column(name = "penalty")
    @Builder.Default
    Long penalty = 0L; // Thời gian phạt (tính bằng phút)

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