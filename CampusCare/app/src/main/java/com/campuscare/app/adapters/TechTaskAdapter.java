package com.campuscare.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.activities.TechJobDetailActivity;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TechTaskAdapter extends RecyclerView.Adapter<TechTaskAdapter.VH> {
    private List<Ticket> tickets;

    public TechTaskAdapter(List<Ticket> tickets) { this.tickets = tickets; }

    public void updateData(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tech_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Ticket t = tickets.get(pos);
        Context ctx = h.itemView.getContext();

        h.tvTicketId.setText("#" + (t.category != null ? t.category : "MISC")
                + "-" + t.getShortId());
        h.tvTitle.setText(t.title != null ? t.title
                : (t.category != null ? t.category + " Issue" : "Issue"));
        h.tvLocation.setText((t.building != null ? t.building : "N/A")
                + (t.room != null ? " · " + t.room : ""));

        String ns = t.getNormalisedStatus();
        boolean isNew  = "new".equalsIgnoreCase(ns);
        boolean isInProgress = "in_progress".equalsIgnoreCase(ns);
        boolean isDone = "done".equalsIgnoreCase(ns) || t.isDone();
        boolean isAssigned = (t.assignedTechId != null || t.assignedTechnicianId != null);

        if (isNew && !isAssigned) {

            h.tvStatusBadge.setText("NEW REQUEST");
            h.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_new));
            h.btnAccept.setVisibility(View.VISIBLE);
            if (h.colorBar != null)
                h.colorBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_new));

        } else if (isInProgress) {

            h.tvStatusBadge.setText("IN PROGRESS");
            h.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_in_progress));
            h.btnAccept.setVisibility(View.GONE);
            if (h.colorBar != null)
                h.colorBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_in_progress));

        } else if (isDone) {

            h.tvStatusBadge.setText("COMPLETED");
            h.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_done));
            h.btnAccept.setVisibility(View.GONE);

            if (h.colorBar != null)
                h.colorBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_done));

        } else {

            h.tvStatusBadge.setText(t.getStatusDisplay());
            h.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
            h.btnAccept.setVisibility(View.GONE);
            if (h.colorBar != null)
                h.colorBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.text_secondary));
        }

        if (h.llRatingRow != null) {
            if (isDone && t.rating > 0) {
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

        h.btnAccept.setOnClickListener(v -> acceptJob(v, h, t, pos));

        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), TechJobDetailActivity.class);
            i.putExtra("ticket_id", t.id);
            v.getContext().startActivity(i);
        });
    }

    private void acceptJob(View v, VH h, Ticket t, int pos) {
        h.btnAccept.setEnabled(false);
        h.btnAccept.setText("Accepting...");

        String token    = ApiClient.getAuthToken();
        String techId   = DataManager.getInstance().getCurrentUser().id;
        String techName = DataManager.getInstance().getCurrentUser().name;

        if (token != null && t.id != null) {
            try {
                long numId = Long.parseLong(t.id);
                ApiClient.getService().acceptRepair(token, numId)
                        .enqueue(new Callback<Ticket>() {
                            @Override
                            public void onResponse(Call<Ticket> call, Response<Ticket> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    DataManager.getInstance().addTicket(response.body());
                                }
                                applyAccepted(h, t, pos, techId, techName);
                            }
                            @Override
                            public void onFailure(Call<Ticket> call, Throwable err) {
                                applyAccepted(h, t, pos, techId, techName);
                                Toast.makeText(v.getContext(),
                                        "Accepted offline. Will sync later.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                return;
            } catch (NumberFormatException ignored) {}
        }

        applyAccepted(h, t, pos, techId, techName);
    }

    private void applyAccepted(VH h, Ticket t, int pos,
                               String techId, String techName) {
        DataManager.getInstance().assignTech(t.id, techId, techName);
        t.assignedTechId         = techId;
        t.assignedTechnicianId   = techId;
        t.assignedTechName       = techName;
        t.status                 = "IN_PROGRESS";

        if (h.itemView.getHandler() != null) {
            h.itemView.post(() -> {

                int currentPos = tickets.indexOf(t);
                if (currentPos >= 0) {
                    tickets.remove(currentPos);
                    notifyItemRemoved(currentPos);
                }
                Toast.makeText(h.itemView.getContext(),
                        "Accepted! Find it in Active Jobs.",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() { return tickets.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTicketId, tvTitle, tvLocation, tvStatusBadge;
        TextView tvStars, tvRatingComment;
        Button btnAccept;
        View colorBar;
        LinearLayout llRatingRow;
        VH(View v) {
            super(v);
            tvTicketId      = v.findViewById(R.id.tv_ticket_id);
            tvTitle         = v.findViewById(R.id.tv_title);
            tvLocation      = v.findViewById(R.id.tv_location);
            tvStatusBadge   = v.findViewById(R.id.tv_status_badge);
            btnAccept       = v.findViewById(R.id.btn_accept);
            colorBar        = v.findViewById(R.id.view_color_bar);
            llRatingRow     = v.findViewById(R.id.ll_rating_row);
            tvStars         = v.findViewById(R.id.tv_stars);
            tvRatingComment = v.findViewById(R.id.tv_rating_comment);
        }
    }
}