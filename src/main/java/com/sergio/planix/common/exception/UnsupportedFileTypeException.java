package com.sergio.planix.common.exception;

/** O arquivo até chegou inteiro, mas o tipo não serve para aquele uso. Vira 415. */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
