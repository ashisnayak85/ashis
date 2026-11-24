# Annotation Reference Guide

For every annotation: Purpose, Syntax, Example, Industry Use Case, If Removed, Best Practices, Interview Question.

---

## SPRING CORE

### @Component
| | |
|---|---|
| **Purpose** | Generic stereotype — marks class as Spring-managed bean |
| **Syntax** | `@Component` on class |
| **Example** | `@Component public class EmployeeMapper` |
| **Industry Use** | Utility classes, mappers not fitting other stereotypes |
| **If Removed** | Class not registered in IoC container; `@Autowired` fails |
| **Best Practice** | Prefer specific stereotypes (`@Service`, `@Repository`) |
| **Interview Q** | Difference between `@Component` and `@Bean`? |

### @Service
| | |
|---|---|
| **Purpose** | Business logic layer stereotype |
| **Syntax** | `@Service` on class |
| **Example** | `@Service public class EmployeeServiceImpl` |
| **Industry Use** | All business logic, transactions, validation |
| **If Removed** | Not a bean; injection into controllers fails |
| **Best Practice** | Interface + Impl pattern for testability |
| **Interview Q** | Why `@Service` instead of `@Component`? |

### @Repository
| | |
|---|---|
| **Purpose** | Data access layer stereotype |
| **Syntax** | `@Repository` on interface/class |
| **Example** | `@Repository public interface EmployeeRepository` |
| **Industry Use** | JPA repositories, DAO classes |
| **If Removed** | Repository not created; DB access fails |
| **Best Practice** | Extend `JpaRepository<Entity, ID>` |
| **Interview Q** | What exception translation does `@Repository` provide? |

### @Controller
| | |
|---|---|
| **Purpose** | MVC controller returning view names |
| **Syntax** | `@Controller` on class |
| **Example** | `@Controller public class DashboardController` |
| **Industry Use** | Server-rendered pages with Thymeleaf/JSP |
| **If Removed** | Request mappings not registered |
| **Best Practice** | Keep thin — delegate to service |
| **Interview Q** | `@Controller` vs `@RestController`? |

### @RestController
| | |
|---|---|
| **Purpose** | `@Controller` + `@ResponseBody` — returns JSON/XML |
| **Syntax** | `@RestController` on class |
| **Example** | `@RestController @RequestMapping("/api/employees")` |
| **Industry Use** | REST APIs, AJAX backends |
| **If Removed** | Returns view name instead of serialized body |
| **Best Practice** | Use `ResponseEntity<T>` for status control |
| **Interview Q** | How to return custom HTTP status with `@RestController`? |

### @Configuration
| | |
|---|---|
| **Purpose** | Marks class as source of bean definitions |
| **Syntax** | `@Configuration` on class |
| **Example** | `@Configuration public class SecurityConfig` |
| **Industry Use** | Security, cache, third-party integrations |
| **If Removed** | `@Bean` methods not processed |
| **Best Practice** | One config class per concern |
| **Interview Q** | `@Configuration` vs `@Component`? |

### @Bean
| | |
|---|---|
| **Purpose** | Declares a method produces a bean managed by Spring |
| **Syntax** | `@Bean` on method in `@Configuration` class |
| **Example** | `@Bean public PasswordEncoder passwordEncoder()` |
| **Industry Use** | Third-party classes you can't annotate |
| **If Removed** | Bean not available for injection |
| **Best Practice** | Method name becomes default bean name |
| **Interview Q** | `@Bean` vs `@Component`? |

### @ComponentScan
| | |
|---|---|
| **Purpose** | Tells Spring where to scan for components |
| **Syntax** | `@ComponentScan(basePackages = "com.enterprise")` |
| **Example** | Included in `@SpringBootApplication` |
| **Industry Use** | Multi-module projects with custom packages |
| **If Removed** | Beans outside default package not found |
| **Best Practice** | Default scan is package of `@SpringBootApplication` |
| **Interview Q** | How to scan multiple packages? |

### @PropertySource
| | |
|---|---|
| **Purpose** | Load properties from custom file |
| **Syntax** | `@PropertySource("classpath:custom.properties")` |
| **Example** | On `@Configuration` class |
| **Industry Use** | Legacy property files, module-specific config |
| **If Removed** | Custom properties not loaded |
| **Best Practice** | Prefer `application.properties` / YAML in Boot |
| **Interview Q** | How does Spring Boot load properties without `@PropertySource`? |

### @Value
| | |
|---|---|
| **Purpose** | Inject property value into field/parameter |
| **Syntax** | `@Value("${app.upload.dir}")` |
| **Example** | `@Value("${server.port:8080}") private int port;` |
| **Industry Use** | Feature flags, configurable paths |
| **If Removed** | Must hardcode values |
| **Best Practice** | Use `@ConfigurationProperties` for groups |
| **Interview Q** | `@Value` vs `@ConfigurationProperties`? |

### @Primary
| | |
|---|---|
| **Purpose** | Preferred bean when multiple candidates exist |
| **Syntax** | `@Primary` on bean definition |
| **Example** | `@Primary @Bean public CacheManager cacheManager()` |
| **Industry Use** | Multiple implementations of same interface |
| **If Removed** | `NoUniqueBeanDefinitionException` |
| **Best Practice** | Combine with `@Qualifier` for clarity |
| **Interview Q** | `@Primary` vs `@Qualifier`? |

