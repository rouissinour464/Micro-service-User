package com.pfe.auth.service;

import com.pfe.auth.dto.auth.*;
import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.dto.user.UpdateProfileRequest;
import com.pfe.auth.dto.user.UpdateUserRequest;
import com.pfe.auth.dto.user.UserInfo;
import com.pfe.auth.entity.AdminCode;
import com.pfe.auth.entity.Role;
import com.pfe.auth.entity.RoleName;
import com.pfe.auth.entity.User;
import com.pfe.auth.repository.AdminCodeRepository;
import com.pfe.auth.repository.RoleRepository;
import com.pfe.auth.repository.UserRepository;
import com.pfe.auth.security.JwtUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final AdminCodeRepository adminCodeRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwt;

    // ======================= AUTHENTIFICATION =======================

    @Transactional
    public AuthResponse registerAdmin(AdminRegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }

        String cleanCode = req.getAdminCode()
                .trim()
                .toUpperCase()
                .replace("–", "-")
                .replace("—", "-")
                .replace("‑", "-")
                .replaceAll("\\s+", "");

        AdminCode ac = adminCodeRepo.findByCode(cleanCode)
                .orElseThrow(() -> new IllegalArgumentException("Code admin invalide"));

        if (ac.isUsed()) {
            throw new IllegalArgumentException("Ce code admin est déjà utilisé");
        }

        Role adminRole = roleRepo.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Role ADMIN introuvable"));

        User admin = new User();
        admin.setFullName(req.getFullName());
        admin.setEmail(req.getEmail());
        admin.setPassword(encoder.encode(req.getPassword()));
        admin.setRole(adminRole);

        userRepo.save(admin);

        ac.setUsed(true);
        adminCodeRepo.save(ac);

        String token = jwt.generateToken(admin.getEmail(), admin.getRole());

        return new AuthResponse(token, "NO_REFRESH", admin.getRole().getName().name());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }

        String token = jwt.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, "NO_REFRESH", user.getRole().getName().name());
    }

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        return new AuthResponse("NEW_TOKEN", "NO_REFRESH", "NONE");
    }

    public void logout(String email) {
        // JWT stateless — rien à faire
    }

    // ======================= PROFILE =======================

    public ProfileResponse getProfile(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        ProfileResponse p = new ProfileResponse();
        p.setId(user.getId());
        p.setFullName(user.getFullName());
        p.setEmail(user.getEmail());
        p.setRole(user.getRole().getName().name());
        return p;
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName());
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            if (userRepo.existsByEmail(req.getEmail()) && !req.getEmail().equals(user.getEmail())) {
                throw new IllegalArgumentException("Email déjà utilisé");
            }
            user.setEmail(req.getEmail());
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(encoder.encode(req.getPassword()));
        }

        userRepo.save(user);
        return getProfile(user.getEmail());
    }

    // ======================= USER MANAGEMENT =======================

    @Transactional
    public UserInfo createUser(CreateUserRequest req, String adminEmail) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }

        Role role = switch (req.getRole().toUpperCase()) {
            case "TEACHER" -> roleRepo.findByName(RoleName.ROLE_ENCADRANT)
                    .orElseThrow(() -> new IllegalArgumentException("Rôle ENCADRANT introuvable"));
            case "STUDENT" -> roleRepo.findByName(RoleName.ROLE_ETUDIANT)
                    .orElseThrow(() -> new IllegalArgumentException("Rôle ETUDIANT introuvable"));
            default -> throw new IllegalArgumentException("Rôle invalide : utilisez TEACHER ou STUDENT");
        };

        User u = new User();
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRole(role);

        userRepo.save(u);

        return new UserInfo(u.getId(), u.getFullName(), u.getEmail(), u.getRole().getName().name());
    }

    public List<UserInfo> listUsers() {
        return userRepo.findAll().stream()
                .map(u -> new UserInfo(u.getId(), u.getFullName(), u.getEmail(), u.getRole().getName().name()))
                .toList();
    }

    public List<UserInfo> searchUsers(String keyword) {
        return userRepo.search(keyword).stream()
                .map(u -> new UserInfo(u.getId(), u.getFullName(), u.getEmail(), u.getRole().getName().name()))
                .toList();
    }

    @Transactional
    public UserInfo updateUser(Long id, UpdateUserRequest req) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (req.getFullName() != null) u.setFullName(req.getFullName());
        if (req.getEmail() != null) u.setEmail(req.getEmail());

        if (req.getRole() != null) {
            Role role = switch (req.getRole().toUpperCase()) {
                case "ADMIN" -> roleRepo.findByName(RoleName.ROLE_ADMIN).orElseThrow();
                case "TEACHER" -> roleRepo.findByName(RoleName.ROLE_ENCADRANT).orElseThrow();
                case "STUDENT" -> roleRepo.findByName(RoleName.ROLE_ETUDIANT).orElseThrow();
                default -> throw new IllegalArgumentException("Rôle invalide");
            };
            u.setRole(role);
        }

        userRepo.save(u);

        return new UserInfo(u.getId(), u.getFullName(), u.getEmail(), u.getRole().getName().name());
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepo.existsById(id))
            throw new IllegalArgumentException("Utilisateur introuvable");
        userRepo.deleteById(id);
    }
}