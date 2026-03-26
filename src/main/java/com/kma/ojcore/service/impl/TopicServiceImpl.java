package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.topics.CreateTopicSdi;
import com.kma.ojcore.dto.request.topics.UpdateTopicSdi;
import com.kma.ojcore.dto.response.topics.*;
import com.kma.ojcore.entity.Topic;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.mapper.TopicMapper;
import com.kma.ojcore.repository.TopicRepository;
import com.kma.ojcore.repository.UserProblemStatusRepository;
import com.kma.ojcore.service.TopicService;
import com.kma.ojcore.utils.EscapeHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;
    private final UserProblemStatusRepository userProblemStatusRepository;

    public TopicServiceImpl(TopicRepository topicRepository, TopicMapper topicMapper, UserProblemStatusRepository userProblemStatusRepository) {
        this.topicRepository = topicRepository;
        this.topicMapper = topicMapper;
        this.userProblemStatusRepository = userProblemStatusRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TopicBasicSdo> userSearchTopics(String name, Pageable pageable) {
        String searchName = EscapeHelper.escapeLike(name);
        return topicRepository.userSearchTopics(searchName, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public TopicDetailsStatisticsSdo getDetailsWithStatisticsBySlug(String slug, UUID userId) {
        Topic topic = topicRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));

        TopicDetailsStatisticsSdo detailsSdo = topicMapper.toDetailsStatisticsSdo(topic);

        List<DifficultyCountProjection> totalStats = topicRepository.countProblemsByDifficultyForTopic(slug);
        for(DifficultyCountProjection difficultyCount : totalStats) {
            switch (difficultyCount.getDifficulty()) {
                case EASY -> detailsSdo.setTotalEasy(difficultyCount.getCount());
                case MEDIUM -> detailsSdo.setTotalMedium(difficultyCount.getCount());
                case HARD -> detailsSdo.setTotalHard(difficultyCount.getCount());
            }
        }

        if(userId == null) {
            return detailsSdo;
        }

        List<DifficultyCountProjection> solvedStats = userProblemStatusRepository.countSolvedProblemsByDifficultyForTopic(userId, slug);
        long solvedTotal = 0;
        for (DifficultyCountProjection stat : solvedStats) {
            solvedTotal+= stat.getCount();
            switch (stat.getDifficulty()) {
                case EASY -> detailsSdo.setSolvedEasy(stat.getCount());
                case MEDIUM -> detailsSdo.setSolvedMedium(stat.getCount());
                case HARD -> detailsSdo.setSolvedHard(stat.getCount());
            }
        }
        detailsSdo.setSolvedTotal(solvedTotal);
        return detailsSdo;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TopicBasicSdo> getAllActiveTopics(Pageable pageable) {
        return topicRepository.allActiveTopics(pageable);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        return topicMapper.toDetailsSdo(topic);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public TopicDetailsSdo createTopic(CreateTopicSdi createTopicSdi) {
        if(topicRepository.existsByName(createTopicSdi.getName())) {
            throw new BusinessException(ErrorCode.TOPIC_ALREADY_EXISTS);
        }

        if(topicRepository.existsBySlug(createTopicSdi.getSlug())) {
            throw new BusinessException(ErrorCode.TOPIC_ALREADY_EXISTS);
        }

        Topic topic = topicMapper.toEntity(createTopicSdi);
        Topic savedTopic = topicRepository.save(topic);
        return topicMapper.toDetailsSdo(savedTopic);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public TopicDetailsSdo updateTopic(UUID topicId, UpdateTopicSdi updateTopicSdi) {
        Topic existingTopic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));

        if(topicRepository.existsByName(updateTopicSdi.getName()) && !existingTopic.getName().equals(updateTopicSdi.getName())) {
            throw new BusinessException(ErrorCode.TOPIC_ALREADY_EXISTS);
        }

        if(topicRepository.existsBySlug(updateTopicSdi.getSlug()) && !existingTopic.getSlug().equals(updateTopicSdi.getSlug())) {
            throw new BusinessException(ErrorCode.TOPIC_ALREADY_EXISTS);
        }

        topicMapper.updateEntityFromSdi(existingTopic, updateTopicSdi);
        Topic updatedTopic = topicRepository.save(existingTopic);
        return topicMapper.toDetailsSdo(updatedTopic);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void softDeleteTopic(UUID topicId) {
        if(!topicRepository.existsById(topicId)) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }
        topicRepository.updateStatusById(EStatus.DELETED, topicId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public UUID restoreTopic(UUID topicId) {
        if(!topicRepository.existsById(topicId)) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }
        topicRepository.updateStatusById(EStatus.ACTIVE, topicId);
        return topicId;
    }
}