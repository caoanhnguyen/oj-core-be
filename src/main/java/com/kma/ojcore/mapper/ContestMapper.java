package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.ContestAdminSdo;
import com.kma.ojcore.dto.response.contests.ContestBasicSdo;
import com.kma.ojcore.dto.response.contests.ContestDetailSdo;
import com.kma.ojcore.entity.Contest;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ContestMapper {

    Contest toEntity(CreateContestSdi sdi);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorUsername")
    ContestAdminSdo toAdminSdo(Contest contest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromSdi(UpdateContestSdi sdi, @MappingTarget Contest contest);

    ContestBasicSdo toBasicSdo(Contest contest);

    ContestDetailSdo toDetailSdo(Contest contest);
}