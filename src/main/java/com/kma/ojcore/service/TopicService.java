package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.topics.CreateTopicSdi;
import com.kma.ojcore.dto.request.topics.UpdateTopicSdi;
import com.kma.ojcore.dto.response.topics.TopicAdminSdo;
import com.kma.ojcore.dto.response.topics.TopicBasicSdo;
import com.kma.ojcore.dto.response.topics.TopicDetailsSdo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TopicService {

    Page<TopicBasicSdo> userSearchTopics(String name, Pageable pageable);

    // ADMIN

    Page<TopicBasicSdo> getAllActiveTopics(Pageable pageable);

    Page<TopicAdminSdo> searchTopics(String name, Pageable pageable);

    TopicDetailsSdo getTopicById(UUID topicId);

    TopicDetailsSdo createTopic(CreateTopicSdi createTopicSdi);

    TopicDetailsSdo updateTopic(UUID topicId, UpdateTopicSdi updateTopicSdi);

    void softDeleteTopic(UUID topicId);

    UUID restoreTopic(UUID topicId);
}
