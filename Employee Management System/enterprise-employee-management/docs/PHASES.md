# 13-Phase Learning Roadmap

Study each phase in order. For every phase: read the concept, explore the code, run the app, answer interview questions.

---

## Phase 1: Spring Boot Setup

### Concept
Spring Boot eliminates XML configuration. `@SpringBootApplication` bootstraps everything.

### Code to Study
- `EmployeeManagementApplication.java`
- `pom.xml`
- `application.properties`

### Key Annotations
`@SpringBootApplication`, `@EnableAutoConfiguration`

### Run
```bash
mvn spring-boot:run
```

### Interview Questions
1. What is Spring Boot vs Spring Framework?
2. What does `@SpringBootApplication` include?
3. How does auto-configuration work?

### Common Mistakes
- Wrong Java version (need 17+ for Boot 3, we use 21)
- Missing `spring-boot-starter-parent` in pom.xml

---

## Phase 2: MVC + Thymeleaf

### Concept
Model-View-Controller separates concerns. Thymeleaf renders server-side HTML.

### Code to Study
- `DashboardController.java` — `@Controller`, returns view name
- `templates/dashboard/index.html` — Thymeleaf `th:text`
- `templates/layout/fragments.html` — Reusable fragments

### Flow
```
Browser → DispatcherServlet → Controller → Model → Thymeleaf → HTML
```

### Interview Questions
1. Difference between `@Controller` and `@RestController`?
2. What is `Model` in Spring MVC?
3. How does Thymeleaf differ from JSP?

---

## Phase 3: JPA + MySQL

### Concept
JPA is the specification; Hibernate is the implementation. Entities map to tables.

### Code to Study
- `entity/Employee.java` — `@Entity`, `@Table`, relationships
- `repository/EmployeeRepository.java` — extends `JpaRepository`
- `db/schema.sql` — Database design

### Key Annotations
`@Entity`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@OneToMany`

### Interview Questions
1. What is the difference between JPA and Hibernate?
2. Explain `@ManyToOne` vs `@OneToMany`
3. What is `ddl-auto=update`?

---

## Phase 4: CRUD Operations

### Concept
Create, Read, Update, Delete through Service layer with transactions.

### Code to Study
- `service/impl/EmployeeServiceImpl.java`
- `mapper/EmployeeMapper.java`
- `dto/EmployeeDTO.java`

### Flow
```
HTML Form → Controller → Service (@Transactional) → Repository → DB
```

### Interview Questions
1. Why use Service layer instead of calling Repository from Controller?
2. What is soft delete?
3. Why use DTO instead of Entity?

---

## Phase 5: AJAX Integration

### Concept
Asynchronous JavaScript updates page without reload.

### Code to Study
- `static/js/employee.js` — `$.ajax()` calls
- `controller/api/EmployeeApiController.java` — JSON responses
- `dto/ApiResponse.java` — Standard response wrapper

### Interview Questions
1. How does AJAX differ from traditional form submit?
2. What is CSRF and why needed in AJAX?
3. Explain `JSON.stringify()` and `@RequestBody`

---

## Phase 6: Validation

### Concept
Validate at multiple layers for defense in depth.

### Code to Study
- `dto/EmployeeDTO.java` — `@NotBlank`, `@Email`, `@Pattern`
- `employee.js` — `form.checkValidity()`
- `GlobalExceptionHandler` — `MethodArgumentNotValidException`

### Interview Questions
1. Difference between `@NotNull`, `@NotBlank`, `@NotEmpty`?
2. What does `@Valid` do?
3. Why backend validation is mandatory?

---

## Phase 7: Spring Security

### Concept
Authentication (who are you) + Authorization (what can you do).

### Code to Study
- `security/SecurityConfig.java`
- `security/CustomUserDetailsService.java`
- `templates/auth/login.html`

### Roles
- `ROLE_ADMIN` — Full access
- `ROLE_MANAGER` — Approve leaves, manage departments
- `ROLE_USER` — Basic access

### Interview Questions
1. How does Spring Security filter chain work?
2. Difference between authentication and authorization?
3. What is BCrypt?

---

## Phase 8: REST APIs

### Concept
RESTful APIs use HTTP methods and status codes.

### Code to Study
- `controller/api/EmployeeApiController.java`
- `postman/EEMS_API.postman_collection.json`

### HTTP Mapping
| Method | Action | Status |
|--------|--------|--------|
| GET | Read | 200 |
| POST | Create | 201 |
| PUT | Update | 200 |
| DELETE | Delete | 200 |

### Interview Questions
1. What is `ResponseEntity`?
2. PUT vs PATCH?
3. How to version REST APIs?

---

## Phase 9: File Upload

### Concept
`MultipartFile` for uploads; store files on disk, metadata in DB.

### Code to Study
- `service/impl/FileStorageServiceImpl.java`
- `controller/api/FileApiController.java`
- `application.properties` — `multipart.max-file-size`

---

## Phase 10: Logging + Exception Handling

### Concept
Logback for structured logging; `@ControllerAdvice` for global errors.

### Code to Study
- `logback-spring.xml`
- `exception/GlobalExceptionHandler.java`

### Log Levels
- **DEBUG** — Development tracing
- **INFO** — Business events
- **WARN** — Recoverable issues
- **ERROR** — Failures requiring attention

---

## Phase 11: Cache + Scheduler + Async

### Concept
- **Cache** — Avoid repeated DB hits
- **Scheduler** — Cron jobs for reports
- **Async** — Non-blocking background tasks

### Code to Study
- `config/CacheConfig.java` — Caffeine setup
- `scheduler/ReportScheduler.java` — `@Scheduled`
- `service/impl/AuditServiceImpl.java` — `@Async`
- `service/impl/EmailServiceImpl.java` — `@Async`

### Interview Questions
1. When NOT to use caching?
2. Explain cron expression `0 0 8 * * *`
3. How does `@Async` work internally?

---

## Phase 12: Testing

### Concept
Unit tests mock dependencies; integration tests verify layers together.

### Code to Study
- `test/.../EmployeeServiceTest.java` — Mockito unit test
- `test/.../EmployeeApiControllerTest.java` — `@WebMvcTest`

### Run
```bash
mvn test
```

### Interview Questions
1. `@MockBean` vs `@Mock`?
2. Difference between unit and integration tests?
3. What does `@WebMvcTest` load?

---

## Phase 13: WAR Deployment

### Concept
Package as WAR for external Tomcat in enterprise environments.

### Code to Study
- `ServletInitializer.java`
- `pom.xml` — `<packaging>war</packaging>`
- `docs/DEPLOYMENT.md`

### Build
```bash
mvn clean package
```

---

## Suggested Study Schedule (4 Weeks)

| Week | Phases | Hours |
|------|--------|-------|
| 1 | 1-4 | 10-15 |
| 2 | 5-8 | 10-15 |
| 3 | 9-11 | 10-15 |
| 4 | 12-13 + Interview Prep | 10-15 |
