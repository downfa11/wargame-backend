package com.ns.player.exception;


public enum ErrorCode {

    NOT_FOUND_CATEGORY_ERROR_MESSAGE("Category not found");


    public final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }
}
