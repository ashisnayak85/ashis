package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiError {
    private int status;
    private String message;
    private LocalDateTime timestamp;
}
