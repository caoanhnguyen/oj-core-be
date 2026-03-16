package com.kma.ojcore.repository;

import com.kma.ojcore.entity.UserProblemStatus;
import com.kma.ojcore.enums.UserProblemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProblemStatusRepository extends JpaRepository<UserProblemStatus, UUID> {

    Optional<UserProblemStatus> findByUserIdAndProblemId(UUID userId, UUID problemId);

    List<UserProblemStatus> findByUserIdAndProblemIdIn(UUID userId, List<UUID> problemIds);

    long countByUserIdAndState(UUID userId, UserProblemState state);
}