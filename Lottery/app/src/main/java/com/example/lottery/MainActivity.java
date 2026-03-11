package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * MainActivity is the entry point of the application. It displays the role selection screen
 * where users can choose to register or sign in as an Entrant, Organizer, or Admin.
 * It also handles automatic login for anonymous users who have previously used the app
 * without registering.
 */
public class MainActivity extends AppCompatActivity {
    private static final String KEY_IS_ANONYMOUS = "isAnonymous";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_FID = "fid";
    private Button signInButton;
    private Button entrantButton;
    private Button organizerButton;
    private Button adminButton;
    private TextView chooseRoleText;
    private TextView signInPrompt;
    private SharedPreferences sharedPreferences;

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

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        entrantButton = findViewById(R.id.entrant_login_button);
        organizerButton = findViewById(R.id.organizer_login_button);
        adminButton = findViewById(R.id.admin_login_button);
        signInButton = findViewById(R.id.btnSignIn);

        chooseRoleText = findViewById(R.id.tvChooseRole);
        signInPrompt = findViewById(R.id.tvSignInHint);

        entrantButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, EntrantRegistrationActivity.class)));

        organizerButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, OrganizerRegistrationActivity.class)));

        adminButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, AdminSignInActivity.class)));

        signInButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, GeneralSignInActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAnonymousSession();
    }

    private void checkAnonymousSession() {
        boolean isAnonymous = sharedPreferences.getBoolean(KEY_IS_ANONYMOUS, false);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String userName = sharedPreferences.getString(KEY_USER_NAME, "Anonymous User");
        String fid = sharedPreferences.getString(KEY_FID, null);

        if (isAnonymous && userId != null && userId.startsWith("anon_") && fid != null) {
            navigateToEntrantMain(userId, userName, true);
        }
    }

    private void navigateToEntrantMain(String userId, String userName, boolean isAnonymous) {
        Intent intent = new Intent(MainActivity.this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        intent.putExtra("isRegistered", false);
        intent.putExtra("isAnonymous", isAnonymous);
        intent.putExtra("fid", sharedPreferences.getString(KEY_FID, ""));

        startActivity(intent);
        finish();
    }
}
