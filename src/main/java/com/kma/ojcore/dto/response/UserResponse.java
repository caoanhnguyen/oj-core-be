package com.kma.ojcore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserResponse {

    UUID id;
    String username;
    String email;
    String fullName;
    String avatarUrl;
    String bio;
    String phoneNumber;
    String address;
    String country;
    String city;
    String school;
    String major;
    String githubUrl;
    String linkedinUrl;
    String website;
    Integer rating;
    Integer solvedCount;
    Integer submissionCount;
    Boolean emailVerified;
    String provider;
    Set<String> roles;
    LocalDateTime createdDate;
}

