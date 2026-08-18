package com.enterprise.ems.exception;

import com.enterprise.ems.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * ================================================================================
 * PURPOSE: Global Exception Handler (Phase 10)
 * ================================================================================
 * ANNOTATION: @ControllerAdvice - intercepts exceptions across ALL controllers
 * ANNOTATION: @ExceptionHandler - maps exception type to handler method
 *
 * FLOW: Exception thrown in Service -> propagates to Controller ->
 *       GlobalExceptionHandler catches it -> returns JSON or error page
 *
 * INTERVIEW Q: @ControllerAdvice vs @ExceptionHandler in controller?
 * A: @ControllerAdvice is global; per-controller handlers are local only.
 * ================================================================================
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }
        return new ModelAndView("error/404").addObject("message", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Object handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ex.getMessage()));
        }
        return new ModelAndView("error/error").addObject("message", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage()));
        }
        return new ModelAndView("error/error").addObject("message", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        // fieldErrors: {"email": "Invalid email format", ...} - lets the frontend
        // highlight the exact input, not just show a generic banner.
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
            messages.add(fe.getDefaultMessage());
        }
        log.warn("Validation failed: {}", fieldErrors);
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Please fix the highlighted field" + (fieldErrors.size() > 1 ? "s" : ""))
                .data(fieldErrors)
                .errors(messages)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
        return new ModelAndView("error/error").addObject("message", "An unexpected error occurred");
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return path.startsWith("/api/") ||
               (accept != null && accept.contains("application/json"));
    }
}
