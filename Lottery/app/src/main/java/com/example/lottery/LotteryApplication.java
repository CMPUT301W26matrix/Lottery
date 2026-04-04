package com.example.lottery;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.FirebaseApp;

/**
 * Base class for maintaining global application state.
 *
 * <p>Initializes Firebase once for the whole app process before any activity starts.</p>
 */
public class LotteryApplication extends Application {

    private static final String TAG = "LotteryApplication";
    public static final String PREFS_NAME = "AppPrefs";
    private PlacesClient placesClient;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this);

        // Initialize Google Places SDK with the New Places API enabled
        try {
            if (!Places.isInitialized()) {
                ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                String apiKey = bundle.getString("com.google.android.geo.API_KEY");

                if (apiKey != null && !apiKey.isEmpty()) {
                    Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);
                    placesClient = Places.createClient(this);
                    Log.d(TAG, "Places SDK initialized with New API enabled.");
                } else {
                    Log.e(TAG, "Places SDK failed to initialize: API Key is missing in Manifest.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Places SDK init failed: " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "Places SDK not available: " + e.getMessage());
        }
    }
}
