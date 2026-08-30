package com.enterprise.ca.mapper;

import com.enterprise.ca.dto.UserDTO;
import com.enterprise.ca.entity.Role;
import com.enterprise.ca.entity.User;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDTO toDTO(User u) {
        return UserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .enabled(u.getEnabled())
                .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .build();
    }
}
