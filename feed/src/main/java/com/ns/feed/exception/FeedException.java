package com.ns.feed.exception;

import lombok.Getter;

@Getter
public class FeedException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String message;

    public FeedException(final ErrorCode errorCode){
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public FeedException(final ErrorCode errorCode, final String message){
        this.errorCode = errorCode;
        this.message = errorCode + " " + message;
    }

    @Override
    public String getMessage() {
        return "[%s] %s".formatted(errorCode, message);
    }
}
