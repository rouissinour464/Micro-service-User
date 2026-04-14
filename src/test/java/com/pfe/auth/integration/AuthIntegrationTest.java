package com.pfe.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.auth.dto.auth.*;
import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.entity.AdminCode;
import com.pfe.auth.entity.Role;
import com.pfe.auth.entity.RoleName;
import com.pfe.auth.repository.AdminCodeRepository;
import com.pfe.auth.repository.RoleRepository;
import com.pfe.auth.repository.UserRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminCodeRepository adminCodeRepository;
    @Autowired private RoleRepository roleRepository;

    private static String adminAccessToken;

    // ============================================================
    // ✅ 0. SETUP — Exécuté AVANT CHAQUE TEST
    // ============================================================
    @BeforeEach
    void setup() {

        // ❌ Ne PAS supprimer les users (sinon login échoue)
        // ❌ Ne PAS supprimer les rôles (cause FK violation)

        // ✅ Supprimer SEULEMENT les codes admin
        adminCodeRepository.deleteAll();

        // ✅ Réinjecter le code admin pour register-admin test
        AdminCode ac = new AdminCode();
        ac.setCode("TEST-CODE-001");
        ac.setUsed(false);
        adminCodeRepository.save(ac);

        // ✅ Ajouter les rôles UNE SEULE FOIS (si table vide)
        if (roleRepository.count() == 0) {

            Role r1 = new Role();
            r1.setName(RoleName.ROLE_ADMIN);
            roleRepository.save(r1);

            Role r2 = new Role();
            r2.setName(RoleName.ROLE_ENCADRANT);
            roleRepository.save(r2);

            Role r3 = new Role();
            r3.setName(RoleName.ROLE_ETUDIANT);
            roleRepository.save(r3);
        }
    }

    // ============================================================
    // ✅ 1. REGISTER ADMIN
    // ============================================================
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

        AuthResponse response = mapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );

        adminAccessToken = response.getToken();

        assertThat(adminCodeRepository.findByCode("TEST-CODE-001").get().isUsed()).isTrue();
    }

    // ============================================================
    // ✅ 2. LOGIN (ADMIN)
    // ============================================================
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

        AuthResponse response = mapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );

        adminAccessToken = response.getToken();
    }

    // ============================================================
    // ✅ 3. CREATE TEACHER (ADMIN ONLY)
    // ============================================================
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

    // ============================================================
    // ✅ 4. PROFILE
    // ============================================================
    @Test
    @Order(4)
    void getProfile_success() throws Exception {

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"));
    }

    // ============================================================
    // ✅ 5. PROFILE WITHOUT TOKEN → Forbidden
    // ============================================================
    @Test
    @Order(5)
    void getProfile_withoutToken_forbidden() throws Exception {

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isForbidden());
    }
}