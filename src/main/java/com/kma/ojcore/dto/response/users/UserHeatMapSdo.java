package com.kma.ojcore.dto.response.users;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserHeatMapSdo {
    int totalSubmissions;
    List<HeatMapItemSdo> heatmapItems;
}
