package com.example.campus_care_backend.web.dto;

public class NotificationDtos {
    public record NotificationResponse(
            Long id,
            String title,
            String body,
            String createdAt,
            boolean read
    ) {}
}
