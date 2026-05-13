package com.example.campus_care_backend.web.controller;

import com.example.campus_care_backend.domain.UserRole;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.repo.UserRepository;
import com.example.campus_care_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService    authService;
    private final UserRepository userRepository;

    @GetMapping("/technicians")
    public List<Map<String, Object>> getTechnicians(
            @RequestHeader("X-Auth-Token") String token) {
        User caller = authService.requireUserByToken(token);
        if (caller.getRole() != UserRole.ADMIN)
            throw new IllegalArgumentException("admin role required");

        return userRepository.findByRole(UserRole.TECHNICIAN).stream()
                .map(u -> Map.<String, Object>of(
                        "id",    u.getId(),
                        "name",  u.getFullName(),
                        "email", u.getEmail(),
                        "role",  "technician"
                ))
                .toList();
    }
}