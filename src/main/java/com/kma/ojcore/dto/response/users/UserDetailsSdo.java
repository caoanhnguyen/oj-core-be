package com.kma.ojcore.dto.response.users;

import com.kma.ojcore.enums.Gender;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserDetailsSdo {

    // Thông tin cá nhân
    UUID id;
    String username;
    String email;
    String fullName;
    Gender gender;
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

    Boolean emailVerified;
    String provider;

    // Thống kê cá nhân
    Integer solvedCount;
    Integer submissionCount;
    Integer acCount;
    Double totalScore;

    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}
