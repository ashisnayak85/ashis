package com.enterprise.ems.service;

import com.enterprise.ems.dto.LocationDTO;
import java.util.List;

public interface LocationService {

    LocationDTO create(LocationDTO dto);

    LocationDTO update(Long id, LocationDTO dto);

    LocationDTO getById(Long id);

    void delete(Long id);

    List<LocationDTO> getAllActive();

    // Lightweight variant for dropdowns/pickers - no per-row employee count query.
    List<LocationDTO> getAllActiveSimple();
}
