package com.example.lottery;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class LotteryApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}