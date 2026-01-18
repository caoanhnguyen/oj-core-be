package com.kma.ojcore.entity;

import com.kma.ojcore.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    @Enumerated(EnumType.STRING)
    RoleName name;

    @Column(name = "description", length = 255)
    String description;

    // -- Relationships -- //

    // Role - User //
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    Set<User> users = new HashSet<>();
}

