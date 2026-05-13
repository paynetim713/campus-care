package com.campuscare.app.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import java.util.Calendar;

public class TechScheduleActivity extends AppCompatActivity {
    private Calendar currentMonth = Calendar.getInstance();
    private int selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_schedule);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1); buildCalendar();
        });
        findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1); buildCalendar();
        });

        buildCalendar();
    }

    private void buildCalendar() {
        TextView tvMonth = findViewById(R.id.tv_month_year);
        String[] months = {"January","February","March","April","May","June",
            "July","August","September","October","November","December"};
        tvMonth.setText(months[currentMonth.get(Calendar.MONTH)] + " " + currentMonth.get(Calendar.YEAR));

        GridLayout grid = findViewById(R.id.calendar_grid);
        grid.removeAllViews();

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        boolean isCurrentMonth = currentMonth.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)
            && currentMonth.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR);

        for (int i = 0; i < firstDay; i++) {
            TextView empty = new TextView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.width = 0;
            empty.setLayoutParams(lp);
            grid.addView(empty);
        }

        for (int d = 1; d <= daysInMonth; d++) {
            final int day = d;
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(d));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(4, 8, 4, 8);
            tv.setTextSize(13);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.width = 0;
            tv.setLayoutParams(lp);

            if (isCurrentMonth && d == today) {
                tv.setBackgroundResource(R.drawable.bg_teal_card);
                tv.setTextColor(Color.WHITE);
            } else if (d == selectedDay) {
                tv.setTextColor(getColor(R.color.primary));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tv.setTextColor(getColor(R.color.text_primary));
            }
            tv.setOnClickListener(v -> { selectedDay = day; buildCalendar(); });
            grid.addView(tv);
        }
    }
}
