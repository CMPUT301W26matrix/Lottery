package com.example.lottery;

import static java.lang.Long.min;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.EntrantEvent;
import com.example.lottery.model.NotificationItem;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.SessionUtil;
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
            btnSendNotification, btnNotifySelected, btnViewLocation, btnSampleWinners, btnSwitchInvited, btnSwitchNotSelected, btnExportCsv;

    private FirebaseFirestore db;

    private ArrayList<EntrantEvent> entrantSignedUpArrayList;
    private ArrayList<EntrantEvent> entrantInvitedArrayList;
    private ArrayList<EntrantEvent> entrantCancelledArrayList;
    private ArrayList<EntrantEvent> entrantWaitedListArrayList;
    private ArrayList<EntrantEvent> entrantNotSelectedArrayList;

    private SignedUpListAdapter signedUpAdapter;
    private CancelledListAdapter cancelledAdapter;
    private WaitedListedListAdapter waitedListAdapter;
    private InvitedListAdapter invitedAdapter;
    private NotSelectedListAdapter notSelectedAdapter;

    private LinearLayout invitedEntrantsListLayout, cancelledEntrantsListLayout,
            signedUpEntrantsListLayout, waitedListEntrantsListLayout, notSelectedEntrantsListLayout, viewLocationLayout;

    private RecyclerView invitedEventsView, signedUpEventsView, waitedListEventsView, cancelledEntrantsView, notSelectedEntrantsView;

    private GoogleMap googleMap;
    private MapView mapView;

    private String eventId;
    private String userId;
    private String eventTitle = "Event";
    private String organizerId;
    private long capacity, maxSampleSize;
    private boolean requireLocation = false;

    private ListenerRegistration entrantsReg;
    private String activeGroupStatus = InvitationFlowUtil.STATUS_WAITLISTED;

    private Set<String> selectedEntrantIds = new HashSet<>();
    private boolean isNotifySelectedMode = false;

    private ActivityResultLauncher<String> createCsvLauncher;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.entrants_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        //get event id from the jump from page to query entrants
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "event id missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = SessionUtil.resolveUserId(this);
        if (userId == null) {
            Toast.makeText(this, R.string.missing_user_info, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();

        createCsvLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
            if (uri != null) {
                exportAcceptedEntrantsToCsv(uri);
            }
        });

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        entrantSignedUpArrayList = new ArrayList<>();
        entrantCancelledArrayList = new ArrayList<>();
        entrantWaitedListArrayList = new ArrayList<>();
        entrantInvitedArrayList = new ArrayList<>();
        entrantNotSelectedArrayList = new ArrayList<>();

        signedUpAdapter = new SignedUpListAdapter(this, entrantSignedUpArrayList);
        cancelledAdapter = new CancelledListAdapter(this, entrantCancelledArrayList);
        waitedListAdapter = new WaitedListedListAdapter(this, entrantWaitedListArrayList);
        invitedAdapter = new InvitedListAdapter(this, entrantInvitedArrayList);
        notSelectedAdapter = new NotSelectedListAdapter(this, entrantNotSelectedArrayList);

        signedUpEventsView.setAdapter(signedUpAdapter);
        waitedListEventsView.setAdapter(waitedListAdapter);
        cancelledEntrantsView.setAdapter(cancelledAdapter);
        invitedEventsView.setAdapter(invitedAdapter);
        notSelectedEntrantsView.setAdapter(notSelectedAdapter);

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
        btnSwitchNotSelected.setOnClickListener(v -> {
            activeGroupStatus = InvitationFlowUtil.STATUS_NOT_SELECTED;
            showLayout(notSelectedEntrantsListLayout);
        });

        btnSendNotification.setOnClickListener(view -> {
            isNotifySelectedMode = false;
            NotificationFragment notificationFragment = new NotificationFragment();
            notificationFragment.show(getSupportFragmentManager(), "Send Notification");
        });

        btnNotifySelected.setOnClickListener(view -> {
            if (selectedEntrantIds.isEmpty()) {
                Toast.makeText(this, "No entrants selected", Toast.LENGTH_SHORT).show();
                return;
            }
            isNotifySelectedMode = true;
            NotificationFragment notificationFragment = new NotificationFragment();
            notificationFragment.show(getSupportFragmentManager(), "Send Notification to Selected");
        });

        btnSampleWinners.setOnClickListener(view -> {
            SampleFragment sampleFragment = new SampleFragment();
            sampleFragment.show(getSupportFragmentManager(), "Sample Winners");
        });

        /**
         * switch to invited component to display the entrants list that have invited by the organizer
         */

        /**
         * switch to view location component to display the entrants on the map
         */
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
                } else if (notSelectedEntrantsListLayout.getVisibility() == View.VISIBLE) {
                    insertMarkers(entrantNotSelectedArrayList);
                }
            }
            showLayout(viewLocationLayout);
        });

        btnExportCsv.setOnClickListener(v -> {
            if (entrantSignedUpArrayList.isEmpty()) {
                Toast.makeText(this, "No enrolled entrants to export", Toast.LENGTH_SHORT).show();
                return;
            }
            createCsvLauncher.launch("enrolled_entrants_" + eventId + ".csv");
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

                organizerId = documentSnapshot.getString("organizerId");
            }
        });
    }

    private void exportAcceptedEntrantsToCsv(Uri uri) {
        StringBuilder csvData = new StringBuilder();
        csvData.append(csvEscape("User ID")).append(",")
                .append(csvEscape("Name")).append(",")
                .append(csvEscape("Email")).append(",")
                .append(csvEscape("Status")).append("\n");

        for (EntrantEvent entrant : entrantSignedUpArrayList) {
            csvData.append(csvEscape(entrant.getUserId())).append(",")
                    .append(csvEscape(entrant.getUserName())).append(",")
                    .append(csvEscape(entrant.getEmail())).append(",")
                    .append(csvEscape(entrant.getStatus())).append("\n");
        }

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(csvData.toString().getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "CSV exported successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing CSV to URI", e);
            Toast.makeText(this, "Failed to export CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private String csvEscape(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public boolean isRequireLocation() {
        return requireLocation;
    }

    public void toggleSelection(String userId) {
        if (selectedEntrantIds.contains(userId)) {
            selectedEntrantIds.remove(userId);
        } else {
            selectedEntrantIds.add(userId);
        }
    }

    public boolean isSelected(String userId) {
        return selectedEntrantIds.contains(userId);
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
                    entrantNotSelectedArrayList.clear();

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

                            EntrantEvent entrant = new EntrantEvent();
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
                            } else if (InvitationFlowUtil.STATUS_NOT_SELECTED.equals(normalizedStatus)) {
                                entrantNotSelectedArrayList.add(entrant);
                            }
                        }
                    }

                    waitedListAdapter.notifyDataSetChanged();
                    invitedAdapter.notifyDataSetChanged();
                    signedUpAdapter.notifyDataSetChanged();
                    cancelledAdapter.notifyDataSetChanged();
                    notSelectedAdapter.notifyDataSetChanged();
                });
    }

    /**
     * implement US 02.05.02, randomly sample a specific number of signed up entrants from the attendees
     *
     * @param size sampling size which is the number of random entrants needed to marked as invited
     */
    @SuppressLint({"DefaultLocale", "NotifyDataSetChanged"})
    @Override
    public void sampling(String size) {
        try {
            //if not a number, prevent executing sampling
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

                    // Update selected entrants to INVITED
                    for (int i = 0; i < sampleSize; i++) {
                        DocumentSnapshot documentSnapshot = waitlistedDocs.get(i);
                        batch.update(
                                documentSnapshot.getReference(),
                                InvitationFlowUtil.buildInvitedEntrantUpdate()
                        );
                    }

                    // US 02.05.03: Update remaining waitlisted entrants to NOT_SELECTED
                    for (int i = sampleSize; i < waitlistedDocs.size(); i++) {
                        DocumentSnapshot documentSnapshot = waitlistedDocs.get(i);
                        batch.update(
                                documentSnapshot.getReference(),
                                InvitationFlowUtil.buildNotSelectedEntrantUpdate()
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
     * Sends a notification specifically to entrants on the waiting list (US 02.07.01)
     * or to selected entrants (US 02.07.02).
     *
     * @param content a sequence of words that will be sent to entrants
     */
    @Override
    public void sendNotification(String content) {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        // Authorization check: Only the organizer can send notifications
        if (currentUserId == null || (organizerId != null && !currentUserId.equals(organizerId))) {
            Toast.makeText(this, "Error: Only the organizer can send notifications", Toast.LENGTH_SHORT).show();
            return;
        }

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
        globalNotif.put("senderId", userId);
        globalNotif.put("senderRole", "organizer");
        globalNotif.put("createdAt", now);

        db.collection(FirestorePaths.NOTIFICATIONS).document(notificationId)
                .set(globalNotif)
                .addOnSuccessListener(aVoid -> fetchRecipientsAndSend(notificationId, content, globalNotif))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notification", e);
                    Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchRecipientsAndSend(String notificationId, String content, Map<String, Object> globalNotif) {
        if (isNotifySelectedMode) {
            sendToSelectedIds(notificationId, content, globalNotif);
        } else {
            sendToWholeWaitlist(notificationId, content, globalNotif);
        }
    }

    private void sendToWholeWaitlist(String notificationId, String content, Map<String, Object> globalNotif) {
        // Strict targeting: always send to WAITLISTED entrants only for US 02.07.01
        Query query = db.collection(FirestorePaths.eventWaitingList(eventId))
                    .whereEqualTo("status", InvitationFlowUtil.STATUS_WAITLISTED);

        query.get().addOnSuccessListener(querySnapshot -> {
            processRecipients(querySnapshot.getDocuments(), notificationId, content, globalNotif, "waiting list entrants");
        });
    }

    private void sendToSelectedIds(String notificationId, String content, Map<String, Object> globalNotif) {
        // For US 02.07.02, we only process the IDs currently in our selection set
        List<String> idsToNotify = new ArrayList<>(selectedEntrantIds);
        if (idsToNotify.isEmpty()) return;

        db.collection(FirestorePaths.eventWaitingList(eventId)).get().addOnSuccessListener(querySnapshot -> {
            List<DocumentSnapshot> filteredDocs = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (idsToNotify.contains(doc.getId())) {
                    filteredDocs.add(doc);
                }
            }
            processRecipients(filteredDocs, notificationId, content, globalNotif, "selected entrants");
        });
    }

    private void processRecipients(List<DocumentSnapshot> recipientDocs, String notificationId, String content, Map<String, Object> globalNotif, String label) {
        WriteBatch batch = db.batch();
        int totalRecipients = recipientDocs.size();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger sentCount = new AtomicInteger(0);

        if (totalRecipients == 0) {
            Toast.makeText(this, "No entrants found to notify", Toast.LENGTH_SHORT).show();
            return;
        }

        for (DocumentSnapshot doc : recipientDocs) {
            String recipientUid = doc.getId();

            db.collection(FirestorePaths.USERS).document(recipientUid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot userDoc = task.getResult();
                    boolean enabled = userDoc.contains("notificationsEnabled")
                            ? userDoc.getBoolean("notificationsEnabled") : true;

                    if (enabled) {
                        DocumentReference recipientRef = db.collection(FirestorePaths.notificationRecipients(notificationId))
                                .document(recipientUid);
                        Map<String, Object> recData = new HashMap<>();
                        recData.put("userId", recipientUid);
                        recData.put("isRead", false);
                        recData.put("createdAt", Timestamp.now());
                        batch.set(recipientRef, recData);

                        DocumentReference inboxRef = db.collection(FirestorePaths.userInbox(recipientUid))
                                .document(notificationId);

                        NotificationItem inboxItem = new NotificationItem(
                                notificationId,
                                (String) globalNotif.get("title"),
                                content,
                                (String) globalNotif.get("type"),
                                eventId,
                                eventTitle,
                                (String) globalNotif.get("senderId"),
                                "organizer",
                                false,
                                (Timestamp) globalNotif.get("createdAt")
                        );
                        batch.set(inboxRef, inboxItem);
                        sentCount.incrementAndGet();
                    }
                }

                if (processedCount.incrementAndGet() == totalRecipients) {
                    if (sentCount.get() > 0) {
                        batch.commit()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this,
                                                "Notification sent to " + sentCount.get() + " " + label,
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to send notifications", e);
                                    Toast.makeText(this,
                                            "Failed to send notifications",
                                            Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this,
                                "All target entrants have notifications disabled",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void initializeViews() {
        btnSwitchWaitedList = findViewById(R.id.entrants_list_waited_list_btn);
        btnSwitchCancelled = findViewById(R.id.entrants_list_cancelled_btn);
        btnSwitchSignedUp = findViewById(R.id.entrants_list_signed_up_btn);
        btnViewLocation = findViewById(R.id.entrants_list_view_location_btn);
        btnSampleWinners = findViewById(R.id.entrants_list_sample_btn);
        btnSwitchInvited = findViewById(R.id.entrants_list_invited_btn);
        btnSwitchNotSelected = findViewById(R.id.entrants_list_not_selected_btn);
        btnSendNotification = findViewById(R.id.entrants_list_send_notification_btn);
        btnNotifySelected = findViewById(R.id.entrants_list_notify_selected_btn);
        btnExportCsv = findViewById(R.id.entrants_list_export_csv_btn);

        // Update button text for clarity
        if (btnSendNotification != null) {
            btnSendNotification.setText("Notify All");
        }

        signedUpEventsView = findViewById(R.id.signed_up_events_view);
        invitedEventsView = findViewById(R.id.invited_events_view);
        waitedListEventsView = findViewById(R.id.waited_list_events_view);
        cancelledEntrantsView = findViewById(R.id.cancelled_entrants_view);
        notSelectedEntrantsView = findViewById(R.id.not_selected_entrants_view);

        signedUpEventsView.setLayoutManager(new LinearLayoutManager(this));
        waitedListEventsView.setLayoutManager(new LinearLayoutManager(this));
        cancelledEntrantsView.setLayoutManager(new LinearLayoutManager(this));
        invitedEventsView.setLayoutManager(new LinearLayoutManager(this));
        notSelectedEntrantsView.setLayoutManager(new LinearLayoutManager(this));

        cancelledEntrantsListLayout = findViewById(R.id.cancelled_entrants_list_layout);
        signedUpEntrantsListLayout = findViewById(R.id.signed_up_entrants_list_layout);
        waitedListEntrantsListLayout = findViewById(R.id.waited_list_entrants_list_layout);
        invitedEntrantsListLayout = findViewById(R.id.invited_entrants_list_layout);
        notSelectedEntrantsListLayout = findViewById(R.id.not_selected_entrants_list_layout);
        viewLocationLayout = findViewById(R.id.view_location_layout);
        mapView = findViewById(R.id.mapView);
    }

    // Source - https://stackoverflow.com/a/30054797
    // Posted by Ankit Khare, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-11, License - CC BY-SA 3.0

    /**
     * nsert makers of entrants' location into the map
     *
     * @param list entrant we want to show their location on the map
     */
    private void insertMarkers(ArrayList<EntrantEvent> list) {
        googleMap.clear();

        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasLocations = false;

        for (int i = 0; i < list.size(); i++) {
            EntrantEvent entrant = list.get(i);
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

    @SuppressLint("NotifyDataSetChanged")
    private void showLayout(LinearLayout target) {
        signedUpEntrantsListLayout.setVisibility(signedUpEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        cancelledEntrantsListLayout.setVisibility(cancelledEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        waitedListEntrantsListLayout.setVisibility(waitedListEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        invitedEntrantsListLayout.setVisibility(invitedEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        notSelectedEntrantsListLayout.setVisibility(notSelectedEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        viewLocationLayout.setVisibility(viewLocationLayout == target ? View.VISIBLE : View.GONE);

        // US 02.07.01 & US 02.07.02: Notification buttons are only for the Waiting List
        boolean isWaitingList = target == waitedListEntrantsListLayout;
        if (btnSendNotification != null) {
            btnSendNotification.setVisibility(isWaitingList ? View.VISIBLE : View.GONE);
        }
        if (btnNotifySelected != null) {
            btnNotifySelected.setVisibility(isWaitingList ? View.VISIBLE : View.GONE);
        }

        // Clear selection when switching tabs
        selectedEntrantIds.clear();
        if (isWaitingList && waitedListAdapter != null) {
            waitedListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap g) {
        googleMap = g;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    /**
     * initialize mapview
     */
    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
    }

    /**
     * inform mapview to free resources when stop using
     */
    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    /**
     * interact with users
     */
    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    /**
     * stop rendering map
     */
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * remove map allocated resources
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (entrantsReg != null) entrantsReg.remove();
        mapView.onDestroy();
    }

    /**
     * control map memory use when system is busy
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
