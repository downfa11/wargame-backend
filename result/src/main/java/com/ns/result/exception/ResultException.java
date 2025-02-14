package com.ns.result.exception;

import lombok.Getter;

@Getter
public class ResultException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String message;

    public ResultException(final ErrorCode errorCode){
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public ResultException(final ErrorCode errorCode, final String message){
        this.errorCode = errorCode;
        this.message = errorCode + " " + message;
    }

    @Override
    public String getMessage() {
        return "[%s] %s".formatted(errorCode, message);
    }
}
