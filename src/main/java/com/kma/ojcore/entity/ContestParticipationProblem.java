package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
    name = "contest_participation_problems",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"participation_id", "contest_problem_id"})
    },
    indexes = {
        @Index(name = "idx_participation_problem", columnList = "participation_id, contest_problem_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestParticipationProblem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    ContestParticipation participation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_problem_id", nullable = false)
    ContestProblem contestProblem;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    Double maxScore = 0.0;

    @Column(name = "penalty", nullable = false)
    @Builder.Default
    Long penalty = 0L;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    Integer failedAttempts = 0;

    @Column(name = "is_ac", nullable = false)
    @Builder.Default
    Boolean isAc = false;
}
