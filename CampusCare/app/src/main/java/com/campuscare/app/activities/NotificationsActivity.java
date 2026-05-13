package com.campuscare.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.adapters.NotificationAdapter;
import com.campuscare.app.models.Notification;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private NotificationAdapter adapter;
    private List<Notification>  currentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        RecyclerView rv = findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationAdapter(currentList, this::onDelete);
        rv.setAdapter(adapter);

        loadNotifications();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView btnReadAll = findViewById(R.id.btn_mark_all_read);
        if (btnReadAll != null) {
            btnReadAll.setOnClickListener(v -> markAllRead());
        }
    }

    private void loadNotifications() {
        String token = ApiClient.getAuthToken();
        if (token != null) {
            ApiClient.getService().getNotifications(token)
                    .enqueue(new Callback<List<Notification>>() {
                        @Override
                        public void onResponse(Call<List<Notification>> call,
                                               Response<List<Notification>> r) {
                            if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                                runOnUiThread(() -> showData(r.body()));
                            } else {
                                runOnUiThread(NotificationsActivity.this::showFallback);
                            }
                        }
                        @Override
                        public void onFailure(Call<List<Notification>> call, Throwable t) {
                            runOnUiThread(NotificationsActivity.this::showFallback);
                        }
                    });
        } else {
            showFallback();
        }
    }

    private void showData(List<Notification> list) {
        currentList.clear();
        currentList.addAll(list);
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        TextView tvEmpty = findViewById(R.id.tv_empty_notif);
        if (tvEmpty != null)
            tvEmpty.setVisibility(currentList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void markAllRead() {
        String token = ApiClient.getAuthToken();
        if (token == null) {

            for (Notification n : currentList) n.read = true;
            adapter.notifyDataSetChanged();
            return;
        }
        ApiClient.getService().markAllNotificationsRead(token)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> r) {
                        runOnUiThread(() -> {
                            for (Notification n : currentList) n.read = true;
                            adapter.notifyDataSetChanged();
                            Toast.makeText(NotificationsActivity.this,
                                    "All marked as read", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        runOnUiThread(() -> Toast.makeText(NotificationsActivity.this,
                                "Failed. Check connection.", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void onDelete(Notification n, int pos) {
        String token = ApiClient.getAuthToken();

        currentList.remove(pos);
        adapter.notifyItemRemoved(pos);
        updateEmptyState();

        if (token == null || n.id == null) return;
        try {
            long numId = Long.parseLong(n.id);
            ApiClient.getService().deleteNotification(token, numId)
                    .enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                        @Override public void onFailure(Call<Void> c, Throwable t) {

                        }
                    });
        } catch (NumberFormatException ignored) {}
    }

    private void showFallback() {
        List<Notification> notifs = new ArrayList<>();
        String uid = DataManager.getInstance().getCurrentUser() != null
                ? DataManager.getInstance().getCurrentUser().id : null;
        if (uid != null) {
            for (com.campuscare.app.models.Ticket t : DataManager.getInstance().getAllTickets()) {
                String owner = t.reportedById != null ? t.reportedById : t.requesterId;
                if (uid.equals(owner)) {
                    if (t.isDone()) {
                        notifs.add(new Notification("Job Completed",
                                "Please rate your experience for Case #" + t.getShortId(), "Recently"));
                    } else if ("in_progress".equalsIgnoreCase(t.getNormalisedStatus())) {
                        notifs.add(new Notification("Technician on the way",
                                (t.assignedTechName != null ? t.assignedTechName : "A technician")
                                        + " accepted request #" + t.getShortId(), "Recently"));
                    } else {
                        notifs.add(new Notification("Request Received",
                                "Your request #" + t.getShortId() + " has been received.", "Recently"));
                    }
                }
            }
        }
        if (notifs.isEmpty())
            notifs.add(new Notification("No Notifications", "You have no notifications yet.", ""));
        showData(notifs);
    }
}