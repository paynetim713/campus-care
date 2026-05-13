package com.campuscare.app.models;

public class ChatMessage {
    public Long   id;
    public Long   ticketId;
    public String channel;
    public Long   senderId;
    public String senderName;
    public String body;
    public String createdAt;

    public ChatMessage() {}
}