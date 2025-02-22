package com.ns.resultquery.exception;

import lombok.Getter;

@Getter
public class ResultQueryException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String message;

    public ResultQueryException(final ErrorCode errorCode){
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public ResultQueryException(final ErrorCode errorCode, final String message){
        this.errorCode = errorCode;
        this.message = errorCode + " " + message;
    }

    @Override
    public String getMessage() {
        return "[%s] %s".formatted(errorCode, message);
    }
}
