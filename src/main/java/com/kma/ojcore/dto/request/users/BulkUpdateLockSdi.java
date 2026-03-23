package com.kma.ojcore.dto.request.users;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkUpdateLockSdi {
    // Danh sách ID của các user bị chọn trên giao diện
    List<UUID> userIds;

    // true = Mở khóa (Active), false = Khóa (Ban)
    Boolean accountNonLocked;
}