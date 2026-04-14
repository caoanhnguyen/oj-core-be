package com.kma.ojcore.repository;

import com.kma.ojcore.entity.ContestWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestWhitelistRepository extends JpaRepository<ContestWhitelist, UUID> {
    
    @Modifying
    @Query("DELETE FROM ContestWhitelist c WHERE c.contest.id = :contestId")
    void deleteByContestId(UUID contestId);

    @Modifying
    @Query("DELETE FROM ContestWhitelist c WHERE c.contest.id = :contestId AND c.email = :email")
    void deleteByContestIdAndEmail(UUID contestId, String email);

    boolean existsByContestIdAndEmail(UUID contestId, String email);
    
    boolean existsByContestId(UUID contestId);

    List<ContestWhitelist> findByContestId(UUID contestId);
}
