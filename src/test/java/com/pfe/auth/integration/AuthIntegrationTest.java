package com.pfe.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.auth.dto.auth.*;
import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.entity.*;
import com.pfe.auth.repository.AdminCodeRepository;
import com.pfe.auth.repository.RoleRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AdminCodeRepository adminCodeRepository;
    @Autowired private RoleRepository roleRepository;

    // ================= SETUP =================
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

    // ================= 1. REGISTER ADMIN =================
    @Test
    @Order(1)
    void registerAdmin_success() throws Exception {

        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setEmail("admin@test.com");
        req.setPassword("123");
        req.setAdminCode("TEST-CODE-001");

        mockMvc.perform(post("/api/auth/register-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }

    // ================= 2. LOGIN =================
    @Test
    @Order(2)
    void login_success() throws Exception {

        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    // ================= 3. CREATE USER =================
    @Test
    @Order(3)
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void createTeacher_success() throws Exception {

        CreateUserRequest req = new CreateUserRequest();
        req.setFullName("Teacher Test");
        req.setEmail("teacher@test.com");
        req.setPassword("123");
        req.setRole("TEACHER");

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("teacher@test.com"));
    }

    // ================= 4. PROFILE =================
    @Test
    @Order(4)
    @WithMockUser(username = "admin@test.com")
    void getProfile_success() throws Exception {

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isOk());
    }

    // ================= 5. PROFILE WITHOUT USER =================
    @Test
    @Order(5)
    void getProfile_withoutToken_forbidden() throws Exception {

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isForbidden());
    }
}
