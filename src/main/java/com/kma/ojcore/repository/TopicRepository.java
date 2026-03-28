package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.topics.DifficultyCountProjection;
import com.kma.ojcore.dto.response.topics.TopicAdminSdo;
import com.kma.ojcore.dto.response.topics.TopicBasicSdo;
import com.kma.ojcore.entity.Topic;
import com.kma.ojcore.enums.EStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    @Query("SELECT new com.kma.ojcore.dto.response.topics.TopicBasicSdo(t.id, t.name, t.slug) " +
            "FROM Topic t " +
            "WHERE t.status = 'ACTIVE'")
    Page<TopicBasicSdo> allActiveTopics(Pageable pageable);

    List<Topic> findByIdInAndStatus(Collection<UUID> topicIds, EStatus status);

    @Query("SELECT new com.kma.ojcore.dto.response.topics.TopicBasicSdo(t.id, t.name, t.slug) " +
            "FROM Topic t " +
            "WHERE (:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '!') " +
            "AND t.status = 'ACTIVE'")
    Page<TopicBasicSdo> userSearchTopics(String name, Pageable pageable);

    @Query("SELECT new com.kma.ojcore.dto.response.topics.TopicAdminSdo(t.id, t.name, t.slug, t.status, t.updatedDate, t.createdBy) " +
            "FROM Topic t " +
            "WHERE (:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '!')")
    Page<TopicAdminSdo> searchTopics(String name, Pageable pageable);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    Optional<Topic> findBySlug(String slug);

    @Modifying
    @Query("UPDATE Topic t SET t.status = :status WHERE t.id = :topicId")
    void updateStatusById(@Param("status") EStatus status, @Param("topicId") UUID topicId);

    @Query("SELECT p.difficulty as difficulty, COUNT(p.id) as count " +
            "FROM Problem p JOIN p.topics t " +
            "WHERE t.slug = :slug AND p.status = 'ACTIVE' AND p.problemStatus = 'PUBLISHED' " +
            "GROUP BY p.difficulty")
    List<DifficultyCountProjection> countProblemsByDifficultyForTopic(@Param("slug") String slug);
}
