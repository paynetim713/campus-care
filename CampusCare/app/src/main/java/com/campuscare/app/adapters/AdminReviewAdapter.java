package com.campuscare.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import java.util.List;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.VH> {

    public static class Review {
        public String ticketId, techName, comment, reviewer, date, adminReply;
        public boolean hasTextComment;
        public int rating;
        public Review(String ticketId, String techName, String comment, String reviewer,
                      String date, int rating, boolean hasTextComment) {
            this.ticketId = ticketId;
            this.techName = techName;
            this.comment  = comment;
            this.reviewer = reviewer;
            this.date     = date;
            this.rating   = rating;
            this.hasTextComment = hasTextComment;
        }
    }

    public interface OnReplyClickListener {
        void onReplyClick(Review review);
    }

    private List<Review> reviews;
    private final OnReplyClickListener onReplyClickListener;

    public AdminReviewAdapter(List<Review> reviews, OnReplyClickListener onReplyClickListener) {
        this.reviews = reviews;
        this.onReplyClickListener = onReplyClickListener;
    }

    public void updateData(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Review r = reviews.get(pos);
        h.tvTechName.setText(r.techName.toUpperCase());
        h.tvComment.setText("\"" + r.comment + "\"");
        h.tvReviewer.setText("BY: " + r.reviewer.toUpperCase());
        h.tvDate.setText(r.date);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < r.rating; i++) stars.append("★");
        h.tvRating.setText(stars + " " + r.rating);

        if (r.adminReply != null && !r.adminReply.trim().isEmpty()) {
            h.tvAdminReply.setVisibility(View.VISIBLE);
            h.tvAdminReply.setText("ADMIN REPLY: " + r.adminReply);
            h.btnReply.setText("Edit Reply");
        } else {
            h.tvAdminReply.setVisibility(View.GONE);
            h.btnReply.setText("Reply");
        }
        h.btnReply.setOnClickListener(v -> {
            if (onReplyClickListener != null) onReplyClickListener.onReplyClick(r);
        });
    }

    @Override public int getItemCount() { return reviews.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTechName, tvComment, tvReviewer, tvDate, tvRating, tvAdminReply;
        Button btnReply;
        VH(View v) {
            super(v);
            tvTechName = v.findViewById(R.id.tv_tech_name);
            tvComment  = v.findViewById(R.id.tv_comment);
            tvReviewer = v.findViewById(R.id.tv_reviewer);
            tvDate     = v.findViewById(R.id.tv_date);
            tvRating   = v.findViewById(R.id.tv_rating);
            tvAdminReply = v.findViewById(R.id.tv_admin_reply);
            btnReply = v.findViewById(R.id.btn_reply);
        }
    }
}