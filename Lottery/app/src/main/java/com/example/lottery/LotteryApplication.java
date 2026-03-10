package com.example.lottery;

import android.app.Application;

import com.google.firebase.FirebaseApp;

/**
 * Base class for maintaining global application state.
 *
 * <p>Initializes Firebase once for the whole app process before any activity starts.</p>
 */
public class LotteryApplication extends Application {
    /**
     * Called when the application process is created.
     * Initializes Firebase services used throughout the app.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase SDK for the entire application.
        FirebaseApp.initializeApp(this);
    }
}
