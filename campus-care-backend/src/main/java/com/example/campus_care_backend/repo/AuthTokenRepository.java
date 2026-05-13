package com.example.campus_care_backend.repo;

import com.example.campus_care_backend.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByToken(String token);
    void deleteByExpiresAtBefore(Instant now);
}
