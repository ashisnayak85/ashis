package com.enterprise.ems.service;
import com.enterprise.ems.dto.EmployeeTypeDTO;
import java.util.List;

public interface EmployeeTypeService {
	EmployeeTypeDTO create(EmployeeTypeDTO dto);

	EmployeeTypeDTO update(Long id, EmployeeTypeDTO dto);

	//EmployeeTypeDTO getById(Long id);

    void delete(Long id);

    List<EmployeeTypeDTO> getAllActive();
    List<EmployeeTypeDTO> getAllActiveSimple(); // add this
}
