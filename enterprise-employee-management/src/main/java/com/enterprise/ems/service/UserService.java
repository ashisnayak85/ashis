package com.enterprise.ems.service;

import com.enterprise.ems.dto.UserDTO;
import java.util.List;

public interface UserService {

    UserDTO createUser(UserDTO dto);

    List<UserDTO> getAllUsers();

    void toggleUserStatus(Long id);
}
