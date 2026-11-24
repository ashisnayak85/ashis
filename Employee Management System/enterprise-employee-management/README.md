# Enterprise Employee Management System (EEMS)

A **complete Spring Boot learning project** designed to take you from JSP/Servlet background to enterprise-grade Spring Boot development.

**Project Location:** `C:\Users\ashis\enterprise-employee-management`

---

## Quick Start

### Prerequisites
- Java 21 (JDK)
- Maven 3.9+
- MySQL 8.0+
- IDE: IntelliJ IDEA or VS Code with Java extensions

### Setup Steps

1. **Create MySQL database** (optional — app auto-creates with `createDatabaseIfNotExist=true`):
   ```sql
   source src/main/resources/db/schema.sql
   ```

2. **Configure database** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=YOUR_PASSWORD
   ```

3. **Build and run**:
   ```bash
   cd C:\Users\ashis\enterprise-employee-management
   mvn clean install
   mvn spring-boot:run
   ```

4. **Open browser**: http://localhost:8080

5. **Login credentials** (auto-seeded on first run):
   | Username | Password   | Role    |
   |----------|------------|---------|
   | admin    | admin123   | ADMIN   |
   | manager  | manager123 | MANAGER |

---

## Documentation Index

| Document | Description |
|----------|-------------|
| [docs/COMPLETE_GUIDE.md](docs/COMPLETE_GUIDE.md) | Full architecture, flows, diagrams |
| [docs/PHASES.md](docs/PHASES.md) | 13-phase learning roadmap |
| [docs/ANNOTATION_REFERENCE.md](docs/ANNOTATION_REFERENCE.md) | Every annotation explained |
| [docs/INTERVIEW_PREP.md](docs/INTERVIEW_PREP.md) | FAQs and scenario questions |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | WAR deployment to Tomcat |
| [postman/EEMS_API.postman_collection.json](postman/EEMS_API.postman_collection.json) | REST API collection |

---

## Technology Stack

Java 21 | Spring Boot 3.4 | Spring MVC | Spring Data JPA | Hibernate | MySQL | Thymeleaf | Bootstrap 5 | jQuery | Spring Security | Maven | Lombok | JUnit | Mockito

---

## Project Structure

```
src/main/java/com/enterprise/ems/
├── config/          # Configuration beans (cache, MVC, data init)
├── controller/      # MVC controllers (return Thymeleaf views)
├── controller/api/  # REST controllers (return JSON)
├── service/         # Business logic interfaces
├── service/impl/    # Service implementations
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA/Hibernate entities
├── dto/             # Data Transfer Objects
├── mapper/          # Entity ↔ DTO conversion
├── exception/       # Custom exceptions + @ControllerAdvice
├── security/        # Spring Security configuration
├── scheduler/       # @Scheduled tasks
├── cache/           # (in config/) Caching setup
├── constant/        # Application constants
└── util/            # Utility classes
```

---

## Modules Implemented

1. Authentication (Spring Security form login)
2. Dashboard (statistics)
3. Employee Management (full CRUD + AJAX)
4. Department Management
5. Attendance Management
6. Leave Management
7. User Management (Admin)
8. Profile Management
9. File Upload
10. REST API Module
11. AJAX Module (jQuery)
12. Reporting (Scheduler)
13. Email Module
14. Cache Module (Caffeine)
15. Audit Logging (Async)

---

## Learning Path

Follow the phases in order: **docs/PHASES.md**

Each phase builds on the previous one. Read the code comments — every important class explains its purpose, annotations, and interview questions.

---

## Run Tests

```bash
mvn test
```

---

## Build WAR for Tomcat

```bash
mvn clean package -DskipTests
# Output: target/employee-management-system.war
```

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for Tomcat deployment steps.
