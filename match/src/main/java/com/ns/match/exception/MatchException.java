package com.ns.match.exception;

import lombok.Getter;

@Getter
public class MatchException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String message;

    public MatchException(final ErrorCode errorCode){
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public MatchException(final ErrorCode errorCode, final String message){
        this.errorCode = errorCode;
        this.message = errorCode + " " + message;
    }

    @Override
    public String getMessage() {
        return "[%s] %s".formatted(errorCode, message);
    }
}
