package com.campuscare.app.utils;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiErrorUtils {
    private ApiErrorUtils() {}

    public static String extractMessage(Response<?> response, String fallback) {
        if (response == null) return fallback;
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) return fallback;
        try {
            String raw = errorBody.string();
            if (raw == null || raw.trim().isEmpty()) return fallback;
            JSONObject json = new JSONObject(raw);
            String message = json.optString("message", "").trim();
            return message.isEmpty() ? fallback : message;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
