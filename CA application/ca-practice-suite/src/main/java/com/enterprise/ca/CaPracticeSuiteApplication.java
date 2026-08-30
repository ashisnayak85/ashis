package com.enterprise.ca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CaPracticeSuiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(CaPracticeSuiteApplication.class, args);
    }
}
