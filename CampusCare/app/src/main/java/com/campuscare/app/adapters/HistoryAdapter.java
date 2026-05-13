package com.campuscare.app.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.activities.LiveTrackingActivity;
import com.campuscare.app.models.Ticket;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private List<Ticket> tickets;

    public HistoryAdapter(List<Ticket> tickets) { this.tickets = tickets; }

    public void updateData(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Ticket t = tickets.get(pos);

        String label = t.category != null ? t.category + " Issue" : "Repair Issue";
        if (t.title != null && !t.title.isEmpty()) label = t.title;
        h.tvTicket.setText("Ticket #" + t.getShortId() + " - " + label);

        h.tvDate.setText("FILED · " + formatDate(t.createdAt, t.createdAtLocal).toUpperCase(Locale.getDefault()));

        View.OnClickListener openDetail = v -> {
            Intent i = new Intent(v.getContext(), LiveTrackingActivity.class);
            i.putExtra("ticket_id", t.id);
            v.getContext().startActivity(i);
        };
        h.itemView.setOnClickListener(openDetail);
        h.btnMore.setOnClickListener(openDetail);

        if (h.llRatingRow != null) {
            if (t.rating > 0) {
                h.llRatingRow.setVisibility(View.VISIBLE);
                if (h.tvStars != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 5; i++) sb.append(i < t.rating ? "★" : "☆");
                    h.tvStars.setText(sb.toString());
                }
                if (h.tvRatingComment != null) {
                    h.tvRatingComment.setText(
                            t.ratingComment != null && !t.ratingComment.isEmpty()
                                    ? "\"" + t.ratingComment + "\""
                                    : "");
                }
            } else {
                h.llRatingRow.setVisibility(View.GONE);
            }
        }

        if (h.tvAdminReply != null) {
            if (t.adminReply != null && !t.adminReply.trim().isEmpty()) {
                h.tvAdminReply.setVisibility(View.VISIBLE);
                h.tvAdminReply.setText("Admin reply: " + t.adminReply);
            } else {
                h.tvAdminReply.setVisibility(View.GONE);
            }
        }
    }

    @Override public int getItemCount() { return tickets.size(); }

    private String formatDate(String isoString, long epochMillis) {

        if (isoString != null && !isoString.isEmpty()) {
            try {
                java.time.Instant instant = java.time.Instant.parse(isoString);
                java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
                return java.time.format.DateTimeFormatter
                        .ofPattern("MMM dd, yyyy", Locale.getDefault())
                        .format(zdt);
            } catch (Exception ignored) {

            }
        }

        if (epochMillis > 0) {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new java.util.Date(epochMillis));
        }
        return "Unknown Date";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTicket, tvDate, tvStars, tvRatingComment, tvAdminReply;
        ImageButton btnMore;
        LinearLayout llRatingRow;
        VH(View v) {
            super(v);
            tvTicket        = v.findViewById(R.id.tv_ticket);
            tvDate          = v.findViewById(R.id.tv_date);
            btnMore         = v.findViewById(R.id.btn_more);
            llRatingRow     = v.findViewById(R.id.ll_rating_row);
            tvStars         = v.findViewById(R.id.tv_stars);
            tvRatingComment = v.findViewById(R.id.tv_rating_comment);
            tvAdminReply    = v.findViewById(R.id.tv_admin_reply);
            v.setClickable(true);
            v.setFocusable(true);
        }
    }
}