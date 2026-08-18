

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * ================================================================================
 * PURPOSE: Main entry point for the Enterprise Employee Management System (EEMS)
 * ================================================================================
 *
 * ANNOTATION: @SpringBootApplication
 * - Combines @Configuration, @EnableAutoConfiguration, @ComponentScan
 * - Triggers Spring Boot auto-configuration and component scanning
 *
 * APPLICATION START FLOW:
 * 1. main() invokes SpringApplication.run()
 * 2. Spring Boot creates ApplicationContext (IoC Container)
 * 3. Component scanning finds @Component, @Service, @Repository, @Controller
 * 4. Beans are created and dependencies injected
 * 5. Embedded Tomcat starts (or WAR deploys to external Tomcat)
 * 6. Application is READY to accept HTTP requests
 *
 * INTERVIEW Q: What does @SpringBootApplication do?
 * A: It is a convenience annotation that enables auto-configuration,
 *    component scanning in the current package and sub-packages,
 *    and registers the class as a configuration source.
 * ================================================================================
 */
@SpringBootApplication
@EnableCaching      // Phase 11: Enables @Cacheable, @CacheEvict
@EnableScheduling   // Phase 11: Enables @Scheduled tasks
@EnableAsync        // Phase 11: Enables @Async background processing
public class EmployeeManagementApplication {

    public static void main(String[] args) {
        // Bootstraps Spring ApplicationContext and starts embedded server
        SpringApplication.run(EmployeeManagementApplication.class, args);
    }
}
