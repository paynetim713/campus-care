package com.campuscare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.adapters.TechTaskAdapter;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.DataManager;
import com.campuscare.app.utils.NotificationBadgeHelper;
import java.util.ArrayList;
import java.util.List;

public class TechJobBoardActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private TechTaskAdapter taskAdapter;
    private TextView tvJobsDone, tvInPipeline, tvActiveJobsHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_job_board);

        drawerLayout     = findViewById(R.id.drawer_layout);
        tvJobsDone       = findViewById(R.id.tv_jobs_done);
        tvInPipeline     = findViewById(R.id.tv_in_pipeline);
        tvActiveJobsHint = findViewById(R.id.tv_active_jobs_hint);

        setupNavigationDrawer();

        RecyclerView rv = findViewById(R.id.rv_tasks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TechTaskAdapter(new ArrayList<>());
        rv.setAdapter(taskAdapter);

        loadTasks();

        if (findViewById(R.id.btn_schedule) != null)
            findViewById(R.id.btn_schedule).setOnClickListener(v ->
                    startActivity(new Intent(this, TechScheduleActivity.class)));

        if (findViewById(R.id.btn_notifications) != null)
            findViewById(R.id.btn_notifications).setOnClickListener(v ->
                    startActivity(new Intent(this, NotificationsActivity.class)));
        NotificationBadgeHelper.refresh(this, findViewById(R.id.badge_dot));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
        NotificationBadgeHelper.refresh(this, findViewById(R.id.badge_dot));
    }

    private void loadTasks() {
        String techId = DataManager.getInstance().getCurrentUser().id;

        DataManager.getInstance().fetchAssignedRepairs(
                new retrofit2.Callback<List<Ticket>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Ticket>> call,
                                           retrofit2.Response<List<Ticket>> response) {
                        if (response.isSuccessful() && response.body() != null)
                            for (Ticket t : response.body())
                                DataManager.getInstance().addTicket(t);
                        fetchAvailableAndRefresh(techId);
                    }
                    @Override
                    public void onFailure(retrofit2.Call<List<Ticket>> call, Throwable t) {
                        fetchAvailableAndRefresh(techId);
                    }
                });
    }

    private void fetchAvailableAndRefresh(String techId) {
        DataManager.getInstance().fetchAvailableRepairs(
                new retrofit2.Callback<List<Ticket>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Ticket>> call,
                                           retrofit2.Response<List<Ticket>> response) {
                        if (response.isSuccessful() && response.body() != null)
                            for (Ticket t : response.body())
                                DataManager.getInstance().addTicket(t);
                        runOnUiThread(() -> refreshDisplay(techId));
                    }
                    @Override
                    public void onFailure(retrofit2.Call<List<Ticket>> call, Throwable t) {
                        runOnUiThread(() -> refreshDisplay(techId));
                    }
                });
    }

    private void refreshDisplay(String techId) {
        List<Ticket> availableTasks = DataManager.getInstance().getTicketsForTech(techId);
        List<Ticket> myActive       = DataManager.getInstance().getActiveTicketsForTech(techId);
        List<Ticket> myDone         = DataManager.getInstance().getCompletedTicketsForTech(techId);

        tvJobsDone.setText(String.valueOf(myDone.size()));
        tvInPipeline.setText(String.format("%02d", myActive.size()));

        taskAdapter.updateData(availableTasks);

        if (tvActiveJobsHint != null) {
            if (!myActive.isEmpty()) {
                tvActiveJobsHint.setText("You have " + myActive.size()
                        + " job(s) in progress → Active Jobs");
                tvActiveJobsHint.setVisibility(View.VISIBLE);
                tvActiveJobsHint.setOnClickListener(v ->
                        startActivity(new Intent(this, TechActiveJobsActivity.class)));
            } else {
                tvActiveJobsHint.setVisibility(View.GONE);
            }
        }
    }

    private void openChatPicker() {
        String techId = DataManager.getInstance().getCurrentUser().id;
        String tNum = techId != null ? techId.replaceAll("^[^0-9]+", "") : "";

        List<Ticket> myActive = new ArrayList<>();
        for (Ticket t : DataManager.getInstance().getAllTickets()) {
            String assigned = t.assignedTechId != null ? t.assignedTechId : t.assignedTechnicianId;
            if (assigned == null) continue;
            String aNum = assigned.replaceAll("^[^0-9]+", "");
            boolean mine = assigned.equals(techId) || (!aNum.isEmpty() && aNum.equals(tNum));
            if (mine && "in_progress".equalsIgnoreCase(t.getNormalisedStatus())) {
                myActive.add(t);
            }
        }

        if (myActive.isEmpty()) {
            android.widget.Toast.makeText(this,
                    "No active jobs to chat on. Accept a job first.",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        if (myActive.size() == 1) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("ticket_id", myActive.get(0).id);
            startActivity(i);
            return;
        }

        // Multiple active jobs — let technician pick
        String[] labels = new String[myActive.size()];
        for (int i = 0; i < myActive.size(); i++) {
            Ticket t = myActive.get(i);
            String loc = (t.building != null ? t.building : "") +
                    (t.room != null ? " · " + t.room : "");
            labels[i] = "#" + t.getShortId() + "  " +
                    (t.category != null ? t.category : "Issue") +
                    (loc.trim().isEmpty() ? "" : " — " + loc.trim());
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select a ticket to chat")
                .setItems(labels, (dialog, which) -> {
                    Intent i = new Intent(this, ChatActivity.class);
                    i.putExtra("ticket_id", myActive.get(which).id);
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupNavigationDrawer() {
        View btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null)
            btnMenu.setOnClickListener(v ->
                    drawerLayout.openDrawer(findViewById(R.id.nav_drawer)));

        User user = DataManager.getInstance().getCurrentUser();
        View navDrawer = findViewById(R.id.nav_drawer);
        if (navDrawer == null) return;

        if (user != null) {
            TextView tvName = navDrawer.findViewById(R.id.tv_tech_name);
            if (tvName != null) tvName.setText(user.name);
        }

        navDrawer.findViewById(R.id.nav_job_board).setOnClickListener(v ->
                drawerLayout.closeDrawers());

        navDrawer.findViewById(R.id.nav_schedule).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, TechScheduleActivity.class));
        });

        navDrawer.findViewById(R.id.nav_active_jobs).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, TechActiveJobsActivity.class));
        });

        navDrawer.findViewById(R.id.nav_completed_jobs).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, TechCompletedJobsActivity.class));
        });

        navDrawer.findViewById(R.id.nav_profile).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, MyProfileActivity.class));
        });

        View navChat = navDrawer.findViewById(R.id.nav_chat);
        if (navChat != null) {
            navChat.setOnClickListener(v -> {
                drawerLayout.closeDrawers();
                openChatPicker();
            });
        }

        navDrawer.findViewById(R.id.nav_logout).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            DataManager.getInstance().setCurrentUser(null);
            com.campuscare.app.utils.ApiClient.clearAll();
            Intent i = new Intent(this, SplashActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
}