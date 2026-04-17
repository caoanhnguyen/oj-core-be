package com.kma.ojcore.dto.response.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsSdo {
    private Long totalProblems;
    private Long activeUsers;
    private Long activeContests;
    private Long totalSubmissions;

    private List<VerdictStat> verdictStats;
    private List<TrendStat> trendStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerdictStat {
        private String verdict;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendStat {
        private String date; // Using ISO string, e.g., "YYYY-MM-DD"
        private Long count;
    }
}
