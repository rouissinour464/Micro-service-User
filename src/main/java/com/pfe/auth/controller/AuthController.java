package com.pfe.auth.controller;

import com.pfe.auth.dto.auth.AdminRegisterRequest;
import com.pfe.auth.dto.auth.AuthResponse;
import com.pfe.auth.dto.auth.LoginRequest;
import com.pfe.auth.dto.auth.RefreshTokenRequest;
import com.pfe.auth.dto.auth.ProfileResponse;
import com.pfe.auth.dto.user.UpdateProfileRequest;
import com.pfe.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    // ✅ Register Admin
    @PostMapping("/register-admin")
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody AdminRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.registerAdmin(req));
    }

    // ✅ Login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }

    // ✅ Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(auth.refreshToken(req));
    }

    // ✅ Logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Principal p) {
        auth.logout(p.getName());
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    // ✅ Get Profile
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Principal p) {
        return ResponseEntity.ok(auth.getProfile(p.getName()));
    }

    // ✅ Update Profile
    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            Principal p,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(auth.updateProfile(p.getName(), req));
    }
}
