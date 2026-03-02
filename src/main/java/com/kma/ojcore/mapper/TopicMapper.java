package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.topics.CreateTopicSdi;
import com.kma.ojcore.dto.request.topics.UpdateTopicSdi;
import com.kma.ojcore.dto.response.topics.TopicDetailsSdo;
import com.kma.ojcore.entity.Topic;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    Topic toEntity(CreateTopicSdi createTopicSdi);

    @Mapping(target = "topicId", source = "id")
    TopicDetailsSdo toDetailsSdo(Topic topic);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromSdi(@MappingTarget Topic topic, UpdateTopicSdi updateTopicSdi);
}
