package com.example.lottery.organizer;

import static java.lang.Long.min;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.entrant.EntrantMapActivity;
import com.example.lottery.R;
import com.example.lottery.adapter.CancelledListAdapter;
import com.example.lottery.adapter.InvitedListAdapter;
import com.example.lottery.adapter.NotSelectedListAdapter;
import com.example.lottery.adapter.SignedUpListAdapter;
import com.example.lottery.adapter.WaitedListedListAdapter;
import com.example.lottery.fragment.SampleFragment;
import com.example.lottery.model.EntrantEvent;
import com.example.lottery.model.NotificationItem;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.SessionUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity to display entrants of different status of a event by list and able to sample they
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch the 4 different status entrant collections from Firestore</li>
 *   <li>Render the 4 different status entrant list and implement view location and sample winners functionality</li>
 *   <li>implement US 02.05.02 Be able to sample number of attendees to register for the event</li>
 *   <li>implement US 02.06.01 Be able to view all chosen entrants</li>
 *   <li>implement US 02.06.02 Be able to see a list of all the cancelled entrants</li>
 * </ul>
 * </p>
 */
public class EntrantsListActivity extends AppCompatActivity implements
        SampleFragment.SamplingListener {

    private static final String TAG = "EntrantsListActivity";
    private Button btnSwitchSignedUp, btnSwitchCancelled, btnSwitchWaitedList,
            btnViewLocation, btnSampleWinners, btnSwitchInvited, btnSwitchNotSelected, btnExportCsv;
    private ImageButton btnBack;
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
            signedUpEntrantsListLayout, waitedListEntrantsListLayout, notSelectedEntrantsListLayout;
    private RecyclerView invitedEventsView, signedUpEventsView, waitedListEventsView, cancelledEntrantsView, notSelectedEntrantsView;
    private TextView waitedListEmptyText, invitedEmptyText, signedUpEmptyText, cancelledEmptyText, notSelectedEmptyText;
    private String eventId;
    private String userId;
    private String eventTitle = "Event";
    private long capacity;
    private boolean requireLocation = false;
    private ListenerRegistration entrantsReg;
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
            showLayout(signedUpEntrantsListLayout);
        });
        btnSwitchCancelled.setOnClickListener(v -> {
            showLayout(cancelledEntrantsListLayout);
        });
        btnSwitchWaitedList.setOnClickListener(v -> {
            showLayout(waitedListEntrantsListLayout);
        });
        btnSwitchInvited.setOnClickListener(v -> {
            showLayout(invitedEntrantsListLayout);
        });
        btnSwitchNotSelected.setOnClickListener(v -> {
            showLayout(notSelectedEntrantsListLayout);
        });

        btnSampleWinners.setOnClickListener(view -> {
            long defaultSize = Math.max(0, Math.min(getRemainingSlots(), entrantWaitedListArrayList.size()));
            SampleFragment sampleFragment = SampleFragment.newInstance(defaultSize);
            sampleFragment.show(getSupportFragmentManager(), "Sample Winners");
        });

        /**
         * navigate to the map screen to view all saved entrant locations for this event (US 02.02.02)
         */
        btnViewLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMapActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        btnExportCsv.setOnClickListener(v -> {
            if (entrantSignedUpArrayList.isEmpty()) {
                Toast.makeText(this, "No enrolled entrants to export", Toast.LENGTH_SHORT).show();
                return;
            }
            createCsvLauncher.launch("enrolled_entrants_" + eventId + ".csv");
        });

        btnBack.setOnClickListener(v -> finish());

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

    private long getRemainingSlots() {
        return capacity - entrantSignedUpArrayList.size() - entrantInvitedArrayList.size();
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

                    if (querySnapshot == null) return;

                    entrantWaitedListArrayList.clear();
                    entrantInvitedArrayList.clear();
                    entrantCancelledArrayList.clear();
                    entrantSignedUpArrayList.clear();
                    entrantNotSelectedArrayList.clear();

                    for (QueryDocumentSnapshot snapshot : querySnapshot) {
                        EntrantEvent entrant = snapshot.toObject(EntrantEvent.class);
                        if (entrant.getUserId() == null) {
                            entrant.setUserId(snapshot.getId());
                        }

                        String normalizedStatus = InvitationFlowUtil.normalizeEntrantStatus(entrant.getStatus());

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

                    notifyAllAdapters();
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyAllAdapters() {
        waitedListAdapter.notifyDataSetChanged();
        invitedAdapter.notifyDataSetChanged();
        signedUpAdapter.notifyDataSetChanged();
        cancelledAdapter.notifyDataSetChanged();
        notSelectedAdapter.notifyDataSetChanged();

        updateEmptyState(entrantWaitedListArrayList, waitedListEventsView, waitedListEmptyText);
        updateEmptyState(entrantInvitedArrayList, invitedEventsView, invitedEmptyText);
        updateEmptyState(entrantSignedUpArrayList, signedUpEventsView, signedUpEmptyText);
        updateEmptyState(entrantCancelledArrayList, cancelledEntrantsView, cancelledEmptyText);
        updateEmptyState(entrantNotSelectedArrayList, notSelectedEntrantsView, notSelectedEmptyText);
    }

    private void updateEmptyState(ArrayList<?> list, RecyclerView recyclerView, TextView emptyText) {
        if (list.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
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

                    long maxSampleSize = Math.max(0, min(
                            getRemainingSlots(),
                            waitlistedDocs.size()
                    ));

                    if (maxSampleSize < sampleSize || sampleSize <= 0) {
                        String errorMessage = String.format(
                                "ERROR: check failed: 0 < sample size <= %d",
                                maxSampleSize
                        );
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    List<String> invitedIds = new ArrayList<>();
                    List<String> notSelectedIds = new ArrayList<>();

                    // Update selected entrants to INVITED
                    for (int i = 0; i < sampleSize; i++) {
                        DocumentSnapshot documentSnapshot = waitlistedDocs.get(i);
                        invitedIds.add(documentSnapshot.getId());
                        batch.update(
                                documentSnapshot.getReference(),
                                InvitationFlowUtil.buildInvitedEntrantUpdate()
                        );
                    }

                    // US 02.05.03: Update remaining waitlisted entrants to NOT_SELECTED
                    for (int i = sampleSize; i < waitlistedDocs.size(); i++) {
                        DocumentSnapshot documentSnapshot = waitlistedDocs.get(i);
                        notSelectedIds.add(documentSnapshot.getId());
                        batch.update(
                                documentSnapshot.getReference(),
                                InvitationFlowUtil.buildNotSelectedEntrantUpdate()
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Sampling complete", Toast.LENGTH_SHORT).show();
                                autoNotifyDrawResults(invitedIds, true);
                                autoNotifyDrawResults(notSelectedIds, false);
                            })
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "sampling commit failed", e)
                            );
                });
    }

    /**
     * Sends automatic system notifications to entrants after a lottery draw.
     *
     * @param userIds  List of user IDs to notify.
     * @param isWinner Whether these users were selected as winners.
     */
    private void autoNotifyDrawResults(List<String> userIds, boolean isWinner) {
        if (userIds.isEmpty()) return;

        String notificationId = UUID.randomUUID().toString();
        String title = isWinner ? "You've been invited!" : "Lottery Update";
        String content = isWinner ?
                "Congratulations! You have been selected for " + eventTitle + ". Please check event details to accept or decline." :
                "We're sorry, you were not selected for " + eventTitle + " this time.";
        String type = isWinner ? "event_invitation" : "draw_result";

        Map<String, Object> globalNotif = new HashMap<>();
        globalNotif.put("notificationId", notificationId);
        globalNotif.put("title", title);
        globalNotif.put("message", content);
        globalNotif.put("type", type);
        globalNotif.put("eventId", eventId);
        globalNotif.put("eventTitle", eventTitle);
        globalNotif.put("senderId", userId);
        globalNotif.put("senderRole", "organizer");
        globalNotif.put("createdAt", Timestamp.now());

        db.collection(FirestorePaths.NOTIFICATIONS).document(notificationId)
                .set(globalNotif)
                .addOnSuccessListener(aVoid -> {
                    processRecipientsByIds(userIds, notificationId, content, globalNotif);
                });
    }

    /**
     * Helper to process recipients using a pre-defined list of IDs.
     */
    private void processRecipientsByIds(List<String> recipientIds, String notificationId, String content, Map<String, Object> globalNotif) {
        WriteBatch batch = db.batch();
        int total = recipientIds.size();
        AtomicInteger processedCount = new AtomicInteger(0);

        for (String recipientUid : recipientIds) {
            db.collection(FirestorePaths.USERS).document(recipientUid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot userDoc = task.getResult();
                    Boolean notifPref = userDoc.getBoolean("notificationsEnabled");
                    boolean enabled = notifPref == null || notifPref;

                    if (enabled) {
                        Map<String, Object> recData = new HashMap<>();
                        recData.put("userId", recipientUid);
                        recData.put("isRead", false);
                        recData.put("createdAt", Timestamp.now());
                        batch.set(db.collection(FirestorePaths.notificationRecipients(notificationId)).document(recipientUid), recData);

                        NotificationItem inboxItem = new NotificationItem(
                                notificationId, (String) globalNotif.get("title"), content,
                                (String) globalNotif.get("type"), eventId, eventTitle,
                                (String) globalNotif.get("senderId"), "organizer", false, (Timestamp) globalNotif.get("createdAt")
                        );
                        batch.set(db.collection(FirestorePaths.userInbox(recipientUid)).document(notificationId), inboxItem);
                    }
                }

                if (processedCount.incrementAndGet() == total) {
                    batch.commit().addOnFailureListener(e -> Log.e(TAG, "Batch notification failed", e));
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
        btnExportCsv = findViewById(R.id.entrants_list_export_csv_btn);
        btnBack = findViewById(R.id.btnBack);

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

        waitedListEmptyText = findViewById(R.id.waited_list_empty_text);
        invitedEmptyText = findViewById(R.id.invited_empty_text);
        signedUpEmptyText = findViewById(R.id.signed_up_empty_text);
        cancelledEmptyText = findViewById(R.id.cancelled_empty_text);
        notSelectedEmptyText = findViewById(R.id.not_selected_empty_text);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showLayout(LinearLayout target) {
        signedUpEntrantsListLayout.setVisibility(signedUpEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        cancelledEntrantsListLayout.setVisibility(cancelledEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        waitedListEntrantsListLayout.setVisibility(waitedListEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        invitedEntrantsListLayout.setVisibility(invitedEntrantsListLayout == target ? View.VISIBLE : View.GONE);
        notSelectedEntrantsListLayout.setVisibility(notSelectedEntrantsListLayout == target ? View.VISIBLE : View.GONE);

        // Export CSV only available for Accepted (Signed Up) entrants
        if (btnExportCsv != null) {
            btnExportCsv.setVisibility(target == signedUpEntrantsListLayout ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * remove map allocated resources
     */
    @Override
    public void onDestroy() {
        if (entrantsReg != null) entrantsReg.remove();
        super.onDestroy();
    }
}
