package com.ns.result.exception;


public enum ErrorCode {

    NOT_FOUND_CATEGORY_ERROR_MESSAGE("Category not found"),
    NOT_FOUND_POST_ERROR_MESSAGE ("Post not found");


    public final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }
}
