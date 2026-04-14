package com.pfe.auth.controller;

import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.dto.user.UpdateUserRequest;
import com.pfe.auth.dto.user.UserInfo;
import com.pfe.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService service;

    // ✅ CREATE USER
    @PostMapping
    public ResponseEntity<UserInfo> createUser(@Valid @RequestBody CreateUserRequest req,
                                               Principal principal) {
        UserInfo user = service.createUser(req, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // ✅ LIST USERS
    @GetMapping
    public ResponseEntity<List<UserInfo>> listUsers() {
        return ResponseEntity.ok(service.listUsers());
    }

    // ✅ SEARCH USERS
    @GetMapping("/search")
    public ResponseEntity<List<UserInfo>> searchUsers(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchUsers(keyword));
    }

    // ✅ UPDATE USER
    @PutMapping("/{id}")
    public ResponseEntity<UserInfo> updateUser(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(service.updateUser(id, req));
    }

    // ✅ DELETE USER
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}