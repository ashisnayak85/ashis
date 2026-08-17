package com.enterprise.ems.security;

import com.enterprise.ems.entity.User;
import com.enterprise.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/*
 * PURPOSE: Loads user from DB for Spring Security authentication
 * Implements UserDetailsService - called during login
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // A login is only usable while it is (a) explicitly enabled AND (b) linked to
        // an employee who is currently active. Deactivating an employee automatically
        // blocks their login - no separate step needed.
        boolean isEnabled = Boolean.TRUE.equals(user.getEnabled())
                && user.getEmployee() != null
                && Boolean.TRUE.equals(user.getEmployee().getActive());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                isEnabled,
                true, true, true,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toSet())
        );
    }
}
