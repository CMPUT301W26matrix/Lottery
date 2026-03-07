package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * MainActivity serves as the entry point of the application.
 * In this prototype, it acts as a simple dashboard with role-based access control.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Simulation of the user's role.
     * Set to true to simulate an Organizer, or false to simulate a regular user.
     */
    private boolean isOrganizer = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Firebase Manual Initialization.
         * Since google-services.json might be missing during initial prototype setup,
         * we manually initialize Firebase with dummy options to prevent the app from crashing
         * when Firestore is accessed in subsequent activities.
         */
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("fake_api_key")
                    .setApplicationId("1:1234567890:android:abcdef")
                    .setProjectId("fake-project-id")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }

        setContentView(R.layout.activity_main);

        // Initialize the navigation button to the Create Event screen
        Button btnGoToCreateEvent = findViewById(R.id.btnGoToCreateEvent);

        /**
         * Role-based visibility logic.
         * Only users identified as Organizers can see and access the Create Event feature.
         */
        if (isOrganizer) {
            btnGoToCreateEvent.setVisibility(View.VISIBLE);
        } else {
            btnGoToCreateEvent.setVisibility(View.GONE);
        }

        // Set listener to navigate to CreateEventActivity when the button is clicked
        btnGoToCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });
    }
}