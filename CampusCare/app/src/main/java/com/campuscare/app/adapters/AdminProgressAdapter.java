package com.campuscare.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.models.Ticket;
import java.util.List;

public class AdminProgressAdapter extends RecyclerView.Adapter<AdminProgressAdapter.VH> {
    private List<Ticket> tickets;
    public AdminProgressAdapter(List<Ticket> tickets) { this.tickets = tickets; }

    public void updateData(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_progress, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Ticket t = tickets.get(pos);
        h.tvCase.setText("Case #" + t.id + " - " + t.title);
        int pct = t.progressPercent > 0 ? t.progressPercent : 20;
        h.tvPercent.setText(pct + "%");
        h.progressBar.setProgress(pct);
        String ns = t.getNormalisedStatus();
        boolean isAssigned = t.assignedTechName != null && !t.assignedTechName.isEmpty();
        boolean isStarted  = "in_progress".equalsIgnoreCase(ns) || "done".equalsIgnoreCase(ns) || t.isDone();
        if (isAssigned) {
            h.tvAssigned.setText("Assigned to: " + t.assignedTechName);
        } else if (isStarted) {
            h.tvAssigned.setText("Assigned");
        } else {
            h.tvAssigned.setText("Unassigned");
        }
    }

    @Override public int getItemCount() { return tickets.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCase, tvPercent, tvAssigned;
        ProgressBar progressBar;
        VH(View v) {
            super(v);
            tvCase = v.findViewById(R.id.tv_case);
            tvPercent = v.findViewById(R.id.tv_percent);
            tvAssigned = v.findViewById(R.id.tv_assigned);
            progressBar = v.findViewById(R.id.progress_bar);
        }
    }
}