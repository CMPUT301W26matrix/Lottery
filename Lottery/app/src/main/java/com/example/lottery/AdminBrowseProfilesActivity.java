package com.example.lottery;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.lottery.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Allows administrators to browse all user profiles in the system.
 */
public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private ListView lvProfiles;
    private TextView tvEmptyProfiles;

    private ArrayList<User> users;
    private ProfileAdapter profileAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        lvProfiles = findViewById(R.id.lvProfiles);
        tvEmptyProfiles = findViewById(R.id.tvEmptyProfiles);

        db = FirebaseFirestore.getInstance();

        users = new ArrayList<>();
        profileAdapter = new ProfileAdapter(this, users);
        lvProfiles.setAdapter(profileAdapter);

        setupNavigation();

        // Simple admin-only access check
        String role = getIntent().getStringExtra("role");
        if (role == null || !role.equals("admin")) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProfiles();
    }

    /**
     * Sets up click listeners for the admin navigation elements.
     */
    private void setupNavigation() {
        // The shared admin nav defaults to the events tab, so this screen retints it.
        highlightProfilesTab();

        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(AdminBrowseProfilesActivity.this, AdminBrowseEventsActivity.class);
                intent.putExtra("role", "admin");
                startActivity(intent);
                finish();
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v ->
                    Toast.makeText(this, "Already viewing profiles", Toast.LENGTH_SHORT).show());
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_images_coming_soon, Toast.LENGTH_SHORT).show());
        }

        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_logs_coming_soon, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Highlights the current profiles tab without changing the shared layout defaults.
     */
    private void highlightProfilesTab() {
        int activeColor = ContextCompat.getColor(this, R.color.primary_blue);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_gray);

        ImageView homeIcon = findViewById(R.id.nav_home_icon);
        TextView homeText = findViewById(R.id.nav_home_text);
        ImageView profilesIcon = findViewById(R.id.nav_profiles_icon);
        TextView profilesText = findViewById(R.id.nav_profiles_text);

        if (homeIcon != null) {
            homeIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        }
        if (homeText != null) {
            homeText.setTextColor(inactiveColor);
        }
        if (profilesIcon != null) {
            profilesIcon.setImageTintList(ColorStateList.valueOf(activeColor));
        }
        if (profilesText != null) {
            profilesText.setTextColor(activeColor);
        }
    }

    /*
     * Loads all user profiles from Firestore
     */
    private void loadProfiles() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Rebuild the list from the latest Firestore snapshot each time.
                    users.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvEmptyProfiles.setText(R.string.no_user_profiles_in_the_system);
                        tvEmptyProfiles.setVisibility(View.VISIBLE);
                        lvProfiles.setVisibility(View.GONE);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");

                        // Keep the list readable even when older documents have missing fields.
                        if (name == null || name.isEmpty()) {
                            name = "Unknown User";
                        }

                        if (email == null || email.isEmpty()) {
                            email = "No email";
                        }

                        if (phone == null) {
                            phone = "";
                        }

                        // The adapter expects a lightweight User object for display only, but not full info
                        users.add(new User(name, email, phone));
                    }

                    tvEmptyProfiles.setVisibility(View.GONE);
                    lvProfiles.setVisibility(View.VISIBLE);
                    profileAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    tvEmptyProfiles.setText(R.string.failed_to_load_profiles);
                    tvEmptyProfiles.setVisibility(View.VISIBLE);
                    lvProfiles.setVisibility(View.GONE);
                    Toast.makeText(AdminBrowseProfilesActivity.this, "Error loading profiles", Toast.LENGTH_SHORT).show();
                });
    }
}
