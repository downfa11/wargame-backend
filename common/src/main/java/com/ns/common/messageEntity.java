package com.ns.common;

import lombok.Data;

@Data
public class messageEntity {

    private String message;
    private Object result;

    public messageEntity(String message, Object result) {
        this.message = message;
        this.result  = result;
    }
}
