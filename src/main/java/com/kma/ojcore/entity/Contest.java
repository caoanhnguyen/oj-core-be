package com.kma.ojcore.entity;

import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.ContestFormat;
import com.kma.ojcore.enums.ContestResourceVisibility;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.enums.ScoreboardVisibility;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Contest extends BaseEntity {

    @Column(nullable = false)
    String title;

    @Column(name = "contest_key", nullable = false, unique = true, length = 100)
    String contestKey; // Unique identifier for the contest, used in URLs and API calls

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false)
    LocalDateTime startTime;

    @Column(nullable = false)
    LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ContestVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    ContestFormat format = ContestFormat.STRICT;

    @Column(name = "duration_minutes")
    Integer durationMinutes;

    @Column(name = "allow_late_registration")
    @Builder.Default
    Boolean allowLateRegistration = false;

    String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoreboard_visibility", nullable = false)
    @Builder.Default
    ScoreboardVisibility scoreboardVisibility = ScoreboardVisibility.VISIBLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_visibility", nullable = false)
    @Builder.Default
    ContestResourceVisibility resourceVisibility = ContestResourceVisibility.ALWAYS_VISIBLE;

    // -- Relationships -- //

    // Contest - User (author) //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    User author;

    // Contest - Problem //
    @OneToMany(mappedBy = "contest", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    List<ContestProblem> problems;

    // Contest - ContestParticipation //
    @OneToMany(mappedBy = "contest", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    List<ContestParticipation> participations;
}
