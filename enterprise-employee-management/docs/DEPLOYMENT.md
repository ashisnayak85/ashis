# WAR Deployment Guide (Phase 13)

## Why WAR Deployment?

| JAR (Embedded Tomcat) | WAR (External Tomcat) |
|----------------------|----------------------|
| `java -jar app.jar` | Deploy to shared Tomcat server |
| Good for microservices, cloud | Good for enterprise on-premise |
| Tomcat bundled inside | Tomcat managed by ops team |

## pom.xml Configuration

```xml
<packaging>war</packaging>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>  <!-- Tomcat provided by external server -->
</dependency>
```

## ServletInitializer

`ServletInitializer.java` extends `SpringBootServletInitializer` so external Tomcat can bootstrap Spring Boot.

## Build WAR

```bash
mvn clean package -DskipTests
```

Output: `target/employee-management-system.war`

## Tomcat Deployment Steps

### 1. Install Tomcat 10.1+
Download from https://tomcat.apache.org (Tomcat 10+ for Jakarta EE / Spring Boot 3)

### 2. Configure Tomcat
Edit `conf/server.xml` if needed (port, context)

### 3. Deploy WAR
```bash
# Copy WAR to Tomcat webapps folder
copy target\employee-management-system.war C:\apache-tomcat-10.1\webapps\

# Or rename for root context
copy target\employee-management-system.war C:\apache-tomcat-10.1\webapps\eems.war
```

### 4. Configure Production Properties
Create `application-prod.properties`:
```properties
spring.datasource.url=jdbc:mysql://prod-server:3306/eems_db
spring.jpa.hibernate.ddl-auto=validate
spring.thymeleaf.cache=true
logging.level.root=WARN
```

Run with profile:
```bash
set JAVA_OPTS=-Dspring.profiles.active=prod
```

### 5. Start Tomcat
```bash
C:\apache-tomcat-10.1\bin\startup.bat
```

### 6. Access Application
- http://localhost:8080/employee-management-system/
- Or http://localhost:8080/eems/ (if renamed)

## Common Deployment Mistakes

1. **Missing ServletInitializer** — WAR won't start Spring context
2. **Tomcat 9 with Spring Boot 3** — Use Tomcat 10+ (Jakarta namespace)
3. **ddl-auto=create in production** — Use `validate` or `none`
4. **Hardcoded credentials** — Use environment variables
5. **Missing MySQL driver on Tomcat** — Driver is inside WAR (OK for Spring Boot WAR)

## Interview Questions

**Q: Difference between JAR and WAR deployment in Spring Boot?**
A: JAR uses embedded Tomcat started by `SpringApplication.run()`. WAR uses external servlet container; `SpringBootServletInitializer` bridges the container lifecycle to Spring Boot.

**Q: Why is spring-boot-starter-tomcat scope `provided`?**
A: External Tomcat already provides servlet container classes. Including them would cause classloader conflicts.
