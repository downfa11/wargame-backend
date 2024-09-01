package com.ns.feed.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class messageEntity {

    private String message;
    private Object result;

    public messageEntity(String message, Object result) {
        this.message = message;
        this.result  = result;
    }
}
