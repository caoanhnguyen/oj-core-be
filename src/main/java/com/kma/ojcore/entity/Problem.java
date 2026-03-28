package com.kma.ojcore.entity;

import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Problem extends BaseEntity {

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, unique = true, length = 255)
    String slug;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "constraints", columnDefinition = "TEXT")
    String constraints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProblemDifficulty difficulty;

    @Column(name = "problem_status", nullable = false)
    @Enumerated(EnumType.STRING)
    ProblemStatus problemStatus = ProblemStatus.DRAFT;

    // Giới hạn tài nguyên (Mặc định cho các ngôn ngữ)
    @Column(name = "time_limit_ms")
    Integer timeLimitMs;

    @Column(name = "memory_limit_mb")
    Integer memoryLimitMb;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    RuleType ruleType = RuleType.ACM;

    @Column(name = "testcase_dir", length = 255)
    String testcaseDir;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "problem_languages", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "language_key", length = 50)
    Set<String> allowedLanguages = new HashSet<>();

    // --- CÁC TRƯỜNG THỐNG KÊ & UI --- //
    @Column(name = "submission_count")
    Long submissionCount = 0L;

    @Column(name = "accepted_count")
    Long acceptedCount = 0L;

    @Column(length = 255)
    String source;

    @Column(columnDefinition = "TEXT")
    String hint;

    @Column(name = "total_score")
    Integer totalScore;

    @Column(name = "input_format", columnDefinition = "TEXT")
    String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    String outputFormat;

    // -- Relationships -- //

    // Problem - Template //
    @OneToMany(mappedBy = "problem", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    List<ProblemTemplate> templates;

    // Problem - User (author) //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User author;

    // Problem - Example //
    @OneToMany(mappedBy = "problem", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    List<ProblemExample> examples;

    // Problem - Image //
    @OneToMany(mappedBy = "problem", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    List<ProblemImage> images;

    // Problem - Topic //
    @ManyToMany
    @JoinTable(
            name = "problem_topics",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    Set<Topic> topics = new HashSet<>();
}
