package com.ns.resultquery.exception;


public enum ErrorCode {
    RETRIEVE_DATA_ERROR_MESSAGE("Error retrieving data: ");


    public final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }
}
