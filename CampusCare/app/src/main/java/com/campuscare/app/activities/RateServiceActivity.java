package com.campuscare.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;

import com.campuscare.app.R;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.utils.ApiErrorUtils;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RateServiceActivity extends AppCompatActivity {
    private int rating = 4;
    private ImageButton[] stars;
    private TextView tvRatingLabel;
    private String ticketId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_service);

        ticketId = getIntent().getStringExtra("ticket_id");
        tvRatingLabel = findViewById(R.id.tv_rating_label);

        stars = new ImageButton[]{
                findViewById(R.id.star1), findViewById(R.id.star2),
                findViewById(R.id.star3), findViewById(R.id.star4), findViewById(R.id.star5)
        };
        updateStars(rating);

        for (int i = 0; i < stars.length; i++) {
            final int starVal = i + 1;
            stars[i].setOnClickListener(v -> {
                rating = starVal;
                updateStars(rating);
            });
        }

        EditText etComments = findViewById(R.id.et_comments);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            String comment = etComments.getText().toString().trim();
            submitRating(rating, comment);
        });
    }

    private void submitRating(int stars, String comment) {
        String token = ApiClient.getAuthToken();
        if (token != null && ticketId != null) {
            try {
                long id = Long.parseLong(ticketId);
                Map<String, Object> body = new HashMap<>();
                body.put("stars", stars);
                body.put("comment", comment);
                ApiClient.getService().rateRepair(token, id, body).enqueue(new Callback<Ticket>() {
                    @Override
                    public void onResponse(Call<Ticket> call, Response<Ticket> response) {
                        runOnUiThread(() -> {
                            if (response.isSuccessful() && response.body() != null) {
                                DataManager.getInstance().addTicket(response.body());
                                showSuccessAndFinish(stars);
                            } else {
                                Toast.makeText(RateServiceActivity.this,
                                        ApiErrorUtils.extractMessage(response, "Failed to submit review."),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<Ticket> call, Throwable t) {
                        runOnUiThread(() -> Toast.makeText(RateServiceActivity.this,
                                "Cannot connect to server. Review was not submitted.",
                                Toast.LENGTH_LONG).show());
                    }
                });
                return;
            } catch (NumberFormatException ignored) {
            }
        }

        if (ticketId != null) {
            for (Ticket t : DataManager.getInstance().getAllTickets()) {
                if (ticketId.equals(t.id)) {
                    t.rating = stars;
                    t.ratingComment = comment;
                    break;
                }
            }
        }
        showSuccessAndFinish(stars);
    }

    private void showSuccessAndFinish(int stars) {
        Toast.makeText(this, "Thank you for your feedback! (" + stars + "/5)", Toast.LENGTH_LONG).show();

        Intent result = new Intent();
        if (ticketId != null) result.putExtra("ticket_id", ticketId);
        result.putExtra("rated", true);
        result.putExtra("stars", stars);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void updateStars(int r) {
        int colorOn = getColor(R.color.star_color);
        int colorOff = getColor(R.color.text_hint);

        String[] labels = {"", "Poor", "Fair", "Good", "Great", "Excellent"};
        if (tvRatingLabel != null) {
            tvRatingLabel.setText(r + " out of 5 - " + labels[r]);
        }

        for (int i = 0; i < stars.length; i++) {
            boolean on = i < r;
            stars[i].setImageResource(on
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);
            ImageViewCompat.setImageTintList(stars[i],
                    ColorStateList.valueOf(on ? colorOn : colorOff));
        }
    }
}
