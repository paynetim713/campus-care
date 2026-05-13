package com.example.campus_care_backend.web.controller;

import com.example.campus_care_backend.entity.ChatMessage;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.repo.ChatMessageRepository;
import com.example.campus_care_backend.service.AuthService;
import com.example.campus_care_backend.service.RepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AuthService authService;
    private final RepairService repairService;
    private final ChatMessageRepository repo;

    @GetMapping("/{ticketId}/{channel}")
    public List<Map<String, Object>> getMessages(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long ticketId,
            @PathVariable String channel) {

        User viewer = authService.requireUserByToken(token);
        repairService.getByIdForUser(viewer, ticketId);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;

        return repo.findByTicketIdAndChannelOrderByCreatedAtAsc(ticketId, channel.toUpperCase())
                .stream()
                .map(m -> Map.<String, Object>of(
                        "id",         m.getId(),
                        "ticketId",   m.getTicketId(),
                        "channel",    m.getChannel(),
                        "senderId",   m.getSenderId(),
                        "senderName", m.getSenderName(),
                        "body",       m.getBody(),
                        "createdAt",  fmt.format(m.getCreatedAt())
                ))
                .toList();
    }

    @PostMapping("/{ticketId}/{channel}")
    public Map<String, Object> sendMessage(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long ticketId,
            @PathVariable String channel,
            @RequestBody Map<String, String> body) {

        User sender = authService.requireUserByToken(token);
        repairService.getByIdForUser(sender, ticketId);
        String text = body.get("body");
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("message body is required");

        ChatMessage m = new ChatMessage();
        m.setTicketId(ticketId);
        m.setChannel(channel.toUpperCase());
        m.setSenderId(sender.getId());
        m.setSenderName(sender.getFullName());
        m.setBody(text.trim());

        ChatMessage saved = repo.save(m);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;

        return Map.of(
                "id",         saved.getId(),
                "ticketId",   saved.getTicketId(),
                "channel",    saved.getChannel(),
                "senderId",   saved.getSenderId(),
                "senderName", saved.getSenderName(),
                "body",       saved.getBody(),
                "createdAt",  fmt.format(saved.getCreatedAt())
        );
    }
}
