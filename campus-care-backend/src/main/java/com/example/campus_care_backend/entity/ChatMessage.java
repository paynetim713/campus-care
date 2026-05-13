package com.example.campus_care_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_ticket",  columnList = "ticket_id"),
        @Index(name = "idx_chat_channel", columnList = "ticket_id,channel")
})
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 120)
    private String senderName;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}