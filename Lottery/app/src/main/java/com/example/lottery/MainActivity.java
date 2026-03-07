package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity serves as the main dashboard and entry point for the application.
 * It implements basic role-based navigation logic to support different user stories.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Simulation of the user's role.
     * Set to true to simulate an Organizer (US 02.01.01 access), 
     * or false to simulate a regular Entrant.
     * TODO: Replace with real user role from Firebase Auth in future sprints.
     */
    private boolean isOrganizer = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the "Create Event" navigation button from the layout
        Button btnGoToCreateEvent = findViewById(R.id.btnGoToCreateEvent);

        /**
         * Access Control Logic for US 02.01.01:
         * Only users identified as Organizers are permitted to see and access 
         * the Create Event feature.
         */
        if (isOrganizer) {
            btnGoToCreateEvent.setVisibility(View.VISIBLE);
        } else {
            btnGoToCreateEvent.setVisibility(View.GONE);
        }

        /**
         * Navigation Setup:
         * Sets a click listener on the button to transition from the Dashboard
         * to the CreateEventActivity (Organizer workflow).
         */
        btnGoToCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });
    }
}
