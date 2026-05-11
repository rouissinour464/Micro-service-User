package com.pfe.auth.controller;

import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.dto.user.UserInfo;
import com.pfe.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /* =========================================================
       ADMIN : CREATE USER
       ========================================================= */

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserInfo> createUser(
            @RequestBody CreateUserRequest req,
            Principal principal
    ) {

        return ResponseEntity.ok(
                authService.createUser(req, principal.getName())
        );
    }

    /* =========================================================
       ADMIN : GET IDS BY ROLE
       ========================================================= */

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Long>> getUsersByRole(
            @RequestParam String role
    ) {

        return ResponseEntity.ok(
                authService.getUserIdsByRole(role)
        );
    }

    /* =========================================================
       INTERNAL FEIGN CALL
       stage-service -> auth-service
       GET /api/users/encadrants
       ========================================================= */

    @GetMapping("/encadrants")
    public ResponseEntity<List<Long>> getEncadrants() {

        return ResponseEntity.ok(
                authService.getUserIdsByRole("ENCADRANT")
        );
    }

    /* =========================================================
       INTERNAL FEIGN CALL
       GET /api/users/by-role?role=ENCADRANT
       ========================================================= */

    @GetMapping("/by-role")
    public ResponseEntity<List<UserInfo>> getUsersByRoleFull(
            @RequestParam String role
    ) {

        return ResponseEntity.ok(
                authService.getUserInfosByRole(role)
        );
    }

    /* =========================================================
       INTERNAL FEIGN CALL
       GET /api/users/{id}
       ========================================================= */

    @GetMapping("/{id}")
    public ResponseEntity<UserInfo> getUserById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                authService.getUserById(id)
        );
    }
}