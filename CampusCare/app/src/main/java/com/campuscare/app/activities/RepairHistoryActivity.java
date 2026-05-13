package com.campuscare.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.adapters.HistoryAdapter;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.utils.DataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RepairHistoryActivity extends AppCompatActivity {

    private HistoryAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_history);

        RecyclerView rv = findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tv_empty);

        String uid = DataManager.getInstance().getCurrentUser().id;
        List<Ticket> local = DataManager.getInstance().getHistoryTicketsForUser(uid);
        showData(local);

        DataManager.getInstance().fetchMyRepairs(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Ticket t : response.body()) {
                        DataManager.getInstance().addTicket(t);
                    }

                    List<Ticket> history = response.body().stream()
                            .filter(Ticket::isDone)
                            .collect(Collectors.toList());
                    runOnUiThread(() -> showData(history));
                } else {

                    runOnUiThread(() -> showData(
                            DataManager.getInstance().getHistoryTicketsForUser(uid)));
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                runOnUiThread(() -> showData(
                        DataManager.getInstance().getHistoryTicketsForUser(uid)));
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void showData(List<Ticket> tickets) {
        adapter.updateData(tickets);
        if (tvEmpty != null) {
            tvEmpty.setVisibility(tickets.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}