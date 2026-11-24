package com.enterprise.ems.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveViewController {

    @GetMapping
    public String listLeaves() {
        return "leave/list";
    }
}
