package com.campuscare.app.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campuscare.app.R;
import com.campuscare.app.models.AuthResponse;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.ApiService;
import com.campuscare.app.utils.DataManager;
import com.campuscare.app.utils.ServerConfigHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Logging in...");
        loadingDialog.setCancelable(false);

        refreshServerSummary();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.tv_forgot).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.btn_server_settings).setOnClickListener(v ->
                ServerConfigHelper.showDialog(this, this::refreshServerSummary));

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            loadingDialog.show();
            loginWithBackend(email, password);
        });
    }

    private void loginWithBackend(String email, String password) {
        ApiService service = ApiClient.getService();
        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", email);
        request.put("password", password);

        service.login((java.util.Map<String, String>) request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                dismissLoading();
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    String frontendRole = mapBackendRoleToFrontend(auth.role, email, auth.fullName);
                    ApiClient.setAuthToken(auth.token);
                    User user = new User(auth.userId, auth.fullName, email, frontendRole);

                    user.password = password;
                    DataManager.getInstance().setCurrentUser(user);
                    Toast.makeText(LoginActivity.this, "Welcome " + auth.fullName + "!", Toast.LENGTH_SHORT).show();
                    navigateByRole(frontendRole);
                } else {

                    Toast.makeText(LoginActivity.this,
                            "Incorrect email or password.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                dismissLoading();

                loginLocally(email, password);
            }
        });
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private String mapBackendRoleToFrontend(String backendRole, String email, String fullName) {
        if (backendRole == null) return "student";
        String r = backendRole.toUpperCase();

        String override = getRoleOverride(email);
        if (override != null) return override;

        switch (r) {
            case "ADMIN":
                return "admin";
            case "TECHNICIAN":
                return "technician";
            case "REQUESTER":
            default:

                if (email != null) {
                    boolean isStudent  = email.endsWith("@siswa.ukm.edu.my");
                    boolean isLecturer = email.endsWith("@ukm.edu.my") && !isStudent;

                    if (isLecturer) return "lecturer";
                    if (isStudent)  return "student";
                }
                return "student";
        }
    }

    private String getRoleOverride(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase();
        if (e.isEmpty()) return null;
        android.content.SharedPreferences prefs = getSharedPreferences(
                "campus_care_role_overrides", MODE_PRIVATE);
        String v = prefs.getString(e, null);
        if (v == null) return null;
        v = v.trim().toLowerCase();
        if ("technician".equals(v) || "student".equals(v) || "lecturer".equals(v) || "admin".equals(v)) {
            return v;
        }
        return null;
    }

    private void loginLocally(String email, String password) {
        User user = DataManager.getInstance().login(email, password);
        if (user != null) {

            user.password = password;
            DataManager.getInstance().setCurrentUser(user);
            Toast.makeText(this, "Logged in (offline mode)", Toast.LENGTH_SHORT).show();
            navigateByRole(user.role);
        } else {
            Toast.makeText(this, "Login failed. No server connection and credentials not recognized.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateByRole(String role) {
        Intent intent;
        switch (role) {
            case "technician":
                intent = new Intent(this, TechJobBoardActivity.class); break;
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class); break;
            default:
                intent = new Intent(this, UserDashboardActivity.class); break;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoading();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshServerSummary();
    }

    private void refreshServerSummary() {
        TextView tvServerInfo = findViewById(R.id.tv_server_info);
        if (tvServerInfo != null) {
            tvServerInfo.setText(ServerConfigHelper.getServerSummary(this));
        }
    }
}
