package com.onehealth.service;

import com.onehealth.dto.*;
import com.onehealth.entity.Organization;
import com.onehealth.entity.Patient;
import com.onehealth.entity.User;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.OrganizationRepository;
import com.onehealth.repository.PatientRepository;
import com.onehealth.repository.UserRepository;
import com.onehealth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(AuthRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("An account with this email already exists.");
        }
        Organization org = organizationRepository.findBySlug(req.getOrganizationSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown clinic organization."));
        if (!org.isActive()) {
            throw new BusinessException("This organization is not currently active.");
        }

        User user = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.PATIENT)
                .organizationId(org.getId())
                .enabled(true)
                .build());

        // If a walk-in with this phone was already registered by a clinic admin
        // (no login), link this new login to that existing record instead of
        // creating a duplicate patient profile with a separate history.
        Patient patient = patientRepository.findByOrganizationIdAndPhone(org.getId(), req.getPhone())
                .stream()
                .filter(p -> p.getUser() == null)
                .findFirst()
                .orElseGet(() -> Patient.builder()
                        .organizationId(org.getId())
                        .name(req.getName())
                        .phone(req.getPhone())
                        .dob(req.getDob())
                        .gender(req.getGender())
                        .build());

        patient.setUser(user);
        patientRepository.save(patient);

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest req) {
        String token = req.getRefreshToken();
        if (!jwtTokenProvider.isValid(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw new BusinessException("Invalid or expired refresh token.");
        }
        String email = jwtTokenProvider.getEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getEmail(), user.getRole().name(), user.getOrganizationId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getEmail(), user.getRole().name(), user.getOrganizationId());

        String orgName = null;
        if (user.getOrganizationId() != null) {
            orgName = organizationRepository.findById(user.getOrganizationId())
                    .map(Organization::getName).orElse(null);
        }

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .organizationId(user.getOrganizationId())
                .organizationName(orgName)
                .userId(user.getId())
                .name(user.getName())
                .build();
    }
}
