package com.pfe.auth.unit;

import com.pfe.auth.dto.auth.*;
import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.dto.user.UserInfo;
import com.pfe.auth.entity.AdminCode;
import com.pfe.auth.entity.Role;
import com.pfe.auth.entity.RoleName;
import com.pfe.auth.entity.User;
import com.pfe.auth.repository.AdminCodeRepository;
import com.pfe.auth.repository.RoleRepository;
import com.pfe.auth.repository.UserRepository;
import com.pfe.auth.repository.RefreshTokenRepository;
import com.pfe.auth.security.JwtUtils;
import com.pfe.auth.service.AuthService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("✅ Tests unitaires PRO – AuthService")
class AuthServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminCodeRepository adminCodeRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User admin;
    private Role adminRole;
    private Role teacherRole;
    private AdminCode adminCode;

    @BeforeEach
    void setup() {

        adminRole = new Role();
        adminRole.setId(1);
        adminRole.setName(RoleName.ROLE_ADMIN);

        teacherRole = new Role();
        teacherRole.setId(2);
        teacherRole.setName(RoleName.ROLE_ENCADRANT);

        admin = new User();
        admin.setId(1L);
        admin.setFullName("Admin Test");
        admin.setEmail("admin@pfe.dz");
        admin.setPassword("HASHED");
        admin.setRole(adminRole);

        adminCode = new AdminCode();
        adminCode.setId(1L);
        adminCode.setCode("FAC-ADMIN-001");
        adminCode.setUsed(false);
    }

    // ======================================================
    // ✅ REGISTER ADMIN
    // ======================================================
    @Test
    void registerAdmin_success() {

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(adminCodeRepository.findByCode(any())).thenReturn(Optional.of(adminCode));
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(any())).thenReturn("HASHED");

        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateToken(anyLong(), anyString(), any(Role.class)))
                .thenReturn("TOKEN123");

        AdminRegisterRequest req = new AdminRegisterRequest(
                "Admin Test",
                "admin@pfe.dz",
                "password123",
                "FAC-ADMIN-001"
        );

        AuthResponse res = authService.registerAdmin(req);

        assertThat(res).isNotNull();
        assertThat(res.getToken()).isEqualTo("TOKEN123");
        assertThat(res.getRole()).isEqualTo("ROLE_ADMIN");

        verify(refreshTokenRepository).save(any());
        verify(adminCodeRepository).save(argThat(AdminCode::isUsed));
    }

    // ======================================================
    // ✅ LOGIN
    // ======================================================
    @Test
    void login_success() {

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtils.generateToken(anyLong(), anyString(), any(Role.class)))
                .thenReturn("TOKEN123");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = new LoginRequest("admin@pfe.dz", "password123");

        AuthResponse res = authService.login(req);

        assertThat(res.getToken()).isEqualTo("TOKEN123");
    }

    // ======================================================
    // ✅ CREATE USER
    // ======================================================
    @Test
    void createTeacher_success() {

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_ENCADRANT))
                .thenReturn(Optional.of(teacherRole));
        when(passwordEncoder.encode(any())).thenReturn("HASHED");

        User teacher = new User();
        teacher.setId(2L);
        teacher.setEmail("teacher@test.com");
        teacher.setRole(teacherRole);

        when(userRepository.save(any())).thenReturn(teacher);

        CreateUserRequest req = new CreateUserRequest(
                "Teacher Test",
                "teacher@test.com",
                "pass123",
                "TEACHER"
        );

        UserInfo info = authService.createUser(req, "admin@pfe.dz");

        assertThat(info.getEmail()).isEqualTo("teacher@test.com");
        assertThat(info.getRole()).isEqualTo("ROLE_ENCADRANT");
    }

    // ======================================================
    // ✅ LOGOUT
    // ======================================================
    @Test
    void logout_success() {

        doNothing().when(refreshTokenRepository)
                .deleteByUserEmail(anyString());

        authService.logout("admin@pfe.dz");

        verify(refreshTokenRepository)
                .deleteByUserEmail("admin@pfe.dz");
    }
}
