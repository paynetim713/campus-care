package com.campuscare.app.utils;

import com.campuscare.app.models.AuthResponse;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Callback;
import retrofit2.Response;

public class DataManager {
    private static DataManager instance;
    private User currentUser;
    private List<User> users = new ArrayList<>();
    private List<Ticket> tickets = new ArrayList<>();

    private List<User> technicianUsers = new ArrayList<>();

    private int ticketCounter = 1;
    private boolean isOnline = false;

    private DataManager() {

        User u1 = new User("u1", "John Student",   "john@uni.edu",  "student",    "user12345");
        User u2 = new User("u2", "Prof. Sarah",    "sarah@uni.edu", "lecturer",   "user12345");
        User u3 = new User("u3", "Mark Stevenson", "mark@fix.edu",  "technician", "tech12345");
        User u4 = new User("u4", "Admin User",     "admin@uni.edu", "admin",      "admin12345");
        users.add(u1); users.add(u2); users.add(u3); users.add(u4);
    }

    public static DataManager getInstance() {
        if (instance == null) instance = new DataManager();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }

    public void setOnlineMode(boolean online) { this.isOnline = online; }
    public boolean isOnline() { return isOnline; }

    public User login(String email, String password) {
        for (User u : users) {
            if (u.email.equals(email)) {
                if (u.password == null || u.password.equals(password)) return u;
                return null;
            }
        }
        return null;
    }

    public boolean changePasswordLocally(String userId, String currentPassword, String newPassword) {
        for (User u : users) {
            if (u.id != null && u.id.equals(userId)) {
                if (u.password == null || u.password.equals(currentPassword)) {
                    u.password = newPassword;
                    if (currentUser != null && currentUser.id != null
                            && currentUser.id.equals(userId)) {
                        currentUser.password = newPassword;
                    }
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public void loginFromApi(AuthResponse auth, String email) {
        this.currentUser = new User(auth.userId, auth.fullName, email, auth.role);
        ApiClient.setAuthToken(auth.token);
        this.isOnline = true;

        clearTickets();
    }

    public boolean register(String name, String email, String password, String role) {
        for (User u : users) if (u.email.equals(email)) return false;
        String id = "u" + (users.size() + 1);
        users.add(new User(id, name, email, role, password));
        return true;
    }

    public List<Ticket> getActiveTicketsForUser(String userId) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            String owner = t.reportedById != null ? t.reportedById
                    : (t.requesterId != null ? t.requesterId : null);
            if (owner != null && owner.equals(userId) && !t.isDone()) result.add(t);
        }
        return result;
    }

    public List<Ticket> getHistoryTicketsForUser(String userId) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            String owner = t.reportedById != null ? t.reportedById
                    : (t.requesterId != null ? t.requesterId : null);
            if (owner != null && owner.equals(userId) && t.isDone()) result.add(t);
        }
        return result;
    }

    public List<Ticket> getTicketsForTech(String techId) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            boolean isNew = "new".equalsIgnoreCase(t.getNormalisedStatus());
            if (!isNew) continue;

            String assigned = t.assignedTechId != null
                    ? t.assignedTechId : t.assignedTechnicianId;

