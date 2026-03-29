package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.contests.ContestParticipantPublicSdo;
import com.kma.ojcore.dto.response.contests.ContestParticipationSdo;
import com.kma.ojcore.entity.ContestParticipation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, UUID> {

    boolean existsByContestIdAndUserId(UUID contestId, UUID userId);

    long countByContestId(UUID contestId);

    Optional<ContestParticipation> findByContestIdAndUserId(UUID contestId, UUID userId);

    @Modifying
    @Query("UPDATE ContestParticipation cp SET cp.isDisqualified = true WHERE cp.contest.id = :contestId AND cp.user.id IN :userIds")
    int banUsersInBulk(@Param("contestId") UUID contestId,
                       @Param("userIds") List<UUID> userIds);

    @Modifying
    @Query("UPDATE ContestParticipation cp SET cp.isDisqualified = false WHERE cp.contest.id = :contestId AND cp.user.id IN :userIds")
    int unbanUsersInBulk(@Param("contestId") UUID contestId, @Param("userIds") List<UUID> userIds);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestParticipationSdo(" +
            "cp.user.id, cp.user.username, cp.user.email, cp.isDisqualified) " +
            "FROM ContestParticipation cp " +
            "WHERE cp.contest.id = :contestId " +
            "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
            "     OR LOWER(cp.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
            "AND (:isDisqualified IS NULL OR cp.isDisqualified = :isDisqualified)",
            countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp " +
                    "WHERE cp.contest.id = :contestId " +
                    "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
                    "     OR LOWER(cp.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
                    "AND (:isDisqualified IS NULL OR cp.isDisqualified = :isDisqualified)")
    Page<ContestParticipationSdo> searchParticipants(@Param("contestId") UUID contestId,
                                                     @Param("keyword") String keyword,
                                                     @Param("isDisqualified") Boolean isDisqualified,
                                                     Pageable pageable);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestParticipantPublicSdo(" +
            "cp.user.id, cp.user.username) " +
            "FROM ContestParticipation cp " +
            "WHERE cp.contest.id = :contestId " +
            "AND cp.isDisqualified = false " +
            "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')",
            countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp " +
                    "WHERE cp.contest.id = :contestId " +
                    "AND cp.isDisqualified = false " +
                    "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')")
    Page<ContestParticipantPublicSdo> searchPublicParticipants(@Param("contestId") UUID contestId,
                                                               @Param("keyword") String keyword,
                                                               Pageable pageable);
}