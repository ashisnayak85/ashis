package com.doctorapp.config;

import com.doctorapp.entity.Specialization;
import com.doctorapp.entity.User;
import com.doctorapp.repository.SpecializationRepository;
import com.doctorapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SpecializationRepository specializationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    private static final List<String> DEFAULT_SPECIALIZATIONS = List.of(
            "General Physician", "Dentist", "Cardiologist", "Dermatologist",
            "Pediatrician", "Gynecologist", "Orthopedic", "ENT Specialist",
            "Ophthalmologist", "Psychiatrist"
    );

    @Override
    public void run(String... args) {
        for (String name : DEFAULT_SPECIALIZATIONS) {
            specializationRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> specializationRepository.save(Specialization.builder().name(name).build()));
        }

        // Seed exactly one ADMIN account so the platform has somewhere to log in as
        // admin from day one - there is no public "register as admin" endpoint by
        // design (admin accounts shouldn't be self-service). Change the password
        // (or the app.admin.* properties / env vars) before any real deployment.
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.save(User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .build());
            log.info("Seeded default admin account -> email: {} / password: {} (change this before deploying!)",
                    adminEmail, adminPassword);
        }
    }
}
