package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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

    @Column(name = "start_time")
    LocalDateTime startTime; // Lúc user bấm nút "Start"

    @Column(name = "end_time")
    LocalDateTime endTime; // Lúc hết giờ của riêng user này

    @Column(name = "is_finished")
    @Builder.Default
    boolean isFinished = false; // Bằng true khi nộp bài sớm hoặc bị hệ thống kick ra

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