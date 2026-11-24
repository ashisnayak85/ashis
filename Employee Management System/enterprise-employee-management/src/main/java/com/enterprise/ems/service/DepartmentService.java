package com.enterprise.ems.service;

import com.enterprise.ems.dto.DepartmentDTO;
import java.util.List;

public interface DepartmentService {

    DepartmentDTO create(DepartmentDTO dto);

    DepartmentDTO update(Long id, DepartmentDTO dto);

    DepartmentDTO getById(Long id);

    void delete(Long id);

    List<DepartmentDTO> getAllActive();
    List<DepartmentDTO> getAllActiveSimple(); // add this
}
