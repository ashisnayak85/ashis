package com.enterprise.ems;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/*
 * ================================================================================
 * PURPOSE: WAR Deployment Support (Phase 13)
 * ================================================================================
 * When deploying as WAR to external Tomcat, Tomcat calls configure() instead of main().
 * This bridges traditional servlet container deployment with Spring Boot.
 *
 * WITHOUT THIS CLASS: WAR deployment to external Tomcat will FAIL.
 * WITH JAR + embedded Tomcat: This class is not used.
 * ================================================================================
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(EmployeeManagementApplication.class);
    }
}
