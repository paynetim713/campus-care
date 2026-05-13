package com.campuscare.app.utils;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campuscare.app.models.Notification;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationBadgeHelper {

    public static void refresh(AppCompatActivity activity, View badgeDot) {
        if (badgeDot == null) return;

        String token = ApiClient.getAuthToken();
        if (token == null) {

            int count = estimateLocalUnread(activity);
            activity.runOnUiThread(() -> applyBadge(badgeDot, count));
            return;
        }

        ApiClient.getService().getNotifications(token)
                .enqueue(new Callback<List<Notification>>() {
                    @Override
                    public void onResponse(Call<List<Notification>> call,
                                           Response<List<Notification>> response) {
                        int count = 0;
                        if (response.isSuccessful() && response.body() != null) {
                            for (Notification n : response.body()) {
                                if (!n.read) count++;
                            }
                        }
                        final int unread = count;
                        activity.runOnUiThread(() -> applyBadge(badgeDot, unread));
                    }

                    @Override
                    public void onFailure(Call<List<Notification>> call, Throwable t) {
                        int count = estimateLocalUnread(activity);
                        activity.runOnUiThread(() -> applyBadge(badgeDot, count));
                    }
                });
    }

    private static void applyBadge(View badgeDot, int unread) {
        if (unread <= 0) {
            badgeDot.setVisibility(View.GONE);
            return;
        }
        badgeDot.setVisibility(View.VISIBLE);
        if (badgeDot instanceof TextView) {
            ((TextView) badgeDot).setText(unread > 9 ? "9+" : String.valueOf(unread));
        }
    }

    private static int estimateLocalUnread(AppCompatActivity activity) {
        DataManager dm = DataManager.getInstance();
        if (dm.getCurrentUser() == null) return 0;

        int count = 0;
        String uid = dm.getCurrentUser().id;
        for (com.campuscare.app.models.Ticket t : dm.getAllTickets()) {
            String owner = t.reportedById != null ? t.reportedById : t.requesterId;
            if (uid != null && uid.equals(owner)) {

                if ("in_progress".equalsIgnoreCase(t.getNormalisedStatus()) || t.isDone())
                    count++;
            }
        }
        return count;
    }
}