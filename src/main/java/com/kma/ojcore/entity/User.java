package com.kma.ojcore.entity;

import com.kma.ojcore.enums.Gender;
import com.kma.ojcore.enums.Provider;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    String username;

    @Column(name = "email", unique = true, length = 100)
    String email;

    @Column(name = "password", length = 255)
    String password;

    @Column(name = "full_name", length = 100)
    String fullName;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    Gender gender;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;

    @Column(name = "phone_number", length = 20)
    String phoneNumber;

    @Column(name = "address", length = 255)
    String address;

    @Column(name = "country", length = 50)
    String country;

    @Column(name = "city", length = 50)
    String city;

    @Column(name = "school", length = 100)
    String school;

    @Column(name = "major", length = 100)
    String major;

    @Column(name = "github_url", length = 255)
    String githubUrl;

    @Column(name = "website", length = 255)
    String website;

    @Column(name = "rating", nullable = false)
    @Builder.Default
    Integer rating = 0;

    @Column(name = "solved_count", nullable = false)
    @Builder.Default
    Integer solvedCount = 0;

    @Column(name = "submission_count", nullable = false)
    @Builder.Default
    Integer submissionCount = 0;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    Boolean emailVerified = false;

    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    Boolean accountNonLocked = true;

    @Column(name = "provider", length = 20)
    @Enumerated(EnumType.STRING)
    Provider provider;

    @Column(name = "provider_id", length = 100)
    String providerId;

    // -- Relationships -- //

    // User - Role //
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    Set<Role> roles = new HashSet<>();

    // User - RefreshToken //
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    Set<RefreshToken> refreshTokens = new HashSet<>();
}
