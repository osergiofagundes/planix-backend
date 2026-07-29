package com.sergio.planix.common;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String message) { super(message); }
}