            boolean forMe = assigned == null
                    || assigned.equals(techId)
                    || assignedMatchesNumeric(assigned, techId);
            if (forMe) result.add(t);
        }
        return result;
    }

    private boolean assignedMatchesNumeric(String a, String b) {
        if (a == null || b == null) return false;
        String numA = a.replaceAll("^[^0-9]+", "");
        String numB = b.replaceAll("^[^0-9]+", "");
        return !numA.isEmpty() && numA.equals(numB);
    }

    public List<Ticket> getActiveTicketsForTech(String techId) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            String assigned = t.assignedTechId != null ? t.assignedTechId : t.assignedTechnicianId;
            boolean inProgress = "in_progress".equalsIgnoreCase(t.getNormalisedStatus());
            boolean mine = assigned != null
                    && (assigned.equals(techId) || assignedMatchesNumeric(assigned, techId));
            if (mine && inProgress) result.add(t);
        }
        return result;
    }

    public List<Ticket> getCompletedTicketsForTech(String techId) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            String assigned = t.assignedTechId != null ? t.assignedTechId : t.assignedTechnicianId;
            boolean mine = assigned != null
                    && (assigned.equals(techId) || assignedMatchesNumeric(assigned, techId));
            if (mine && t.isDone()) result.add(t);
        }
        return result;
    }

    public List<Ticket> getUnassignedTickets() {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            boolean isNew = "new".equalsIgnoreCase(t.getNormalisedStatus());
            if (isNew && t.assignedTechId == null && t.assignedTechnicianId == null)
                result.add(t);
        }
        return result;
    }

    public List<Ticket> getAllTickets() { return tickets; }

    public void addTicket(Ticket t) {
        if (t.id == null) {
            t.id = "local-" + ticketCounter++;
        }
        for (int i = 0; i < tickets.size(); i++) {
            if (t.id.equals(tickets.get(i).id)) {
                tickets.set(i, t);
                return;
            }
        }
        tickets.add(t);
    }

    public void updateTicketStatus(String ticketId, String status) {
        for (Ticket t : tickets) {
            if (ticketId != null && ticketId.equals(t.id)) {
                t.status = status;
                break;
            }
        }
    }

    public void assignTech(String ticketId, String techId, String techName) {
        for (Ticket t : tickets) {
            if (ticketId != null && ticketId.equals(t.id)) {
                t.assignedTechId       = techId;
                t.assignedTechnicianId = techId;
                t.assignedTechName     = techName;

                break;
            }
        }
    }

    public void clearTickets() {
        tickets.clear();
        ticketCounter = 1;
    }

    public List<User> getUsersByRole(String role) {

        if ("technician".equals(role) && !technicianUsers.isEmpty()) {
            return new ArrayList<>(technicianUsers);
        }
        List<User> result = new ArrayList<>();
        for (User u : users) if (role.equals(u.role)) result.add(u);
        return result;
    }

    public List<User> getAllUsers() { return users; }

    public void setTechnicianUsers(List<User> techs) {
        if (techs == null) techs = new ArrayList<>();
        technicianUsers = new ArrayList<>(techs);
    }

    public void replaceAllUsers(List<User> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
    }

    public void upsertUser(User u) {
        if (u == null) return;

        if (u.email != null && !u.email.isEmpty()) {
            for (int i = 0; i < users.size(); i++) {
                User existing = users.get(i);
                if (existing != null && existing.email != null
                        && existing.email.equalsIgnoreCase(u.email)) {
                    users.set(i, u);
                    break;
                }
            }

            boolean found = false;
            for (User existing : users) {
                if (existing != null && existing.email != null
                        && existing.email.equalsIgnoreCase(u.email)) {
                    found = true;
                    break;
                }
            }
            if (!found) users.add(u);
        } else if (u.id != null && !u.id.isEmpty()) {
            boolean found = false;
            for (int i = 0; i < users.size(); i++) {
                User existing = users.get(i);
                if (existing != null && existing.id != null && existing.id.equals(u.id)) {
                    users.set(i, u);
                    found = true;
                    break;
                }
            }
            if (!found) users.add(u);
        } else {
            users.add(u);
        }

        if ("technician".equalsIgnoreCase(u.role)) {
            if (u.id == null || u.id.isEmpty()) {

                if (u.email != null && !u.email.isEmpty()) {
                    boolean exists = false;
                    for (User t : technicianUsers) {
                        if (t != null && t.email != null
                                && t.email.equalsIgnoreCase(u.email)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) technicianUsers.add(u);
                } else {
                    technicianUsers.add(u);
                }
            } else {
                boolean replaced = false;
                for (int i = 0; i < technicianUsers.size(); i++) {
                    User existing = technicianUsers.get(i);
                    if (existing != null && existing.id != null && existing.id.equals(u.id)) {
                        technicianUsers.set(i, u);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) technicianUsers.add(u);
            }
        }
    }

    public void fetchAllUsersForAdmin(Callback<List<User>> callback) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            callback.onResponse(null,
                    retrofit2.Response.success(new ArrayList<>(users)));
            return;
        }

        ApiClient.getService().adminListUsers(token).enqueue(new Callback<List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<List<java.util.Map<String, Object>>> call,
                                   retrofit2.Response<List<java.util.Map<String, Object>>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure(null, new RuntimeException("failed to fetch users"));
                    return;
                }

                List<User> newUsers = new ArrayList<>();
                List<User> newTechs = new ArrayList<>();

                for (java.util.Map<String, Object> m : response.body()) {
                    if (m == null) continue;

                    Object idObj = m.get("id");
                    String id = null;
                    if (idObj instanceof Number) {
                        id = String.valueOf(((Number) idObj).longValue());
                    } else if (idObj != null) {
                        id = String.valueOf(idObj).trim();
                    }
                    String fullName = (String) m.get("fullName");
                    String email = (String) m.get("email");
                    String backendRole = m.get("role") != null ? String.valueOf(m.get("role")) : null;

                    String frontendRole = backendRoleToFrontend(backendRole, email, fullName);
                    User u = new User(id, fullName, email, frontendRole);
                    newUsers.add(u);
                    if ("technician".equalsIgnoreCase(frontendRole)) newTechs.add(u);
                }

                replaceAllUsers(newUsers);
                setTechnicianUsers(newTechs);
                callback.onResponse(null, retrofit2.Response.success(newUsers));
            }

            @Override
            public void onFailure(retrofit2.Call<List<java.util.Map<String, Object>>> call, Throwable t) {

                callback.onFailure(null, t);
            }
        });
    }

    private String backendRoleToFrontend(String backendRole, String email, String fullName) {
        if (backendRole == null) return "student";
        String r = backendRole.trim().toUpperCase();
        switch (r) {
            case "ADMIN":
                return "admin";
            case "TECHNICIAN":
                return "technician";
            case "REQUESTER":
            default:

                String e = email != null ? email.trim().toLowerCase() : "";
                boolean isStudentDomain  = e.endsWith("@siswa.ukm.edu.my");
                boolean isLecturerDomain = e.endsWith("@ukm.edu.my") && !isStudentDomain;

                if (isLecturerDomain) return "lecturer";
                if (isStudentDomain)  return "student";

                String n = fullName != null ? fullName.trim().toLowerCase() : "";
                boolean looksLikeLecturer =
                        n.contains("prof") || n.contains("dr") || n.contains("lecturer");
                return looksLikeLecturer ? "lecturer" : "student";
        }
    }

    public List<Ticket> getInProgressTickets() {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            if ("in_progress".equalsIgnoreCase(t.getNormalisedStatus()))
                result.add(t);
        }
        return result;
    }

    public void removeTicket(String ticketId) {
        tickets.removeIf(t -> ticketId != null && ticketId.equals(t.id));
    }

    public void fetchMyRepairs(Callback<List<Ticket>> callback) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            callback.onResponse(null,
                    Response.success(getActiveTicketsForUser(currentUser.id)));
            return;
        }
        ApiClient.getService().getMyRepairs(token).enqueue(callback);
    }

    public void fetchAssignedRepairs(Callback<List<Ticket>> callback) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            callback.onResponse(null,
                    Response.success(getActiveTicketsForTech(currentUser.id)));
            return;
        }
        ApiClient.getService().getAssignedRepairs(token).enqueue(callback);
    }

    public void fetchAvailableRepairs(Callback<List<Ticket>> callback) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            callback.onResponse(null, Response.success(getUnassignedTickets()));
            return;
        }
        ApiClient.getService().getAvailableRepairs(token).enqueue(callback);
    }

    public void fetchAllRepairsForAdmin(Callback<List<Ticket>> callback) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            callback.onResponse(null, Response.success(getAllTickets()));
            return;
        }
        ApiClient.getService().getAllRepairs(token).enqueue(callback);
    }

    public void fetchTechnicians(Callback<List<User>> callback) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            callback.onResponse(null, Response.success(getUsersByRole("technician")));
            return;
        }
        ApiClient.getService().getTechnicians(token).enqueue(callback);
    }

    public void createRepair(Ticket ticket, Callback<Ticket> callback) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            addTicket(ticket);
            callback.onResponse(null, Response.success(ticket));
            return;
        }
        Map<String, Object> request = new HashMap<>();
        request.put("building",  ticket.building);
        request.put("floor",     ticket.floor);
        request.put("room",      ticket.room);
        request.put("category",  ticket.category);
        request.put("details",   ticket.details);
        request.put("photoUrl",  ticket.photoUrl);
        ApiClient.getService().createRepair(token, request).enqueue(callback);
    }
}