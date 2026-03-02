package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entity để lưu các ví dụ (examples) của Problem
 * Tách biệt khỏi TestCase vì mục đích khác nhau:
 * - ProblemExample: Hiển thị cho user hiểu đề bài (luôn public, 2-3 examples)
 * - TestCase: Dùng cho chấm điểm (có thể ẩn, nhiều testcases)
 */
@Entity
@Table(name = "problem_examples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemExample extends BaseEntity {

    /**
     * Input data của example
     * VD: "nums = [2,7,11,15], target = 9"
     */
    @Column(name = "input_data", columnDefinition = "TEXT", nullable = false)
    String inputData;

    /**
     * Expected output của example
     * VD: "[0,1]"
     */
    @Column(name = "output_data", columnDefinition = "TEXT", nullable = false)
    String outputData;

    /**
     * Giải thích cho example (HTML content)
     * Có thể chứa ảnh minh họa
     * VD: "
     * <p>
     * Because nums[0] + nums[1] == 9, we return [0, 1].
     * </p>
     * "
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    String explanation;

    /**
     * Thứ tự hiển thị (Example 1, 2, 3...)
     */
    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    // -- Relationships -- //

    /**
     * Problem mà example này thuộc về
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;
}
