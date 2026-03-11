package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 * MainActivity is the entry point of the application. It displays the role selection screen
 * where users can choose to register or sign in as an Entrant, Organizer, or Admin (not yet implemented).
 * It also handles automatic login for anonymous users who have previously used the app
 * without registering.
 *
 * <p>This activity checks for existing anonymous sessions in SharedPreferences and
 * automatically navigates returning anonymous users to the EntrantMainActivity.</p>
 *
 * @see EntrantRegistrationActivity
 * @see OrganizerRegistrationActivity
 * @see GeneralSignInActivity
 */
public class MainActivity extends AppCompatActivity {
    private Button signInButton;
    private Button entrantButton;
    private Button organizerButton;
    private Button adminButton;
    private TextView chooseRoleText;
    private TextView signInPrompt;

    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    private static final String KEY_IS_ANONYMOUS = "isAnonymous";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_FID = "fid";


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

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

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

    @Override
    protected void onStart() {
        super.onStart();
        // Check for anonymous session when activity starts
        checkAnonymousSession();
    }

    /**
     * Check if there's an existing anonymous session
     * If yes, automatically navigate to EntrantMainActivity
     */
    private void checkAnonymousSession() {
        boolean isAnonymous = sharedPreferences.getBoolean(KEY_IS_ANONYMOUS, false);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String userName = sharedPreferences.getString(KEY_USER_NAME, "Anonymous User");
        String fid = sharedPreferences.getString(KEY_FID, null);

        // Check for valid anonymous session: must be anonymous, have userId, and have FID
        // The userId should start with "anon_" to confirm it's an anonymous user
        if (isAnonymous && userId != null && userId.startsWith("anon_") && fid != null) {
            // User has a valid anonymous session, auto-login
            navigateToEntrantMain(userId, userName, true);
        }
    }

    /**
    // Navigate to EntrantMainActivity for anonymous users
     */
    private void navigateToEntrantMain(String userId, String userName, boolean isAnonymous) {
        Intent intent = new Intent(MainActivity.this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        intent.putExtra("isRegistered", false);
        intent.putExtra("isAnonymous", isAnonymous);
        String fid = sharedPreferences.getString(KEY_FID, "");
        intent.putExtra("fid", fid);

        startActivity(intent);
        finish(); // Close MainActivity so user can't go back to role selection
    }

}