package com.ns.match.exception;


public enum ErrorCode {
    ALREADY_EXIST_IN_QUEUE("MembershipId 이미 큐에 존재");


    public final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }
}
