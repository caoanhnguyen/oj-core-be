package com.kma.ojcore.controller.topics;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.topics.TopicBasicSdo;
import com.kma.ojcore.dto.response.topics.TopicDetailsStatisticsSdo;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.TopicService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/topics")
@AllArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public ApiResponse<?> searchTopics(@RequestParam(required = false) String name,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TopicBasicSdo> topics = topicService.userSearchTopics(name, pageable);

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Topics retrieved successfully")
                .data(topics)
                .build();
    }

    @GetMapping("{slug}/details")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getTopicDetailsWithStatistics(@PathVariable String slug,
                                                        @AuthenticationPrincipal UserPrincipal user) {
        UUID userId = user.getId();
        TopicDetailsStatisticsSdo detailsSdo = topicService.getDetailsWithStatisticsBySlug(slug, userId);

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Topic details with statistics retrieved successfully")
                .data(detailsSdo)
                .build();
    }
}
