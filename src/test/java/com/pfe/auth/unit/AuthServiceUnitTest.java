package com.pfe.auth.unit;

import com.pfe.auth.dto.auth.*;
import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.entity.AdminCode;
import com.pfe.auth.entity.Role;
import com.pfe.auth.entity.RoleName;
import com.pfe.auth.entity.User;
import com.pfe.auth.repository.AdminCodeRepository;
import com.pfe.auth.repository.RoleRepository;
import com.pfe.auth.repository.UserRepository;
import com.pfe.auth.security.JwtUtils;
import com.pfe.auth.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pfe.auth.dto.user.UserInfo;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - AuthService")
class AuthServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminCodeRepository adminCodeRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtUtils jwt;

    @InjectMocks
    private AuthService authService;

    private User adminUser;
    private AdminCode validCode;
    private Role adminRole;
    private Role teacherRole;

    @BeforeEach
    void setup() {

        // ✅ Préparation Roles
        adminRole = new Role();
        adminRole.setId(1);
        adminRole.setName(RoleName.ROLE_ADMIN);

        teacherRole = new Role();
        teacherRole.setId(2);
        teacherRole.setName(RoleName.ROLE_ENCADRANT);

        // ✅ Préparation User admin
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFullName("Admin Test");
        adminUser.setEmail("admin@pfe.dz");
        adminUser.setPassword("HASHED");
        adminUser.setRole(adminRole);

        // ✅ Code Admin FAC
        validCode = new AdminCode();
        validCode.setId(1L);
        validCode.setCode("FAC-ADMIN-001");
        validCode.setUsed(false);
    }

    //══════════════════════════════════
    // ✅ REGISTER ADMIN
    //══════════════════════════════════
    @Nested
    @DisplayName("registerAdmin()")
    class RegisterAdmin {

        private AdminRegisterRequest req() {
            AdminRegisterRequest r = new AdminRegisterRequest();
            r.setFullName("Admin Test");
            r.setEmail("admin@pfe.dz");
            r.setPassword("password123");
            r.setAdminCode("FAC-ADMIN-001");
            return r;
        }

        @Test
        @DisplayName("✅ Inscription réussie avec code valide")
        void success() {

            when(userRepository.existsByEmail("admin@pfe.dz")).thenReturn(false);
            when(adminCodeRepository.findByCode("FAC-ADMIN-001")).thenReturn(Optional.of(validCode));
            when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
            when(encoder.encode(any())).thenReturn("HASHED");
            when(userRepository.save(any())).thenReturn(adminUser);
            when(jwt.generateToken(any(), any())).thenReturn("TOKEN123");

            AuthResponse res = authService.registerAdmin(req());

            assertThat(res.getToken()).isEqualTo("TOKEN123");
            assertThat(res.getRole()).isEqualTo("ROLE_ADMIN");
            verify(adminCodeRepository).save(argThat(AdminCode::isUsed));
        }

        @Test
        @DisplayName("❌ Email déjà utilisé")
        void emailExists() {
            when(userRepository.existsByEmail("admin@pfe.dz")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerAdmin(req()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email déjà utilisé");
        }

        @Test
        @DisplayName("❌ Code admin invalide")
        void invalidCode() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(adminCodeRepository.findByCode(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerAdmin(req()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Code admin invalide");
        }
    }

    //══════════════════════════════════
    // ✅ LOGIN
    //══════════════════════════════════
    @Nested
    @DisplayName("login()")
    class LoginTest {

        private LoginRequest req(String pass) {
            LoginRequest r = new LoginRequest();
            r.setEmail("admin@pfe.dz");
            r.setPassword(pass);
            return r;
        }

        @Test
        @DisplayName("✅ Login OK")
        void success() {
            when(userRepository.findByEmail("admin@pfe.dz")).thenReturn(Optional.of(adminUser));
            when(encoder.matches("password123", "HASHED")).thenReturn(true);
            when(jwt.generateToken(any(), any())).thenReturn("TOKEN123");

            AuthResponse res = authService.login(req("password123"));

            assertThat(res.getToken()).isEqualTo("TOKEN123");
        }

        @Test
        @DisplayName("❌ Email introuvable")
        void emailNotFound() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req("pass")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }

        @Test
        @DisplayName("❌ Mot de passe incorrect")
        void wrongPassword() {
            when(userRepository.findByEmail("admin@pfe.dz")).thenReturn(Optional.of(adminUser));
            when(encoder.matches(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req("wrongpass")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("incorrect");
        }
    }

    //══════════════════════════════════
    // ✅ CREATE USER
    //══════════════════════════════════
    @Nested
    @DisplayName("createUser()")
    class CreateUserTest {

        private CreateUserRequest req(String role) {
            CreateUserRequest r = new CreateUserRequest();
            r.setFullName("User Test");
            r.setEmail("user@test.com");
            r.setPassword("pass1234");
            r.setRole(role);
            return r;
        }

        @Test
        @DisplayName("✅ Admin crée un TEACHER")
        void createTeacher() {

            User teacher = new User();
            teacher.setId(2L);
            teacher.setFullName("User Test");
            teacher.setEmail("user@test.com");
            teacher.setPassword("HASHED");
            teacher.setRole(teacherRole);

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(roleRepository.findByName(RoleName.ROLE_ENCADRANT)).thenReturn(Optional.of(teacherRole));
            when(encoder.encode(any())).thenReturn("HASHED");
            when(userRepository.save(any())).thenReturn(teacher);

            UserInfo info = authService.createUser(req("TEACHER"), "admin@pfe.dz");

            assertThat(info.getRole()).isEqualTo("ROLE_ENCADRANT");
            assertThat(info.getEmail()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("❌ Rôle invalide")
        void invalidRole() {
            when(userRepository.existsByEmail(any())).thenReturn(false);

            assertThatThrownBy(() -> authService.createUser(req("SUPERADMIN"), "admin@pfe.dz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rôle invalide");
        }
    }

    //══════════════════════════════════
    // ✅ LOGOUT
    //══════════════════════════════════
    @Test
    @DisplayName("✅ Logout ne fait rien (stateless)")
    void logout_ok() {
        authService.logout("admin@pfe.dz");
        assertThat(true).isTrue();
    }
}