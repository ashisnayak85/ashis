package com.enterprise.ems.controller;

import com.enterprise.ems.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeViewController {

    private final DepartmentService departmentService;

    @GetMapping
    public String listEmployees(Model model) {
        model.addAttribute("departments", departmentService.getAllActive());
        return "employee/list";
    }
}
