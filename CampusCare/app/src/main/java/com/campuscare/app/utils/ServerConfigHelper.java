package com.campuscare.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ServerConfigHelper {
    private static final String PREFS_NAME = "campus_care_prefs";
    private static final String KEY_SERVER_URL = "server_url";

    private ServerConfigHelper() {}

    public static void applySavedServerUrl(Context context) {
        String saved = readSavedServerUrl(context);
        if (saved != null) {
            ApiClient.updateBaseUrl(saved);
        }
    }

    public static String getCurrentServerUrl(Context context) {
        String saved = readSavedServerUrl(context);
        return saved != null ? saved : ensureTrailingSlash(ApiClient.getBaseUrl());
    }

    public static String getServerSummary(Context context) {
        return "Server: " + getCurrentServerUrl(context);
    }

    public static void showDialog(AppCompatActivity activity, Runnable onSaved) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 20);
        root.setPadding(pad, dp(activity, 12), pad, dp(activity, 4));

        TextView help = new TextView(activity);
        help.setText("Default server is cloud-hosted. Only change this if you are running a local backend.");
        help.setTextSize(13f);
        root.addView(help);

        EditText etUrl = new EditText(activity);
        etUrl.setHint("https://campus-care-backend-production.up.railway.app");
        etUrl.setSingleLine(true);
        etUrl.setText(trimTrailingSlash(getCurrentServerUrl(activity)));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = dp(activity, 12);
        etUrl.setLayoutParams(inputParams);
        root.addView(etUrl);

        TextView status = new TextView(activity);
        status.setTextSize(12f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.topMargin = dp(activity, 10);
        status.setLayoutParams(statusParams);
        root.addView(status);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Server Settings")
                .setView(root)
                .setPositiveButton("Save", null)
                .setNeutralButton("Test", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String normalized = normalizeForSaving(etUrl.getText().toString());
                if (normalized == null) {
                    etUrl.setError("Enter a valid host or URL");
                    return;
                }
                saveServerUrl(activity, normalized);
                status.setText("Saved: " + normalized);
                Toast.makeText(activity, "Server address saved.", Toast.LENGTH_SHORT).show();
                if (onSaved != null) onSaved.run();
                dialog.dismiss();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                String normalized = normalizeForSaving(etUrl.getText().toString());
                if (normalized == null) {
                    etUrl.setError("Enter a valid host or URL");
                    return;
                }
                status.setText("Testing connection...");
                testConnection(activity, normalized, status);
            });
        });

        dialog.show();
    }

    private static void testConnection(AppCompatActivity activity, String baseUrl, TextView statusView) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "api/health")
                .get()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                activity.runOnUiThread(() ->
                        statusView.setText("Connection failed. Make sure the phone and laptop are on the same Wi-Fi and the backend is running."));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String message = response.isSuccessful()
                        ? "Connection successful."
                        : "Server responded, but the health check failed (" + response.code() + ").";
                response.close();
                activity.runOnUiThread(() -> statusView.setText(message));
            }
        });
    }

    private static void saveServerUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
        ApiClient.updateBaseUrl(url);
    }

    private static String readSavedServerUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_SERVER_URL, null);
        return normalizeForSaving(saved);
    }

    private static String normalizeForSaving(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim();
        if (normalized.isEmpty()) return null;
        if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            normalized = "http://" + normalized;
        }
        Uri uri = Uri.parse(normalized);
        if (uri.getScheme() == null || uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            return null;
        }
        return ensureTrailingSlash(normalized);
    }

    private static String ensureTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
