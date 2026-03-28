package com.kma.ojcore.repository;

import com.kma.ojcore.entity.Contest;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContestRepository extends JpaRepository<Contest, UUID> {

    @Query("SELECT c FROM Contest c LEFT JOIN FETCH c.author WHERE " +
            "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' ) AND " +
            "(:ruleType IS NULL OR c.ruleType = :ruleType) AND " +
            "(:contestStatus IS NULL OR " +
            "  (:contestStatus = 'UPCOMING' AND c.startTime > CURRENT_TIMESTAMP) OR " +
            "  (:contestStatus = 'ONGOING' AND c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP) OR " +
            "  (:contestStatus = 'ENDED' AND c.endTime <= CURRENT_TIMESTAMP)) " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN c.startTime <= CURRENT_TIMESTAMP AND c.endTime > CURRENT_TIMESTAMP THEN 1 " +
            "    WHEN c.startTime > CURRENT_TIMESTAMP THEN 2 " +
            "    ELSE 3 " +
            "  END ASC, " +
            "  c.startTime DESC")
    Page<Contest> searchAdminContests(@Param("keyword") String keyword,
                                      @Param("ruleType") RuleType ruleType,
                                      @Param("contestStatus") String contestStatus,
                                      Pageable pageable);
}
