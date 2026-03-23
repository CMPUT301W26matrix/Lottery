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

import com.example.lottery.model.Entrant;
import com.example.lottery.model.NotificationItem;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Activity to display entrants of different status of a event by list and map and able to sample they
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
    private String eventTitle = "Event";
    private long capacity, maxSampleSize;
    private boolean requireLocation = false;

    private ListenerRegistration entrantsReg;
    private String activeGroupStatus = InvitationFlowUtil.STATUS_WAITLISTED;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrants_list);

        db = FirebaseFirestore.getInstance();

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

        btnSwitchSignedUp.setOnClickListener(v -> {
            activeGroupStatus = InvitationFlowUtil.STATUS_ACCEPTED;
            showLayout(signedUpEntrantsListLayout);
        });
        btnSwitchCancelled.setOnClickListener(v -> {
            activeGroupStatus = InvitationFlowUtil.STATUS_CANCELLED;
            showLayout(cancelledEntrantsListLayout);
        });
        btnSwitchWaitedList.setOnClickListener(v -> {
            activeGroupStatus = InvitationFlowUtil.STATUS_WAITLISTED;
            showLayout(waitedListEntrantsListLayout);
        });
        btnSwitchInvited.setOnClickListener(v -> {
            activeGroupStatus = InvitationFlowUtil.STATUS_INVITED;
            showLayout(invitedEntrantsListLayout);
        });

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

        db.collection(FirestorePaths.EVENTS).document(eventId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Unified: use capacity instead of maxCapacity
                Long cap = documentSnapshot.getLong("capacity");
                capacity = cap != null ? cap : 0L;
                
                String title = documentSnapshot.getString("title");
                if (title != null) {
                    eventTitle = title;
                }

                Boolean reqLoc = documentSnapshot.getBoolean("requireLocation");
                requireLocation = reqLoc != null && reqLoc;
            }
        });
    }

    public boolean isRequireLocation() {
        return requireLocation;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void listenToEntrants() {
        entrantsReg = db.collection(FirestorePaths.eventWaitingList(eventId))
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
                            // Unified: remove all fallback getters
                            String userId = snapshot.getString("userId");
                            String userName = snapshot.getString("userName");
                            String email = snapshot.getString("email");
                            Timestamp registeredAt = snapshot.getTimestamp("registeredAt");
                            Timestamp invitedAt = snapshot.getTimestamp("invitedAt");
                            Timestamp acceptedAt = snapshot.getTimestamp("acceptedAt");
                            Timestamp cancelledAt = snapshot.getTimestamp("cancelledAt");
                            GeoPoint location = snapshot.getGeoPoint("location");
                            String status = snapshot.getString("status");

                            if (userId == null || userId.trim().isEmpty()) {
                                continue;
                            }

                            Entrant entrant = new Entrant();
                            entrant.setUserId(userId);
                            entrant.setUserName(userName != null ? userName : "");
                            entrant.setEmail(email);
                            entrant.setStatus(status);
                            entrant.setRegisteredAt(registeredAt);
                            entrant.setInvitedAt(invitedAt);
                            entrant.setAcceptedAt(acceptedAt);
                            entrant.setCancelledAt(cancelledAt);
                            entrant.setLocation(location);

                            String normalizedStatus = InvitationFlowUtil.normalizeEntrantStatus(status);

                            if (InvitationFlowUtil.STATUS_WAITLISTED.equals(normalizedStatus)) {
                                entrantWaitedListArrayList.add(entrant);
                            } else if (InvitationFlowUtil.STATUS_INVITED.equals(normalizedStatus)) {
                                entrantInvitedArrayList.add(entrant);
                            } else if (InvitationFlowUtil.STATUS_ACCEPTED.equals(normalizedStatus)) {
                                entrantSignedUpArrayList.add(entrant);
                            } else if (InvitationFlowUtil.STATUS_CANCELLED.equals(normalizedStatus)) {
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

        db.collection(FirestorePaths.eventWaitingList(eventId))
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
                            capacity - entrantSignedUpArrayList.size() - entrantInvitedArrayList.size(),
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

    /**
     * Sends a notification to the currently visible group of entrants.
     */
    @Override
    public void sendNotification(String content) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        String notificationId = UUID.randomUUID().toString();
        Timestamp now = Timestamp.now();

        // 1. Create global notification log
        Map<String, Object> globalNotif = new HashMap<>();
        globalNotif.put("notificationId", notificationId);
        globalNotif.put("title", "Update for " + eventTitle);
        globalNotif.put("message", content);
        globalNotif.put("type", "general");
        globalNotif.put("eventId", eventId);
        globalNotif.put("eventTitle", eventTitle);
        globalNotif.put("senderId", currentUserId);
        globalNotif.put("senderRole", "organizer");
        globalNotif.put("createdAt", now);

        db.collection(FirestorePaths.NOTIFICATIONS).document(notificationId)
                .set(globalNotif)
                .addOnSuccessListener(aVoid -> fetchRecipientsAndSend(notificationId, content, globalNotif));
    }

    private void fetchRecipientsAndSend(String notificationId, String content, Map<String, Object> globalNotif) {
        Query query = db.collection(FirestorePaths.eventWaitingList(eventId))
                    .whereEqualTo("status", activeGroupStatus);

        query.get().addOnSuccessListener(querySnapshot -> {
            WriteBatch batch = db.batch();
            int count = querySnapshot.size();

            for (QueryDocumentSnapshot doc : querySnapshot) {
                String recipientUid = doc.getId();

                // 2. Create recipient entry in global log
                DocumentReference recipientRef = db.collection(FirestorePaths.notificationRecipients(notificationId))
                        .document(recipientUid);
                Map<String, Object> recData = new HashMap<>();
                recData.put("userId", recipientUid);
                recData.put("isRead", false);
                recData.put("createdAt", Timestamp.now());
                batch.set(recipientRef, recData);

                // 3. Create inbox entry for the user
                DocumentReference inboxRef = db.collection(FirestorePaths.userInbox(recipientUid))
                        .document(notificationId);
                
                NotificationItem inboxItem = new NotificationItem(
                        notificationId,
                        (String) globalNotif.get("title"),
                        content,
                        "general",
                        eventId,
                        eventTitle,
                        (String) globalNotif.get("senderId"),
                        "organizer",
                        false,
                        (Timestamp) globalNotif.get("createdAt")
                );
                batch.set(inboxRef, inboxItem);
            }

            if (count > 0) {
                batch.commit().addOnSuccessListener(unused -> 
                    Toast.makeText(this, "Notification sent to " + count + " entrants", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "No entrants found in this group", Toast.LENGTH_SHORT).show();
            }
        });
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
}