### @Qualifier
| | |
|---|---|
| **Purpose** | Specify exact bean by name when multiple exist |
| **Syntax** | `@Qualifier("beanName")` with `@Autowired` |
| **Example** | `@Autowired @Qualifier("mysqlDataSource")` |
| **Industry Use** | Multi-datasource, strategy pattern |
| **If Removed** | Ambiguous injection failure |
| **Best Practice** | Use meaningful bean names |
| **Interview Q** | When to use `@Qualifier` over `@Primary`? |

### @Autowired
| | |
|---|---|
| **Purpose** | Automatic dependency injection |
| **Syntax** | On field, constructor, or setter |
| **Example** | `@Autowired private EmployeeService employeeService;` |
| **Industry Use** | Everywhere Spring manages dependencies |
| **If Removed** | Must manually instantiate dependencies |
| **Best Practice** | Constructor injection (immutable, testable) |
| **Interview Q** | Types of dependency injection in Spring? |

---

## SPRING BOOT

### @SpringBootApplication
Combines `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.

**Interview Q:** What are its three constituent annotations?

### @EnableAutoConfiguration
Enables Spring Boot auto-config based on classpath.

**If Removed:** Must manually configure DataSource, MVC, etc.

### @ConfigurationProperties
Binds property prefix to POJO: `@ConfigurationProperties(prefix = "app")`.

**Best Practice:** Type-safe alternative to multiple `@Value` annotations.

---

## SPRING MVC

### @RequestMapping
Maps HTTP requests to handler. Can be on class and method.

### @GetMapping / @PostMapping / @PutMapping / @DeleteMapping
HTTP method-specific shortcuts for `@RequestMapping(method=...)`.

### @PathVariable
Extract URI variable: `@GetMapping("/{id}")` + `@PathVariable Long id`.

### @RequestParam
Extract query parameter: `@RequestParam(defaultValue="0") int page`.

### @RequestBody
Deserialize HTTP body to Java object (JSON → DTO).

### @ResponseBody
Serialize return value to HTTP body (used implicitly in `@RestController`).

### @ModelAttribute
Bind form data to object for MVC (not REST JSON).

---

## VALIDATION

### @Valid
Triggers validation on annotated object: `create(@Valid @RequestBody EmployeeDTO dto)`.

### @NotNull
Field must not be null (null fails, empty string passes).

### @NotBlank
String must have non-whitespace characters.

### @NotEmpty
Collection/array/string must not be null or empty.

### @Size
`@Size(min=2, max=50)` — length constraint.

### @Email
Valid email format.

### @Pattern
Regex: `@Pattern(regexp="^[A-Z0-9]+$")`.

### @Min / @Max
Numeric bounds.

---

## JPA

### @Entity
Marks class as JPA entity mapped to database table.

### @Table
Custom table name: `@Table(name = "employee")`.

### @Id
Primary key field.

### @GeneratedValue
Auto-generate PK: `strategy = GenerationType.IDENTITY` for MySQL auto-increment.

### @Column
Column mapping: name, nullable, unique, length.

### @Transient
Field not persisted to database.

### @OneToOne / @OneToMany / @ManyToOne / @ManyToMany
Relationship mappings. `@ManyToOne` + `@JoinColumn` is most common (Employee → Department).

### @JoinColumn
FK column: `@JoinColumn(name = "department_id")`.

### @JoinTable
Join table for many-to-many (users ↔ roles).

---

## HIBERNATE

### @CreationTimestamp
Auto-set on insert.

### @UpdateTimestamp
Auto-update on modification.

---

## LOMBOK

### @Data
Generates getter, setter, toString, equals, hashCode.

### @Getter / @Setter
Individual accessor generation.

### @NoArgsConstructor / @AllArgsConstructor
Constructor generation (JPA requires no-arg constructor).

### @Builder
Builder pattern: `Employee.builder().firstName("John").build()`.

### @ToString
Generates toString (exclude lazy collections in entities).

---

## TRANSACTION

### @Transactional
Wraps method in database transaction. Rolls back on RuntimeException.

**Best Practice:** Apply on service layer, not controller or repository.

**Interview Q:** What is transaction propagation?

---

## SECURITY

### @EnableWebSecurity
Enables Spring Security filter chain.

### @PreAuthorize
Method-level security: `@PreAuthorize("hasRole('ADMIN')")`.

### @Secured
Role check: `@Secured("ROLE_ADMIN")` (legacy).

### @RolesAllowed
JSR-250 annotation: `@RolesAllowed("ADMIN")`.

---

## ASYNC / SCHEDULER / CACHE

### @EnableAsync / @Async
Background thread execution for non-blocking operations.

### @EnableScheduling / @Scheduled
Cron jobs: `@Scheduled(cron = "0 0 8 * * *")`.

### @EnableCaching / @Cacheable / @CacheEvict
Cache read/write/evict operations.

---

## EXCEPTION

### @ControllerAdvice
Global exception handler across all controllers.

### @ExceptionHandler
Maps exception type to handler method within advice or controller.

---

## TESTING

### @SpringBootTest
Full application context integration test.

### @WebMvcTest
MVC layer slice test (controller only).

### @MockBean
Adds mock to Spring test context (replaces real bean).

**Interview Q:** `@MockBean` vs `@Mock` with Mockito?

---

*See INTERVIEW_PREP.md for scenario-based questions on each topic.*
