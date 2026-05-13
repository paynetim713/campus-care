package com.example.campus_care_backend.service;

import com.example.campus_care_backend.entity.Notification;
import com.example.campus_care_backend.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public void push(Long userId, String title, String body) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setBody(body);
        notificationRepository.save(n);
    }
}
