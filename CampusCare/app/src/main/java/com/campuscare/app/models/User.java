package com.campuscare.app.models;

public class User {
    public String id;
    public String name;
    public String email;
    public String role;
    public String password;

    public User() {}

    public User(String id, String name, String email, String role) {
        this.id    = id;
        this.name  = name;
        this.email = email;
        this.role  = role;
    }

    public User(String id, String name, String email, String role, String password) {
        this.id       = id;
        this.name     = name;
        this.email    = email;
        this.role     = role;
        this.password = password;
    }

    public String getDisplayName() {
        if (name != null && !name.isEmpty()) return name;
        if (email != null && !email.isEmpty()) return email;
        return "User";
    }

    public String getRoleDisplay() {
        if (role == null) return "User";
        switch (role.toLowerCase()) {
            case "admin":      return "Administrator";
            case "technician": return "Technician";
            case "requester":  return "Staff / Student";
            case "lecturer":   return "Lecturer";
            case "student":    return "Student";
            default:           return "User";
        }
    }
}