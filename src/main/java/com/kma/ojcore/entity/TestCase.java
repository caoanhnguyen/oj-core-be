package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestCase extends BaseEntity{

    @Column(name = "input_data", columnDefinition = "TEXT")
    String inputData; // Nếu ngắn (<64KB) lưu thẳng vào đây

    @Column(name = "input_url")
    String inputUrl; // Nếu dài, lưu URL MinIO

    @Column(name = "output_data", columnDefinition = "TEXT")
    String outputData;

    @Column(name = "output_url")
    String outputUrl;

    @Column(name = "is_sample")
    boolean isSample = false; // Testcase mẫu để hiển thị

    @Column(name = "is_hidden")
    boolean isHidden = false; // Testcase ẩn để chấm điểm

    Integer orderIndex; // Thứ tự chạy

    @Column(name = "illustration_url")
    String illustrationUrl; // URL hình minh họa (MinIO) nếu có

    // -- Relationships -- //

    // TestCase - Problem //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;
}
