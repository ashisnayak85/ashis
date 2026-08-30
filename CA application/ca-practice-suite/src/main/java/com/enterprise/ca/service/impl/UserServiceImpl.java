package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.UserDTO;
import com.enterprise.ca.entity.Role;
import com.enterprise.ca.entity.User;
import com.enterprise.ca.exception.BusinessException;
import com.enterprise.ca.exception.DuplicateResourceException;
import com.enterprise.ca.exception.ResourceNotFoundException;
import com.enterprise.ca.mapper.UserMapper;
import com.enterprise.ca.repository.RoleRepository;
import com.enterprise.ca.repository.UserRepository;
import com.enterprise.ca.service.AuditService;
import com.enterprise.ca.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    public UserDTO createUser(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("Password is required when creating a user");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : dto.getRoles()) {
            String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            roles.add(roleRepository.findByName(normalized)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)));
        }

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .enabled(dto.getEnabled() == null || dto.getEnabled())
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        auditService.log("CREATE", "User", saved.getId(), "Created user: " + saved.getUsername());
        return userMapper.toDTO(saved);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toDTO).toList();
    }

    @Override
    public void setEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setEnabled(enabled);
        userRepository.save(user);
        auditService.log(enabled ? "ENABLE" : "DISABLE", "User", id, "User " + user.getUsername() + (enabled ? " enabled" : " disabled"));
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.log("PASSWORD_CHANGE", "User", user.getId(), "Password changed by " + username);
    }
}
