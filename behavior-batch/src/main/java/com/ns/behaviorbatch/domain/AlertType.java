package com.ns.behaviorbatch.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AlertType {
    FREQUENT_DEATH("고의적 죽음"),
    BAD_WORD("욕설 감지"),
    BAD_PATTERN("비속어 패턴");

    private final String description;


    @JsonValue
    public String getDescription() {
        return description;
    }
}
