package com.sergio.planix.common;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) { super(message); }
}
