package com.enterprise.ems.service;

import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.entity.Department;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.mapper.EmployeeMapper;
import com.enterprise.ems.repository.DepartmentRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * =============================================================================
 * UNIT TEST (Phase 12) - EmployeeServiceImpl
 * =============================================================================
 * STRATEGY: Mock dependencies, test business logic in isolation
 * @ExtendWith(MockitoExtension.class): Enables @Mock and @InjectMocks
 * @InjectMocks: Creates instance and injects mocks
 * @Mock: Creates mock implementation of dependency
 * =============================================================================
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void createEmployee_Success() {
        EmployeeDTO dto = EmployeeDTO.builder()
                .employeeCode("EMP100")
                .firstName("Test")
                .lastName("User")
                .email("test@eems.com")
                .departmentId(1L)
                .dateOfJoining(LocalDate.now())
                .build();

        Department dept = Department.builder().id(1L).name("IT").build();
        Employee entity = Employee.builder().id(1L).employeeCode("EMP100").build();

        when(employeeRepository.existsByEmployeeCode("EMP100")).thenReturn(false);
        when(employeeRepository.existsByEmail("test@eems.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(employeeMapper.toEntity(dto, dept)).thenReturn(entity);
        when(employeeRepository.save(any())).thenReturn(entity);
        when(employeeMapper.toDTO(entity)).thenReturn(dto);

        EmployeeDTO result = employeeService.create(dto);

        assertNotNull(result);
        assertEquals("EMP100", result.getEmployeeCode());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void createEmployee_DuplicateCode_ThrowsException() {
        EmployeeDTO dto = EmployeeDTO.builder().employeeCode("EMP001").email("dup@eems.com").build();
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.create(dto));
        verify(employeeRepository, never()).save(any());
    }
}
