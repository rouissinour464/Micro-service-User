package com.pfe.auth.service;

import com.pfe.auth.dto.auth.AdminRegisterRequest;
import com.pfe.auth.dto.auth.AuthResponse;
import com.pfe.auth.dto.auth.LoginRequest;
import com.pfe.auth.dto.auth.ProfileResponse;
import com.pfe.auth.dto.auth.RefreshTokenRequest;
import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.dto.user.UpdateProfileRequest;
import com.pfe.auth.dto.user.UpdateUserRequest;
import com.pfe.auth.dto.user.UserInfo;
import com.pfe.auth.entity.AdminCode;
import com.pfe.auth.entity.RefreshToken;
import com.pfe.auth.entity.Role;
import com.pfe.auth.entity.RoleName;
import com.pfe.auth.entity.User;
import com.pfe.auth.repository.AdminCodeRepository;
import com.pfe.auth.repository.RefreshTokenRepository;
import com.pfe.auth.repository.RoleRepository;
import com.pfe.auth.repository.UserRepository;
import com.pfe.auth.security.JwtUtils;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final AdminCodeRepository adminCodeRepo;
    private final RoleRepository roleRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwt;

    /* =========================================================
       AUTH
       ========================================================= */

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
                .replaceAll("\\s+", "");

        AdminCode ac = adminCodeRepo.findByCode(cleanCode)
                .orElseThrow(() ->
                        new IllegalArgumentException("Code admin invalide"));

        if (ac.isUsed()) {
            throw new IllegalArgumentException("Code déjà utilisé");
        }

        Role adminRole = roleRepo.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() ->
                        new IllegalArgumentException("Rôle ADMIN introuvable"));

        User admin = new User();
        admin.setFullName(req.getFullName());
        admin.setEmail(req.getEmail());
        admin.setPassword(encoder.encode(req.getPassword()));
        admin.setRole(adminRole);

        userRepo.save(admin);

        ac.setUsed(true);
        adminCodeRepo.save(ac);

        String token = jwt.generateToken(
                admin.getId(),
                admin.getEmail(),
                admin.getRole()
        );

        String refreshToken = createRefreshToken(admin.getEmail());

        return new AuthResponse(
                token,
                refreshToken,
                admin.getRole().getName().name()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }

        String token = jwt.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        String refreshToken = createRefreshToken(user.getEmail());

        return new AuthResponse(
                token,
                refreshToken,
                user.getRole().getName().name()
        );
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {

        RefreshToken rt = refreshTokenRepo.findByToken(req.getRefreshToken())
                .orElseThrow(() ->
                        new IllegalArgumentException("Refresh token invalide"));

        User user = userRepo.findByEmail(rt.getUserEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable"));

        refreshTokenRepo.delete(rt);

        String newRefresh = createRefreshToken(user.getEmail());

        String newAccess = jwt.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse(
                newAccess,
                newRefresh,
                user.getRole().getName().name()
        );
    }

    @Transactional
    public void logout(String email) {
        refreshTokenRepo.deleteByUserEmail(email);
    }

    /* =========================================================
       PROFILE
       ========================================================= */

    public ProfileResponse getProfile(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable"));

        ProfileResponse p = new ProfileResponse();

        p.setId(user.getId());
        p.setFullName(user.getFullName());
        p.setEmail(user.getEmail());
        p.setRole(user.getRole().getName().name());

        return p;
    }

    @Transactional
    public ProfileResponse updateProfile(
            String email,
            UpdateProfileRequest req
    ) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable"));

        if (req.getFullName() != null) {
            user.setFullName(req.getFullName());
        }

        if (req.getEmail() != null) {

            if (userRepo.existsByEmail(req.getEmail())
                    && !req.getEmail().equals(user.getEmail())) {

                throw new IllegalArgumentException("Email déjà utilisé");
            }

            user.setEmail(req.getEmail());
        }

        if (req.getPassword() != null) {
            user.setPassword(encoder.encode(req.getPassword()));
        }

        userRepo.save(user);

        return getProfile(user.getEmail());
    }

    /* =========================================================
       USER MANAGEMENT
       ========================================================= */

    @Transactional
    public UserInfo createUser(
            CreateUserRequest req,
            String adminEmail
    ) {

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }

        Role role = switch (req.getRole().toUpperCase()) {

            case "TEACHER", "ENSEIGNANT", "ENCADRANT" ->
                    roleRepo.findByName(RoleName.ROLE_ENCADRANT)
                            .orElseThrow();

            case "STUDENT", "ETUDIANT" ->
                    roleRepo.findByName(RoleName.ROLE_ETUDIANT)
                            .orElseThrow();

            case "ADMIN" ->
                    roleRepo.findByName(RoleName.ROLE_ADMIN)
                            .orElseThrow();

            default ->
                    throw new IllegalArgumentException("Rôle invalide");
        };

        User u = new User();

        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRole(role);

        userRepo.save(u);

        return mapToUserInfo(u);
    }

    public List<UserInfo> listUsers() {

        return userRepo.findAll()
                .stream()
                .map(this::mapToUserInfo)
                .toList();
    }

    public List<UserInfo> searchUsers(String query) {

        if (query == null || query.isBlank()) {
            return listUsers();
        }

        String q = query.toLowerCase();

        return userRepo.findAll()
                .stream()
                .filter(u ->
                        u.getFullName().toLowerCase().contains(q)
                                || u.getEmail().toLowerCase().contains(q)
                )
                .map(this::mapToUserInfo)
                .toList();
    }

    /* =========================================================
       USERS BY ROLE
       ========================================================= */

    public List<Long> getUserIdsByRole(String role) {

        RoleName roleName = parseRole(role);

        return userRepo.findByRole_Name(roleName)
                .stream()
                .map(User::getId)
                .toList();
    }

    public List<UserInfo> getUserInfosByRole(String role) {

        RoleName roleName = parseRole(role);

        return userRepo.findByRole_Name(roleName)
                .stream()
                .map(this::mapToUserInfo)
                .toList();
    }

    /* =========================================================
       GET USER BY ID
       ========================================================= */

    public UserInfo getUserById(Long id) {

        User u = userRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable"));

        return mapToUserInfo(u);
    }

    /* =========================================================
       UPDATE USER
       ========================================================= */

    @Transactional
    public UserInfo updateUser(
            Long id,
            UpdateUserRequest req
    ) {

        User u = userRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable"));

        if (req.getFullName() != null) {
            u.setFullName(req.getFullName());
        }

        if (req.getEmail() != null) {
            u.setEmail(req.getEmail());
        }

        if (req.getRole() != null) {
            u.setRole(
                    roleRepo.findByName(parseRole(req.getRole()))
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Rôle invalide"))
            );
        }

        userRepo.save(u);

        return mapToUserInfo(u);
    }

    @Transactional
    public void deleteUser(Long id) {

        if (!userRepo.existsById(id)) {
            throw new IllegalArgumentException("Utilisateur introuvable");
        }

        userRepo.deleteById(id);
    }

    /* =========================================================
       HELPERS
       ========================================================= */

    private String createRefreshToken(String email) {

        RefreshToken rt = new RefreshToken();

        rt.setToken(UUID.randomUUID().toString());
        rt.setUserEmail(email);

        return refreshTokenRepo.save(rt).getToken();
    }

    private RoleName parseRole(String role) {

        return switch (role.toUpperCase()) {

            case "ETUDIANT", "STUDENT" ->
                    RoleName.ROLE_ETUDIANT;

            case "ENSEIGNANT", "TEACHER", "ENCADRANT" ->
                    RoleName.ROLE_ENCADRANT;

            case "ADMIN" ->
                    RoleName.ROLE_ADMIN;

            default ->
                    throw new IllegalArgumentException("Rôle invalide");
        };
    }

    private UserInfo mapToUserInfo(User u) {

        return new UserInfo(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole().getName().name()
        );
    }
}