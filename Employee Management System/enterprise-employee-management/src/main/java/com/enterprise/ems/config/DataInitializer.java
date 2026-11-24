package com.enterprise.ems.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/*
 * PURPOSE: Seeds initial data on first application startup, so the app is
 * usable immediately without any manual SQL.
 *
 * IMPORTANT: this creates exactly ONE bootstrap account - an Employee record
 * AND a linked User record for the admin, created together the same way the
 * normal "create user" flow works (Employee first, User second, then linked).
 * Every other login must be created by that admin through the app, by picking
 * an existing active employee - never seeded directly here.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            LocationRepository locationRepository,
            DesignationRepository designationRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (roleRepository.count() > 0) return; // Already initialized

            // Create roles
            Role adminRole = roleRepository.save(Role.builder().name(AppConstants.ROLE_ADMIN).description("Administrator").build());
            Role managerRole = roleRepository.save(Role.builder().name(AppConstants.ROLE_MANAGER).description("Manager").build());
            Role userRole = roleRepository.save(Role.builder().name(AppConstants.ROLE_USER).description("Regular User").build());

            // Create departments
            Department admin = departmentRepository.save(Department.builder().name("Administration").code("ADM").description("System Administration").build());
            Department it = departmentRepository.save(Department.builder().name("Information Technology").code("IT").description("IT Department").build());
            Department hr = departmentRepository.save(Department.builder().name("Human Resources").code("HR").description("HR Department").build());
            departmentRepository.save(Department.builder().name("Finance").code("FIN").description("Finance Department").build());

            // Create locations - independent of department, every employee sits at exactly one
            Location bangalore = locationRepository.save(Location.builder()
                    .name("Bangalore HQ").code("BLR").city("Bangalore").state("Karnataka")
                    .country("India").pincode("560001").active(true).build());
            Location mumbai = locationRepository.save(Location.builder()
                    .name("Mumbai Office").code("BOM").city("Mumbai").state("Maharashtra")
                    .country("India").pincode("400001").active(true).build());

            // Designations - previously free text on Employee, now a managed lookup
            // table (same pattern as Department).
            Designation sysAdminDesignation = designationRepository.save(
                    Designation.builder().name("System Administrator").active(true).build());
            Designation softwareEngineerDesignation = designationRepository.save(
                    Designation.builder().name("Software Engineer").active(true).build());
            Designation hrManagerDesignation = designationRepository.save(
                    Designation.builder().name("HR Manager").active(true).build());

            // --- Bootstrap admin: Employee first, then User, then link them ---
            // Mirrors exactly how UserServiceImpl.createUser() links the two, so the
            // very first account behaves the same way as every account created after it.
            Employee adminEmployee = employeeRepository.save(Employee.builder()
                    .employeeCode("ADM001").firstName("System").lastName("Administrator")
                    .email("admin@eems.com").mobile("9999999999")
                    .dateOfJoining(LocalDate.now())
                    .designation(sysAdminDesignation)
                    .department(admin).location(bangalore).active(true).build());

            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            String adminUsername = adminEmployee.getEmployeeCode().toLowerCase();
            String adminPassword = "Admin@123";
            User adminUser = userRepository.save(User.builder()
                    .username(adminUsername)
                    .email(adminEmployee.getEmail())
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(adminRoles)
                    .enabled(true)
                    .build());

            adminEmployee.setUser(adminUser);
            employeeRepository.save(adminEmployee);

            log.warn("=================================================================");
            log.warn(" Bootstrap admin account created for first-time setup:");
            log.warn("   username: {}", adminUsername);
            log.warn("   password: {}", adminPassword);
            log.warn(" Log in and change this password immediately (Profile > Change password).");
            log.warn("=================================================================");

            // Sample ACTIVE employees with NO user account yet - deliberately left
            // unlinked so you have real data to test "New User" (pick employee -> email creds).
            employeeRepository.save(Employee.builder()
                    .employeeCode("EMP001").firstName("John").lastName("Doe")
                    .email("john.doe@eems.com").mobile("9876543210")
                    .dateOfJoining(LocalDate.of(2023, 1, 15))
                    .salary(new BigDecimal("75000")).designation(softwareEngineerDesignation)
                    .department(it).location(bangalore).active(true).build());

            employeeRepository.save(Employee.builder()
                    .employeeCode("EMP002").firstName("Jane").lastName("Smith")
                    .email("jane.smith@eems.com").mobile("9876543211")
                    .dateOfJoining(LocalDate.of(2022, 6, 1))
                    .salary(new BigDecimal("85000")).designation(hrManagerDesignation)
                    .department(hr).location(mumbai).active(true).build());
        };
    }
}
