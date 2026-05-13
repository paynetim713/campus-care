package com.campuscare.app.adapters;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.activities.LiveTrackingActivity;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketAdapter extends RecyclerView.Adapter<AdminTicketAdapter.VH> {
    private List<Ticket> tickets;
    private List<Ticket> allTickets;

    public AdminTicketAdapter(List<Ticket> tickets) {
        this.tickets    = new java.util.ArrayList<>(tickets);
        this.allTickets = new java.util.ArrayList<>(tickets);
    }

    public void updateData(List<Ticket> newTickets) {
        this.allTickets = new java.util.ArrayList<>(newTickets);
        this.tickets    = new java.util.ArrayList<>(newTickets);
        notifyDataSetChanged();
    }

    public void filterByStatus(String status) {
        if (status == null || "ALL".equals(status)) {
            tickets = new java.util.ArrayList<>(allTickets);
        } else if ("NEW".equals(status)) {
            tickets = allTickets.stream()
                    .filter(t -> "new".equalsIgnoreCase(t.getNormalisedStatus()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            tickets = allTickets.stream()
                    .filter(t -> status.equalsIgnoreCase(t.getNormalisedStatus()))
                    .collect(java.util.stream.Collectors.toList());
        }
        notifyDataSetChanged();
    }

    public int countByStatus(String status) {
        if (status == null || "ALL".equals(status)) return allTickets.size();
        if ("NEW".equals(status))
            return (int) allTickets.stream()
                    .filter(t -> "new".equalsIgnoreCase(t.getNormalisedStatus()))
                    .count();
        return (int) allTickets.stream()
                .filter(t -> status.equalsIgnoreCase(t.getNormalisedStatus()))
                .count();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_ticket, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Ticket t = tickets.get(pos);

        h.tvTitle.setText(t.title != null ? t.title
                : (t.category != null ? t.category + " Issue" : "Issue"));
        String bld = t.building != null ? t.building.toUpperCase() : "N/A";
        h.tvLocation.setText(bld + " - ROOM " + (t.room != null ? t.room : "?"));

        if (h.tvStatus != null) {
            String ns = t.getNormalisedStatus();
            switch (ns) {
                case "in_progress":
                    h.tvStatus.setText("IN PROGRESS");
                    h.tvStatus.setTextColor(
                            ContextCompat.getColor(h.itemView.getContext(), R.color.status_in_progress));
                    break;
                case "done":
                    h.tvStatus.setText("DONE");
                    h.tvStatus.setTextColor(
                            ContextCompat.getColor(h.itemView.getContext(), R.color.status_done));
                    break;
                default:

                    boolean isAssigned = t.assignedTechId != null
                            || t.assignedTechnicianId != null
                            || (t.assignedTechName != null && !t.assignedTechName.isEmpty());
                    h.tvStatus.setText(isAssigned ? "ASSIGNED" : "NEW");
                    h.tvStatus.setTextColor(
                            ContextCompat.getColor(h.itemView.getContext(),
                                    isAssigned ? R.color.status_in_progress : R.color.status_new));
                    break;
            }
        }

        String ns = t.getNormalisedStatus();
        boolean isNew = "new".equalsIgnoreCase(ns);
        boolean isInProgress = "in_progress".equalsIgnoreCase(ns);
        boolean isDone = "done".equalsIgnoreCase(ns) || t.isDone();

        String assigned = t.assignedTechId != null ? t.assignedTechId : t.assignedTechnicianId;
        if (assigned != null) {

            String techLabel = t.assignedTechName;
            if (techLabel == null || techLabel.isEmpty()) {
                String numId = assigned.replaceAll("^[^0-9]+", "");
                for (User u : DataManager.getInstance().getUsersByRole("technician")) {
                    if (u.id == null) continue;
                    String uNum = u.id.replaceAll("^[^0-9]+", "");
                    if (assigned.equals(u.id) || (!numId.isEmpty() && numId.equals(uNum))) {
                        techLabel = u.name;
                        t.assignedTechName = u.name;
                        break;
                    }
                }
            }
            if (techLabel == null || techLabel.isEmpty()) techLabel = "Tech #" + assigned;
            h.btnAssign.setText("Reassign: " + techLabel);
        } else if (!isNew && (isInProgress || isDone)) {

            h.btnAssign.setText("Assigned");
        } else {
            h.btnAssign.setText("Assign");
        }

        boolean enableAssign = isNew;
        h.btnAssign.setEnabled(enableAssign);
        h.btnAssign.setAlpha(enableAssign ? 1.0f : 0.4f);

        h.btnAssign.setOnClickListener(v -> {
            if (!enableAssign) return;
            showAssignDialog(v, h, t, pos);
        });

        if (h.btnDetails != null) {
            h.btnDetails.setOnClickListener(v -> {

                Intent i = new Intent(v.getContext(), LiveTrackingActivity.class);
                i.putExtra("ticket_id", t.id);
                v.getContext().startActivity(i);
            });
        }
    }

    private void showAssignDialog(View v, VH h, Ticket t, int pos) {
        List<User> techs = DataManager.getInstance().getUsersByRole("technician");
        if (techs.isEmpty()) {
            Toast.makeText(v.getContext(), "No technicians available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = techs.stream().map(u -> u.name).toArray(String[]::new);

        new AlertDialog.Builder(v.getContext())
                .setTitle("Assign Technician")
                .setItems(names, (d, which) -> {
                    User tech = techs.get(which);
                    doAssign(v, h, t, pos, tech);
                })
                .show();
    }

    private void doAssign(View v, VH h, Ticket t, int pos, User tech) {
        String token = ApiClient.getAuthToken();

        DataManager.getInstance().assignTech(t.id, tech.id, tech.name);
        t.assignedTechId       = tech.id;
        t.assignedTechnicianId = tech.id;
        t.assignedTechName     = tech.name;

        Toast.makeText(v.getContext(), tech.name + " assigned!", Toast.LENGTH_SHORT).show();
        notifyItemChanged(pos);

        if (token == null || t.id == null) return;
        try {
            long numRepairId = Long.parseLong(t.id);
            long numTechId   = Long.parseLong(tech.id.replaceAll("^[^0-9]+", ""));

            Map<String, Object> body = new HashMap<>();
            body.put("technicianId", numTechId);

            h.btnAssign.setEnabled(false);
            ApiClient.getService().assignRepair(token, numRepairId, body)
                    .enqueue(new Callback<Ticket>() {
                        @Override
                        public void onResponse(Call<Ticket> call, Response<Ticket> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Ticket updated = response.body();
                                if (updated.photoPath == null && t.photoPath != null)
                                    updated.photoPath = t.photoPath;
                                DataManager.getInstance().addTicket(updated);
                            }
                            if (h.itemView.getHandler() != null) {
                                h.itemView.post(() -> {
                                    h.btnAssign.setEnabled(true);
                                    notifyItemChanged(pos);
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<Ticket> call, Throwable err) {

                            if (h.itemView.getHandler() != null) {
                                h.itemView.post(() -> h.btnAssign.setEnabled(true));
                            }
                        }
                    });
        } catch (NumberFormatException ignored) {

        }
    }

    private static String nvl(String s) {
        return (s != null && !s.isEmpty()) ? s : "—";
    }

    @Override public int getItemCount() { return tickets.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView    tvTitle, tvLocation, tvStatus;
        TextView    btnAssign;
        ImageButton btnDetails;

        VH(View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tv_ticket_title);
            tvLocation = v.findViewById(R.id.tv_ticket_location);
            tvStatus   = v.findViewById(R.id.tv_ticket_status);
            btnAssign  = v.findViewById(R.id.btn_assign);
            btnDetails = v.findViewById(R.id.btn_details);
        }
    }
}