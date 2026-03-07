package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity serves as the primary dashboard for the application.
 * It coordinates navigation between different features based on the user's role.
 * 
 * Supports US 02.01.01 by providing an entry point for organizers.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * A simulated flag to represent the user's role in the system.
     * When true, the user is treated as an Organizer and granted access to event creation.
     */
    private boolean isOrganizer = true;

    /**
     * Initializes the activity, sets up the layout, and configures role-based access to UI elements.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnGoToCreateEvent = findViewById(R.id.btnGoToCreateEvent);

        // Access Control Logic: Only organizers can see the Create Event button.
        if (isOrganizer) {
            btnGoToCreateEvent.setVisibility(View.VISIBLE);
        } else {
            btnGoToCreateEvent.setVisibility(View.GONE);
        }

        // Navigate to the event creation screen.
        btnGoToCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });
    }
}
