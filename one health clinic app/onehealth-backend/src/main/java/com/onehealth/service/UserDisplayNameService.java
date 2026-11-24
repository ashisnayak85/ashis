package com.onehealth.service;

import com.onehealth.entity.ClinicAdmin;
import com.onehealth.entity.Doctor;
import com.onehealth.entity.Patient;
import com.onehealth.entity.User;
import com.onehealth.repository.ClinicAdminRepository;
import com.onehealth.repository.DoctorRepository;
import com.onehealth.repository.PatientRepository;
import com.onehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User.name is only ever populated for OWNER/SUPER_ADMIN - CLINIC_ADMIN,
 * DOCTOR, and PATIENT keep their name on their own profile entity instead
 * (see User.java javadoc). This is the single place that knows how to resolve
 * a display name regardless of role, so the lookup logic doesn't get
 * duplicated (and drift) across AuthService, audit logging, etc.
 */
@Service
@RequiredArgsConstructor
public class UserDisplayNameService {

    private final UserRepository userRepository;
    private final ClinicAdminRepository clinicAdminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public String resolve(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "Unknown user";
        }
        return resolve(user);
    }

    @Transactional(readOnly = true)
    public String resolve(User user) {
        return switch (user.getRole()) {
            case OWNER, SUPER_ADMIN -> user.getName();
            case CLINIC_ADMIN -> clinicAdminRepository.findByUserId(user.getId())
                    .map(ClinicAdmin::getName).orElse(null);
            case DOCTOR -> doctorRepository.findByUserId(user.getId())
                    .map(Doctor::getName).orElse(null);
            case PATIENT -> patientRepository.findByUserId(user.getId())
                    .map(Patient::getName).orElse(null);
        };
    }
}
