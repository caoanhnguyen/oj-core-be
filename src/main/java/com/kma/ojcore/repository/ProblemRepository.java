package com.kma.ojcore.repository;

import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemDifficulty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    boolean existsBySlug(String slug);

    Optional<Problem> findBySlugAndStatus(String slug, EStatus status);

    Optional<Problem> findByIdAndStatus(UUID id, EStatus status);

    Page<Problem> findByStatus(EStatus status, Pageable pageable);

    Page<Problem> findByStatusAndDifficulty(EStatus status, ProblemDifficulty difficulty, Pageable pageable);

    Page<Problem> findByStatusAndTitleContainingIgnoreCase(EStatus status, String title, Pageable pageable);

    Page<Problem> findByStatusAndDifficultyAndTitleContainingIgnoreCase(EStatus status, ProblemDifficulty difficulty, String title, Pageable pageable);
}
