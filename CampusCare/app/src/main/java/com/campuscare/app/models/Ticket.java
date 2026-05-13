package com.campuscare.app.models;

public class Ticket {
    public String id;
    public String requesterId;
    public String assignedTechnicianId;
    public String building;
    public String floor;
    public String room;
    public String category;
    public String details;
    public String photoUrl;
    public String status;
    public String eta;
    public String technicianNote;
    public int rating;
    public String ratingComment;
    public String adminReply;
    public String createdAt;
    public String updatedAt;

    public String title;
    public String photoPath;
    public String assignedTechId;
    public String assignedTechName;
    public String reportedById;
    public String expectedFinish;
    public String techNote;
    public long createdAtLocal;
    public int progressPercent;

    public Ticket() {}

    public Ticket(String id, String title, String building, String floor, String room,
                  String category, String details, String status, String reportedById) {
        this.id = id;
        this.title = title;
        this.building = building;
        this.floor = floor;
        this.room = room;
        this.category = category;
        this.details = details;
        this.status = status;
        this.reportedById = reportedById;
        this.createdAtLocal = System.currentTimeMillis();
    }

    public String getIdString() {
        return id != null ? id : "";
    }

    public String getLocationString() {
        return building + " - RM " + room;
    }

    public String getStatusDisplay() {
        if (status == null) return "UNKNOWN";
        String s = status.trim().toUpperCase().replace(' ', '_');
        switch (s) {
            case "NEW":
                return "NEW";
            case "IN_PROGRESS":
                return "IN PROGRESS";
            case "DONE":
            case "COMPLETED":
                return "DONE";
            default:
                return s;
        }
    }

    public String getNormalisedStatus() {
        if (status == null) return "new";
        String s = status.trim().toUpperCase().replace(' ', '_');

        s = s.replace('-', '_');
        if (s.contains("IN") && s.contains("PROGRESS")) return "in_progress";
        if (s.contains("DONE") || s.contains("COMPLETED")) return "done";
        if (s.contains("NEW")) return "new";
        return "new";
    }

    public boolean isDone() {
        if (status == null) return false;
        String s = status.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return "DONE".equals(s) || "COMPLETED".equals(s) || s.contains("DONE") || s.contains("COMPLETED");
    }

    public String getShortId() {
        if (id == null) return "0000";
        try {
            return String.format("%04d", Long.parseLong(id) % 10000);
        } catch (NumberFormatException e) {
            return id;
        }
    }
}