package com.campuscare.app;

import android.app.Application;

import com.campuscare.app.utils.ServerConfigHelper;

public class CampusCareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ServerConfigHelper.applySavedServerUrl(this);
    }
}
