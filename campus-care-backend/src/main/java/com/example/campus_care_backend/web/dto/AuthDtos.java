package com.example.campus_care_backend.web.dto;

import com.example.campus_care_backend.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,

            String role,
            @NotBlank String password
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank String newPassword
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String fullName,
            UserRole role
    ) {}
}