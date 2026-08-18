package com.enterprise.ems.service;

import com.enterprise.ems.dto.DesignationDTO;
import java.util.List;

public interface DesignationService {

    DesignationDTO create(DesignationDTO dto);

    DesignationDTO update(Long id, DesignationDTO dto);

    DesignationDTO getById(Long id);

    void delete(Long id);

    List<DesignationDTO> getAllActive();

    List<DesignationDTO> getAllActiveSimple();
}
