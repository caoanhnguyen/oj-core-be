package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.topics.CreateTopicSdi;
import com.kma.ojcore.dto.request.topics.UpdateTopicSdi;
import com.kma.ojcore.dto.response.topics.TopicAdminSdo;
import com.kma.ojcore.dto.response.topics.TopicBasicSdo;
import com.kma.ojcore.dto.response.topics.TopicDetailsSdo;
import com.kma.ojcore.entity.Topic;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.mapper.TopicMapper;
import com.kma.ojcore.repository.TopicRepository;
import com.kma.ojcore.service.TopicService;
import com.kma.ojcore.utils.EscapeHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;

    public TopicServiceImpl(TopicRepository topicRepository, TopicMapper topicMapper) {
        this.topicRepository = topicRepository;
        this.topicMapper = topicMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TopicBasicSdo> userSearchTopics(String name, Pageable pageable) {
        String searchName = EscapeHelper.escapeLike(name);
        return topicRepository.userSearchTopics(searchName, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TopicAdminSdo> searchTopics(String name, Pageable pageable) {
        String searchName = EscapeHelper.escapeLike(name);
        return topicRepository.searchTopics(searchName, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public TopicDetailsSdo getTopicById(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
        return topicMapper.toDetailsSdo(topic);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public TopicDetailsSdo createTopic(CreateTopicSdi createTopicSdi) {
        // Validate name and slug uniqueness
        if(topicRepository.existsByName(createTopicSdi.getName())) {
            throw new IllegalArgumentException("Topic name must be unique");
        }

        if(topicRepository.existsBySlug(createTopicSdi.getSlug())) {
            throw new IllegalArgumentException("Topic slug must be unique");
        }

        Topic topic = topicMapper.toEntity(createTopicSdi);
        Topic savedTopic = topicRepository.save(topic);
        return topicMapper.toDetailsSdo(savedTopic);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public TopicDetailsSdo updateTopic(UUID topicId, UpdateTopicSdi updateTopicSdi) {
        // Check if topic exists
        Topic existingTopic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found"));

        // Validate name and slug uniqueness
        if(topicRepository.existsByName(updateTopicSdi.getName()) && !existingTopic.getName().equals(updateTopicSdi.getName())) {
            throw new IllegalArgumentException("Topic name must be unique");
        }

        if(topicRepository.existsBySlug(updateTopicSdi.getSlug()) && !existingTopic.getSlug().equals(updateTopicSdi.getSlug())) {
            throw new IllegalArgumentException("Topic slug must be unique");
        }

        topicMapper.updateEntityFromSdi(existingTopic, updateTopicSdi);
        Topic updatedTopic = topicRepository.save(existingTopic);
        return topicMapper.toDetailsSdo(updatedTopic);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void softDeleteTopic(UUID topicId) {
        // Check if topic exists
        if(!topicRepository.existsById(topicId)) {
            throw new IllegalArgumentException("Topic not found");
        }
        // Soft delete topic
        topicRepository.updateStatusById(EStatus.DELETED, topicId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public UUID restoreTopic(UUID topicId) {
        // Check if topic exists
        if(!topicRepository.existsById(topicId)) {
            throw new IllegalArgumentException("Topic not found");
        }
        // Restore topic
        topicRepository.updateStatusById(EStatus.ACTIVE, topicId);
        return topicId;
    }
}
