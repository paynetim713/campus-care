package com.example.campus_care_backend.config;

import com.example.campus_care_backend.domain.UserRole;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.repo.UserRepository;
import com.example.campus_care_backend.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SeedDataConfig {
    private final UserRepository userRepository;
    private final PasswordService passwordService;

    @Bean
    public CommandLineRunner seedUsers() {
        return args -> {
            ensureUser("Admin",             "admin@uni.edu",   "123456", UserRole.ADMIN);
            ensureUser("Mark Stevenson",    "tech@uni.edu",    "123456",  UserRole.TECHNICIAN);
            ensureUser("John Student",      "john@uni.edu",    "123456",  UserRole.REQUESTER);
            ensureUser("Prof. Sarah",       "sarah@uni.edu",   "123456",  UserRole.REQUESTER);
        };
    }

    private void ensureUser(String name, String email, String password, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) return;
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordService.hash(password));
        u.setRole(role);
        userRepository.save(u);
    }
}