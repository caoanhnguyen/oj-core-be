package com.kma.ojcore.controller.topics;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.topics.TopicBasicSdo;
import com.kma.ojcore.service.TopicService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/topics")
@AllArgsConstructor
public class UserTopicController {

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
}
