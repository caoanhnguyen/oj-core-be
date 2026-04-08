package com.kma.ojcore.controller;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.prefix}/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("")
    public ApiResponse<?> getRankingsByRuleType(@RequestParam RuleType ruleType,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.builder()
                .status(200)
                .message("Rankings retrieved successfully")
                .data(rankingService.getRanking(ruleType, pageable))
                .build();
    }
}
