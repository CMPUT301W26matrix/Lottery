package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity is a role selector entry point for organizer and administrator flows.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpenOrganizerView = findViewById(R.id.btnOpenOrganizerView);
        Button btnOpenAdminView = findViewById(R.id.btnOpenAdminView);

        btnOpenOrganizerView.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerBrowseEventsActivity.class)));
        btnOpenAdminView.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseEventsActivity.class)));
    }
}
