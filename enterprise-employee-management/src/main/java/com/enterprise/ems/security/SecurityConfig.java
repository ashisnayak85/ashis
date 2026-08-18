package com.enterprise.ems.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.List;

/*
 * ================================================================================
 * PURPOSE: Spring Security Configuration (Phase 7)
 * ================================================================================
 * SECURITY FLOW:
 * 1. User submits login form
 * 2. UsernamePasswordAuthenticationFilter intercepts
 * 3. AuthenticationManager validates credentials via UserDetailsService
 * 4. SecurityContext stores authenticated principal
 * 5. Subsequent requests checked against authorization rules
 *
 * ANNOTATION: @EnableWebSecurity - enables Spring Security
 * ANNOTATION: @EnableMethodSecurity - enables @PreAuthorize on methods
 * ================================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Allow the React dev server (and later, wherever the built SPA is hosted) to call this API.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // React can't read a Thymeleaf <meta> tag, so hand the CSRF token out as a
            // readable cookie (XSRF-TOKEN) instead. The frontend echoes it back as the
            // X-XSRF-TOKEN header on POST/PUT/DELETE - standard Spring Security SPA pattern.
            //.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .csrf(csrf -> csrf
            	    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            	    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            	    // The biometric device is a machine, not a browser - it has no session/
            	    // cookie to carry a CSRF token, and it authenticates via the X-Device-Key
            	    // header instead (checked in AttendanceApiController#biometricPunch).
            	    .ignoringRequestMatchers("/api/attendance/biometric")
            	)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                // The biometric machine can't do a session login - it authenticates via the
                // X-Device-Key header checked inside AttendanceApiController#biometricPunch.
                .requestMatchers("/api/attendance/biometric").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**", "/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                // Old behaviour redirected to an HTML page. React needs JSON back so it
                // can decide what to render - these handlers replace the redirects.
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":true,\"username\":\"" + authentication.getName() + "\"}");
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Invalid username or password\"}");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Vite's default dev port is 5173 (CRA would be 3000) - add/replace with your
        // real deployed frontend origin(s) once the SPA is hosted somewhere.
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        // Required so the session cookie (JSESSIONID) and CSRF cookie travel with requests.
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: industry standard password hashing
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
