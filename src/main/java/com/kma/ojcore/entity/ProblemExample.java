package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "problem_examples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemExample extends BaseEntity {

    // Đầu vào thô dùng để vứt cho máy chấm (VD: "3 9 \n 2 7 11")
    @Column(name = "raw_input", columnDefinition = "TEXT", nullable = false)
    String rawInput;

    // Đầu ra thô để so sánh kết quả (VD: "0 1")
    @Column(name = "raw_output", columnDefinition = "TEXT", nullable = false)
    String rawOutput;

    // Giải thích chi tiết bằng HTML Editor
    @Column(columnDefinition = "LONGTEXT")
    String explanation;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    // -- Relationships -- //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;
}