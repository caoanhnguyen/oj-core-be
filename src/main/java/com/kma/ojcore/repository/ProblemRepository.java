package com.kma.ojcore.repository;

import com.kma.ojcore.dto.request.problems.ProblemFilter;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.ProblemStatus;
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
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    boolean existsBySlug(String slug);

    Optional<Problem> findBySlugAndStatus(String slug, EStatus status);

    Optional<Problem> findByIdAndStatus(UUID id, EStatus status);

    @Query("SELECT new com.kma.ojcore.dto.response.problems.ProblemResponse(p.id, p.title, p.slug, p.difficulty, p.status, p.problemStatus, p.createdDate, p.updatedDate) " +
            "FROM Problem p " +
            "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(:keyword)) " +
            "AND (:difficulty IS NULL OR p.difficulty = :difficulty) " +
            "AND (:status IS NULL OR p.status = :status)" +
            "AND (:problemStatus IS NULL OR p.problemStatus = :problemStatus)" +
            "AND (:topicSlugs IS NULL OR EXISTS (SELECT t FROM p.topics t WHERE t.slug IN :topicSlugs))")
    Page<ProblemResponse> searchProblemsForAdmin(@Param("keyword") String keyword,
                                                 @Param("difficulty") ProblemDifficulty difficulty,
                                                 @Param("status") EStatus status,
                                                 @Param("problemStatus") ProblemStatus problemStatus,
                                                 @Param("topicSlugs") List<String> topicSlugs,
                                                 Pageable pageable);

    @Modifying
    @Query("Update Problem p set p.status = :status where p.id = :id")
    void updateStatusById(@Param("status") EStatus status, @Param("id") UUID id);
}
