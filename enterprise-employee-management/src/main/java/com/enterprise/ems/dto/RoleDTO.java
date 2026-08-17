package com.enterprise.ems.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO {

    private Long id;

    // e.g. "ROLE_ADMIN" - this is the exact value the frontend must send back
    // in UserDTO.roleName when creating a user.
    private String name;

    private String description;
}
