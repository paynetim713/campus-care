package com.campuscare.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.adapters.TechTaskAdapter;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.utils.DataManager;
import java.util.List;

public class TechCompletedJobsActivity extends AppCompatActivity {

    private TechTaskAdapter adapter;
    private TextView tvEmpty, tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_task_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_page_title);
        tvTitle.setText("Completed Jobs");

        tvCount = findViewById(R.id.tv_count);
        tvEmpty = findViewById(R.id.tv_empty);
        tvEmpty.setText("No completed jobs yet.");

        RecyclerView rv = findViewById(R.id.rv_tasks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechTaskAdapter(getCompletedTasks());
        rv.setAdapter(adapter);

        updateUI();
        refreshFromApi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.updateData(getCompletedTasks());
        updateUI();
    }

    private void refreshFromApi() {
        DataManager.getInstance().fetchAssignedRepairs(
                new retrofit2.Callback<List<Ticket>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Ticket>> call,
                                           retrofit2.Response<List<Ticket>> resp) {
                        if (resp.isSuccessful() && resp.body() != null)
                            for (Ticket t : resp.body())
                                DataManager.getInstance().addTicket(t);
                        runOnUiThread(() -> {
                            adapter.updateData(getCompletedTasks());
                            updateUI();
                        });
                    }
                    @Override
                    public void onFailure(retrofit2.Call<List<Ticket>> call, Throwable t) {}
                });
    }

    private List<Ticket> getCompletedTasks() {
        String techId = DataManager.getInstance().getCurrentUser().id;
        return DataManager.getInstance().getCompletedTicketsForTech(techId);
    }

    private void updateUI() {
        List<Ticket> tasks = getCompletedTasks();
        tvCount.setText(String.valueOf(tasks.size()));
        tvEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }
}