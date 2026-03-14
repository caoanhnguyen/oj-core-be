package com.kma.ojcore.controller.topics;

import com.kma.ojcore.dto.request.topics.CreateTopicSdi;
import com.kma.ojcore.dto.request.topics.UpdateTopicSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.topics.TopicAdminSdo;
import com.kma.ojcore.dto.response.topics.TopicBasicSdo;
import com.kma.ojcore.dto.response.topics.TopicDetailsSdo;
import com.kma.ojcore.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/admin/topics")
@RequiredArgsConstructor
@Validated
public class AdminTopicController {

    private final TopicService topicService;

    @GetMapping("/active")
    public ApiResponse<?> searchActiveTopics(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "100") int size,
                                            @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TopicBasicSdo> topics = topicService.getAllActiveTopics(pageable);

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Active topics retrieved successfully")
                .data(topics)
                .build();
    }

    @GetMapping
    public ApiResponse<?> searchTopics(@RequestParam(required = false) String name,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TopicAdminSdo> topics = topicService.searchTopics(name, pageable);

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Topics retrieved successfully")
                .data(topics)
                .build();
    }

    @GetMapping("/{topicId}")
    public ApiResponse<?> getTopicById(@PathVariable UUID topicId) {
        TopicDetailsSdo topicDetails = topicService.getTopicById(topicId);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Topic details retrieved successfully")
                .data(topicDetails)
                .build();
    }

    @PostMapping
    public ApiResponse<?> createTopic(@Valid @RequestBody CreateTopicSdi createTopicSdi) {
        TopicDetailsSdo createdTopic = topicService.createTopic(createTopicSdi);
        return ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Topic created successfully")
                .data(createdTopic)
                .build();
    }

    @PutMapping("/{topicId}")
    public ApiResponse<?> updateTopic(@PathVariable UUID topicId,
                                      @Valid @RequestBody UpdateTopicSdi updateTopicSdi) {
        TopicDetailsSdo sdo = topicService.updateTopic(topicId, updateTopicSdi);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Topic updated successfully")
                .data(sdo)
                .build();
    }

    @DeleteMapping("/{topicId}")
    public ApiResponse<?> softDeleteTopic(@PathVariable UUID topicId) {
        topicService.softDeleteTopic(topicId);
        return ApiResponse.builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message("Topic deleted successfully")
                .data(null)
                .build();
    }

    @PatchMapping("/{topicId}/restore")
    public ApiResponse<?> restoreTopic(@PathVariable UUID topicId) {
        UUID id = topicService.restoreTopic(topicId);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Topic restored successfully")
                .data(id)
                .build();
    }
}
