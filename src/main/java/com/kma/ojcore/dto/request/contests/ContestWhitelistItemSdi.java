package com.kma.ojcore.dto.request.contests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ContestWhitelistItemSdi {
    String email;
    String fullName;
    String phoneNumber;
    String note;
}
