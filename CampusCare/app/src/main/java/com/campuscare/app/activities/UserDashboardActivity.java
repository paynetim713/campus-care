package com.campuscare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.adapters.ActiveOrdersAdapter;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.DataManager;
import com.campuscare.app.utils.NotificationBadgeHelper;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);

        setupNavigationDrawer();
        loadActiveOrders();

        findViewById(R.id.btn_report_now).setOnClickListener(v ->
                startActivity(new Intent(this, ReportIssueActivity.class)));

        findViewById(R.id.tv_history).setOnClickListener(v ->
                startActivity(new Intent(this, RepairHistoryActivity.class)));

        ImageButton btnBell = findViewById(R.id.btn_notifications);
        btnBell.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        NotificationBadgeHelper.refresh(this, findViewById(R.id.badge_dot));
    }

    private void setupNavigationDrawer() {

        findViewById(R.id.btn_menu).setOnClickListener(v ->
                drawerLayout.openDrawer(findViewById(R.id.nav_drawer)));

        User user = DataManager.getInstance().getCurrentUser();
        View navDrawer = findViewById(R.id.nav_drawer);
        if (navDrawer != null && user != null) {
            android.widget.TextView tvName = navDrawer.findViewById(R.id.tv_user_name);
            android.widget.TextView tvRole = navDrawer.findViewById(R.id.tv_user_role);
            if (tvName != null) tvName.setText(user.name);
            if (tvRole != null) {
                String label = "Requester";
                if ("student".equalsIgnoreCase(user.role))      label = "Student";
                else if ("teacher".equalsIgnoreCase(user.role)) label = "Teacher";
                tvRole.setText(label);
            }

            navDrawer.findViewById(R.id.nav_report_issue).setOnClickListener(v -> {
                drawerLayout.closeDrawers();
                startActivity(new Intent(this, ReportIssueActivity.class));
            });
            navDrawer.findViewById(R.id.nav_active_orders).setOnClickListener(v ->
                    drawerLayout.closeDrawers());

            navDrawer.findViewById(R.id.nav_history).setOnClickListener(v -> {
                drawerLayout.closeDrawers();
                startActivity(new Intent(this, RepairHistoryActivity.class));
            });
            navDrawer.findViewById(R.id.nav_profile).setOnClickListener(v -> {
                drawerLayout.closeDrawers();
                startActivity(new Intent(this, MyProfileActivity.class));
            });

            View navChat = navDrawer.findViewById(R.id.nav_chat);
            if (navChat != null) {
                navChat.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    String uid = DataManager.getInstance().getCurrentUser().id;
                    com.campuscare.app.models.Ticket latest = null;
                    for (com.campuscare.app.models.Ticket t
                            : DataManager.getInstance().getActiveTicketsForUser(uid)) {
                        if (latest == null || (t.id != null && latest.id != null
                                && t.id.compareTo(latest.id) > 0)) {
                            latest = t;
                        }
                    }
                    Intent i = new Intent(this, ChatActivity.class);
                    if (latest != null) i.putExtra("ticket_id", latest.id);

                    startActivity(i);
                });
            }
            navDrawer.findViewById(R.id.nav_logout).setOnClickListener(v -> {
                DataManager.getInstance().setCurrentUser(null);
                com.campuscare.app.utils.ApiClient.clearAll();
                Intent i = new Intent(this, SplashActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            });
        }
    }

    private void loadActiveOrders() {
        DataManager.getInstance().fetchMyRepairs(new retrofit2.Callback<java.util.List<com.campuscare.app.models.Ticket>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.campuscare.app.models.Ticket>> call,
                                   retrofit2.Response<java.util.List<com.campuscare.app.models.Ticket>> response) {
                java.util.List<com.campuscare.app.models.Ticket> orders;
                if (response.isSuccessful() && response.body() != null) {

                    for (com.campuscare.app.models.Ticket t : response.body()) {
                        DataManager.getInstance().addTicket(t);
                    }
                    orders = response.body().stream()
                            .filter(t -> !t.isDone())
                            .collect(java.util.stream.Collectors.toList());
                } else {
                    String uid = DataManager.getInstance().getCurrentUser().id;
                    orders = DataManager.getInstance().getActiveTicketsForUser(uid);
                }
                runOnUiThread(() -> updateOrdersList(orders));
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.campuscare.app.models.Ticket>> call, Throwable t) {
                String uid = DataManager.getInstance().getCurrentUser().id;
                updateOrdersList(DataManager.getInstance().getActiveTicketsForUser(uid));
            }
        });
    }

    private void updateOrdersList(java.util.List<com.campuscare.app.models.Ticket> orders) {
        RecyclerView rv = findViewById(R.id.rv_active_orders);
        if (rv.getAdapter() == null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new ActiveOrdersAdapter(orders, ticket -> {
                Intent i = new Intent(this, LiveTrackingActivity.class);
                i.putExtra("ticket_id", ticket.id);
                startActivity(i);
            }));
        } else {
            ((ActiveOrdersAdapter) rv.getAdapter()).updateData(orders);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveOrders();
        NotificationBadgeHelper.refresh(this, findViewById(R.id.badge_dot));
    }
}