package com.example.campus_care_backend.repo;

import com.example.campus_care_backend.domain.UserRole;
import com.example.campus_care_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRole(UserRole role);
}