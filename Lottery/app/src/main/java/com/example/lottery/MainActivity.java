package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity serves as the primary dashboard for the application.
 * It coordinates navigation between different features based on the user's role.
 * 
 * Supports US 02.01.01 by providing an entry point for organizers.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Simulation of the user's role.
     * Set to true to simulate an Organizer (US 02.01.01 access), 
     * or false to simulate a regular Entrant.
     * TODO: Replace with real user role from Firebase Auth in future sprints.
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

        Button btnViewEvents = findViewById(R.id.btnViewEvents);
        btnViewEvents.setOnClickListener(v -> showEventPickerDialog());
    }

    /**
     * Events Picker Dialog:
     * Get instance from Firestore backend
     * Initialize array for ids and titles
     * Show AlertDialog and ask for user's choice
     * Use intent to transition to EventDetailsActivity
     *
     * @return void
     */
    private void showEventPickerDialog() {
        FirebaseFirestore.getInstance().collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> ids = new ArrayList<>();
                    List<String> titles = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ids.add(doc.getId());
                        String title = doc.getString("title");
                        titles.add(title != null ? title : doc.getId());
                    }
                    if (ids.isEmpty()) {
                        Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Select an Event")
                            .setItems(titles.toArray(new String[0]), (dialog, which) -> {
                                Intent intent = new Intent(this, EventDetailsActivity.class);
                                intent.putExtra("eventId", ids.get(which));
                                startActivity(intent);
                            })
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }
}
