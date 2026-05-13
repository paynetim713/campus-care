package com.campuscare.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.models.Notification;

import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnDeleteListener {
        void onDelete(Notification n, int position);
    }

    private List<Notification>  items;
    private final OnDeleteListener onDelete;

    public NotificationAdapter(List<Notification> items, OnDeleteListener onDelete) {
        this.items    = items;
        this.onDelete = onDelete;
    }

    public void updateData(List<Notification> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Notification n = items.get(pos);

        h.tvTitle.setText(n.title != null ? n.title : "");
        h.tvMessage.setText(n.body != null ? n.body : "");
        h.tvTime.setText(formatTime(n.createdAt));

        int titleColor = n.read
                ? h.itemView.getContext().getColor(com.campuscare.app.R.color.text_secondary)
                : h.itemView.getContext().getColor(com.campuscare.app.R.color.primary);
        h.tvTitle.setTextColor(titleColor);
        h.tvTitle.setTypeface(null, n.read
                ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);

        if (h.tvUnreadDot != null)
            h.tvUnreadDot.setVisibility(n.read ? View.INVISIBLE : View.VISIBLE);

        if (h.btnDelete != null) {
            h.btnDelete.setOnClickListener(v -> {
                int currentPos = h.getAdapterPosition();
                if (currentPos != RecyclerView.NO_ID && onDelete != null)
                    onDelete.onDelete(n, currentPos);
            });
        }
    }

    @Override public int getItemCount() { return items.size(); }

    private String formatTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            java.time.Instant instant = java.time.Instant.parse(iso);
            java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
            return java.time.format.DateTimeFormatter
                    .ofPattern("MMM dd, HH:mm", java.util.Locale.getDefault()).format(zdt);
        } catch (Exception e) {
            return iso.length() > 16 ? iso.substring(0, 16).replace("T", " ") : iso;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView    tvTitle, tvMessage, tvTime, tvUnreadDot;
        ImageButton btnDelete;
        VH(View v) {
            super(v);
            tvTitle     = v.findViewById(R.id.tv_title);
            tvMessage   = v.findViewById(R.id.tv_message);
            tvTime      = v.findViewById(R.id.tv_time);
            tvUnreadDot = v.findViewById(R.id.tv_unread_dot);
            btnDelete   = v.findViewById(R.id.btn_delete_notif);
        }
    }
}