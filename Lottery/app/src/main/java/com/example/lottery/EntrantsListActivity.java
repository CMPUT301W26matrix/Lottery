package com.example.lottery;

import static java.lang.Long.min;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays organizer-side entrant lists for one event.
 *
 * <p>All entrant data in this activity is read from:</p>
 *
 * <pre>
 * events/{eventId}/entrant_events/{userId}
 * </pre>
 *
 * <p>The status field in entrant_events is the single source of truth.</p>
 *
 * <p>Supported organizer views:</p>
 * <ul>
 *     <li>Waiting entrants</li>
 *     <li>Invited entrants</li>
 *     <li>Accepted entrants</li>
 *     <li>Cancelled / declined entrants</li>
 *     <li>Map view for locations</li>
 * </ul>
 *
 * <p>This activity also supports:</p>
 * <ul>
 *     <li>Sending notifications to the currently selected entrant group</li>
 *     <li>Sampling winners from the waiting list</li>
 * </ul>
 */
public class EntrantsListActivity extends AppCompatActivity
        implements NotificationFragment.NotificationListener,
        SampleFragment.SamplingListener,
        OnMapReadyCallback {

    /** Log tag for debugging. */
    private static final String TAG = "EntrantsListActivity";

    /** Buttons for switching between entrant groups and actions. */
    private Button btnSwitchSignedUp;
    private Button btnSwitchCancelled;
    private Button btnSwitchWaitedList;
    private Button btnSendNotification;
    private Button btnViewLocation;
    private Button btnSampleWinners;
    private Button btnSwitchInvited;

    /** Firestore instance. */
    private FirebaseFirestore db;

    /** In-memory entrant lists for each organizer tab. */
    private ArrayList<Entrant> entrantSignedUpArrayList;
    private ArrayList<Entrant> entrantInvitedArrayList;
    private ArrayList<Entrant> entrantCancelledArrayList;
    private ArrayList<Entrant> entrantWaitedListArrayList;

    /** RecyclerView adapters. */
    private SignedUpListAdapter signedUpAdapter;
    private CancelledListAdapter cancelledAdapter;
    private WaitedListedListAdapter waitedListAdapter;
    private InvitedListAdapter invitedAdapter;

    /** Layout containers for tab switching. */
    private LinearLayout invitedEntrantsListLayout;
    private LinearLayout cancelledEntrantsListLayout;
    private LinearLayout signedUpEntrantsListLayout;
    private LinearLayout waitedListEntrantsListLayout;
    private LinearLayout viewLocationLayout;

    /** RecyclerViews for each entrant group. */
    private RecyclerView invitedEventsView;
    private RecyclerView signedUpEventsView;
    private RecyclerView waitedListEventsView;
    private RecyclerView cancelledEntrantsView;

    /** Google Map references. */
    private GoogleMap googleMap;
    private MapView mapView;

    /** Current event ID. */
    private String eventId;

    /** Selection capacity and current sample size ceiling. */
    private long selectedCapacity;
    private long maxSampleSize;

    /** Snapshot listeners for live updates by status. */
    private ListenerRegistration waitedListReg;
    private ListenerRegistration invitedReg;
    private ListenerRegistration cancelledReg;
    private ListenerRegistration signedUpReg;

    /**
     * Initializes the activity, Firebase, views, adapters, listeners, and live queries.
     *
     * @param savedInstanceState previously saved state, if any
     */
    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrants_list);

        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Service unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        entrantSignedUpArrayList = new ArrayList<>();
        entrantCancelledArrayList = new ArrayList<>();
        entrantWaitedListArrayList = new ArrayList<>();
        entrantInvitedArrayList = new ArrayList<>();

        signedUpAdapter = new SignedUpListAdapter(this, entrantSignedUpArrayList);
        cancelledAdapter = new CancelledListAdapter(this, entrantCancelledArrayList);
        waitedListAdapter = new WaitedListedListAdapter(this, entrantWaitedListArrayList);
        invitedAdapter = new InvitedListAdapter(this, entrantInvitedArrayList);

        signedUpEventsView.setAdapter(signedUpAdapter);
        waitedListEventsView.setAdapter(waitedListAdapter);
        cancelledEntrantsView.setAdapter(cancelledAdapter);
        invitedEventsView.setAdapter(invitedAdapter);

        showLayout(waitedListEntrantsListLayout);

        btnSwitchSignedUp.setOnClickListener(v -> showLayout(signedUpEntrantsListLayout));
        btnSwitchCancelled.setOnClickListener(v -> showLayout(cancelledEntrantsListLayout));
        btnSwitchWaitedList.setOnClickListener(v -> showLayout(waitedListEntrantsListLayout));
        btnSwitchInvited.setOnClickListener(v -> showLayout(invitedEntrantsListLayout));

        btnSendNotification.setOnClickListener(v -> {
            NotificationFragment notificationFragment = new NotificationFragment();
            notificationFragment.show(getSupportFragmentManager(), "Send Notification");
        });

        btnSampleWinners.setOnClickListener(v -> {
            SampleFragment sampleFragment = new SampleFragment();
            sampleFragment.show(getSupportFragmentManager(), "Sample Winners");
        });

        btnViewLocation.setOnClickListener(v -> {
            if (googleMap != null) {
                if (waitedListEntrantsListLayout.getVisibility() == View.VISIBLE) {
                    insertMarkers(entrantWaitedListArrayList);
                } else if (signedUpEntrantsListLayout.getVisibility() == View.VISIBLE) {
                    insertMarkers(entrantSignedUpArrayList);
                } else if (cancelledEntrantsListLayout.getVisibility() == View.VISIBLE) {
                    insertMarkers(entrantCancelledArrayList);
                } else if (invitedEntrantsListLayout.getVisibility() == View.VISIBLE) {
                    insertMarkers(entrantInvitedArrayList);
                }
            }
            showLayout(viewLocationLayout);
        });

        attachWaitedListListener();
        attachInvitedListListener();
        attachAcceptedListListener();
        attachCancelledListListener();
        loadSelectedCapacity();
    }

    /**
     * Attaches the waiting-list live query.
     *
     * <p>Reads entrants whose status is {@code waiting}.</p>
     */
    private void attachWaitedListListener() {
        waitedListReg = db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .whereEqualTo("status", InvitationFlowUtil.STATUS_WAITING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "waiting listener error", e);
                        return;
                    }

                    entrantWaitedListArrayList.clear();

                    if (querySnapshot != null) {
                        for (DocumentSnapshot snapshot : querySnapshot) {
                            String userId = snapshot.getString("userId");
                            GeoPoint location = snapshot.getGeoPoint("location");
                            Timestamp joinedTime = snapshot.getTimestamp("joinedWaitlistAt");

                            String name = userId;
                            Entrant entrant = new Entrant(name, userId, joinedTime, location);
                            entrantWaitedListArrayList.add(entrant);
                        }
                    }

                    waitedListAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Attaches the invited-list live query.
     *
     * <p>Reads entrants whose status is {@code invited}.</p>
     */
    private void attachInvitedListListener() {
        invitedReg = db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .whereEqualTo("status", InvitationFlowUtil.STATUS_INVITED)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "invited listener error", e);
                        return;
                    }

                    entrantInvitedArrayList.clear();

                    if (querySnapshot != null) {
                        for (DocumentSnapshot snapshot : querySnapshot) {
                            String userId = snapshot.getString("userId");
                            GeoPoint location = snapshot.getGeoPoint("location");
                            Timestamp invitedTime = snapshot.getTimestamp("invitedAt");
                            String name = userId;

                            Entrant entrant = new Entrant(invitedTime, name, userId, null, location);
                            entrantInvitedArrayList.add(entrant);
                        }
                    }

                    invitedAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Attaches the accepted-list live query.
     *
     * <p>Reads entrants whose status is {@code accepted}.</p>
     */
    private void attachAcceptedListListener() {
        signedUpReg = db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .whereEqualTo("status", InvitationFlowUtil.STATUS_ACCEPTED)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "accepted listener error", e);
                        return;
                    }

                    entrantSignedUpArrayList.clear();

                    if (querySnapshot != null) {
                        for (DocumentSnapshot snapshot : querySnapshot) {
                            String userId = snapshot.getString("userId");
                            GeoPoint location = snapshot.getGeoPoint("location");
                            Timestamp invitedTime = snapshot.getTimestamp("invitedAt");
                            Timestamp acceptedTime = snapshot.getTimestamp("acceptedAt");
                            String name = userId;

                            Entrant entrant = new Entrant(name, userId, invitedTime, null, location, acceptedTime);
                            entrantSignedUpArrayList.add(entrant);
                        }
                    }

                    signedUpAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Attaches the cancelled / declined list live query.
     *
     * <p>Reads entrants whose status is {@code declined}.</p>
     */
    private void attachCancelledListListener() {
        cancelledReg = db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .whereEqualTo("status", InvitationFlowUtil.STATUS_DECLINED)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "declined listener error", e);
                        return;
                    }

                    entrantCancelledArrayList.clear();

                    if (querySnapshot != null) {
                        for (DocumentSnapshot snapshot : querySnapshot) {
                            String userId = snapshot.getString("userId");
                            GeoPoint location = snapshot.getGeoPoint("location");
                            Timestamp invitedTime = snapshot.getTimestamp("invitedAt");
                            Timestamp cancelledTime = snapshot.getTimestamp("cancelledAt");
                            String name = userId;

                            Entrant entrant = new Entrant(name, location, userId, cancelledTime, invitedTime, null);
                            entrantCancelledArrayList.add(entrant);
                        }
                    }

                    cancelledAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Loads the event's selected capacity from Firestore.
     *
     * <p>Uses {@code selectedCapacity} first and falls back to {@code maxCapacity} if needed.</p>
     */
    private void loadSelectedCapacity() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        selectedCapacity = 0L;
                        return;
                    }

                    Long selectedCap = documentSnapshot.getLong("selectedCapacity");
                    Long fallbackCap = documentSnapshot.getLong("maxCapacity");

                    if (selectedCap != null) {
                        selectedCapacity = selectedCap;
                    } else if (fallbackCap != null) {
                        selectedCapacity = fallbackCap;
                    } else {
                        selectedCapacity = 0L;
                    }
                });
    }

    /**
     * Samples a specific number of waiting entrants and marks them as invited.
     *
     * <p>This updates the same entrant_events documents instead of moving data between collections.</p>
     *
     * @param size requested sample size as text
     */
    @SuppressLint({"DefaultLocale", "NotifyDataSetChanged"})
    @Override
    public void sampling(String size) {
        try {
            Integer.parseInt(size);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ERROR: sample size must be an integer", Toast.LENGTH_LONG).show();
            return;
        }

        int sampleSize = Integer.parseInt(size);
        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.collection("entrant_events")
                .whereEqualTo("status", InvitationFlowUtil.STATUS_WAITING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No entrants in the waiting list", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<DocumentSnapshot> data = queryDocumentSnapshots.getDocuments();
                    Collections.shuffle(data);

                    maxSampleSize = min(
                            selectedCapacity - entrantSignedUpArrayList.size() - entrantInvitedArrayList.size(),
                            entrantWaitedListArrayList.size()
                    );

                    if (maxSampleSize < sampleSize || sampleSize <= 0) {
                        String errorMessage = String.format(
                                "ERROR: check failed: 0 < sample size < %d",
                                maxSampleSize
                        );
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    Timestamp now = Timestamp.now();

                    for (int i = 0; i < sampleSize; i++) {
                        DocumentSnapshot documentSnapshot = data.get(i);
                        batch.update(
                                documentSnapshot.getReference(),
                                "status", InvitationFlowUtil.STATUS_INVITED,
                                "invitedAt", now,
                                "updatedAt", now
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, "Winners sampled successfully", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Sampling failed", Toast.LENGTH_SHORT).show()
                            );
                });
    }

    /**
     * Sends an organizer notification to the currently selected entrant group.
     *
     * <p>Recipients are determined by the visible tab and read from entrant_events.</p>
     *
     * @param content notification message body
     */
    @Override
    public void sendNotification(String content) {
        if (content == null || content.trim().isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetStatus;

        if (waitedListEntrantsListLayout.getVisibility() == View.VISIBLE) {
            targetStatus = InvitationFlowUtil.STATUS_WAITING;
        } else if (signedUpEntrantsListLayout.getVisibility() == View.VISIBLE) {
            targetStatus = InvitationFlowUtil.STATUS_ACCEPTED;
        } else if (cancelledEntrantsListLayout.getVisibility() == View.VISIBLE) {
            targetStatus = InvitationFlowUtil.STATUS_DECLINED;
        } else if (invitedEntrantsListLayout.getVisibility() == View.VISIBLE) {
            targetStatus = InvitationFlowUtil.STATUS_INVITED;
        } else {
            Toast.makeText(this, "No list selected", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .whereEqualTo("status", targetStatus)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int sentCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.isEmpty()) {
                            continue;
                        }

                        Map<String, Object> notification = new HashMap<>();
                        notification.put("type", "organizer_message");
                        notification.put("eventId", eventId);
                        notification.put("title", "Organizer Update");
                        notification.put("message", content.trim());
                        notification.put("isRead", false);
                        notification.put("createdAt", Timestamp.now());

                        db.collection("users")
                                .document(userId)
                                .collection("notifications")
                                .add(notification);

                        sentCount++;
                    }

                    Toast.makeText(this, "Sent to " + sentCount + " user(s)", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Binds UI views and sets RecyclerView layout managers.
     */
    private void initializeViews() {
        btnSwitchWaitedList = findViewById(R.id.entrants_list_waited_list_btn);
        btnSwitchCancelled = findViewById(R.id.entrants_list_cancelled_btn);
        btnSwitchSignedUp = findViewById(R.id.entrants_list_signed_up_btn);
        btnViewLocation = findViewById(R.id.entrants_list_view_location_btn);
        btnSampleWinners = findViewById(R.id.entrants_list_sample_btn);
        btnSwitchInvited = findViewById(R.id.entrants_list_invited_btn);
        btnSendNotification = findViewById(R.id.entrants_list_send_notification_btn);

        signedUpEventsView = findViewById(R.id.signed_up_events_view);
        invitedEventsView = findViewById(R.id.invited_events_view);
        waitedListEventsView = findViewById(R.id.waited_list_events_view);
        cancelledEntrantsView = findViewById(R.id.cancelled_entrants_view);

        signedUpEventsView.setLayoutManager(new LinearLayoutManager(this));
        waitedListEventsView.setLayoutManager(new LinearLayoutManager(this));
        cancelledEntrantsView.setLayoutManager(new LinearLayoutManager(this));
        invitedEventsView.setLayoutManager(new LinearLayoutManager(this));

        cancelledEntrantsListLayout = findViewById(R.id.cancelled_entrants_list_layout);
        signedUpEntrantsListLayout = findViewById(R.id.signed_up_entrants_list_layout);
        waitedListEntrantsListLayout = findViewById(R.id.waited_list_entrants_list_layout);
        invitedEntrantsListLayout = findViewById(R.id.invited_entrants_list_layout);
        viewLocationLayout = findViewById(R.id.view_location_layout);

        mapView = findViewById(R.id.mapView);
    }

    /**
     * Inserts markers for entrant locations into the map.
     *
     * @param list entrants whose locations should be displayed
     */
    private void insertMarkers(ArrayList<Entrant> list) {
        googleMap.clear();
        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasLocations = false;

        for (Entrant entrant : list) {
            GeoPoint geoLocation = entrant.getLocation();
            if (geoLocation == null) {
                continue;
            }

            final LatLng position = new LatLng(
                    geoLocation.getLatitude(),
                    geoLocation.getLongitude()
            );

            final MarkerOptions options = new MarkerOptions().position(position);
            googleMap.addMarker(options);
            builder.include(position);
            hasLocations = true;
        }

        if (!hasLocations) {
            Toast.makeText(this, "No entrant locations available for this list.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows one layout and hides all others.
     *
     * @param target target layout to show
     */
    private void showLayout(LinearLayout target) {
        signedUpEntrantsListLayout.setVisibility(signedUpEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        cancelledEntrantsListLayout.setVisibility(cancelledEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        waitedListEntrantsListLayout.setVisibility(waitedListEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        invitedEntrantsListLayout.setVisibility(invitedEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        viewLocationLayout.setVisibility(viewLocationLayout == target ? View.VISIBLE : View.GONE);
    }

    /**
     * Called when the Google Map is ready to use.
     *
     * @param g initialized GoogleMap instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap g) {
        googleMap = g;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    /**
     * Starts MapView lifecycle.
     */
    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
    }

    /**
     * Stops MapView lifecycle.
     */
    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    /**
     * Resumes MapView lifecycle.
     */
    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    /**
     * Pauses MapView lifecycle.
     */
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * Cleans up listeners and MapView resources.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (waitedListReg != null) waitedListReg.remove();
        if (invitedReg != null) invitedReg.remove();
        if (cancelledReg != null) cancelledReg.remove();
        if (signedUpReg != null) signedUpReg.remove();
        mapView.onDestroy();
    }

    /**
     * Forwards low-memory signal to MapView.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}