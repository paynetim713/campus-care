package com.campuscare.app.adapters;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.ApiClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {
    private List<User> users;

    public AdminUserAdapter(List<User> users) { this.users = users; }

    public void updateData(List<User> newUsers) {
        users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        User u = users.get(pos);

        String initial = (u.name != null && !u.name.isEmpty())
                ? u.name.substring(0, 1).toUpperCase() : "?";
        h.tvInitial.setText(initial);
        h.tvName.setText(u.name != null ? u.name : "");
        h.tvEmail.setText(u.email != null ? u.email : "");

        if (h.btnMore != null) {
            h.btnMore.setOnClickListener(v -> {

                int currentPos = h.getAdapterPosition();
                if (currentPos == RecyclerView.NO_ID) return;
                User target = users.get(currentPos);

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete User")
                        .setMessage("Remove \"" + target.name + "\"\n" + target.email
                                + "?\n\nThis cannot be undone.")
                        .setPositiveButton("Delete", (d, w) ->
                                performDelete(v, target, currentPos))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void performDelete(View v, User target, int pos) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            Toast.makeText(v.getContext(),
                    "Cannot delete: auth token is missing. Please login again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (target == null || target.id == null) {
                Toast.makeText(v.getContext(),
                        "Cannot delete: user id is missing.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String cleaned = target.id.replaceAll("[^0-9]", "").trim();
            if (cleaned == null || cleaned.isEmpty()) {
                Toast.makeText(v.getContext(),
                        "Cannot delete: user id is invalid (" + target.id + ").",
                        Toast.LENGTH_LONG).show();
                return;
            }

            long numId = Long.parseLong(cleaned);

            ApiClient.getService().deleteUser(token, numId)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> r) {
                            if (r.isSuccessful()) {

                                String deletedId = target.id;
                                int removedIndex = -1;
                                for (int i = 0; i < users.size(); i++) {
                                    User u = users.get(i);
                                    if (u != null && u.id != null && u.id.equals(deletedId)) {
                                        removedIndex = i;
                                        break;
                                    }
                                }
                                if (removedIndex >= 0) {
                                    users.remove(removedIndex);
                                    notifyItemRemoved(removedIndex);
                                } else {

                                    notifyDataSetChanged();
                                }
                                if (v.getContext() != null)
                                    Toast.makeText(v.getContext(),
                                            target.name + " removed.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(v.getContext(),
                                        "Delete failed (status " + r.code() + ")",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(v.getContext(),
                                    "Cannot connect to server.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(v.getContext(),
                    "Cannot delete: invalid user id (" + target.id + ").",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override public int getItemCount() { return users.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView    tvInitial, tvName, tvEmail;
        ImageButton btnMore;
        VH(View v) {
            super(v);
            tvInitial = v.findViewById(R.id.tv_avatar_initial);
            tvName    = v.findViewById(R.id.tv_name);
            tvEmail   = v.findViewById(R.id.tv_email);
            btnMore   = v.findViewById(R.id.btn_more);
        }
    }
}