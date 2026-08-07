package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * =============================================================================
 * INTEGRATION TEST (Phase 12) - EmployeeApiController
 * =============================================================================
 * @WebMvcTest: Loads only MVC layer (controller, filters, advice)
 * @MockBean: Replaces real EmployeeService with mock in test context
 * @WithMockUser: Simulates authenticated user for security
 * =============================================================================
 */
@WebMvcTest(EmployeeApiController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class EmployeeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    @WithMockUser
    void getEmployeeById_ReturnsOk() throws Exception {
        EmployeeDTO dto = EmployeeDTO.builder().id(1L).employeeCode("EMP001").firstName("John").build();
        when(employeeService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeCode").value("EMP001"));
    }

    @Test
    @WithMockUser
    void createEmployee_ReturnsCreated() throws Exception {
        EmployeeDTO dto = EmployeeDTO.builder()
                .employeeCode("EMP100").firstName("Test").lastName("User")
                .email("test@eems.com").departmentId(1L).dateOfJoining(java.time.LocalDate.now())
                .build();

        when(employeeService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
}
