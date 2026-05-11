package com.pfe.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.auth.dto.auth.*;
import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.entity.AdminCode;
import com.pfe.auth.entity.Role;
import com.pfe.auth.entity.RoleName;
import com.pfe.auth.repository.AdminCodeRepository;
import com.pfe.auth.repository.RoleRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false) // ✅ FIX CRITIQUE
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AdminCodeRepository adminCodeRepository;
    @Autowired private RoleRepository roleRepository;

    private static String adminAccessToken;

    @BeforeEach
    void setup() {

        adminCodeRepository.deleteAll();

        AdminCode code = new AdminCode();
        code.setCode("TEST-CODE-001");
        code.setUsed(false);
        adminCodeRepository.save(code);

        if (roleRepository.count() == 0) {
            Role admin = new Role();
            admin.setName(RoleName.ROLE_ADMIN);
            roleRepository.save(admin);

            Role enc = new Role();
            enc.setName(RoleName.ROLE_ENCADRANT);
            roleRepository.save(enc);

            Role etu = new Role();
            etu.setName(RoleName.ROLE_ETUDIANT);
            roleRepository.save(etu);
        }
    }

    @Test
    @Order(1)
    void registerAdmin_success() throws Exception {

        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setFullName("Admin Test");
        req.setEmail("admin@test.com");
        req.setPassword("admin123");
        req.setAdminCode("TEST-CODE-001");

        var result = mockMvc.perform(post("/api/auth/register-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        adminAccessToken = mapper
                .readValue(result.getResponse().getContentAsString(), AuthResponse.class)
                .getToken();
    }

    @Test
    @Order(2)
    void login_success() throws Exception {

        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("admin123");

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        adminAccessToken = mapper
                .readValue(result.getResponse().getContentAsString(), AuthResponse.class)
                .getToken();
    }

    @Test
    @Order(3)
    void createTeacher_success() throws Exception {

        CreateUserRequest req = new CreateUserRequest();
        req.setFullName("Prof Test");
        req.setEmail("prof@test.com");
        req.setPassword("prof123");
        req.setRole("TEACHER");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("prof@test.com"))
                .andExpect(jsonPath("$.role").value("ROLE_ENCADRANT"));
    }

    @Test
    @Order(4)
    void getProfile_success() throws Exception {

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"));
    }

    @Test
    @Order(5)
    void getProfile_withoutToken_forbidden() throws Exception {

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isForbidden());
    }
}
