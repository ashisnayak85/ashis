package com.enterprise.ems.service.hiring;

import com.enterprise.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * PURPOSE: Generates the next employee code (EMP001, EMP002, ...) when a hire is
 * converted to an Employee, so HR never has to type or mistype one.
 * Looks at existing codes with the prefix, takes the highest number and adds one.
 * The unique constraint on employee.employee_code is still the final safety net.
 */
@Component
@RequiredArgsConstructor
public class EmployeeCodeGenerator {

    private static final String PREFIX = "EMP";
    private static final Pattern NUMBERED = Pattern.compile("^" + PREFIX + "(\\d+)$");

    private final EmployeeRepository employeeRepository;

    public String next() {
        long max = 0;
        for (String code : employeeRepository.findEmployeeCodesByPrefix(PREFIX)) {
            Matcher m = NUMBERED.matcher(code);
            if (m.matches()) {
                try {
                    max = Math.max(max, Long.parseLong(m.group(1)));
                } catch (NumberFormatException ignored) {
                    // absurdly long numeric suffix - ignore it
                }
            }
        }
        long candidate = max + 1;
        String code = format(candidate);
        while (employeeRepository.existsByEmployeeCode(code)) {
            code = format(++candidate);
        }
        return code;
    }

    private String format(long number) {
        return PREFIX + String.format("%03d", number);
    }
}
