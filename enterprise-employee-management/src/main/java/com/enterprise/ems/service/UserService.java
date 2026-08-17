package com.enterprise.ems.service;

import com.enterprise.ems.dto.UserDTO;
import java.util.List;

public interface UserService {

    UserDTO createUser(UserDTO dto);

    List<UserDTO> getAllUsers();

    void toggleUserStatus(Long id);

    // Self-service password change - usable by both ADMIN and EMPLOYEE roles.
    void changePassword(String username, String currentPassword, String newPassword);
}
