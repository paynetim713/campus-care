package com.campuscare.app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.campuscare.app.R;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.models.UploadResponse;
import com.campuscare.app.utils.ApiErrorUtils;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveTrackingActivity extends AppCompatActivity {

    private static final long POLL_MS = 10_000;
    private static final int  MAX_KB  = 800;

    private String ticketId;
    private final Handler       pollHandler  = new Handler(Looper.getMainLooper());
    private final Runnable      pollRunnable = () -> { refreshFromApi(); schedulePoll(); };
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> rateLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Ticket t = findTicket(ticketId);
                    if (t != null) renderTicket(t);
                    refreshFromApi();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_tracking);

        ticketId = getIntent().getStringExtra("ticket_id");

        renderTicket(findTicket(ticketId));
        refreshFromApi();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_chat).setOnClickListener(v -> {
            Ticket current = findTicket(ticketId);

            boolean assigned = current != null
                    && (current.assignedTechnicianId != null || current.assignedTechId != null);
            if (!assigned) {
                Toast.makeText(this,
                        "Chat is available once a technician has been assigned.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, ChatActivity.class);
            if (ticketId != null) i.putExtra("ticket_id", ticketId);
            startActivity(i);
        });

        Button btnDelete = findViewById(R.id.btn_delete_request);
        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmDelete());

        Button btnReview = findViewById(R.id.btn_confirm_review);
        if (btnReview != null) btnReview.setOnClickListener(v -> {
            Ticket t = findTicket(ticketId);
            if (t == null || !t.isDone()) {
                Toast.makeText(this, "You can rate once the job is completed.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (t.rating > 0) {
                Toast.makeText(this, "You have already submitted a review.", Toast.LENGTH_SHORT).show();
                renderTicket(t);
                return;
            }
            Intent i = new Intent(this, RateServiceActivity.class);
            if (ticketId != null) i.putExtra("ticket_id", ticketId);
            rateLauncher.launch(i);
        });
    }

    @Override protected void onResume()  { super.onResume();  refreshFromApi(); schedulePoll(); }
    @Override protected void onPause()   { super.onPause();   pollHandler.removeCallbacks(pollRunnable); }
    @Override protected void onDestroy() { super.onDestroy(); pollHandler.removeCallbacks(pollRunnable); ioExecutor.shutdown(); }

    private void schedulePoll() {
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, POLL_MS);
    }

    private void refreshFromApi() {
        String token = ApiClient.getAuthToken();
        if (token == null || ticketId == null) return;
        try {
            long numId = Long.parseLong(ticketId);
            ApiClient.getService().getRepairById(token, numId)
                    .enqueue(new Callback<Ticket>() {
                        @Override public void onResponse(Call<Ticket> call, Response<Ticket> r) {
                            if (r.isSuccessful() && r.body() != null) {
                                Ticket fresh = r.body();
                                Ticket cached = findTicket(ticketId);
                                if (fresh.photoPath == null && cached != null)
                                    fresh.photoPath = cached.photoPath;
                                DataManager.getInstance().addTicket(fresh);
                                runOnUiThread(() -> renderTicket(fresh));
                            }
                        }
                        @Override public void onFailure(Call<Ticket> call, Throwable t) {}
                    });
        } catch (NumberFormatException ignored) {}
    }

    private void renderTicket(Ticket ticket) {
        if (ticket == null) return;

        setTextSafe(R.id.tv_ticket_id, "#" + ticket.getShortId());
        setTextSafe(R.id.tv_category,
                ticket.category != null ? ticket.category + " Issue" : "Issue");
        String loc = nvl(ticket.building, "")
                + (ticket.floor != null ? " · Floor " + ticket.floor : "")
                + (ticket.room  != null ? " · " + ticket.room : "");
        setTextSafe(R.id.tv_location, loc.trim());
        setTextSafe(R.id.tv_details,  nvl(ticket.details, ""));

        String techDisplay = resolveTechName(ticket);
        setTextSafe(R.id.tv_tech_name, techDisplay);

        setTextSafe(R.id.tv_status, statusLabel(ticket));

        loadEvidencePhotos(ticket);
        updateDeleteButton(ticket);
        updateReviewButton(ticket);
    }

    private String resolveTechName(Ticket ticket) {
        if (ticket == null) return "Not yet assigned";

        if (ticket.assignedTechName != null && !ticket.assignedTechName.isEmpty())
            return ticket.assignedTechName;

        String rawId = ticket.assignedTechnicianId != null
                ? ticket.assignedTechnicianId : ticket.assignedTechId;
        if (rawId == null) {

            String ns = ticket.getNormalisedStatus();
            if ("in_progress".equalsIgnoreCase(ns) || "done".equalsIgnoreCase(ns)) {
                return "Technician (assigned)";
            }
            return "Not yet assigned";
        }

        String numId = rawId.replaceAll("^[^0-9]+", "");
        for (com.campuscare.app.models.User u
                : DataManager.getInstance().getUsersByRole("technician")) {
            if (u.id == null) continue;
            String uNum = u.id.replaceAll("^[^0-9]+", "");
            if (rawId.equals(u.id) || (!numId.isEmpty() && numId.equals(uNum)))
                return u.name;
        }

        return "Technician #" + numId;
    }

    private void updateDeleteButton(Ticket ticket) {
        Button btn = findViewById(R.id.btn_delete_request);
        if (btn == null) return;
        if (!canRequesterAct()) {
            btn.setVisibility(View.GONE);
            return;
        }
        boolean isNew = ticket.getNormalisedStatus() == null
                || "new".equalsIgnoreCase(ticket.getNormalisedStatus());
        btn.setVisibility(isNew ? View.VISIBLE : View.GONE);
    }

    private void updateReviewButton(Ticket ticket) {
        Button btn = findViewById(R.id.btn_confirm_review);
        if (btn == null || ticket == null) return;

        if (!canRequesterAct()) {
            btn.setVisibility(View.GONE);
            return;
        }

        if (!ticket.isDone()) {
            btn.setVisibility(View.GONE);
            return;
        }

        btn.setVisibility(View.VISIBLE);
        boolean rated = ticket.rating > 0;
        if (rated) {
            btn.setEnabled(false);
            btn.setAlpha(0.6f);
            btn.setText("REVIEW SUBMITTED");
        } else {
            btn.setEnabled(true);
            btn.setAlpha(1.0f);
            btn.setText("CONFIRM & REVIEW");
        }
    }

    private boolean canRequesterAct() {
        String role = DataManager.getInstance().getCurrentUser() != null
                ? DataManager.getInstance().getCurrentUser().role
                : null;
        if (role == null) return false;
        return "student".equalsIgnoreCase(role) || "lecturer".equalsIgnoreCase(role);
    }

    private void loadEvidencePhotos(Ticket ticket) {
        HorizontalScrollView hsv   = findViewById(R.id.hsv_evidence);
        LinearLayout         strip = (hsv != null) ? (LinearLayout) hsv.getChildAt(0) : null;
        ImageView            ivSingle = findViewById(R.id.iv_ticket_photo);

        View syncBanner = findViewById(R.id.banner_photo_sync);

        List<String> localPaths  = splitComma(ticket.photoPath);
        List<String> serverUrls  = splitComma(ticket.photoUrl);

        boolean hasServer = !serverUrls.isEmpty();
        boolean hasLocal  = !localPaths.isEmpty();

        if (!hasServer && !hasLocal) {
            if (hsv != null) hsv.setVisibility(View.GONE);
            if (ivSingle != null) ivSingle.setVisibility(View.GONE);
            hideSyncBanner(syncBanner);

            TextView tvNoPhoto = findViewById(R.id.tv_no_evidence);
            if (tvNoPhoto != null) {
                tvNoPhoto.setText("No evidence photos uploaded");
                tvNoPhoto.setVisibility(View.VISIBLE);
            }
            return;
        }

        TextView tvNoPhoto = findViewById(R.id.tv_no_evidence);
        if (tvNoPhoto != null) tvNoPhoto.setVisibility(View.GONE);

        if (!hasServer && hasLocal) {
            showSyncBanner(syncBanner, ticket);
        } else {
            hideSyncBanner(syncBanner);
        }

        int count = Math.max(localPaths.size(), serverUrls.size());
        if (strip != null) {
            strip.removeAllViews();
            hsv.setVisibility(View.VISIBLE);
            if (ivSingle != null) ivSingle.setVisibility(View.GONE);

            int dp100 = dp(100), dp8 = dp(8);
            for (int i = 0; i < count; i++) {
                String local  = i < localPaths.size() ? localPaths.get(i) : null;
                String server = i < serverUrls.size() ? serverUrls.get(i)  : null;

                ImageView iv = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp100, dp100);
                lp.setMargins(0, 0, dp8, 0);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackgroundResource(R.drawable.bg_input);

                if (hasServer && server != null) {

                    loadAbsoluteUrl(iv, server);
                } else if (local != null) {

                    try { iv.setImageURI(Uri.parse(local)); }
                    catch (Exception e) { iv.setImageResource(android.R.drawable.ic_menu_gallery); }
                }

                final String tapUrl   = (hasServer && server != null) ? server : null;
                final String tapLocal = local;
                iv.setOnClickListener(v -> showPhotoFullscreen(tapUrl, tapLocal));
                strip.addView(iv);
            }
        } else if (ivSingle != null) {
            String local  = localPaths.isEmpty()  ? null : localPaths.get(0);
            String server = serverUrls.isEmpty()   ? null : serverUrls.get(0);
            if (hasServer) {
                loadAbsoluteUrl(ivSingle, server);
                ivSingle.setVisibility(View.VISIBLE);
            } else if (local != null) {
                try { ivSingle.setImageURI(Uri.parse(local)); ivSingle.setVisibility(View.VISIBLE); }
                catch (Exception e) { ivSingle.setVisibility(View.GONE); }
            }
        }
    }

    private void showSyncBanner(View banner, Ticket ticket) {
        if (banner == null) return;
        banner.setVisibility(View.VISIBLE);
        TextView tvMsg = banner.findViewById(R.id.tv_sync_message);
        if (tvMsg != null)
            tvMsg.setText("⚠ Photo not synced to server. Tech cannot see it yet.");
        Button btnSync = banner.findViewById(R.id.btn_sync_photo);
        if (btnSync != null) {
            btnSync.setText("Upload Now");
            btnSync.setOnClickListener(v -> retryPhotoUpload(ticket, btnSync));
        }
    }

    private void hideSyncBanner(View banner) {
        if (banner != null) banner.setVisibility(View.GONE);
    }

    private void retryPhotoUpload(Ticket ticket, Button btnSync) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> localPaths = splitComma(ticket.photoPath);
        if (localPaths.isEmpty()) return;

        btnSync.setEnabled(false);
        btnSync.setText("Uploading…");

        List<String> uploadedUrls = new ArrayList<>();
        retryUploadNext(0, localPaths, uploadedUrls, ticket, token, btnSync);
    }

    private void retryUploadNext(int idx, List<String> paths, List<String> urls,
                                 Ticket ticket, String token, Button btnSync) {
        if (idx >= paths.size()) {
            if (urls.isEmpty()) {
                runOnUiThread(() -> {
                    btnSync.setEnabled(true);
                    btnSync.setText("Upload Now");
                    Toast.makeText(this, "Upload failed. Check your connection.", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            String combined = String.join(",", urls);
            patchPhotoUrl(ticket, combined, token, btnSync);
            return;
        }

        String path = paths.get(idx);
        Uri uri = Uri.parse(path);

        ioExecutor.execute(() -> {
            try {
                byte[] bytes = compressImage(uri, MAX_KB);
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
                MultipartBody.Part part = MultipartBody.Part.createFormData(
                        "file", "retry_" + idx + ".jpg", reqFile);

                ApiClient.getService().uploadPhoto(token, part)
                        .enqueue(new Callback<UploadResponse>() {
                            @Override public void onResponse(Call<UploadResponse> call,
                                                             Response<UploadResponse> r) {
                                if (r.isSuccessful() && r.body() != null && r.body().url != null)
                                    urls.add(r.body().url);
                                retryUploadNext(idx + 1, paths, urls, ticket, token, btnSync);
                            }
                            @Override public void onFailure(Call<UploadResponse> call, Throwable t) {
                                retryUploadNext(idx + 1, paths, urls, ticket, token, btnSync);
                            }
                        });
            } catch (IOException e) {
                retryUploadNext(idx + 1, paths, urls, ticket, token, btnSync);
            }
        });
    }

    private void patchPhotoUrl(Ticket ticket, String photoUrl, String token, Button btnSync) {
        if (ticket.id == null) return;
        try {
            long numId = Long.parseLong(ticket.id);
            Map<String, String> body = new HashMap<>();
            body.put("photoUrl", photoUrl);

            ApiClient.getService().updateRepairPhoto(token, numId, body)
                    .enqueue(new Callback<Ticket>() {
                        @Override public void onResponse(Call<Ticket> call, Response<Ticket> r) {
                            runOnUiThread(() -> {
                                if (r.isSuccessful() && r.body() != null) {
                                    Ticket updated = r.body();
                                    if (updated.photoPath == null)
                                        updated.photoPath = ticket.photoPath;
                                    DataManager.getInstance().addTicket(updated);
                                    renderTicket(updated);
                                    Toast.makeText(LiveTrackingActivity.this,
                                            "Photos uploaded! Technician can now see them.",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    btnSync.setEnabled(true);
                                    btnSync.setText("Upload Now");
                                    Toast.makeText(LiveTrackingActivity.this,
                                            "Upload failed. Try again.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        @Override public void onFailure(Call<Ticket> call, Throwable t) {
                            runOnUiThread(() -> {
                                btnSync.setEnabled(true);
                                btnSync.setText("Upload Now");
                                Toast.makeText(LiveTrackingActivity.this,
                                        "Upload failed. Check connection.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } catch (NumberFormatException ignored) {}
    }

    private void confirmDelete() {
        Ticket t = findTicket(ticketId);
        if (t == null) return;
        if (!"new".equalsIgnoreCase(t.getNormalisedStatus())) {
            Toast.makeText(this,
                    "Cannot delete – a technician has already accepted this request.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to delete this repair request?")
                .setPositiveButton("Delete", (d, w) -> performDelete())
                .setNegativeButton("Cancel", null).show();
    }

    private void performDelete() {
        String token = ApiClient.getAuthToken();
        if (token != null && ticketId != null) {
            try {
                long numId = Long.parseLong(ticketId);
                ApiClient.getService().deleteRepair(token, numId)
                        .enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> r) {
                                runOnUiThread(() -> {
                                    if (r.isSuccessful()) {
                                        DataManager.getInstance().removeTicket(ticketId);
                                        Toast.makeText(LiveTrackingActivity.this,
                                                "Request deleted.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(LiveTrackingActivity.this,
                                                ApiErrorUtils.extractMessage(r, "Delete failed."),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            @Override public void onFailure(Call<Void> call, Throwable e) {
                                runOnUiThread(() -> Toast.makeText(LiveTrackingActivity.this,
                                        "Cannot connect to server. Request was not deleted.",
                                        Toast.LENGTH_LONG).show());
                            }
                        });
                return;
            } catch (NumberFormatException ignored) {}
        }
        DataManager.getInstance().removeTicket(ticketId);
        Toast.makeText(this, "Request deleted.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private byte[] compressImage(Uri uri, int maxKB) throws IOException {
        android.content.ContentResolver cr = getContentResolver();
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) throw new IOException("cannot open " + uri);
            BitmapFactory.decodeStream(is, null, opts);
        }
        int sampleSize = 1;
        long pixels = (long) opts.outWidth * opts.outHeight;
        while (pixels / (sampleSize * sampleSize) > 2_000_000L) sampleSize *= 2;

        opts = new BitmapFactory.Options();
        opts.inSampleSize      = sampleSize;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap;
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) throw new IOException("cannot open " + uri);
            bitmap = BitmapFactory.decodeStream(is, null, opts);
        }
        if (bitmap == null) throw new IOException("failed to decode bitmap");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 85;
        do {
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            quality -= 10;
        } while (baos.size() > maxKB * 1024 && quality > 20);

        bitmap.recycle();
        return baos.toByteArray();
    }

    private void showPhotoFullscreen(String serverPath, String localPath) {
        ImageView ivFull = new ImageView(this);
        ivFull.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        ivFull.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivFull.setAdjustViewBounds(true);

        if (serverPath != null) {
            String absolute = ApiClient.toAbsoluteUrl(serverPath);
            com.bumptech.glide.Glide.with(this)
                    .load(absolute)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivFull);
        } else if (localPath != null) {
            try { ivFull.setImageURI(Uri.parse(localPath)); }
            catch (Exception ignored) {}
        }

        new AlertDialog.Builder(this)
                .setView(ivFull)
                .setPositiveButton("Close", null)
                .show();
    }

    private void loadAbsoluteUrl(ImageView iv, String serverPath) {
        String absolute = ApiClient.toAbsoluteUrl(serverPath);
        if (absolute == null) { iv.setVisibility(View.GONE); return; }
        com.bumptech.glide.Glide.with(this)
                .load(absolute)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(iv);
        iv.setVisibility(View.VISIBLE);
    }

    private String statusLabel(Ticket t) {
        if (t == null) return "⏳ Waiting for Technician";
        String ns = t.getNormalisedStatus();
        switch (ns) {
            case "new": {
                boolean assigned = t.assignedTechnicianId != null || t.assignedTechId != null;
                return assigned ? "⏳ Assigned – Awaiting Acceptance" : "⏳ Waiting for Technician";
            }
            case "in_progress": {
                String eta = t.eta != null ? t.eta : t.expectedFinish;
                return "🔧 Technician On the Way" + (eta != null ? "\nETA: " + eta : "");
            }
            case "done":

                return (canRequesterAct() && t.rating > 0) ? "✅ Review Submitted" : "✅ Job Completed";
            default:
                return t.getStatusDisplay();
        }
    }

    private void setTextSafe(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }

    private List<String> splitComma(String s) {
        List<String> list = new ArrayList<>();
        if (s == null || s.isEmpty()) return list;
        for (String p : s.split(",")) { String t = p.trim(); if (!t.isEmpty()) list.add(t); }
        return list;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private String nvl(String s, String fb) { return (s != null && !s.isEmpty()) ? s : fb; }

    private Ticket findTicket(String id) {
        if (id == null) return null;
        for (Ticket t : DataManager.getInstance().getAllTickets())
            if (t.id != null && t.id.equals(id)) return t;
        return null;
    }
}
