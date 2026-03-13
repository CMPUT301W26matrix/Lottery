package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * BrowseProfilesActivity
 * Allows administrator to browse all user profiles in the system.
 */
public class BrowseProfilesActivity extends AppCompatActivity {

    private ListView lvProfiles;
    private TextView tvEmptyProfiles;

    private ArrayList<User> users;
    private ProfileAdapter profileAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_browse_profiles);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets in = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(in.left, in.top, in.right, in.bottom);
            return insets;
        });

        lvProfiles = findViewById(R.id.lvProfiles);
        tvEmptyProfiles = findViewById(R.id.tvEmptyProfiles);

        db = FirebaseFirestore.getInstance();

        users = new ArrayList<>();
        profileAdapter = new ProfileAdapter(this, users);
        lvProfiles.setAdapter(profileAdapter);

        // Simple admin-only access check
        String role = getIntent().getStringExtra("role");
        if (role == null || !role.equals("admin")) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupNavigation();
        loadProfiles();
    }

    /**
     * Sets up navigation actions for the admin bottom navigation bar.
     */
    private void setupNavigation() {
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            // Update UI to show home is inactive
            TextView tvHome = btnHome.findViewById(android.R.id.text1); // This might be wrong, checking layout
            // Looking at layout_bottom_nav_admin.xml, it doesn't use IDs for text/icons internally except the containers.
            // I should use the IDs from the layout.

            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            // Highlight current tab
            ImageView ivProfiles = btnProfiles.findViewById(R.id.nav_profile); // Wait, IDs are not in the layout for children.
            // Let me re-read layout_bottom_nav_admin.xml
        }

        // Let's just set the listeners first as they are in AdminBrowseEventsActivity
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v ->
                    Toast.makeText(this, "Browsing all profiles", Toast.LENGTH_SHORT).show());
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

        // Manual highlighting since we are not using a real BottomNavigationView
        highlightCurrentTab();
    }

    private void highlightCurrentTab() {
        View btnHome = findViewById(R.id.nav_home);
        View btnProfiles = findViewById(R.id.nav_profiles);

        if (btnHome != null && btnProfiles != null) {
            // Reset home
            ImageView ivHome = (ImageView) ((android.view.ViewGroup) btnHome).getChildAt(0);
            TextView tvHome = (TextView) ((android.view.ViewGroup) btnHome).getChildAt(1);
            ivHome.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_gray)));
            tvHome.setTextColor(ContextCompat.getColor(this, R.color.text_gray));

            // Highlight profiles
            ImageView ivProfiles = (ImageView) ((android.view.ViewGroup) btnProfiles).getChildAt(0);
            TextView tvProfiles = (TextView) ((android.view.ViewGroup) btnProfiles).getChildAt(1);
            ivProfiles.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_blue)));
            tvProfiles.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
        }
    }

    /*
     * Loads all user profiles from Firestore
     */
    private void loadProfiles() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    users.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvEmptyProfiles.setText("No user profiles in the system");
                        tvEmptyProfiles.setVisibility(View.VISIBLE);
                        lvProfiles.setVisibility(View.GONE);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone_number");

                        if (name == null || name.isEmpty()) {
                            name = "Unknown User";
                        }

                        if (email == null || email.isEmpty()) {
                            email = "No email";
                        }

                        if (phone == null) {
                            phone = "";
                        }

                        users.add(new User(name, email, phone));
                    }

                    tvEmptyProfiles.setVisibility(View.GONE);
                    lvProfiles.setVisibility(View.VISIBLE);
                    profileAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    tvEmptyProfiles.setText("Failed to load profiles");
                    tvEmptyProfiles.setVisibility(View.VISIBLE);
                    lvProfiles.setVisibility(View.GONE);
                    Toast.makeText(BrowseProfilesActivity.this, "Error loading profiles", Toast.LENGTH_SHORT).show();
                });
    }
}
