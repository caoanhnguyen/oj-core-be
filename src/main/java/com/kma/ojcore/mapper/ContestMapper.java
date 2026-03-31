package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.ContestAdminSdo;
import com.kma.ojcore.dto.response.contests.ContestBasicSdo;
import com.kma.ojcore.dto.response.contests.ContestDetailSdo;
import com.kma.ojcore.dto.response.contests.ContestParticipationSdo;
import com.kma.ojcore.entity.Contest;
import com.kma.ojcore.entity.ContestParticipation;
import com.kma.ojcore.enums.ContestStatus;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ContestMapper {

    Contest toEntity(CreateContestSdi sdi);

    @Mapping(target = "contestStatus", expression = "java(getRealTimeStatus(contest.getStartTime(), contest.getEndTime()))")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorUsername")
    ContestAdminSdo toAdminSdo(Contest contest);

    ContestParticipationSdo toParticipationSdo(ContestParticipation contestParticipation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromSdi(UpdateContestSdi sdi, @MappingTarget Contest contest);

    ContestBasicSdo toBasicSdo(Contest contest);

    @Mapping(target = "contestStatus", expression = "java(getRealTimeStatus(contest.getStartTime(), contest.getEndTime()))")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorUsername")
    ContestDetailSdo toDetailSdo(Contest contest);

    default ContestStatus getRealTimeStatus(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (startTime == null || endTime == null) return null;
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        if (now.isBefore(startTime)) {
            return com.kma.ojcore.enums.ContestStatus.UPCOMING;
        } else if (now.isAfter(endTime)) {
            return com.kma.ojcore.enums.ContestStatus.ENDED;
        }
        return com.kma.ojcore.enums.ContestStatus.ONGOING;
    }
}