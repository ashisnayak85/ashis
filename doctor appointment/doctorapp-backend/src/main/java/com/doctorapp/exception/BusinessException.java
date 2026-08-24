package com.doctorapp.exception;

/** Thrown for valid-but-not-allowed situations, e.g. booking a slot that's already taken. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
