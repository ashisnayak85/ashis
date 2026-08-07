# Interview Preparation Guide

---

## Spring Boot

**Q1: What is Spring Boot?**
Spring Boot is an opinionated framework on top of Spring that provides auto-configuration, embedded servers, and starter dependencies to build production-ready applications quickly.

**Q2: How does auto-configuration work?**
Spring Boot checks classpath (e.g., `mysql-connector` present) and conditionally creates beans (DataSource). Controlled by `@ConditionalOnClass`, `@ConditionalOnMissingBean`.

**Q3: Scenario — Application fails to start with "Port already in use"**
Change `server.port` in application.properties or kill the process on port 8080.

**Real-time use case:** Microservices each run on different ports; externalized config via properties.

---

## IoC & Dependency Injection

**Q1: What is IoC?**
Inversion of Control — framework creates and manages objects instead of developer using `new`.

**Q2: Types of DI?**
Constructor (recommended), setter, field injection.

**Q3: Scenario — Circular dependency between ServiceA and ServiceB**
Refactor to extract shared logic to ServiceC, or use `@Lazy` on one dependency.

**Real-time use case:** PaymentService depends on NotificationService which depends on AuditService — all injected by Spring.

---

## MVC

**Q1: Explain DispatcherServlet**
Front controller that routes requests to appropriate `@Controller` based on URL mapping.

**Q2: What is Model?**
Container for data passed from controller to view.

**Q3: Scenario — User submits form, validation fails**
Return same view with error messages in Model; Thymeleaf displays them.

---

## JPA & Hibernate

**Q1: JPA vs Hibernate?**
JPA is specification (interface); Hibernate is implementation.

**Q2: What is N+1 problem?**
Loading N employees triggers N additional queries for departments. Fix with `JOIN FETCH` or `@EntityGraph`.

**Q3: Scenario — LazyInitializationException**
Accessing lazy collection outside transaction. Fix: use DTO, `@Transactional(readOnly=true)`, or fetch join.

**Q4: ddl-auto options?**
`create`, `create-drop`, `update`, `validate`, `none`. Production: `validate` or `none`.

---

## Spring Security

**Q1: Authentication vs Authorization?**
Authentication = identity verification. Authorization = permission check.

**Q2: How does BCrypt work?**
One-way hashing with salt. Same password produces different hashes each time.

**Q3: Scenario — API returns 403 for admin user**
Check role name: `hasRole('ADMIN')` expects `ROLE_ADMIN` in database.

---

## REST API

**Q1: What is idempotency?**
GET, PUT, DELETE are idempotent (same result on repeat). POST is not.

**Q2: ResponseEntity purpose?**
Full control over HTTP status, headers, and body.

**Q3: Scenario — Client sends invalid JSON**
`HttpMessageNotReadableException` → return 400 Bad Request via `@ControllerAdvice`.

---

## AJAX

**Q1: Why CSRF token in AJAX?**
Prevents cross-site request forgery — attacker site can't forge POST without token.

**Q2: JSON request flow?**
JS `JSON.stringify()` → HTTP body → Jackson deserializes → `@RequestBody` DTO.

---

## Validation

**Q1: @NotNull vs @NotBlank?**
`@NotNull`: any non-null value. `@NotBlank`: non-null AND non-whitespace string.

**Q2: Scenario — Duplicate employee email on create**
Service checks `existsByEmail()` → throws `DuplicateResourceException` → 409 Conflict.

---

## Caching

**Q1: When to cache?**
Frequently read, rarely changed data (departments, config).

**Q2: When NOT to cache?**
Real-time data, user-specific sensitive data without proper key isolation.

**Q3: @Cacheable vs @CacheEvict?**
Cacheable stores result; CacheEvict removes entries on update/delete.

---

## Scheduling

**Q1: Cron expression `0 0 8 * * MON-FRI`?**
8:00 AM every weekday.

**Q2: Scenario — Scheduled job runs twice in cluster**
Use ShedLock or Quartz with clustering for distributed lock.

---

## Async

**Q1: How does @Async work?**
Spring creates proxy; method runs in thread pool (`TaskExecutor`).

**Q2: Scenario — @Async not working**
Must call from different bean (not self-invocation); `@EnableAsync` required.

---

## Transactions

**Q1: What does @Transactional do?**
Begins transaction before method, commits on success, rolls back on RuntimeException.

**Q2: readOnly=true benefit?**
Hibernate skips dirty checking; DB can optimize read-only transaction.

**Q3: Scenario — Partial data saved on error**
Transaction not covering all operations. Ensure `@Transactional` on service method encompassing all DB calls.

---

## Testing

**Q1: Unit vs Integration test?**
Unit: mocks dependencies, tests one class. Integration: real/similar context, tests layers together.

**Q2: @WebMvcTest loads what?**
Controller, MVC infrastructure, JSON converters — NOT full service/repository layer.

---

## Scenario-Based Questions (Practice)

1. **Employee delete should soft-delete, not remove from DB.** How implemented?
   → `employee.setActive(false)` in service.

2. **Manager should approve leave but not delete users.** How?
   → `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` on approve; `hasRole('ADMIN')` on delete.

3. **Dashboard loads slow due to repeated department queries.** Fix?
   → `@Cacheable("departments")` on `getAllActive()`.

4. **Welcome email blocks user registration response.** Fix?
   → `@Async` on `sendWelcomeEmail()`.

5. **Postman POST returns 403.** Why?
   → CSRF enabled; disable for API with proper token or use stateless JWT for APIs.

---

## Real-Time Use Cases

| Use Case | Spring Feature |
|----------|---------------|
| HR portal with login | Spring Security + Thymeleaf |
| Mobile app backend | REST Controllers + JWT |
| Nightly payroll report | @Scheduled |
| Profile photo upload | MultipartFile + FileStorage |
| Audit compliance | Async AuditLog |
| High-traffic department list | @Cacheable + Caffeine |

---

*Practice explaining each answer aloud. Interviewers value clarity over memorization.*
