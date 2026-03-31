package com.example.lottery;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.EntrantEvent;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Activity to display entrants' locations on a map for a specific event.
 * Shows all entrants regardless of their individual status if requireLocation is true for the event.
 */
public class EntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "EntrantMapActivity";
    private final ArrayList<EntrantEvent> entrants = new ArrayList<>();
    private GoogleMap googleMap;
    private MapView mapView;
    private String eventId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.view_location_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null) {
            Toast.makeText(this, "Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        checkRequirementAndFetch();
    }

    /**
     * US 02.02.02 Implementation:
     * Check if the event requires location. If so, fetch all entrants' locations.
     */
    private void checkRequirementAndFetch() {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean requireLocation = documentSnapshot.getBoolean("requireLocation");
                        if (requireLocation != null && requireLocation) {
                            fetchAllEntrantsLocations();
                        } else {
                            // If location is not required, the map should technically be empty or not shown.
                            // Here we show a toast as per user request.
                            Toast.makeText(this, R.string.location_not_required_for_event, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking event requirement", e));
    }

    /**
     * Fetches all entrants from the waitingList subcollection who have location data.
     */
    private void fetchAllEntrantsLocations() {
        db.collection(FirestorePaths.eventWaitingList(eventId))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    entrants.clear();
                    for (QueryDocumentSnapshot snapshot : querySnapshot) {
                        EntrantEvent entrant = snapshot.toObject(EntrantEvent.class);
                        if (entrant.getUserId() == null) {
                            entrant.setUserId(snapshot.getId());
                        }

                        // We collect all entrants who have a location, regardless of their status
                        // (waitlisted, invited, accepted, etc.)
                        if (entrant.getLocation() != null) {
                            entrants.add(entrant);
                        }
                    }

                    if (entrants.isEmpty()) {
                        Toast.makeText(this, R.string.no_entrant_locations_available, Toast.LENGTH_LONG).show();
                    }

                    if (googleMap != null) {
                        insertMarkers(entrants);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching entrants", e));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap g) {
        googleMap = g;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Default view: Canada/North America if no markers yet
        LatLng defaultCenter = new LatLng(56.1304, -106.3468);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 3f));

        if (!entrants.isEmpty()) {
            insertMarkers(entrants);
        }
    }

    /**
     * Clears existing markers and adds new ones for the list of entrants.
     * Adjusts camera to fit all markers.
     */
    private void insertMarkers(ArrayList<EntrantEvent> list) {
        if (googleMap == null || list.isEmpty()) return;
        googleMap.clear();

        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasLocations = false;

        for (EntrantEvent entrant : list) {
            GeoPoint geoLocation = entrant.getLocation();
            if (geoLocation != null) {
                final LatLng position = new LatLng(geoLocation.getLatitude(), geoLocation.getLongitude());

                String statusLabel = InvitationFlowUtil.normalizeEntrantStatus(entrant.getStatus());
                String title = (entrant.getUserName() != null ? entrant.getUserName() : "Unknown Entrant");
                String snippet = "Status: " + statusLabel;

                googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(snippet));

                builder.include(position);
                hasLocations = true;
            }
        }

        if (hasLocations) {
            try {
                LatLngBounds bounds = builder.build();
                int padding = 200; // Offset from edges
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                googleMap.animateCamera(cu);
            } catch (IllegalStateException e) {
                Log.w(TAG, "Map bounds could not be calculated yet");
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
