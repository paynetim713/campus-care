package com.campuscare.app.models;

public class Message {
    public String text;
    public boolean isSent;
    public long timestamp;

    public Message(String text, boolean isSent) {
        this.text = text; this.isSent = isSent; this.timestamp = System.currentTimeMillis();
    }
}
