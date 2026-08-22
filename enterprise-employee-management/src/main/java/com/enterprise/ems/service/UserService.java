package com.enterprise.ems.service;

import com.enterprise.ems.dto.UserDTO;
import java.util.List;

public interface UserService {

    UserDTO createUser(UserDTO dto);

    List<UserDTO> getAllUsers();

    void toggleUserStatus(Long id);

    // Self-service password change - usable by both ADMIN and EMPLOYEE roles.
    void changePassword(String username, String currentPassword, String newPassword);

    // Step 1 of "forgot password": looks up the account, creates a reset token,
    // and emails the link. Deliberately never throws/reveals whether the
    // username/email matched - the controller always returns the same response.
    void initiatePasswordReset(String usernameOrEmail);

    // Step 2: validates the token (exists, not used, not expired) and sets the new password.
    void resetPassword(String token, String newPassword);
}
