package com.campuscare.app.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.campuscare.app.R;
import com.campuscare.app.models.AuthResponse;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private TextView tabStudent, tabLecturer, tabTech;
    private TextView tvEmailFormat;
    private ProgressDialog loadingDialog;

    private String selectedRole = "student";

    private static final String DOMAIN_STUDENT  = "@siswa.ukm.edu.my";
    private static final String DOMAIN_LECTURER = "@ukm.edu.my";

    private static final String FORMAT_STUDENT    = "Format: matric@siswa.ukm.edu.my";
    private static final String FORMAT_LECTURER   = "Format: name@ukm.edu.my";
    private static final String FORMAT_TECHNICIAN = "Use your work or campus email";

    private static final String HINT_STUDENT    = "matric@siswa.ukm.edu.my";
    private static final String HINT_LECTURER   = "name@ukm.edu.my";
    private static final String HINT_TECHNICIAN = "your@email.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName        = findViewById(R.id.et_name);
        etEmail       = findViewById(R.id.et_email);
        etPassword    = findViewById(R.id.et_password);
        tvEmailFormat = findViewById(R.id.tv_email_format);
        tabStudent    = findViewById(R.id.tab_student);
        tabLecturer   = findViewById(R.id.tab_lecturer);
        tabTech       = findViewById(R.id.tab_tech);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Creating account...");
        loadingDialog.setCancelable(false);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tabStudent.setOnClickListener(v  -> selectRole("student"));
        tabLecturer.setOnClickListener(v -> selectRole("lecturer"));
        tabTech.setOnClickListener(v     -> selectRole("technician"));
        selectRole("student");

        findViewById(R.id.btn_register).setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String email    = etEmail.getText().toString().trim().toLowerCase();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isEmailValid(email)) {
                return;
            }
            if (password.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            loadingDialog.show();
            registerWithBackend(name, email, password);
        });
    }

    private boolean isEmailValid(String email) {
        switch (selectedRole) {
            case "student":
                if (!email.endsWith(DOMAIN_STUDENT)) {
                    Toast.makeText(this,
                            "Student email must end with " + DOMAIN_STUDENT,
                            Toast.LENGTH_LONG).show();
                    return false;
                }
                break;
            case "lecturer":
                if (!email.endsWith(DOMAIN_LECTURER) || email.endsWith(DOMAIN_STUDENT)) {
                    Toast.makeText(this,
                            "Lecturer email must end with " + DOMAIN_LECTURER,
                            Toast.LENGTH_LONG).show();
                    return false;
                }
                break;
            default:

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
        }
        return true;
    }

    private void selectRole(String role) {
        selectedRole = role;
        setTabInactive(tabStudent);
        setTabInactive(tabLecturer);
        setTabInactive(tabTech);

        switch (role) {
            case "student":
                setTabActive(tabStudent);
                etEmail.setHint(HINT_STUDENT);
                tvEmailFormat.setText(FORMAT_STUDENT);
                tvEmailFormat.setVisibility(android.view.View.VISIBLE);
                break;
            case "lecturer":
                setTabActive(tabLecturer);
                etEmail.setHint(HINT_LECTURER);
                tvEmailFormat.setText(FORMAT_LECTURER);
                tvEmailFormat.setVisibility(android.view.View.VISIBLE);
                break;
            case "technician":
                setTabActive(tabTech);
                etEmail.setHint(HINT_TECHNICIAN);
                tvEmailFormat.setText(FORMAT_TECHNICIAN);
                tvEmailFormat.setVisibility(android.view.View.VISIBLE);
                break;
        }

        etEmail.setText("");
    }

    private void setTabActive(TextView tab) {
        tab.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_tab_active));
        tab.setTextColor(ContextCompat.getColor(this, R.color.primary));
        tab.setTypeface(null, Typeface.BOLD);
    }

    private void setTabInactive(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tab.setTypeface(null, Typeface.NORMAL);
    }

    private void registerWithBackend(String name, String email, String password) {
        ApiService service = ApiClient.getService();
        Map<String, String> request = new HashMap<>();
        request.put("fullName", name);
        request.put("email", email);
        request.put("password", password);

        String backendRole;
        if ("technician".equalsIgnoreCase(selectedRole)) {
            backendRole = "TECHNICIAN";
        } else {
            backendRole = "REQUESTER";
        }
        request.put("role", backendRole);

        service.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                dismissLoading();
                if (response.isSuccessful() && response.body() != null) {

                    AuthResponse auth = response.body();
                    Toast.makeText(RegisterActivity.this,
                            "Account created! Backend role: " + auth.role,
                            Toast.LENGTH_LONG).show();

                    Toast.makeText(RegisterActivity.this,
                            "Account created! Please sign in.", Toast.LENGTH_SHORT).show();

                    persistRoleOverride(email, selectedRole);

                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {

                    String msg = "Registration failed. The email may already be in use.";
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                dismissLoading();
                Toast.makeText(RegisterActivity.this,
                        "Cannot connect to server. Please check your connection and try again.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void persistRoleOverride(String email, String frontendRole) {
        if (email == null) return;
        String e = email.trim().toLowerCase();
        if (e.isEmpty()) return;

        if (!"technician".equalsIgnoreCase(frontendRole)
                && !"student".equalsIgnoreCase(frontendRole)
                && !"lecturer".equalsIgnoreCase(frontendRole)) {
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences(
                "campus_care_role_overrides", MODE_PRIVATE);
        prefs.edit().putString(e, frontendRole.toLowerCase()).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoading();
    }
}