package com.enterprise.ems.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.entity.*;
import com.enterprise.ems.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/*
 * PURPOSE: Seeds initial data on first application startup
 * Runs after all beans are created (Application Ready phase)
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (roleRepository.count() > 0) return; // Already initialized

            // Create roles
            Role adminRole = roleRepository.save(Role.builder().name(AppConstants.ROLE_ADMIN).description("Administrator").build());
            Role managerRole = roleRepository.save(Role.builder().name(AppConstants.ROLE_MANAGER).description("Manager").build());
            Role userRole = roleRepository.save(Role.builder().name(AppConstants.ROLE_USER).description("Regular User").build());

            // Create admin user (password: admin123)
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@eems.com")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(adminRoles)
                    .enabled(true)
                    .build());

            // Create manager user (password: manager123)
            Set<Role> managerRoles = new HashSet<>();
            managerRoles.add(managerRole);
            userRepository.save(User.builder()
                    .username("manager")
                    .email("manager@eems.com")
                    .password(passwordEncoder.encode("manager123"))
                    .roles(managerRoles)
                    .enabled(true)
                    .build());

            // Create departments
            Department it = departmentRepository.save(Department.builder().name("Information Technology").code("IT").description("IT Department").build());
            Department hr = departmentRepository.save(Department.builder().name("Human Resources").code("HR").description("HR Department").build());
            Department fin = departmentRepository.save(Department.builder().name("Finance").code("FIN").description("Finance Department").build());

            // Create sample employees
            employeeRepository.save(Employee.builder()
                    .employeeCode("EMP001").firstName("John").lastName("Doe")
                    .email("john.doe@eems.com").mobile("9876543210")
                    .dateOfJoining(LocalDate.of(2023, 1, 15))
                    .salary(new BigDecimal("75000")).designation("Software Engineer")
                    .department(it).active(true).build());

            employeeRepository.save(Employee.builder()
                    .employeeCode("EMP002").firstName("Jane").lastName("Smith")
                    .email("jane.smith@eems.com").mobile("9876543211")
                    .dateOfJoining(LocalDate.of(2022, 6, 1))
                    .salary(new BigDecimal("85000")).designation("HR Manager")
                    .department(hr).active(true).build());
        };
    }
}
