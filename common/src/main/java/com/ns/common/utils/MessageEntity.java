package com.ns.common.utils;

import lombok.Data;

@Data
public class MessageEntity {

    private String message;
    private Object result;

    public MessageEntity(String message, Object result) {
        this.message = message;
        this.result  = result;
    }
}