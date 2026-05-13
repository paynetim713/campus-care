package com.example.campus_care_backend.web.dto;

import com.example.campus_care_backend.domain.RepairStatus;
import jakarta.validation.constraints.NotBlank;

public class RepairDtos {
    public record CreateRepairRequest(
            @NotBlank String building,
            @NotBlank String floor,
            @NotBlank String room,
            @NotBlank String category,
            @NotBlank String details,
            String photoUrl
    ) {}

    public record RepairResponse(
            Long id,
            Long requesterId,
            Long assignedTechnicianId,
            String building,
            String floor,
            String room,
            String category,
            String details,
            String photoUrl,
            RepairStatus status,
            String eta,
            String technicianNote,
            Integer rating,
            String ratingComment,
            String adminReply,
            String createdAt,
            String updatedAt
    ) {}

    public record UpdateTaskRequest(
            RepairStatus status,
            String eta,
            String technicianNote
    ) {}

    public record RateRequest(
            int stars,
            String comment
    ) {}

    public record AdminReplyRequest(
            String reply
    ) {}
}
