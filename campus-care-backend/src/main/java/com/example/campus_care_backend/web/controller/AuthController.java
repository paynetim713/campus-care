package com.example.campus_care_backend.web.controller;

import com.example.campus_care_backend.domain.UserRole;
import com.example.campus_care_backend.entity.AuthToken;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.service.AuthService;
import com.example.campus_care_backend.service.PasswordService;
import com.example.campus_care_backend.repo.UserRepository;
import com.example.campus_care_backend.web.dto.AuthDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordService passwordService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest req) {

        String roleStr = req.role();
        UserRole backendRole;
        if (roleStr == null || roleStr.isBlank()) {
            backendRole = UserRole.REQUESTER;
        } else {
            try {
                backendRole = UserRole.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid role: must be REQUESTER or TECHNICIAN");
            }
        }

        User u;
        if (backendRole == UserRole.TECHNICIAN) {
            u = authService.registerTechnician(req.fullName(), req.email(), req.password());
        } else {

            u = authService.registerRequester(req.fullName(), req.email(), req.password());
        }

        AuthToken t = authService.login(req.email(), req.password());
        return new AuthDtos.AuthResponse(t.getToken(), u.getId(), u.getFullName(), u.getRole());
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        AuthToken t = authService.login(req.email(), req.password());
        User u = authService.requireUserByToken(t.getToken());
        return new AuthDtos.AuthResponse(t.getToken(), u.getId(), u.getFullName(), u.getRole());
    }

    @PostMapping("/change-password")
    public void changePassword(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        authService.changePassword(token, req.currentPassword(), req.newPassword());
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest req) {
        try {
            authService.initiatePasswordReset(req.email());
        } catch (IllegalArgumentException e) {
            if ("email not found".equalsIgnoreCase(e.getMessage())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to send reset email");
        }
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.newPassword());
    }

    @PostMapping("/admin/create-user")
    public Map<String, Object> adminCreateUser(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {

        User caller = authService.requireUserByToken(token);
        if (caller.getRole() != UserRole.ADMIN)
            throw new IllegalArgumentException("admin role required");

        String fullName = body.get("fullName");
        String email    = body.get("email");
        String password = body.get("password");
        String roleStr  = body.get("role");

        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("fullName required");
        if (email    == null || email.isBlank())    throw new IllegalArgumentException("email required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("password required");
        if (roleStr  == null || roleStr.isBlank())  throw new IllegalArgumentException("role required");

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid role: must be REQUESTER, TECHNICIAN, or ADMIN");
        }

        userRepository.findByEmail(email.toLowerCase()).ifPresent(u -> {
            throw new IllegalArgumentException("email already exists");
        });

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.toLowerCase().trim());
        newUser.setPasswordHash(passwordService.hash(password));
        newUser.setRole(role);
        User saved = userRepository.save(newUser);

        return Map.of(
                "id",       saved.getId(),
                "fullName", saved.getFullName(),
                "email",    saved.getEmail(),
                "role",     saved.getRole().name()
        );
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers(
            @RequestHeader("X-Auth-Token") String token) {
        User caller = authService.requireUserByToken(token);
        if (caller.getRole() != UserRole.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin required");
        return userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id",       u.getId(),
                        "fullName", u.getFullName(),
                        "email",    u.getEmail(),
                        "role",     u.getRole().name()))
                .toList();
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        User caller = authService.requireUserByToken(token);
        if (caller.getRole() != UserRole.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin required");
        if (caller.getId().equals(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cannot delete your own account");
        userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        userRepository.deleteById(id);
    }
}