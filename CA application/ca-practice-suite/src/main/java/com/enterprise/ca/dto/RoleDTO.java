package com.enterprise.ca.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
}
