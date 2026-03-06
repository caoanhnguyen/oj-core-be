package com.kma.ojcore.repository;

import com.kma.ojcore.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
    
    java.util.List<TestCase> findByProblemId(UUID problemId);
    
    void deleteByProblemId(UUID problemId);
}
