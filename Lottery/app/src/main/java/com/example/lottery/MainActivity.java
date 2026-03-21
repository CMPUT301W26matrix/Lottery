package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * MainActivity serves as the main dashboard for organizers.
 *
 * <p>This activity retrieves event data from Firestore and displays it
 * in a RecyclerView. It also shows summary statistics for events
 * including active, closed, pending, and total events.</p>
 *
 * <p>The activity implements {@link EventAdapter.OnEventClickListener}
 * to respond when an event in the list is selected.</p>
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Log tag used for debugging.
     */
    private static final String TAG = "MainActivity";
    private static final String KEY_IS_ANONYMOUS = "isAnonymous";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_FID = "fid";
    /**
     * RecyclerView displaying the list of events.
     */
    private RecyclerView rvEvents;
    /**
     * Adapter used to bind event data to the RecyclerView.
     */
    private EventAdapter adapter;
    /**
     * List storing events retrieved from Firestore.
     */
    private List<Event> eventList;
    /**
     * TextView shown when no events exist.
     */
    private TextView tvNoEvents;
    /**
     * TextViews displaying event statistics.
     */
    private TextView tvActiveCount, tvClosedCount, tvPendingCount, tvTotalCount;
    /**
     * Firestore database reference.
     */
    private FirebaseFirestore db;
    private Button signInButton;
    private Button entrantButton;
    private Button organizerButton;
    private Button adminButton;
    private TextView chooseRoleText;
    private TextView signInPrompt;
    private SharedPreferences sharedPreferences;

    /**
     * Initializes the activity, sets up UI components,
     * and loads events from Firestore.
     *
     * @param savedInstanceState the saved state of the activity
     */
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

        sharedPreferences = getSharedPreferences(LotteryApplication.PREFS_NAME, MODE_PRIVATE);

        entrantButton = findViewById(R.id.entrant_login_button);
        organizerButton = findViewById(R.id.organizer_login_button);
        adminButton = findViewById(R.id.admin_login_button);
        signInButton = findViewById(R.id.btnSignIn);

        setupNavigation();
    }

    /**
     * Sets up navigation actions for the bottom navigation bar.
     */
    private void setupNavigation() {

        View btnCreate = findViewById(R.id.nav_create_container);

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, OrganizerCreateEventActivity.class))
            );
        }

        View btnHome = findViewById(R.id.nav_home);

        if (btnHome != null) {
            btnHome.setOnClickListener(v ->
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
            );
        }
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

    /**
     * Reloads event data whenever the activity resumes.
     */
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
