package com.enterprise.ems.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentViewController {

    @GetMapping
    public String listDepartments() {
        return "department/list";
    }
}
