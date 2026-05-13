package com.campuscare.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.campuscare.app.R;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TechJobDetailActivity extends AppCompatActivity {

    private String currentStatus = "new";
    private String ticketId;
    private Ticket ticket;
    private Button btnNew, btnFixing, btnDone, btnSave;
    private TextView tvFinish;
    private Button btnAccept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_job_detail);

        ticketId = getIntent().getStringExtra("ticket_id");
        ticket   = findTicket(ticketId);

        btnNew    = findViewById(R.id.btn_status_new);
        btnFixing = findViewById(R.id.btn_status_fixing);
        btnDone   = findViewById(R.id.btn_status_done);
        btnSave   = findViewById(R.id.btn_save);
        btnAccept = findViewById(R.id.btn_accept_job);

        tvFinish = ((LinearLayout) findViewById(R.id.ll_expected_finish))
                .findViewById(R.id.tv_expected_finish);

        EditText etNote = findViewById(R.id.et_note);

        renderTicket(ticket);

        if (ticket != null) {
            String eta = ticket.eta != null ? ticket.eta
                    : (ticket.expectedFinish != null ? ticket.expectedFinish : null);
            if (eta != null && !eta.isEmpty()) tvFinish.setText(eta);
            if (ticket.technicianNote != null) etNote.setText(ticket.technicianNote);
            if (ticket.techNote != null && etNote.getText().toString().isEmpty())
                etNote.setText(ticket.techNote);
        }

        refreshFromApiAndLoadPhotos();

        btnNew.setOnClickListener(v    -> selectStatus("new"));
        btnFixing.setOnClickListener(v -> selectStatus("in_progress"));
        btnDone.setOnClickListener(v   -> selectStatus("done"));

        LinearLayout llFinish = findViewById(R.id.ll_expected_finish);
        llFinish.setOnClickListener(v -> openDateTimePicker());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_chat).setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            if (ticketId != null) i.putExtra("ticket_id", ticketId);
            startActivity(i);
        });
        btnSave.setOnClickListener(v -> saveChanges(etNote));

        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> acceptJob());
        }

        applyEditingLock();
    }

    private void applyEditingLock() {
        String techId    = DataManager.getInstance().getCurrentUser().id;
        boolean accepted = isAcceptedByMe(ticket, techId);
        boolean isDone   = ticket != null && ticket.isDone();

        btnNew.setEnabled(accepted);
        btnFixing.setEnabled(accepted);
        btnDone.setEnabled(accepted && !isDone);
        btnNew.setAlpha(accepted ? 1.0f : 0.4f);
        btnFixing.setAlpha(accepted ? 1.0f : 0.4f);
        btnDone.setAlpha(accepted ? 1.0f : 0.4f);

        btnSave.setEnabled(accepted);
        btnSave.setAlpha(accepted ? 1.0f : 0.4f);

        LinearLayout llFinish = findViewById(R.id.ll_expected_finish);
        if (llFinish != null) llFinish.setEnabled(accepted);
        EditText etNote = findViewById(R.id.et_note);
        if (etNote != null) etNote.setEnabled(accepted);

        if (btnAccept != null) {
            boolean isNew        = ticket != null
                    && ("new".equalsIgnoreCase(ticket.getNormalisedStatus()));
            boolean assignedToMe = isAssignedToMe(ticket, techId);
            btnAccept.setVisibility((isNew && assignedToMe) ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isAcceptedByMe(Ticket t, String techId) {
        if (t == null || techId == null) return false;
        boolean inProgress = "in_progress".equalsIgnoreCase(t.getNormalisedStatus());
        return inProgress && isAssignedToMe(t, techId);
    }

    private void acceptJob() {
        if (ticket == null || ticketId == null) return;
        if (btnAccept != null) { btnAccept.setEnabled(false); btnAccept.setText("Accepting..."); }

        String token  = ApiClient.getAuthToken();
        String techId = DataManager.getInstance().getCurrentUser().id;

        if (token == null) {
            applyLocalAccept(techId);
            applyEditingLock();
            return;
        }

        try {
            long numId = Long.parseLong(ticketId);
            ApiClient.getService().acceptRepair(token, numId)
                    .enqueue(new Callback<Ticket>() {
                        @Override public void onResponse(Call<Ticket> call, Response<Ticket> r) {
                            if (r.isSuccessful() && r.body() != null) {
                                Ticket fresh = r.body();
                                if (fresh.photoPath == null && ticket != null)
                                    fresh.photoPath = ticket.photoPath;
                                ticket = fresh;
                                DataManager.getInstance().addTicket(fresh);
                            } else {
                                applyLocalAccept(techId);
                            }
                            runOnUiThread(() -> {
                                renderTicket(ticket);
                                loadEvidencePhotos(ticket);
                                applyEditingLock();
                                Toast.makeText(TechJobDetailActivity.this,
                                        "Job accepted! Update the status as you work.",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onFailure(Call<Ticket> call, Throwable t) {
                            applyLocalAccept(techId);
                            runOnUiThread(() -> {
                                applyEditingLock();
                                Toast.makeText(TechJobDetailActivity.this,
                                        "Accepted offline. Will sync later.",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } catch (NumberFormatException e) {
            applyLocalAccept(techId);
            applyEditingLock();
        }
    }

    private void applyLocalAccept(String techId) {
        if (ticket != null) {
            ticket.assignedTechId       = techId;
            ticket.assignedTechnicianId = techId;
            ticket.status               = "IN_PROGRESS";
        }
        DataManager.getInstance().assignTech(ticketId, techId,
                DataManager.getInstance().getCurrentUser().name);
    }

    private void openDateTimePicker() {
        final Calendar cal = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, year, month, day) ->
                        new TimePickerDialog(this,
                                (v2, hour, min) -> tvFinish.setText(
                                        String.format(Locale.getDefault(),
                                                "%02d %s %d, %02d:%02d",
                                                day, monthAbbr(month), year, hour, min)),
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE), true).show(),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }

    private static String monthAbbr(int m) {
        return new String[]{"Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"}[Math.max(0, Math.min(m, 11))];
    }

    private void renderTicket(Ticket t) {
        if (t == null) return;

        ((TextView) findViewById(R.id.tv_ticket_id))
                .setText("#" + t.getShortId());
        ((TextView) findViewById(R.id.tv_location))
                .setText(nvl(t.building, "") + (t.room != null ? ", " + t.room : ""));
        ((TextView) findViewById(R.id.tv_floor_area))
                .setText("FLOOR " + nvl(t.floor, "-") + " - " + nvl(t.category, ""));

        TextView tvDesc = findViewById(R.id.tv_description);
        if (tvDesc != null) {
            if (t.details != null && !t.details.isEmpty()) {
                tvDesc.setText(t.details);
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }
        }

        currentStatus = t.getNormalisedStatus();
        selectStatus(currentStatus);
        updateRatingRow(t);

    }

    private void updateRatingRow(Ticket t) {
        LinearLayout ll = findViewById(R.id.ll_rating_row);
        TextView tvStars = findViewById(R.id.tv_stars);
        TextView tvComment = findViewById(R.id.tv_rating_comment);
        if (ll == null || tvStars == null || tvComment == null) return;

        if (t == null || !t.isDone()) {
            ll.setVisibility(View.GONE);
            return;
        }

        ll.setVisibility(View.VISIBLE);
        boolean hasRating = t.rating > 0;
        if (hasRating) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append(i < t.rating ? "★" : "☆");
            tvStars.setText(sb.toString());
            tvComment.setText(
                    t.ratingComment != null && !t.ratingComment.isEmpty()
                            ? "\"" + t.ratingComment + "\""
                            : "");
        } else {
            tvStars.setText("☆☆☆☆☆");
            tvComment.setText("No review yet.");
        }
    }

    private void loadEvidencePhotos(Ticket t) {
        HorizontalScrollView hsv   = findViewById(R.id.hsv_evidence);
        LinearLayout         llRow = findViewById(R.id.ll_evidence_photos);
        TextView             label = findViewById(R.id.tv_evidence_label);
        if (hsv == null || llRow == null || t == null) return;

        llRow.removeAllViews();

        String csv = (t.photoUrl != null && !t.photoUrl.isEmpty())
                ? t.photoUrl : t.photoPath;

        if (csv == null || csv.isEmpty()) {
            hsv.setVisibility(View.GONE);
            if (label != null) label.setVisibility(View.GONE);
            return;
        }

        int loaded = 0;
        for (String raw : csv.split(",")) {
            String url = raw.trim();
            if (url.isEmpty()) continue;

            ImageView iv = new ImageView(this);
            int sizePx = (int)(160 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMarginEnd((int)(8 * getResources().getDisplayMetrics().density));
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundResource(R.drawable.bg_input);

            if (url.startsWith("content://") || url.startsWith("file://")) {
                iv.setImageURI(Uri.parse(url));
            } else {

                String absolute = ApiClient.toAbsoluteUrl(url);
                com.bumptech.glide.Glide.with(this)
                        .load(absolute)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(iv);
            }

            final String tapUrl = url;
            iv.setOnClickListener(v -> showPhotoFullscreen(tapUrl));
            llRow.addView(iv);
            loaded++;
        }

        boolean has = loaded > 0;
        hsv.setVisibility(has ? View.VISIBLE : View.GONE);
        if (label != null) label.setVisibility(has ? View.VISIBLE : View.GONE);
    }

    private void refreshFromApiAndLoadPhotos() {
        String token = ApiClient.getAuthToken();

        if (token == null) {

            if (ticket != null) runOnUiThread(() -> loadEvidencePhotos(ticket));
            return;
        }

        if (ticketId == null) {
            if (ticket != null) runOnUiThread(() -> loadEvidencePhotos(ticket));
            return;
        }

        long numId;
        try {
            numId = Long.parseLong(ticketId);
        } catch (NumberFormatException e) {

            if (ticket != null) runOnUiThread(() -> loadEvidencePhotos(ticket));
            return;
        }

        ApiClient.getService().getRepairById(token, numId)
                .enqueue(new Callback<Ticket>() {
                    @Override
                    public void onResponse(Call<Ticket> call, Response<Ticket> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            Ticket fresh = r.body();
                            if (fresh.photoPath == null && ticket != null)
                                fresh.photoPath = ticket.photoPath;
                            ticket = fresh;
                            DataManager.getInstance().addTicket(fresh);

                            runOnUiThread(() -> {
                                renderTicket(fresh);

                                EditText etNote = findViewById(R.id.et_note);
                                if (etNote != null && fresh.technicianNote != null
                                        && etNote.getText().toString().isEmpty())
                                    etNote.setText(fresh.technicianNote);
                                if (tvFinish != null && fresh.eta != null
                                        && tvFinish.getText().toString().isEmpty())
                                    tvFinish.setText(fresh.eta);

                                loadEvidencePhotos(fresh);
                                applyEditingLock();
                            });
                        } else {

                            runOnUiThread(() -> {
                                if (ticket != null) loadEvidencePhotos(ticket);
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<Ticket> call, Throwable t) {
                        runOnUiThread(() -> {
                            if (ticket != null) loadEvidencePhotos(ticket);
                        });
                    }
                });
    }

    private void saveChanges(EditText etNote) {
        if (ticket == null) { finish(); return; }

        String eta   = tvFinish != null ? tvFinish.getText().toString().trim() : "";
        String note  = etNote.getText().toString().trim();
        String token = ApiClient.getAuthToken();

        DataManager.getInstance().updateTicketStatus(ticketId, currentStatus);
        ticket.status         = currentStatus;
        ticket.eta            = eta;
        ticket.expectedFinish = eta;
        ticket.techNote       = note;
        ticket.technicianNote = note;

        if (token == null || ticketId == null) {
            Toast.makeText(this, "Saved locally.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            long numId = Long.parseLong(ticketId);
            pushStatusUpdate(token, numId, eta, note);
        } catch (NumberFormatException ignored) {
            Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void pushStatusUpdate(String token, long numId, String eta, String note) {
        if (btnSave != null) { btnSave.setEnabled(false); btnSave.setText("Saving..."); }

        String backendStatus;
        switch (currentStatus) {
            case "in_progress": backendStatus = "IN_PROGRESS"; break;
            case "done":        backendStatus = "DONE";        break;
            default:            backendStatus = "IN_PROGRESS"; break;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", backendStatus);
        if (!eta.isEmpty())  body.put("eta", eta);
        if (!note.isEmpty()) body.put("technicianNote", note);

        ApiClient.getService().updateRepairTask(token, numId, body)
                .enqueue(new Callback<Ticket>() {
                    @Override public void onResponse(Call<Ticket> call, Response<Ticket> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            Ticket fresh = r.body();
                            if (fresh.photoPath == null && ticket != null)
                                fresh.photoPath = ticket.photoPath;
                            DataManager.getInstance().addTicket(fresh);
                        }
                        runOnUiThread(() -> {
                            String msg;
                            switch (currentStatus) {
                                case "in_progress": msg = "Status set to FIXING – check Active Jobs!"; break;
                                case "done":        msg = "Job marked DONE – great work!"; break;
                                default:            msg = "Changes saved!";
                            }
                            Toast.makeText(TechJobDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                    @Override public void onFailure(Call<Ticket> call, Throwable err) {
                        runOnUiThread(() -> {
                            Toast.makeText(TechJobDetailActivity.this,
                                    "Saved locally. Will sync when connected.",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });
    }

    private void selectStatus(String status) {
        currentStatus = status;
        if (btnNew == null || btnFixing == null || btnDone == null) return;

        int unselBg    = R.drawable.bg_chip_unselected;
        int unselColor = getColor(R.color.text_secondary);
        btnNew.setBackgroundResource(unselBg);    btnNew.setTextColor(unselColor);
        btnFixing.setBackgroundResource(unselBg); btnFixing.setTextColor(unselColor);
        btnDone.setBackgroundResource(unselBg);   btnDone.setTextColor(unselColor);

        int white = getColor(R.color.white);
        switch (status) {
            case "new":
                btnNew.setBackgroundResource(R.drawable.bg_status_new);
                btnNew.setTextColor(white); break;
            case "in_progress":
                btnFixing.setBackgroundResource(R.drawable.bg_status_fixing);
                btnFixing.setTextColor(white); break;
            case "done":
                btnDone.setBackgroundResource(R.drawable.bg_status_done);
                btnDone.setTextColor(white); break;
        }
    }

    private void showPhotoFullscreen(String url) {
        ImageView ivFull = new ImageView(this);
        ivFull.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        ivFull.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivFull.setAdjustViewBounds(true);

        if (url.startsWith("content://") || url.startsWith("file://")) {
            try { ivFull.setImageURI(Uri.parse(url)); }
            catch (Exception ignored) {}
        } else {
            com.bumptech.glide.Glide.with(this)
                    .load(ApiClient.toAbsoluteUrl(url))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivFull);
        }

        new AlertDialog.Builder(this)
                .setView(ivFull)
                .setPositiveButton("Close", null)
                .show();
    }

    private boolean isAssignedToMe(Ticket t, String techId) {
        if (t == null || techId == null) return false;
        String assigned = t.assignedTechId != null ? t.assignedTechId : t.assignedTechnicianId;
        if (assigned == null) return false;
        if (assigned.equals(techId)) return true;
        String numA = assigned.replaceAll("^[^0-9]+", "");
        String numB = techId.replaceAll("^[^0-9]+", "");
        return !numA.isEmpty() && numA.equals(numB);
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    private Ticket findTicket(String id) {
        if (id == null) return null;
        for (Ticket t : DataManager.getInstance().getAllTickets())
            if (t.id != null && t.id.equals(id)) return t;
        return null;
    }
}