package com.kma.ojcore.repository;

import com.kma.ojcore.entity.Contest;
import com.kma.ojcore.enums.ContestStatus;
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

    @Query("SELECT c FROM Contest c " +
            "WHERE (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:ruleType IS NULL OR c.ruleType = :ruleType) " +
            "AND (:contestStatus IS NULL OR c.contestStatus = :contestStatus) " +
            "ORDER BY c.createdDate DESC")
    Page<Contest> searchAdminContests(@Param("keyword") String keyword,
                                      @Param("ruleType") RuleType ruleType,
                                      @Param("contestStatus") ContestStatus contestStatus,
                                      Pageable pageable);
}
