package com.campuscare.app.activities;

import android.app.ProgressDialog;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campuscare.app.R;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etCode, etNewPassword;
    private Button btnSend;
    private ProgressDialog loadingDialog;
    private CountDownTimer resendTimer;

    private static final long RESEND_COOLDOWN_MS = 30_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.et_email);
        etCode = findViewById(R.id.et_code);
        etNewPassword = findViewById(R.id.et_new_password);
        btnSend = findViewById(R.id.btn_send);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Please wait...");
        loadingDialog.setCancelable(false);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim().toLowerCase();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_LONG).show();
                return;
            }
            loadingDialog.show();
            sendVerificationCode(email);
        });

        findViewById(R.id.btn_verify_reset).setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString();
            if (!isFiveDigitCode(code)) {
                Toast.makeText(this, "Please enter a valid 5-digit code", Toast.LENGTH_LONG).show();
                return;
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                Toast.makeText(this, "Please enter a new password", Toast.LENGTH_LONG).show();
                return;
            }
            loadingDialog.show();
            resetPassword(code, newPassword);
        });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isFiveDigitCode(String code) {
        return code != null && code.matches("\\d{5}");
    }

    private void sendVerificationCode(String email) {
        ApiService service = ApiClient.getService();
        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        service.forgotPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                dismissLoading();
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Verification code sent to " + email, Toast.LENGTH_LONG).show();
                    startResendCooldown();
                } else {
                    String msg = (response.code() == 404)
                            ? "Email not found. Please check and try again."
                            : "Failed to send email. Please try again later.";
                    Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                dismissLoading();
                Toast.makeText(ForgotPasswordActivity.this,
                        "Cannot connect to server. Please check your connection and try again.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword(String code, String newPassword) {
        ApiService service = ApiClient.getService();
        Map<String, String> request = new HashMap<>();
        request.put("token", code);
        request.put("newPassword", newPassword);

        service.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                dismissLoading();
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Password reset successful. Please login.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Invalid or expired code.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                dismissLoading();
                Toast.makeText(ForgotPasswordActivity.this,
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

    private void startResendCooldown() {
        if (btnSend == null) return;
        if (resendTimer != null) resendTimer.cancel();

        btnSend.setEnabled(false);
        resendTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished + 999L) / 1000L;
                btnSend.setText("Resend in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                btnSend.setEnabled(true);
                btnSend.setText("Send Verification Code");
                resendTimer = null;
            }
        };
        resendTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        dismissLoading();
    }
}