package com.sergio.planix.common.exception;

public class BoardNotEmptyException extends RuntimeException {
    public BoardNotEmptyException(String message) { super(message); }
}