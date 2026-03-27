package com.kma.ojcore.repository;

import com.kma.ojcore.entity.ContestParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, UUID> {

    boolean existsByContestIdAndUserId(UUID contestId, UUID userId);

    long countByContestId(UUID contestId);
}