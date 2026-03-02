package com.kma.ojcore.repository;

import com.kma.ojcore.entity.ProblemExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository để quản lý ProblemExample
 */
@Repository
public interface ProblemExampleRepository extends JpaRepository<ProblemExample, UUID> {

    /**
     * Tìm tất cả examples của một Problem, sắp xếp theo orderIndex
     */
    List<ProblemExample> findByProblemIdOrderByOrderIndexAsc(UUID problemId);

    /**
     * Xóa tất cả examples của một Problem
     */
    void deleteByProblemId(UUID problemId);
}
