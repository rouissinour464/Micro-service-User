package com.pfe.auth.dto.auth;

import lombok.Data;

@Data
public class AdminRegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String adminCode;
}