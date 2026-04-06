package com.kma.ojcore.dto.request.submissions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RejudgeSdi {

    // Danh sách submission ID cụ thể muốn rejudge
    List<UUID> submissionIds;

    // Hoặc rejudge toàn bộ submission của 1 bài toán
    UUID problemId;

    // Hoặc rejudge toàn bộ submission của 1 cuộc thi
    UUID contestId;
}
