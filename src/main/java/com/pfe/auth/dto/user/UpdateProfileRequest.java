package com.pfe.auth.dto.user;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String email;     // ✅ nécessaire
    private String password;  // ✅ DOIT correspondre à "password" envoyé par le front
}