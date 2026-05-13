package com.example.campus_care_backend.web.controller;

import com.example.campus_care_backend.entity.Notification;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.repo.NotificationRepository;
import com.example.campus_care_backend.service.AuthService;
import com.example.campus_care_backend.web.dto.NotificationDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final AuthService authService;
    private final NotificationRepository repo;

    @GetMapping
    public List<NotificationDtos.NotificationResponse> list(
            @RequestHeader("X-Auth-Token") String token) {
        User u = authService.requireUserByToken(token);
        DateTimeFormatter f = DateTimeFormatter.ISO_INSTANT;
        return repo.findByUserIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(n -> new NotificationDtos.NotificationResponse(
                        n.getId(), n.getTitle(), n.getBody(),
                        f.format(n.getCreatedAt()), n.isRead()))
                .toList();
    }

    @PatchMapping("/{id}/read")
    public void markRead(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        User u = authService.requireUserByToken(token);
        Notification n = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("notification not found"));
        if (!n.getUserId().equals(u.getId()))
            throw new IllegalArgumentException("not your notification");
        n.setRead(true);
        repo.save(n);
    }

    @PatchMapping("/read-all")
    public void markAllRead(@RequestHeader("X-Auth-Token") String token) {
        User u = authService.requireUserByToken(token);
        List<Notification> all = repo.findByUserIdOrderByCreatedAtDesc(u.getId());
        all.forEach(n -> n.setRead(true));
        repo.saveAll(all);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        User u = authService.requireUserByToken(token);
        Notification n = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("notification not found"));
        if (!n.getUserId().equals(u.getId()))
            throw new IllegalArgumentException("not your notification");
        repo.delete(n);
    }
}