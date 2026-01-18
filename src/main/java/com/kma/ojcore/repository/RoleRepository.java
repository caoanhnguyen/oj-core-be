package com.kma.ojcore.repository;

import com.kma.ojcore.entity.Role;
import com.kma.ojcore.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    Boolean existsByName(RoleName name);

    default Role getUserRole() {
        return findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("User Role not found"));
    }

    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
    Set<Role> getRoleByUserId(UUID userId);
}

