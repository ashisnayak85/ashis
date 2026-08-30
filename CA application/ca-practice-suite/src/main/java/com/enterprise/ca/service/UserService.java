package com.enterprise.ca.service;

import com.enterprise.ca.dto.UserDTO;
import java.util.List;

public interface UserService {
    UserDTO createUser(UserDTO dto);
    List<UserDTO> getAllUsers();
    void setEnabled(Long id, boolean enabled);
    void changePassword(String username, String currentPassword, String newPassword);
}
