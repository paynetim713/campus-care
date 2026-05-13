package com.campuscare.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campuscare.app.R;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.ApiErrorUtils;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.ApiService;
import com.campuscare.app.utils.DataManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyProfileActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        User user = DataManager.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            ((TextView) findViewById(R.id.tv_profile_name)).setText(displayName);
            ((TextView) findViewById(R.id.tv_full_name)).setText(displayName);
            ((TextView) findViewById(R.id.tv_email)).setText(user.email != null ? user.email : "-");
            ((TextView) findViewById(R.id.tv_profile_role)).setText(user.getRoleDisplay());
            ((TextView) findViewById(R.id.tv_role)).setText(user.getRoleDisplay());
        }

        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        Button btnChange = findViewById(R.id.btn_change_password);
        btnChange.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String current = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(current)) {
            etCurrentPassword.setError("Enter current password");
            return;
        }
        if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
            etNewPassword.setError("New password must be at least 6 characters");
            return;
        }
        if (!newPass.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        User user = DataManager.getInstance().getCurrentUser();
        if (user == null) return;

        if (user.password != null && !user.password.equals(current)) {
            etCurrentPassword.setError("Current password is incorrect");
            Toast.makeText(this, "Current password is incorrect.", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = ApiClient.getAuthToken();
        if (token != null) {
            Map<String, String> body = new HashMap<>();
            body.put("currentPassword", current);
            body.put("newPassword", newPass);

            ApiClient.getService().changePassword(token, body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            user.password = newPass;
                            DataManager.getInstance().changePasswordLocally(user.id, current, newPass);
                            showSuccess();
                        } else {
                            Toast.makeText(MyProfileActivity.this,
                                    ApiErrorUtils.extractMessage(response, "Password update failed."),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    runOnUiThread(() -> Toast.makeText(MyProfileActivity.this,
                            "Cannot connect to server. Password was not changed.",
                            Toast.LENGTH_LONG).show());
                }
            });
        } else {

            user.password = newPass;
            DataManager.getInstance().changePasswordLocally(user.id, current, newPass);
            showSuccess();
        }
    }

    private void showSuccess() {
        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
        etCurrentPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");
    }
}
