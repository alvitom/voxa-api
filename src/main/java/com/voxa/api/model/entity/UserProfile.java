package com.voxa.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
//@Entity
//@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private final String userId;

    private final String name;

    private final LocalDate birthday;

    private final String bio;

    @Column(name = "profile_picture_url")
    private final String profilePictureUrl;

    @Column(name = "post_count")
    private final Integer postCount;

    @Column(name = "follower_count")
    private final Integer followerCount;

    @Column(name = "following_count")
    private final Integer followingCount;
}
