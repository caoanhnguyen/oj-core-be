package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Topic extends BaseEntity{

    @Column(name = "name", nullable = false, unique = true, length = 100)
    String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    String slug;

    @Column(name = "description", columnDefinition = "TEXT", length = 1000)
    String description;

     // -- Relationships -- //

    // Topic - Problem //
    @ManyToMany(mappedBy = "topics", fetch = FetchType.LAZY)
    @Builder.Default
    Set<Problem> problems = new HashSet<>();
}
