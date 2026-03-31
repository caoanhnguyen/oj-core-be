package com.kma.ojcore.entity;

import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.RuleType;
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

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    String password;

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
