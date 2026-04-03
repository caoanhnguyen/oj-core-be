package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.contests.ContestBasicSdo;
import com.kma.ojcore.entity.Contest;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestRepository extends JpaRepository<Contest, UUID> {

    @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestBasicSdo(" +
            "c.id, c.title, c.startTime, c.endTime, c.ruleType, " +
            "null, " +
            "c.visibility, " +
            "(SELECT COUNT(p) FROM ContestParticipation p WHERE p.contest.id = c.id), " +
            "c.status, c.durationMinutes, c.format, c.allowLateRegistration) " +
            "FROM Contest c " +
            "WHERE (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
            "AND (:ruleType IS NULL OR c.ruleType = :ruleType) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:visibility IS NULL OR c.visibility = :visibility) " +
            "AND (:contestStatus IS NULL OR " +
            "  (:contestStatus = 'UPCOMING' AND c.startTime > CURRENT_TIMESTAMP) OR " +
            "  (:contestStatus = 'ONGOING' AND c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP) OR " +
            "  (:contestStatus = 'ENDED' AND c.endTime <= CURRENT_TIMESTAMP)) " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP THEN 1 " +
            "    WHEN c.startTime > CURRENT_TIMESTAMP THEN 2 " +
            "    ELSE 3 " +
            "  END ASC, " +
            "  c.startTime DESC",
            countQuery = "SELECT COUNT(c) FROM Contest c WHERE " +
                    "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
                    "AND (:ruleType IS NULL OR c.ruleType = :ruleType) " +
                    "AND (:status IS NULL OR c.status = :status) " +
                    "AND (:visibility IS NULL OR c.visibility = :visibility) " +
                    "AND (:contestStatus IS NULL OR " +
                    "  (:contestStatus = 'UPCOMING' AND c.startTime > CURRENT_TIMESTAMP) OR " +
                    "  (:contestStatus = 'ONGOING' AND c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP) OR " +
                    "  (:contestStatus = 'ENDED' AND c.endTime <= CURRENT_TIMESTAMP))")
    Page<ContestBasicSdo> searchAdminContests(@Param("keyword") String keyword,
                                              @Param("ruleType") RuleType ruleType,
                                              @Param("contestStatus") String contestStatusStr,
                                              @Param("visibility") ContestVisibility visibility,
                                              @Param("status") EStatus status,
                                              Pageable pageable);

    @Query("SELECT c FROM Contest c LEFT JOIN FETCH c.author WHERE c.id = :id " +
            "AND (:status IS NULL OR c.status = :status)")
    Optional<Contest> findContestWithAuthorByIdAndStatus(@Param("id") UUID id,
                                                         @Param("status") EStatus status);

    boolean existsByIdAndStatus(UUID id, EStatus status);

    @Query("SELECT c FROM Contest c LEFT JOIN FETCH c.problems " +
            "WHERE c.id = :contestId " +
            "AND (:status IS NULL OR c.status = :status)")
    Optional<Contest> findByIdAndStatus(@Param("contestId") UUID contestId,
                                        @Param("status") EStatus status);
}
