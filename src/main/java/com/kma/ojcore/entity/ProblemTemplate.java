package com.kma.ojcore.entity;

import com.kma.ojcore.enums.ProgrammingLanguage;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "problem_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    ProgrammingLanguage language;

    // Khung code hiển thị cho User thấy trên Editor
    @Column(name = "code_template", columnDefinition = "TEXT", nullable = false)
    String codeTemplate;

    // -- Relationships -- //

    // ProblemTemplate - Problem //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;
}