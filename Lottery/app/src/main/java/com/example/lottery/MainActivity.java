package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button signInButton;
    private Button entrantButton;
    private Button organizerButton;
    private Button adminButton;
    private TextView chooseRoleText;
    private TextView signInPrompt;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize firebase
        db = FirebaseFirestore.getInstance();

        // Role Selection buttons
        entrantButton = findViewById(R.id.entrant_login_button);
        organizerButton = findViewById(R.id.organizer_login_button);
        adminButton = findViewById(R.id.admin_login_button);

        // Sign-in button (for registered users)
        signInButton = findViewById(R.id.btnSignIn);

        // Text views
        chooseRoleText = findViewById(R.id.tvChooseRole);
        signInPrompt = findViewById(R.id.tvSignInHint);

        entrantButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, EntrantRegistrationActivity.class);
            startActivity(intent);
        });

        organizerButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, OrganizerRegistrationActivity.class);
            startActivity(intent);
        });

        adminButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AdminSignInActivity.class);
            startActivity(intent);
        });

        signInButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, GeneralSignInActivity.class);
            startActivity(intent);
        });

    }

}