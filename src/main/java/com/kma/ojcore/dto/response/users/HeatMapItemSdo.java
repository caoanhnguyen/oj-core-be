package com.kma.ojcore.dto.response.users;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HeatMapItemSdo {

    LocalDateTime date;
    Integer count;
}
