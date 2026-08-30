package com.enterprise.ca.service;

import com.enterprise.ca.dto.ChartOfAccountDTO;
import java.util.List;

public interface ChartOfAccountService {
    ChartOfAccountDTO create(ChartOfAccountDTO dto);
    ChartOfAccountDTO update(Long id, ChartOfAccountDTO dto);
    void delete(Long id);
    List<ChartOfAccountDTO> getAllActive();
}
