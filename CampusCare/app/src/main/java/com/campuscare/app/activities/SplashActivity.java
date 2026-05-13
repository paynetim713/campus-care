package com.campuscare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campuscare.app.R;
import com.campuscare.app.utils.ServerConfigHelper;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        refreshServerSummary();

        Button signInBtn = findViewById(R.id.btn_sign_in);
        signInBtn.setBackgroundTintList(null);

        findViewById(R.id.btn_sign_in).setOnClickListener(v ->
            startActivity(new Intent(this, LoginActivity.class)));

        findViewById(R.id.btn_create_account).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.btn_server_settings).setOnClickListener(v ->
                ServerConfigHelper.showDialog(this, this::refreshServerSummary));
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
