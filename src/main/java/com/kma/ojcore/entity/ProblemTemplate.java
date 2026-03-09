package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "problem_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"problem_id", "language_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemTemplate extends BaseEntity {

    @Column(name = "language_key", nullable = false)
    String languageKey;

    // Khung code hiển thị cho User thấy trên Editor (Đã bao gồm hàm main cấu hình sẵn I/O)
    // Dùng TEXT là quá dư dả (chứa được ~65.000 ký tự)
    @Column(name = "code_template", columnDefinition = "TEXT", nullable = false)
    String codeTemplate;

    // -- Relationships -- //

    // ProblemTemplate - Problem //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;
}