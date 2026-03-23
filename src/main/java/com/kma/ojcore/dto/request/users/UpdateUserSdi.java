package com.kma.ojcore.dto.request.users;

import com.kma.ojcore.enums.Gender;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserSdi {

    // Thông tin cá nhân
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
}
