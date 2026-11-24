package com.enterprise.ems.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserViewController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers() {
        return "user/list";
    }
}
