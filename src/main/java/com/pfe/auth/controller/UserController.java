package com.pfe.auth.controller;

import com.pfe.auth.dto.user.CreateUserRequest;
import com.pfe.auth.dto.user.UserInfo;
import com.pfe.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping("/users")
    public ResponseEntity<UserInfo> createUser(@RequestBody CreateUserRequest req,
                                               Principal principal) {

        return ResponseEntity.ok(authService.createUser(req, principal.getName()));
    }
}