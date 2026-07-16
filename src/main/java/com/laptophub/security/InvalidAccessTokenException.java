package com.laptophub.security;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
