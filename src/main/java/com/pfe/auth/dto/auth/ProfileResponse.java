package com.pfe.auth.dto.auth;

import lombok.Data;

@Data
public class ProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String role;
}