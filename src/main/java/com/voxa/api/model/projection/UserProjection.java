package com.voxa.api.model.projection;

import com.voxa.api.model.entity.Gender;

import java.time.LocalDate;

public interface UserProjection {
    String getEmail();
    String getUsername();
    String getName();
    LocalDate getBirthday();
    Gender getGender();
    String getPpUrl();
    Integer getPostCount();
    Integer getFollowerCount();
    Integer getFollowingCount();
}
