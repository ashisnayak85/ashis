package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpecializationDTO {
    private Long id;
    private String name;
    private boolean active;
}
