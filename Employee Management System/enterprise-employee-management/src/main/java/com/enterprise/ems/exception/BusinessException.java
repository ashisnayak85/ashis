package com.enterprise.ems.exception;

/*
 * PURPOSE: Base runtime exception for business rule violations
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
