package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.UserDTO;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.Role;
import com.enterprise.ems.entity.User;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.RoleRepository;
import com.enterprise.ems.repository.UserRepository;
import com.enterprise.ems.service.EmailService;
import com.enterprise.ems.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    // Avoids ambiguous glyphs (0/O, 1/l/I) since a human reads this out of an email.
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private EmailService emailService;

    public UserServiceImpl(UserRepository userRepository,
                           EmployeeRepository employeeRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDTO createUser(UserDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + dto.getEmployeeId()));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BusinessException("Cannot grant login access to an inactive employee");
        }
        if (employee.getUser() != null) {
            throw new DuplicateResourceException("This employee already has a user account");
        }
        // Username and email are derived from the employee record - single source of
        // truth, so the two can never drift apart the way free-typed fields could.
        String username = employee.getEmployeeCode().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("A user already exists for this employee code");
        }
        if (userRepository.existsByEmail(employee.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        Role role = roleRepository.findByName(dto.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + dto.getRoleName()));
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        // Generated fresh every time, encoded before storage - never seen by the admin,
        // only ever leaves this method inside the outgoing email.
        String temporaryPassword = generateTemporaryPassword();

        User user = User.builder()
                .username(username)
                .email(employee.getEmail())
                .password(passwordEncoder.encode(temporaryPassword))
                .roles(roles)
                .enabled(true)
                .build();
        User saved = userRepository.save(user);

        // Employee owns the user_id foreign key, so the link is persisted here.
        employee.setUser(saved);
        employeeRepository.save(employee);

        if (emailService != null) {
            emailService.sendCredentialsEmail(saved.getEmail(), saved.getUsername(), temporaryPassword);
        }

        return UserDTO.builder()
                .id(saved.getId())
                .employeeId(employee.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .roleName(role.getName())
                .enabled(saved.getEnabled())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserDTO.builder()
                        .id(u.getId())
                        .employeeId(u.getEmployee() != null ? u.getEmployee().getId() : null)
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .roleName(u.getRoles().stream().findFirst().map(Role::getName).orElse(""))
                        .enabled(u.getEnabled())
                        .build())
                .toList();
    }

    @Override
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}