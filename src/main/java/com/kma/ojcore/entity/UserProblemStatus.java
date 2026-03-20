package com.kma.ojcore.entity;

import com.kma.ojcore.enums.UserProblemState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_problem_status", indexes = {
        @Index(name = "idx_user_problem", columnList = "user_id, problem_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProblemStatus extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    UserProblemState state;

    @Column(name = "max_score")
    Double maxScore; // Điểm cao nhất đạt được (dùng cho OI, có thể null nếu chưa nộp hoặc chỉ nộp ACM)

    // -- Relationships -- //

    // User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    // Problem
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;
}