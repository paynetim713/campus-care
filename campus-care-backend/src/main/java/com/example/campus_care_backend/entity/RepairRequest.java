package com.example.campus_care_backend.entity;

import com.example.campus_care_backend.domain.RepairStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "repair_requests", indexes = {

        @Index(name = "idx_repairs_requester", columnList = "requester_id"),
        @Index(name = "idx_repairs_tech",      columnList = "assigned_technician_id"),
        @Index(name = "idx_repairs_status",    columnList = "status")
})
public class RepairRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requesterId;

    private Long assignedTechnicianId;

    @Column(nullable = false)
    private String building;

    @Column(nullable = false)
    private String floor;

    @Column(nullable = false)
    private String room;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 2000)
    private String details;

    @Column(length = 2000)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus status = RepairStatus.NEW;

    private String eta;

    @Column(length = 2000)
    private String technicianNote;

    private Integer rating;

    @Column(length = 2000)
    private String ratingComment;

    @Column(length = 2000)
    private String adminReply;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}