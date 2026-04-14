package com.kma.ojcore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "contest_whitelist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestWhitelist extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "note", length = 500)
    private String note;
}
