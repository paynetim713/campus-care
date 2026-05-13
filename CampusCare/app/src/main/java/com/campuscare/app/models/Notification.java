package com.campuscare.app.models;

public class Notification {
    public String  id;
    public String  title;
    public String  body;
    public String  createdAt;
    public boolean read;

    public Notification() {}

    public Notification(String title, String body, String createdAt) {
        this.title     = title;
        this.body      = body;
        this.createdAt = createdAt;
        this.read      = false;
    }
}