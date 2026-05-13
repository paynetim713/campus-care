package com.campuscare.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.models.Ticket;
import java.util.List;

public class ActiveOrdersAdapter extends RecyclerView.Adapter<ActiveOrdersAdapter.VH> {
    private List<Ticket> tickets;
    private OnTicketClickListener listener;

    public interface OnTicketClickListener { void onTicketClick(Ticket ticket); }

    public ActiveOrdersAdapter(List<Ticket> tickets, OnTicketClickListener listener) {
        this.tickets = tickets; this.listener = listener;
    }

    public void updateData(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Ticket t = tickets.get(pos);
        h.tvTitle.setText(t.title != null ? t.title
                : (t.category != null ? t.category + " Issue" : "Issue"));
        h.tvLocation.setText(t.getLocationString());
        h.tvStatus.setText(t.getStatusDisplay());
        String st = t.getNormalisedStatus();
        switch (st) {
            case "new":         h.tvStatus.setTextColor(Color.parseColor("#EF4444")); break;
            case "in_progress": h.tvStatus.setTextColor(Color.parseColor("#F59E0B")); break;
            case "done":
            case "completed":   h.tvStatus.setTextColor(Color.parseColor("#10B981")); break;
            default:            h.tvStatus.setTextColor(Color.parseColor("#6B7280")); break;
        }
        h.itemView.setOnClickListener(v -> listener.onTicketClick(t));
    }

    @Override public int getItemCount() { return tickets.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvStatus;
        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_title);
            tvLocation = v.findViewById(R.id.tv_location);
            tvStatus = v.findViewById(R.id.tv_status);
        }
    }
}