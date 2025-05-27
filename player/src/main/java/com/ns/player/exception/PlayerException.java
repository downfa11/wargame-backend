package com.ns.player.exception;

import lombok.Getter;

@Getter
public class PlayerException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String message;

    public PlayerException(final ErrorCode errorCode){
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public PlayerException(final ErrorCode errorCode, final String message){
        this.errorCode = errorCode;
        this.message = errorCode + " " + message;
    }

    @Override
    public String getMessage() {
        return "[%s] %s".formatted(errorCode, message);
    }
}
