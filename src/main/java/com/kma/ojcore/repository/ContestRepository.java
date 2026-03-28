package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.contests.ContestBasicSdo;
import com.kma.ojcore.entity.Contest;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestRepository extends JpaRepository<Contest, UUID> {

    @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestBasicSdo(" +
            "c.id, c.title, c.startTime, c.endTime, c.ruleType, c.visibility, COUNT(p.id), c.isVisible, c.status) " +
            "FROM Contest c LEFT JOIN c.participations p " +
            "WHERE (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') AND " +
            "(:ruleType IS NULL OR c.ruleType = :ruleType) AND " +
            "(:statusString IS NULL OR " +
            "  (:statusString = 'UPCOMING' AND c.startTime > CURRENT_TIMESTAMP) OR " +
            "  (:statusString = 'ONGOING' AND c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP) OR " +
            "  (:statusString = 'ENDED' AND c.endTime <= CURRENT_TIMESTAMP)" +
            ") AND " +
            "(:isVisible IS NULL OR c.isVisible = :isVisible) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "GROUP BY c.id, c.title, c.startTime, c.endTime, c.ruleType, c.visibility, c.isVisible " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP THEN 1 " +
            "    WHEN c.startTime > CURRENT_TIMESTAMP THEN 2 " +
            "    ELSE 3 " +
            "  END ASC, " +
            "  c.startTime DESC",
            countQuery = "SELECT COUNT(c) FROM Contest c WHERE " +
                    "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') AND " +
                    "(:ruleType IS NULL OR c.ruleType = :ruleType) AND " +
                    "(:statusString IS NULL OR " +
                    "  (:statusString = 'UPCOMING' AND c.startTime > CURRENT_TIMESTAMP) OR " +
                    "  (:statusString = 'ONGOING' AND c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP) OR " +
                    "  (:statusString = 'ENDED' AND c.endTime <= CURRENT_TIMESTAMP)" +
                    ") " +
                    "AND (:isVisible IS NULL OR c.isVisible = :isVisible)")
    Page<ContestBasicSdo> searchAdminContests(@Param("keyword") String keyword,
                                              @Param("ruleType") RuleType ruleType,
                                              @Param("statusString") String statusString,
                                              @Param("isVisible") Boolean isVisible,
                                              @Param("status") EStatus status,
                                              Pageable pageable);

    @Query("SELECT c FROM Contest c LEFT JOIN FETCH c.author WHERE c.id = :id")
    Optional<Contest> findContestWithAuthorById(@Param("id") UUID id);

    @Modifying
    @Query("Update Contest c set c.status = :status where c.id = :id")
    void updateStatusById(@Param("status") EStatus status, @Param("id") UUID id);
}
