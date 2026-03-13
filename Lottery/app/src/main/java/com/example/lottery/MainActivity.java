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
 * MainActivity is the entry point of the application.
 * It shows the role selection screen where users can:
 * - Register or continue as an Entrant
 * - Register or continue as an Organizer
 * - Sign in as an Admin
 * - Use the general sign-in screen
 *
 * It also checks whether an anonymous entrant session already exists
 * and redirects that user automatically to EntrantMainActivity.
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        entrantButton = findViewById(R.id.entrant_login_button);
        organizerButton = findViewById(R.id.organizer_login_button);
        adminButton = findViewById(R.id.admin_login_button);
        signInButton = findViewById(R.id.btnSignIn);

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
        checkAnonymousSession();
    }

    /**
     * Checks whether an anonymous entrant session exists in SharedPreferences.
     * If so, automatically redirects the user to EntrantMainActivity.
     */
    private void checkAnonymousSession() {
        boolean isAnonymous = sharedPreferences.getBoolean(KEY_IS_ANONYMOUS, false);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String userName = sharedPreferences.getString(KEY_USER_NAME, "Anonymous User");
        String fid = sharedPreferences.getString(KEY_FID, null);

        if (isAnonymous && userId != null && userId.startsWith("anon_") && fid != null) {
            navigateToEntrantMain(userId, userName, true);
        }
    }

    /**
     * Navigates to EntrantMainActivity and passes the saved entrant session info.
     *
     * @param userId       the stored user id
     * @param userName     the stored user name
     * @param isAnonymous  whether the user is anonymous
     */
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