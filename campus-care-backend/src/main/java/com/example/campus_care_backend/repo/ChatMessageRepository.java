package com.example.campus_care_backend.repo;

import com.example.campus_care_backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByTicketIdAndChannelOrderByCreatedAtAsc(Long ticketId, String channel);
}