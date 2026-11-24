package com.enterprise.ems.service;

import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {

    // Active employees not yet linked to a User account - for the "grant access" picker
    List<EmployeeDTO> getAvailableForUserCreation();

    EmployeeDTO create(EmployeeDTO dto);

    EmployeeDTO update(Long id, EmployeeDTO dto);

    EmployeeDTO getById(Long id);

    void delete(Long id);

    PageResponse<EmployeeDTO> getAll(Pageable pageable);

    PageResponse<EmployeeDTO> search(String keyword, Pageable pageable);

    // Loads every row in the Employee table (active + inactive) - "SELECT * FROM Employee"
    PageResponse<EmployeeDTO> getAllEmployees(Pageable pageable);

    PageResponse<EmployeeDTO> searchAllEmployees(String keyword, Pageable pageable);

    // Same filter as getAll/search (active employees only), but returns the full
    // matching set as a ready-to-download .xlsx instead of a page of DTOs.
    byte[] exportEmployees(String keyword);

    // Same filter as getAllEmployees/searchAllEmployees (every row, active or not).
    byte[] exportAllEmployees(String keyword);
}
