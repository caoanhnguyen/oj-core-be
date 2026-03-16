package com.kma.ojcore.dto.response.topics;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicDetailsStatisticsSdo {
    UUID topicId;
    String name;
    String slug;
    String description;
    String status;
    LocalDateTime updatedDate;
    LocalDateTime createdDate;
    String createdBy;
    String updatedBy;

    // Thống kê tổng thể của Topic
    long totalEasy;
    long totalMedium;
    long totalHard;

    // Thống kê cá nhân của User (Nếu là guest thì toàn bộ là 0)
    long solvedTotal;
    long solvedEasy;
    long solvedMedium;
    long solvedHard;
}
