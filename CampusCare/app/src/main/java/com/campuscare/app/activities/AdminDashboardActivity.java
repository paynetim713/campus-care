package com.campuscare.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.adapters.*;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;
import com.campuscare.app.utils.ApiService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private View layoutDashboard, layoutUsers, layoutTickets, layoutReviews;
    private TextView tabDash, tabUsers, tabTickets, tabReviews;
    private AdminUserAdapter userAdapter;
    private AdminProgressAdapter progressAdapter;
    private AdminTicketAdapter ticketAdapter;
    private String currentUserSubtab   = "student";
    private String currentTicketFilter = "ALL";
    private DrawerLayout drawerLayout;

    private TextView tvMonthlyRequests, tvAvgResolution;
    private TextView tvReviewsCount, tvReviewsAvg;
    private EditText etReviewSearch;
    private CheckBox cbReviewsTextOnly;
    private Spinner spReviewRatingFilter, spReviewSort;
    private AdminReviewAdapter reviewAdapter;
    private final List<AdminReviewAdapter.Review> allReviews = new java.util.ArrayList<>();
    private final List<AdminReviewAdapter.Review> filteredReviews = new java.util.ArrayList<>();
    private int visibleReviewCount = 0;
    private static final int REVIEW_PAGE_SIZE = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);

        SharedPreferences prefs = getSharedPreferences("campus_care_prefs", MODE_PRIVATE);
        String savedUrl = prefs.getString("server_url", null);
        if (savedUrl != null) ApiClient.updateBaseUrl(savedUrl);

        setupNavigationDrawer();

        layoutDashboard = findViewById(R.id.layout_dashboard);
        layoutUsers     = findViewById(R.id.layout_users);
        layoutTickets   = findViewById(R.id.layout_tickets);
        layoutReviews   = findViewById(R.id.layout_reviews);

        tabDash    = findViewById(R.id.tab_dashboard);
        tabUsers   = findViewById(R.id.tab_users);
        tabTickets = findViewById(R.id.tab_tickets);
        tabReviews = findViewById(R.id.tab_reviews);

        tabDash.setOnClickListener(v    -> switchTab(0));
        tabUsers.setOnClickListener(v   -> switchTab(1));
        tabTickets.setOnClickListener(v -> switchTab(2));
        tabReviews.setOnClickListener(v -> switchTab(3));

        tvMonthlyRequests = findViewById(R.id.tv_monthly_requests);
        tvAvgResolution   = findViewById(R.id.tv_avg_resolution);
        tvReviewsCount = findViewById(R.id.tv_reviews_count);
        tvReviewsAvg = findViewById(R.id.tv_reviews_avg);
        etReviewSearch = findViewById(R.id.et_review_search);
        cbReviewsTextOnly = findViewById(R.id.cb_reviews_text_only);
        spReviewRatingFilter = findViewById(R.id.sp_review_rating_filter);
        spReviewSort = findViewById(R.id.sp_review_sort);

        RecyclerView rvProgress = findViewById(R.id.rv_progress);
        rvProgress.setLayoutManager(new LinearLayoutManager(this));
        progressAdapter = new AdminProgressAdapter(DataManager.getInstance().getInProgressTickets());
        rvProgress.setAdapter(progressAdapter);

        RecyclerView rvUsers = findViewById(R.id.rv_users);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new AdminUserAdapter(DataManager.getInstance().getUsersByRole("student"));
        rvUsers.setAdapter(userAdapter);

        TextView subtabStudents = findViewById(R.id.subtab_students);
        TextView subtabTeachers = findViewById(R.id.subtab_teachers);
        TextView subtabTechs    = findViewById(R.id.subtab_technicians);
        subtabStudents.setOnClickListener(v -> switchUserSubtab("student",  subtabStudents, subtabTeachers, subtabTechs));
        subtabTeachers.setOnClickListener(v -> switchUserSubtab("lecturer", subtabStudents, subtabTeachers, subtabTechs));
        subtabTechs.setOnClickListener(v    -> switchUserSubtab("technician", subtabStudents, subtabTeachers, subtabTechs));

        findViewById(R.id.btn_add_user).setOnClickListener(v ->
                startActivity(new Intent(this, AdminAddUserActivity.class)));

        RecyclerView rvTickets = findViewById(R.id.rv_tickets);
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new AdminTicketAdapter(DataManager.getInstance().getAllTickets());
        rvTickets.setAdapter(ticketAdapter);

        TextView stAll  = findViewById(R.id.ticket_tab_all);
        TextView stNew  = findViewById(R.id.ticket_tab_new);
        TextView stProg = findViewById(R.id.ticket_tab_inprogress);
        TextView stDone = findViewById(R.id.ticket_tab_done);
        if (stAll != null) {
            stAll.setOnClickListener(v  -> switchTicketFilter("ALL",         stAll, stNew, stProg, stDone));
            stNew.setOnClickListener(v  -> switchTicketFilter("NEW",         stAll, stNew, stProg, stDone));
            stProg.setOnClickListener(v -> switchTicketFilter("IN_PROGRESS", stAll, stNew, stProg, stDone));
            stDone.setOnClickListener(v -> switchTicketFilter("DONE",        stAll, stNew, stProg, stDone));
        }

        setupReviewControls();
        loadReviews();
        switchTab(0);

        loadUsersFromApi();

        loadAllTicketsFromApi();

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            DataManager.getInstance().setCurrentUser(null);
            com.campuscare.app.utils.ApiClient.clearAll();
            Intent i = new Intent(this, SplashActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void loadTechniciansFromApi() {
        DataManager.getInstance().fetchTechnicians(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DataManager.getInstance().setTechnicianUsers(response.body());
                    if (userAdapter != null) {
                        userAdapter.updateData(DataManager.getInstance().getUsersByRole(currentUserSubtab));
                    }
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {

            }
        });
    }

    private void loadAllTicketsFromApi() {
        DataManager.getInstance().fetchAllRepairsForAdmin(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Ticket t : response.body()) {
                        DataManager.getInstance().addTicket(t);
                    }
                    runOnUiThread(() -> {
                        progressAdapter.updateData(DataManager.getInstance().getInProgressTickets());
                        ticketAdapter.updateData(DataManager.getInstance().getAllTickets());

                        ticketAdapter.filterByStatus(currentTicketFilter);
                        updateTicketFilterCounts();
                        loadReviews();

                        updateDashboardStats();
                    });
                }
            }
            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                runOnUiThread(this::updateStatsFromCache);
            }

            private void updateStatsFromCache() {
                updateDashboardStats();
            }
        });
    }

    private void updateDashboardStats() {
        if (tvMonthlyRequests == null || tvAvgResolution == null) return;

        List<Ticket> all = DataManager.getInstance().getAllTickets();

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String monthPrefix = String.format(Locale.US, "%04d-%02d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);

        int monthlyCount = 0;
        long totalResolutionMs = 0;
        int doneCount = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (Ticket t : all) {

            if (t.createdAt != null && t.createdAt.startsWith(monthPrefix)) {
                monthlyCount++;
            }

            if (t.isDone() && t.createdAt != null && t.updatedAt != null) {
                try {

                    String c = t.createdAt.replaceAll("\\.[0-9]+Z?$", "").replace("Z", "");
                    String u = t.updatedAt.replaceAll("\\.[0-9]+Z?$", "").replace("Z", "");
                    Date created = sdf.parse(c);
                    Date updated = sdf.parse(u);
                    if (created != null && updated != null && updated.after(created)) {
                        totalResolutionMs += (updated.getTime() - created.getTime());
                        doneCount++;
                    }
                } catch (ParseException ignored) {  }
            }
        }

        tvMonthlyRequests.setText(String.valueOf(monthlyCount));

        if (doneCount > 0) {
            double avgHours = (totalResolutionMs / (double) doneCount) / 3_600_000.0;
            if (avgHours < 1.0) {
                long avgMins = Math.round(avgHours * 60);
                tvAvgResolution.setText(avgMins + "m");
            } else {
                tvAvgResolution.setText(String.format(Locale.US, "%.1fh", avgHours));
            }
        } else {
            tvAvgResolution.setText("—");
        }
    }

    private void loadReviews() {
        RecyclerView rvReviews = findViewById(R.id.rv_reviews);
        if (rvReviews == null) return;
        if (rvReviews.getLayoutManager() == null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            rvReviews.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy <= 0 || filteredReviews.isEmpty()) return;
                    LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (lm == null) return;
                    int last = lm.findLastVisibleItemPosition();
                    if (last >= visibleReviewCount - 3 && visibleReviewCount < filteredReviews.size()) {
                        visibleReviewCount = Math.min(visibleReviewCount + REVIEW_PAGE_SIZE, filteredReviews.size());
                        refreshReviewListView();
                    }
                }
            });
        }

        allReviews.clear();
        for (Ticket t : DataManager.getInstance().getAllTickets()) {
            if (t.rating > 0) {
                String techId = t.assignedTechId != null ? t.assignedTechId : t.assignedTechnicianId;
                String tech = resolveName(t.assignedTechName, techId, "technician", "Technician");

                String rawId = t.reportedById != null ? t.reportedById : t.requesterId;
                String reviewer = resolveName(null, rawId, "student", null);
                if (reviewer == null) reviewer = resolveName(null, rawId, "lecturer", null);
                if (reviewer == null) reviewer = rawId != null ? "User #" + rawId : "User";

                boolean hasTextComment = t.ratingComment != null && !t.ratingComment.trim().isEmpty();
                String comment = hasTextComment ? t.ratingComment : "Rated " + t.rating + " stars";
                String date = t.updatedAt != null ? t.updatedAt : "Recently";
                AdminReviewAdapter.Review review =
                        new AdminReviewAdapter.Review(t.id, tech, comment, reviewer, date, t.rating, hasTextComment);
                review.adminReply = t.adminReply;
                allReviews.add(review);
            }
        }

        applyReviewFilters(true);
    }

    private void setupReviewControls() {
        if (spReviewRatingFilter != null) {
            ArrayAdapter<String> ratingAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"All Ratings", "5 Stars", "4 Stars", "3 Stars", "2 Stars", "1 Star"});
            ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spReviewRatingFilter.setAdapter(ratingAdapter);
            spReviewRatingFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    applyReviewFilters(true);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (spReviewSort != null) {
            ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"Newest", "Oldest", "Highest Rating", "Lowest Rating"});
            sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spReviewSort.setAdapter(sortAdapter);
            spReviewSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    applyReviewFilters(true);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (etReviewSearch != null) {
            etReviewSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyReviewFilters(true);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (cbReviewsTextOnly != null) {
            cbReviewsTextOnly.setOnCheckedChangeListener((buttonView, isChecked) -> applyReviewFilters(true));
        }
    }

    private void applyReviewFilters(boolean resetVisibleCount) {
        filteredReviews.clear();
        String keyword = etReviewSearch != null && etReviewSearch.getText() != null
                ? etReviewSearch.getText().toString().trim().toLowerCase(Locale.US) : "";
        int exactRating = parseRatingFilter();
        boolean textOnly = cbReviewsTextOnly != null && cbReviewsTextOnly.isChecked();

        for (AdminReviewAdapter.Review r : allReviews) {
            if (exactRating > 0 && r.rating != exactRating) continue;
            if (textOnly && !r.hasTextComment) continue;
            if (!keyword.isEmpty()) {
                String hay = (r.techName + " " + r.comment + " " + r.reviewer).toLowerCase(Locale.US);
                if (!hay.contains(keyword)) continue;
            }
            filteredReviews.add(r);
        }

        String sort = spReviewSort != null && spReviewSort.getSelectedItem() != null
                ? spReviewSort.getSelectedItem().toString() : "Newest";
        filteredReviews.sort((a, b) -> {
            switch (sort) {
                case "Oldest":
                    return safeString(a.date).compareToIgnoreCase(safeString(b.date));
                case "Highest Rating":
                    return Integer.compare(b.rating, a.rating);
                case "Lowest Rating":
                    return Integer.compare(a.rating, b.rating);
                case "Newest":
                default:
                    return safeString(b.date).compareToIgnoreCase(safeString(a.date));
            }
        });

        if (resetVisibleCount) visibleReviewCount = Math.min(REVIEW_PAGE_SIZE, filteredReviews.size());
        else if (visibleReviewCount == 0 && !filteredReviews.isEmpty()) visibleReviewCount = Math.min(REVIEW_PAGE_SIZE, filteredReviews.size());

        refreshReviewStats();
        refreshReviewListView();
    }

    private void refreshReviewListView() {
        RecyclerView rvReviews = findViewById(R.id.rv_reviews);
        if (rvReviews == null) return;

        List<AdminReviewAdapter.Review> page = new java.util.ArrayList<>();
        int upper = Math.min(visibleReviewCount, filteredReviews.size());
        for (int i = 0; i < upper; i++) page.add(filteredReviews.get(i));

        if (reviewAdapter == null) {
            reviewAdapter = new AdminReviewAdapter(page, this::showReplyDialog);
            rvReviews.setAdapter(reviewAdapter);
        } else {
            reviewAdapter.updateData(page);
        }
    }

    private void refreshReviewStats() {
        if (tvReviewsCount == null || tvReviewsAvg == null) return;
        int count = filteredReviews.size();
        double avg = 0.0;
        for (AdminReviewAdapter.Review r : filteredReviews) avg += r.rating;
        if (count > 0) avg /= count;

        tvReviewsCount.setText("Reviews: " + count);
        tvReviewsAvg.setText(String.format(Locale.US, "Avg: %.1f", avg));
    }

    private int parseRatingFilter() {
        if (spReviewRatingFilter == null || spReviewRatingFilter.getSelectedItem() == null) return 0;
        String selected = spReviewRatingFilter.getSelectedItem().toString();
        if (selected.startsWith("5")) return 5;
        if (selected.startsWith("4")) return 4;
        if (selected.startsWith("3")) return 3;
        if (selected.startsWith("2")) return 2;
        if (selected.startsWith("1")) return 1;
        return 0;
    }

    private void showReplyDialog(AdminReviewAdapter.Review review) {
        if (review == null) return;
        EditText et = new EditText(this);
        et.setHint("Write admin reply...");
        et.setText(review.adminReply != null ? review.adminReply : "");
        et.setMinLines(2);

        new AlertDialog.Builder(this)
                .setTitle("Reply to Review")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String reply = et.getText() != null ? et.getText().toString().trim() : "";
                    String token = ApiClient.getAuthToken();
                    Long repairId = parseLongOrNull(review.ticketId);
                    if (token == null || repairId == null) {
                        Toast.makeText(this, "Cannot save reply: missing auth or ticket id.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ApiService api = ApiClient.getService();
                    Map<String, String> body = new HashMap<>();
                    body.put("reply", reply);
                    api.saveAdminReply(token, repairId, body).enqueue(new Callback<Ticket>() {
                        @Override
                        public void onResponse(Call<Ticket> call, Response<Ticket> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                DataManager.getInstance().addTicket(response.body());
                                loadReviews();
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Reply saved.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Failed to save reply.", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Ticket> call, Throwable t) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Reply save failed: " + (t != null ? t.getMessage() : "unknown"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String safeString(String s) { return s == null ? "" : s; }

    private Long parseLongOrNull(String raw) {
        if (raw == null) return null;
        try { return Long.parseLong(raw.trim()); }
        catch (Exception ignored) { return null; }
    }

    private String resolveName(String explicitName, String rawId,
                               String role, String fallback) {
        if (explicitName != null && !explicitName.isEmpty()) return explicitName;
        if (rawId == null || rawId.isEmpty()) return fallback;
        String numId = rawId.replaceAll("^[^0-9]+", "");
        for (User u : DataManager.getInstance().getUsersByRole(role)) {
            if (u.id == null) continue;
            String uNum = u.id.replaceAll("^[^0-9]+", "");
            if (rawId.equals(u.id) || (!numId.isEmpty() && numId.equals(uNum)))
                return u.name;
        }
        return fallback;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (userAdapter != null) {
            userAdapter.updateData(DataManager.getInstance().getUsersByRole(currentUserSubtab));
        }
        loadUsersFromApi();
        loadAllTicketsFromApi();
    }

    private void loadUsersFromApi() {
        DataManager.getInstance().fetchAllUsersForAdmin(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {

                runOnUiThread(() -> {
                    if (userAdapter != null) {
                        userAdapter.updateData(DataManager.getInstance().getUsersByRole(currentUserSubtab));
                    }
                });
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                runOnUiThread(() ->
                        Toast.makeText(AdminDashboardActivity.this,
                                "Users refresh failed: " + (t != null ? t.getMessage() : "unknown"),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setupNavigationDrawer() {
        findViewById(R.id.btn_menu).setOnClickListener(v ->
                drawerLayout.openDrawer(findViewById(R.id.nav_drawer)));

        User user = DataManager.getInstance().getCurrentUser();
        if (user != null) {
            View navView = findViewById(R.id.nav_drawer);
            if (navView != null) {
                TextView tvName = navView.findViewById(R.id.tv_admin_name);
                if (tvName != null) tvName.setText(user.name);
            }
        }

        View navDrawer = findViewById(R.id.nav_drawer);
        if (navDrawer != null) {
            navDrawer.findViewById(R.id.nav_dashboard).setOnClickListener(v -> {
                drawerLayout.closeDrawers(); switchTab(0);
            });
            navDrawer.findViewById(R.id.nav_users).setOnClickListener(v -> {
                drawerLayout.closeDrawers(); switchTab(1);
            });
            navDrawer.findViewById(R.id.nav_tickets).setOnClickListener(v -> {
                drawerLayout.closeDrawers(); switchTab(2);
            });
            navDrawer.findViewById(R.id.nav_reviews).setOnClickListener(v -> {
                drawerLayout.closeDrawers(); switchTab(3);
            });
            View navProfile = navDrawer.findViewById(R.id.nav_profile);
            if (navProfile != null) {
                navProfile.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    startActivity(new Intent(AdminDashboardActivity.this, MyProfileActivity.class));
                });
            }

            View navChat = navDrawer.findViewById(R.id.nav_chat);
            if (navChat != null) {
                navChat.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    startActivity(new Intent(AdminDashboardActivity.this,
                            AdminChatListActivity.class));
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

    private void switchTab(int tab) {
        layoutDashboard.setVisibility(View.GONE);
        layoutUsers.setVisibility(View.GONE);
        layoutTickets.setVisibility(View.GONE);
        layoutReviews.setVisibility(View.GONE);

        int normalColor = R.color.admin_text_secondary;
        tabDash.setBackgroundResource(0);    tabDash.setTextColor(getColor(normalColor));
        tabUsers.setBackgroundResource(0);   tabUsers.setTextColor(getColor(normalColor));
        tabTickets.setBackgroundResource(0); tabTickets.setTextColor(getColor(normalColor));
        tabReviews.setBackgroundResource(0); tabReviews.setTextColor(getColor(normalColor));

        switch (tab) {
            case 0:
                layoutDashboard.setVisibility(View.VISIBLE);
                tabDash.setBackgroundResource(R.drawable.bg_teal_card);
                tabDash.setTextColor(getColor(R.color.white));
                updateDashboardStats();
                break;
            case 1:
                layoutUsers.setVisibility(View.VISIBLE);
                tabUsers.setBackgroundResource(R.drawable.bg_teal_card);
                tabUsers.setTextColor(getColor(R.color.white));
                break;
            case 2:
                layoutTickets.setVisibility(View.VISIBLE);
                tabTickets.setBackgroundResource(R.drawable.bg_teal_card);
                tabTickets.setTextColor(getColor(R.color.white));
                break;
            case 3:
                layoutReviews.setVisibility(View.VISIBLE);
                tabReviews.setBackgroundResource(R.drawable.bg_teal_card);
                tabReviews.setTextColor(getColor(R.color.white));
                loadReviews();
                break;
        }
    }

    private void switchUserSubtab(String role, TextView... tabs) {
        currentUserSubtab = role;
        for (TextView t : tabs) {
            t.setBackgroundResource(0);
            t.setTextColor(getColor(R.color.admin_text_secondary));
        }
        int idx = role.equals("student") ? 0 : role.equals("lecturer") ? 1 : 2;
        tabs[idx].setBackgroundResource(R.drawable.bg_teal_card);
        tabs[idx].setTextColor(getColor(R.color.white));
        userAdapter.updateData(DataManager.getInstance().getUsersByRole(role));
    }

    private void switchTicketFilter(String status, TextView... tabs) {
        currentTicketFilter = status;

        for (TextView t : tabs) {
            t.setBackgroundResource(0);
            t.setTextColor(getColor(R.color.admin_text_secondary));
        }

        int idx;
        switch (status) {
            case "NEW":         idx = 1; break;
            case "IN_PROGRESS": idx = 2; break;
            case "DONE":        idx = 3; break;
            default:            idx = 0; break;
        }
        tabs[idx].setBackgroundResource(R.drawable.bg_teal_card);
        tabs[idx].setTextColor(getColor(R.color.white));

        ticketAdapter.filterByStatus(status);
    }

    private void updateTicketFilterCounts() {
        TextView stAll  = findViewById(R.id.ticket_tab_all);
        TextView stNew  = findViewById(R.id.ticket_tab_new);
        TextView stProg = findViewById(R.id.ticket_tab_inprogress);
        TextView stDone = findViewById(R.id.ticket_tab_done);
        if (stAll == null) return;

        int cAll  = ticketAdapter.countByStatus("ALL");
        int cNew  = ticketAdapter.countByStatus("NEW");
        int cProg = ticketAdapter.countByStatus("IN_PROGRESS");
        int cDone = ticketAdapter.countByStatus("DONE");

        stAll.setText("ALL (" + cAll + ")");
        stNew.setText("NEW (" + cNew + ")");
        stProg.setText("IN PROGRESS (" + cProg + ")");
        stDone.setText("DONE (" + cDone + ")");
    }

    private void showSettingsDialog() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(8), pad, dp(4));

        TextView tvServerLabel = new TextView(this);
        tvServerLabel.setText("SERVER URL");
        tvServerLabel.setTextSize(10f);
        tvServerLabel.setTextColor(getColor(R.color.admin_text_secondary));
        tvServerLabel.setLetterSpacing(0.08f);
        tvServerLabel.setPadding(0, 0, 0, dp(6));
        root.addView(tvServerLabel);

        EditText etUrl = new EditText(this);
        etUrl.setHint("http://10.0.2.2:8080/");
        etUrl.setText(ApiClient.getBaseUrl() + "/");
        etUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        etUrl.setSingleLine(true);
        etUrl.setBackgroundResource(R.drawable.bg_input);
        etUrl.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        urlParams.bottomMargin = dp(6);
        etUrl.setLayoutParams(urlParams);
        root.addView(etUrl);

        TextView tvUrlHint = new TextView(this);
        tvUrlHint.setText("Use 10.0.2.2:8080 for emulator · your LAN IP for real device");
        tvUrlHint.setTextSize(11f);
        tvUrlHint.setTextColor(getColor(R.color.text_secondary));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.bottomMargin = dp(20);
        tvUrlHint.setLayoutParams(hintParams);
        root.addView(tvUrlHint);

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.divider));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.bottomMargin = dp(16);
        divider.setLayoutParams(divParams);
        root.addView(divider);

        TextView tvPwLabel = new TextView(this);
        tvPwLabel.setText("ACCOUNT");
        tvPwLabel.setTextSize(10f);
        tvPwLabel.setTextColor(getColor(R.color.admin_text_secondary));
        tvPwLabel.setLetterSpacing(0.08f);
        tvPwLabel.setPadding(0, 0, 0, dp(8));
        root.addView(tvPwLabel);

        Button btnChangePw = new Button(this);
        btnChangePw.setText("Change Password");

        btnChangePw.setTextColor(getColor(R.color.admin_text_secondary));
        btnChangePw.setBackgroundResource(R.drawable.bg_chip_unselected);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.bottomMargin = dp(4);
        btnChangePw.setLayoutParams(btnParams);
        root.addView(btnChangePw);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(root)
                .setPositiveButton("Save URL", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newUrl = etUrl.getText().toString().trim();
                if (newUrl.isEmpty()) {
                    etUrl.setError("URL cannot be empty");
                    return;
                }
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    etUrl.setError("Must start with http:// or https://");
                    return;
                }
                saveServerUrl(newUrl);
                dialog.dismiss();
            });

            btnChangePw.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(AdminDashboardActivity.this, MyProfileActivity.class));
            });
        });

        dialog.show();
    }

    private void saveServerUrl(String url) {

        if (!url.endsWith("/")) url = url + "/";

        SharedPreferences prefs = getSharedPreferences("campus_care_prefs", MODE_PRIVATE);
        prefs.edit().putString("server_url", url).apply();

        ApiClient.updateBaseUrl(url);

        Toast.makeText(this,
                "Server URL updated. Reconnecting…", Toast.LENGTH_SHORT).show();

        loadUsersFromApi();
        loadAllTicketsFromApi();
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }
}