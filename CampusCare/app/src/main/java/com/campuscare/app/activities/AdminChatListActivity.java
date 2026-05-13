package com.campuscare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminChatListActivity extends AppCompatActivity {

    private TicketChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.admin_bg));

        android.widget.LinearLayout header = new android.widget.LinearLayout(this);
        header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int p = dp(20);
        header.setPadding(p, dp(24), p, dp(16));

        android.widget.ImageButton btnBack = new android.widget.ImageButton(this);
        btnBack.setImageResource(android.R.drawable.ic_media_previous);
        btnBack.setBackgroundResource(0);
        android.widget.LinearLayout.LayoutParams backLp =
                new android.widget.LinearLayout.LayoutParams(dp(40), dp(40));
        backLp.setMarginEnd(dp(12));
        btnBack.setLayoutParams(backLp);
        btnBack.setOnClickListener(v -> finish());
        header.addView(btnBack);

        android.widget.LinearLayout titles = new android.widget.LinearLayout(this);
        titles.setOrientation(android.widget.LinearLayout.VERTICAL);
        TextView tvSub = new TextView(this);
        tvSub.setText("ADMIN");
        tvSub.setTextSize(10f);
        tvSub.setTextColor(getColor(R.color.admin_text_secondary));
        tvSub.setLetterSpacing(0.1f);
        titles.addView(tvSub);
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Case Chats");
        tvTitle.setTextSize(20f);
        tvTitle.setTextColor(getColor(R.color.admin_text));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        titles.addView(tvTitle);
        header.addView(titles);
        root.addView(header);

        TextView tvHint = new TextView(this);
        tvHint.setText("Tap a case to view or join the group chat");
        tvHint.setTextSize(12f);
        tvHint.setTextColor(getColor(R.color.admin_text_secondary));
        tvHint.setPadding(p, 0, p, dp(12));
        root.addView(tvHint);

        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setPadding(dp(12), 0, dp(12), dp(12));
        rv.setClipToPadding(false);
        android.widget.LinearLayout.LayoutParams rvLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0);
        rvLp.weight = 1;
        rv.setLayoutParams(rvLp);

        adapter = new TicketChatAdapter(new ArrayList<>());
        rv.setAdapter(adapter);
        root.addView(rv);

        setContentView(root);
        loadTickets();
    }

    private void loadTickets() {
        String token = ApiClient.getAuthToken();
        if (token != null) {
            ApiClient.getService().getAllRepairs(token)
                    .enqueue(new Callback<List<Ticket>>() {
                        @Override public void onResponse(Call<List<Ticket>> c,
                                                         Response<List<Ticket>> r) {
                            if (r.isSuccessful() && r.body() != null) {
                                for (Ticket t : r.body())
                                    DataManager.getInstance().addTicket(t);
                            }
                            runOnUiThread(() -> adapter.updateData(
                                    DataManager.getInstance().getAllTickets()));
                        }
                        @Override public void onFailure(Call<List<Ticket>> c, Throwable t) {
                            runOnUiThread(() -> adapter.updateData(
                                    DataManager.getInstance().getAllTickets()));
                        }
                    });
        } else {
            adapter.updateData(DataManager.getInstance().getAllTickets());
        }
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    private class TicketChatAdapter
            extends RecyclerView.Adapter<TicketChatAdapter.VH> {

        private List<Ticket> items;
        TicketChatAdapter(List<Ticket> items) { this.items = items; }

        void updateData(List<Ticket> list) { items = list; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            android.widget.LinearLayout card = new android.widget.LinearLayout(parent.getContext());
            card.setOrientation(android.widget.LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_admin_card);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            card.setLayoutParams(lp);

            android.widget.LinearLayout row1 = new android.widget.LinearLayout(parent.getContext());
            row1.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvId = new TextView(parent.getContext());
            tvId.setTag("tv_id");
            tvId.setTextSize(11f);
            tvId.setTextColor(parent.getContext().getColor(R.color.admin_text_secondary));
            android.widget.LinearLayout.LayoutParams idLp =
                    new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            idLp.weight = 1;
            tvId.setLayoutParams(idLp);
            row1.addView(tvId);

            TextView tvStatus = new TextView(parent.getContext());
            tvStatus.setTag("tv_status");
            tvStatus.setTextSize(10f);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setLetterSpacing(0.05f);
            row1.addView(tvStatus);
            card.addView(row1);

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTag("tv_title");
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(parent.getContext().getColor(R.color.admin_text));
            android.widget.LinearLayout.LayoutParams titleLp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            titleLp.topMargin = dp(4);
            titleLp.bottomMargin = dp(6);
            tvTitle.setLayoutParams(titleLp);
            card.addView(tvTitle);

            TextView tvOpenChat = new TextView(parent.getContext());
            tvOpenChat.setTag("tv_chat");
            tvOpenChat.setText("💬 Open Group Chat →");
            tvOpenChat.setTextSize(12f);
            tvOpenChat.setTextColor(parent.getContext().getColor(R.color.primary));
            tvOpenChat.setTypeface(null, android.graphics.Typeface.BOLD);
            tvOpenChat.setBackground(ContextCompat.getDrawable(
                    parent.getContext(), R.drawable.bg_chip_unselected));
            tvOpenChat.setPadding(dp(12), dp(6), dp(12), dp(6));
            card.addView(tvOpenChat);

            return new VH(card);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Ticket t = items.get(pos);

            ((TextView) h.itemView.findViewWithTag("tv_id"))
                    .setText("#" + t.getShortId()
                            + (t.category != null ? "  " + t.category : ""));

            String subject = t.title != null && !t.title.isEmpty()
                    ? t.title : (t.category != null ? t.category + " Issue" : "Issue");
            ((TextView) h.itemView.findViewWithTag("tv_title")).setText(subject);

            TextView tvStatus = h.itemView.findViewWithTag("tv_status");
            tvStatus.setText(t.getStatusDisplay());
            int color;
            switch (t.getStatusDisplay()) {
                case "IN PROGRESS": color = R.color.status_in_progress; break;
                case "DONE":        color = R.color.status_done;        break;
                default:            color = R.color.status_new;         break;
            }
            tvStatus.setTextColor(h.itemView.getContext().getColor(color));

            h.itemView.findViewWithTag("tv_chat").setOnClickListener(v -> {
                Intent i = new Intent(AdminChatListActivity.this, ChatActivity.class);
                i.putExtra("ticket_id", t.id);
                startActivity(i);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }
    }
}
