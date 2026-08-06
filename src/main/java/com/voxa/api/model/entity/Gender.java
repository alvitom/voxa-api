package com.voxa.api.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Gender {
    MALE("Male"),
    FEMALE("Female");

    private final String gender;
}
