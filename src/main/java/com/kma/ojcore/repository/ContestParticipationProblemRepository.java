package com.kma.ojcore.repository;

import com.kma.ojcore.entity.ContestParticipation;
import com.kma.ojcore.entity.ContestParticipationProblem;
import com.kma.ojcore.entity.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestParticipationProblemRepository extends JpaRepository<ContestParticipationProblem, UUID> {
    
    Optional<ContestParticipationProblem> findByParticipationAndContestProblem(ContestParticipation participation, ContestProblem contestProblem);

    List<ContestParticipationProblem> findByParticipationUserIdInAndContestProblemContestId(List<UUID> userIds, UUID contestId);
}
