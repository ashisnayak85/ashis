package com.onehealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OneHealth Clinic Platform - multi-branch clinic booking + owner analytics.
 * Independent codebase from the public doctor-appointment marketplace project;
 * see README.md for how the two differ and why they're separate.
 */
@SpringBootApplication
public class OneHealthApplication {
    public static void main(String[] args) {
        SpringApplication.run(OneHealthApplication.class, args);
    }
}
