package com.example.campus_care_backend.service;

import com.example.campus_care_backend.domain.UserRole;
import com.example.campus_care_backend.entity.AuthToken;
import com.example.campus_care_backend.entity.PasswordResetToken;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.repo.AuthTokenRepository;
import com.example.campus_care_backend.repo.PasswordResetTokenRepository;
import com.example.campus_care_backend.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordService passwordService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    public User registerRequester(String fullName, String email, String password) {
        String e = normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(e).ifPresent(u -> {
            throw new IllegalArgumentException("email already exists");
        });
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(e);
        u.setPasswordHash(passwordService.hash(password));
        u.setRole(UserRole.REQUESTER);
        return userRepository.save(u);
    }

    public User registerTechnician(String fullName, String email, String password) {
        String e = normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(e).ifPresent(u -> {
            throw new IllegalArgumentException("email already exists");
        });
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(e);
        u.setPasswordHash(passwordService.hash(password));
        u.setRole(UserRole.TECHNICIAN);
        return userRepository.save(u);
    }

    public AuthToken login(String email, String password) {
        String e = normalizeEmail(email);
        User u = userRepository.findByEmailIgnoreCase(e)
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));
        String hash = passwordService.hash(password);
        if (!hash.equals(u.getPasswordHash())) throw new IllegalArgumentException("invalid credentials");

        AuthToken t = new AuthToken();
        t.setUserId(u.getId());
        t.setToken(UUID.randomUUID().toString().replace("-", ""));
        t.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        return tokenRepository.save(t);
    }

    public void changePassword(String token, String currentPassword, String newPassword) {
        User u = requireUserByToken(token);
        String currentHash = passwordService.hash(currentPassword);
        if (!currentHash.equals(u.getPasswordHash())) {
            throw new IllegalArgumentException("current password is incorrect");
        }
        u.setPasswordHash(passwordService.hash(newPassword));
        userRepository.save(u);
    }

    public User requireUserByToken(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("missing token");
        AuthToken t = tokenRepository.findByToken(token).orElseThrow(() -> new IllegalArgumentException("invalid token"));
        if (Instant.now().isAfter(t.getExpiresAt())) throw new IllegalArgumentException("token expired");
        return userRepository.findById(t.getUserId()).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    public void initiatePasswordReset(String email) {
        String e = normalizeEmail(email);
        User u = userRepository.findByEmailIgnoreCase(e)
                .orElseThrow(() -> new IllegalArgumentException("email not found"));

        PasswordResetToken t = new PasswordResetToken();
        t.setUserId(u.getId());
        t.setToken(generateFiveDigitCode());
        t.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        passwordResetTokenRepository.save(t);

        String subject = "Password Reset - CampusCare";
        String body = """
                You requested a password reset for your CampusCare account.

                Verification code:
                %s

                This code expires in 10 minutes.
                If you did not request this, you can ignore this email.
                """.formatted(t.getToken());
        mailService.sendPlainText(e, subject, body);
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("missing reset token");
        if (newPassword == null || newPassword.isBlank()) throw new IllegalArgumentException("missing newPassword");

        PasswordResetToken t = passwordResetTokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("invalid reset token"));
        if (t.getUsedAt() != null) throw new IllegalArgumentException("reset token already used");
        if (Instant.now().isAfter(t.getExpiresAt())) throw new IllegalArgumentException("reset token expired");

        User u = userRepository.findById(t.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        u.setPasswordHash(passwordService.hash(newPassword));
        userRepository.save(u);

        t.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(t);
    }

    private String normalizeEmail(String email) {
        if (email == null) throw new IllegalArgumentException("missing email");
        String e = email.trim().toLowerCase();
        if (e.isBlank()) throw new IllegalArgumentException("missing email");
        return e;
    }

    private String generateFiveDigitCode() {

        for (int i = 0; i < 20; i++) {
            int n = ThreadLocalRandom.current().nextInt(10000, 100000);
            String code = String.valueOf(n);
            if (passwordResetTokenRepository.findByToken(code).isEmpty()) return code;
        }
        throw new IllegalStateException("failed to generate unique verification code");
    }
}