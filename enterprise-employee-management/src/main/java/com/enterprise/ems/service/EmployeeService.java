package com.enterprise.ems.service;

import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeDTO create(EmployeeDTO dto);

    EmployeeDTO update(Long id, EmployeeDTO dto);

    EmployeeDTO getById(Long id);

    void delete(Long id);

    PageResponse<EmployeeDTO> getAll(Pageable pageable);

    PageResponse<EmployeeDTO> search(String keyword, Pageable pageable);

    // Loads every row in the Employee table (active + inactive) - "SELECT * FROM Employee"
    PageResponse<EmployeeDTO> getAllEmployees(Pageable pageable);

    PageResponse<EmployeeDTO> searchAllEmployees(String keyword, Pageable pageable);
}
