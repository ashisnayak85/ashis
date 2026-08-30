package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.RoleDTO;
import com.enterprise.ca.repository.RoleRepository;
import com.enterprise.ca.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(r -> RoleDTO.builder().id(r.getId()).name(r.getName()).description(r.getDescription()).build())
                .toList();
    }
}
