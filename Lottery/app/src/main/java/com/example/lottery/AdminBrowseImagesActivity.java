package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminBrowseImagesActivity serves as the administrator image browser, displaying all event posters.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Displays a list of all events that have a poster image uploaded.</li>
 *   <li>Each item shows a poster thumbnail, event title, and scheduled date.</li>
 *   <li>Handles navigation to the image detail/preview screen on item click.</li>
 *   <li>Fetches event data from Firestore and filters for non-empty posterUri on the client side.</li>
 * </ul>
 * </p>
 */
public class AdminBrowseImagesActivity extends AppCompatActivity implements AdminImageAdapter.OnImageClickListener {

    private static final String TAG = "AdminBrowseImages";

    /**
     * RecyclerView for displaying the list of event poster images.
     */
    private RecyclerView rvImages;
    /**
     * Adapter for binding image data to the RecyclerView.
     */
    private AdminImageAdapter adapter;
    /**
     * List to hold the events that have poster images.
     */
    private List<Event> imageList;
    /**
     * TextView displayed when no images are found.
     */
    private TextView tvNoImages;
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_browse_images);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets in = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(in.left, in.top, in.right, in.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Get userId from intent or shared preferences
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
        }

        rvImages = findViewById(R.id.rvImages);
        tvNoImages = findViewById(R.id.tvNoImages);

        imageList = new ArrayList<>();
        adapter = new AdminImageAdapter(imageList, this);
        rvImages.setLayoutManager(new LinearLayoutManager(this));
        rvImages.setAdapter(adapter);

        AdminNavigationHelper.setup(this, AdminNavigationHelper.AdminTab.IMAGES, userId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }


    /**
     * Loads events from Firestore and filters for those with a non-empty posterUri.
     *
     * <p>Uses client-side filtering because Firestore cannot reliably query for
     * non-null and non-empty string fields in a single query.</p>
     */
    private void loadImages() {
        db.collection(FirestorePaths.EVENTS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    imageList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            event.setEventId(document.getId());
                            String uri = event.getPosterUri();
                            if (uri != null && !uri.trim().isEmpty()) {
                                imageList.add(event);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping document " + document.getId(), e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvNoImages.setVisibility(imageList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    Toast.makeText(this, R.string.failed_to_load_events, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles clicks on individual image items in the RecyclerView.
     *
     * @param event The Event object whose poster was clicked.
     */
    @Override
    public void onImageClick(Event event) {
        Intent intent = new Intent(this, AdminImageDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}
