package com.example.lottery;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.libraries.places.api.Places;
import com.google.firebase.FirebaseApp;

/**
 * Base class for maintaining global application state.
 *
 * <p>Initializes Firebase once for the whole app process before any activity starts.</p>
 */
public class LotteryApplication extends Application {

    public static final String PREFS_NAME = "AppPrefs";
    private static final String TAG = "LotteryApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this);

        // Initialize Google Places SDK with the New Places API enabled
        if (!Places.isInitialized()) {
            try {
                // Fetch the API key from Manifest metadata to ensure consistency
                ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                String apiKey = bundle.getString("com.google.android.geo.API_KEY");

                if (apiKey != null && !apiKey.isEmpty()) {
                    // MUST use initializeWithNewPlacesApiEnabled to avoid legacy errors
                    Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);
                    Log.d(TAG, "Places SDK initialized with New API enabled.");
                } else {
                    Log.e(TAG, "Places SDK failed to initialize: API Key is missing in Manifest.");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
            } catch (NullPointerException e) {
                Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
            }
        }
    }
}
