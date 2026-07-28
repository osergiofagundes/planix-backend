package com.sergio.planix.common;

public class BoardNotEmptyException extends RuntimeException {
    public BoardNotEmptyException(String message) { super(message); }
}