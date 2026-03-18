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
 * Activity to display entrants of different status of a event by list and map and able to sample they
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch the 4 different status entrant collections from Firestore</li>
 *   <li>Render the 4 different status entrant list and implement view location and sample winners functionality</li>
 *   <li>implement US 02.02.02 Be Able To See On A Map Where Entrants Joined My Event</li>
 *   <li>implement US 02.05.02 Be able to sample number of attendees to register for the event</li>
 *   <li>implement US 02.06.01 Be able to view all chosen entrants</li>
 *   <li>implement US 02.06.02 Be able to see a list of all the cancelled entrants</li>
 * </ul>
 * </p>
 */
public class EntrantsListActivity extends AppCompatActivity implements
        NotificationFragment.NotificationListener,
        SampleFragment.SamplingListener,
        OnMapReadyCallback {

    private static final String TAG = "EntrantsListActivity";

    private Button btnSwitchSignedUp, btnSwitchCancelled, btnSwitchWaitedList,
            btnSendNotification, btnViewLocation, btnSampleWinners, btnSwitchInvited;

    private FirebaseFirestore db;

    private ArrayList<Entrant> entrantSignedUpArrayList;
    private ArrayList<Entrant> entrantInvitedArrayList;
    private ArrayList<Entrant> entrantCancelledArrayList;
    private ArrayList<Entrant> entrantWaitedListArrayList;

    private SignedUpListAdapter signedUpAdapter;
    private CancelledListAdapter cancelledAdapter;
    private WaitedListedListAdapter waitedListAdapter;
    private InvitedListAdapter invitedAdapter;

    private LinearLayout invitedEntrantsListLayout, cancelledEntrantsListLayout,
            signedUpEntrantsListLayout, waitedListEntrantsListLayout, viewLocationLayout;

    private RecyclerView invitedEventsView, signedUpEventsView, waitedListEventsView, cancelledEntrantsView;

    private GoogleMap googleMap;
    private MapView mapView;

    private String eventId;
    private long maxCapacity, maxSampleSize;

    private ListenerRegistration entrantsReg;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrants_list);

        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Service Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "event id missing", Toast.LENGTH_SHORT).show();
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

        btnSendNotification.setOnClickListener(view -> {
            NotificationFragment notificationFragment = new NotificationFragment();
            notificationFragment.show(getSupportFragmentManager(), "Send Notification");
        });

        btnSampleWinners.setOnClickListener(view -> {
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

        listenToEntrants();

        db.collection("events").document(eventId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long cap = documentSnapshot.getLong("maxCapacity");
                maxCapacity = cap != null ? cap : 0L;
            }
        });
    }

    /**
     * Single-source listener for entrants under:
     * events/{eventId}/entrants
     */
    @SuppressLint("NotifyDataSetChanged")
    private void listenToEntrants() {
        entrantsReg = db.collection("events")
                .document(eventId)
                .collection("entrants")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "entrants listener error", e);
                        return;
                    }

                    entrantWaitedListArrayList.clear();
                    entrantInvitedArrayList.clear();
                    entrantCancelledArrayList.clear();
                    entrantSignedUpArrayList.clear();

                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot snapshot : querySnapshot) {
                            String userId = getFirstNonEmptyString(
                                    snapshot.getString("userId"),
                                    snapshot.getString("entrant_id")
                            );

                            String userName = getFirstNonEmptyString(
                                    snapshot.getString("userName"),
                                    snapshot.getString("entrant_name"),
                                    snapshot.getString("name")
                            );

                            Timestamp registeredAt = getFirstNonNullTimestamp(
                                    snapshot.getTimestamp("registeredAt"),
                                    snapshot.getTimestamp("registration_time"),
                                    snapshot.getTimestamp("registrationTime")
                            );

                            Timestamp invitedAt = getFirstNonNullTimestamp(
                                    snapshot.getTimestamp("invitedAt"),
                                    snapshot.getTimestamp("invited_time")
                            );

                            Timestamp acceptedAt = getFirstNonNullTimestamp(
                                    snapshot.getTimestamp("acceptedAt"),
                                    snapshot.getTimestamp("signed_up_time")
                            );

                            Timestamp cancelledAt = getFirstNonNullTimestamp(
                                    snapshot.getTimestamp("cancelledAt"),
                                    snapshot.getTimestamp("cancelled_time"),
                                    snapshot.getTimestamp("declinedAt")
                            );

                            GeoPoint location = snapshot.getGeoPoint("location");
                            String status = InvitationFlowUtil.normalizeEntrantStatus(
                                    snapshot.getString("status")
                            );

                            if (userId == null || userId.trim().isEmpty()) {
                                continue;
                            }

                            if (userName == null) {
                                userName = "";
                            }

                            Entrant entrant;

                            if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) {
                                entrant = new Entrant(userName, userId, registeredAt, location);
                                entrantWaitedListArrayList.add(entrant);
                            } else if (InvitationFlowUtil.STATUS_INVITED.equals(status)) {
                                entrant = new Entrant(invitedAt, userName, userId, registeredAt, location);
                                entrantInvitedArrayList.add(entrant);
                            } else if (InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                                entrant = new Entrant(userName, userId, invitedAt, registeredAt, location, acceptedAt);
                                entrantSignedUpArrayList.add(entrant);
                            } else if (InvitationFlowUtil.STATUS_CANCELLED.equals(status)
                                    || InvitationFlowUtil.STATUS_DECLINED.equals(status)) {
                                entrant = new Entrant(userName, location, userId, cancelledAt, invitedAt, registeredAt);
                                entrantCancelledArrayList.add(entrant);
                            }
                        }
                    }

                    waitedListAdapter.notifyDataSetChanged();
                    invitedAdapter.notifyDataSetChanged();
                    signedUpAdapter.notifyDataSetChanged();
                    cancelledAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void sampling(String size) {
        try {
            Integer.parseInt(size);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ERROR: sample size must be an integer", Toast.LENGTH_LONG).show();
            return;
        }

        int sampleSize = Integer.parseInt(size);

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> waitlistedDocs = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                        if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) {
                            waitlistedDocs.add(doc);
                        }
                    }

                    if (waitlistedDocs.isEmpty()) {
                        Toast.makeText(this, "no entrants in the waitlist", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Collections.shuffle(waitlistedDocs);

                    maxSampleSize = min(
                            maxCapacity - entrantSignedUpArrayList.size() - entrantInvitedArrayList.size(),
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

                    for (int i = 0; i < sampleSize; i++) {
                        DocumentSnapshot documentSnapshot = waitlistedDocs.get(i);
                        batch.update(
                                documentSnapshot.getReference(),
                                InvitationFlowUtil.buildInvitedEntrantUpdate()
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, "Sampling complete", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "sampling commit failed", e)
                            );
                });
    }

    @Override
    public void sendNotification(String content) {
        // Leave this for the next step after data structure cleanup.
    }

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
     * Insert markers of entrants' locations into the map.
     *
     * @param list entrants whose locations will be shown
     */
    private void insertMarkers(ArrayList<Entrant> list) {
        googleMap.clear();

        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasLocations = false;

        for (int i = 0; i < list.size(); i++) {
            Entrant entrant = list.get(i);
            GeoPoint geoLocation = entrant.getLocation();
            if (geoLocation == null) {
                continue;
            }

            final LatLng position = new LatLng(geoLocation.getLatitude(), geoLocation.getLongitude());
            final MarkerOptions options = new MarkerOptions().position(position);
            googleMap.addMarker(options);
            builder.include(position);
            hasLocations = true;
        }

        if (!hasLocations) {
            Toast.makeText(this, "No entrant locations available for this list.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLayout(LinearLayout target) {
        signedUpEntrantsListLayout.setVisibility(signedUpEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        cancelledEntrantsListLayout.setVisibility(cancelledEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        waitedListEntrantsListLayout.setVisibility(waitedListEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        invitedEntrantsListLayout.setVisibility(invitedEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        viewLocationLayout.setVisibility(viewLocationLayout == target ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap g) {
        googleMap = g;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (entrantsReg != null) entrantsReg.remove();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private String getFirstNonEmptyString(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private Timestamp getFirstNonNullTimestamp(Timestamp... values) {
        for (Timestamp value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}