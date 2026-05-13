package com.campuscare.app.activities;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.campuscare.app.R;
import com.campuscare.app.models.User;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAddUserActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private TextView tabRequester, tabTechnician, tabAdmin;
    private TextView tvEmailHint;
    private ProgressDialog loadingDialog;

    private String selectedRole = "REQUESTER";

    private static final String DOMAIN_STUDENT  = "@siswa.ukm.edu.my";
    private static final String DOMAIN_LECTURER = "@ukm.edu.my";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_user);

        etName        = findViewById(R.id.et_name);
        etEmail       = findViewById(R.id.et_email);
        etPassword    = findViewById(R.id.et_password);
        tabRequester  = findViewById(R.id.tab_requester);
        tabTechnician = findViewById(R.id.tab_technician);
        tabAdmin      = findViewById(R.id.tab_admin_role);
        tvEmailHint   = findViewById(R.id.tv_email_hint);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Creating account…");
        loadingDialog.setCancelable(false);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tabRequester.setOnClickListener(v  -> selectRole("REQUESTER"));
        tabTechnician.setOnClickListener(v -> selectRole("TECHNICIAN"));
        tabAdmin.setOnClickListener(v      -> selectRole("ADMIN"));
        selectRole("REQUESTER");

        findViewById(R.id.btn_create).setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String email    = etEmail.getText().toString().trim().toLowerCase();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isEmailValid(email)) return;
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            createUser(name, email, password);
        });
    }

    private void selectRole(String role) {
        selectedRole = role;
        setInactive(tabRequester);
        setInactive(tabTechnician);
        setInactive(tabAdmin);
        switch (role) {
            case "REQUESTER":
                setActive(tabRequester);
                etEmail.setHint("matric@siswa.ukm.edu.my");
                if (tvEmailHint != null)
                    tvEmailHint.setText("Format: matric@siswa.ukm.edu.my");
                break;
            case "TECHNICIAN":
                setActive(tabTechnician);
                etEmail.setHint("tech@domain.com");
                if (tvEmailHint != null)
                    tvEmailHint.setText("Any valid work email accepted");
                break;
            case "ADMIN":
                setActive(tabAdmin);
                etEmail.setHint("admin@domain.com");
                if (tvEmailHint != null)
                    tvEmailHint.setText("Any valid email accepted");
                break;
        }
        etEmail.setText("");
    }

    private void setActive(TextView tab) {
        tab.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_tab_active));
        tab.setTextColor(ContextCompat.getColor(this, R.color.primary));
        tab.setTypeface(null, Typeface.BOLD);
    }

    private void setInactive(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tab.setTypeface(null, Typeface.NORMAL);
    }

    private boolean isEmailValid(String email) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }
        if ("REQUESTER".equals(selectedRole)) {
            if (!email.endsWith(DOMAIN_STUDENT) && !email.endsWith(DOMAIN_LECTURER)) {
                Toast.makeText(this,
                        "Requester email must end with " + DOMAIN_STUDENT
                                + " (student) or " + DOMAIN_LECTURER + " (lecturer)",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        return true;
    }

    private void createUser(String name, String email, String password) {
        String token = ApiClient.getAuthToken();
        if (token == null) {
            Toast.makeText(this, "Not connected to server.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();

        Map<String, String> body = new HashMap<>();
        body.put("fullName", name);
        body.put("email",    email);
        body.put("password", password);
        body.put("role",     selectedRole);

        ApiClient.getService().adminCreateUser(token, body)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> r) {
                        dismissLoading();
                        if (r.isSuccessful() && r.body() != null) {
                            Map<String, Object> res = r.body();

                            String createdName = (String) res.get("fullName");
                            if (createdName == null || createdName.isEmpty()) createdName = name;

                            String createdEmail = (String) res.get("email");
                            if (createdEmail == null || createdEmail.isEmpty()) createdEmail = email;

                            String frontendRole = mapAdminRoleToFrontend(selectedRole, createdEmail);

                            String createdId = null;
                            Object idObj = res.get("userId");
                            if (idObj == null) idObj = res.get("id");
                            if (idObj != null) createdId = String.valueOf(idObj);

                            User u = new User(createdId, createdName, createdEmail, frontendRole);
                            DataManager.getInstance().upsertUser(u);

                            persistRoleOverride(createdEmail, frontendRole);

                            Toast.makeText(AdminAddUserActivity.this,
                                    "User \"" + createdName + "\" created successfully!",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            String msg = "Failed. The email may already be in use.";
                            Toast.makeText(AdminAddUserActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        dismissLoading();
                        Toast.makeText(AdminAddUserActivity.this,
                                "Cannot connect to server.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String mapAdminRoleToFrontend(String backendRole, String email) {
        if (backendRole == null) return "student";
        String r = backendRole.toUpperCase();
        switch (r) {
            case "ADMIN":
                return "admin";
            case "TECHNICIAN":
                return "technician";
            case "REQUESTER":
            default:

                if (email != null) {
                    boolean isStudent = email.endsWith("@siswa.ukm.edu.my");
                    boolean isLecturer = email.endsWith("@ukm.edu.my") && !isStudent;
                    if (isLecturer) return "lecturer";
                    return "student";
                }
                return "student";
        }
    }

    private void persistRoleOverride(String email, String frontendRole) {
        if (email == null || frontendRole == null) return;
        String e = email.trim().toLowerCase();
        if (e.isEmpty()) return;
        if (!("technician".equalsIgnoreCase(frontendRole)
                || "student".equalsIgnoreCase(frontendRole)
                || "lecturer".equalsIgnoreCase(frontendRole)
                || "admin".equalsIgnoreCase(frontendRole))) {
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences(
                "campus_care_role_overrides", MODE_PRIVATE);
        prefs.edit().putString(e, frontendRole.toLowerCase()).apply();
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    @Override
    protected void onDestroy() { super.onDestroy(); dismissLoading(); }
}