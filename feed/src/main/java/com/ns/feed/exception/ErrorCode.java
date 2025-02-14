package com.ns.feed.exception;


public enum ErrorCode {
    NOT_FOUND_MEMBERSHIP_ERROR_MESSAGE("User not Found."),
    NOT_FOUND_POST_ERROR_MESSAGE("Post not found"),
    NOT_FOUND_COMMENT_ERROR_MESSAGE("Comment not found");

    public final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
