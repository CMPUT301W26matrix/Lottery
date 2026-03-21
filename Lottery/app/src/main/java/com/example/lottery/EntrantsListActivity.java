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
public class EntrantsListActivity extends AppCompatActivity implements NotificationFragment.NotificationListener, SampleFragment.SamplingListener, OnMapReadyCallback {
    static final String COL_WAITED_LISTED = "waited_listed_collections";
    static final String COL_INVITED = "invited_collections";
    static final String COL_CANCELLED = "cancelled_collections";
    static final String COL_SIGNED_UP = "signed_up_collections";
    private static final String TAG = "EntrantsListActivity";
    private Button btnSwitchSignedUp, btnSwitchCancelled, btnSwitchWaitedList, btnSendNotification, btnViewLocation, btnSampleWinners, btnSwitchInvited;
    private FirebaseFirestore db;
    private ArrayList<Entrant> entrantSignedUpArrayList;
    private ArrayList<Entrant> entrantInvitedArrayList;
    private ArrayList<Entrant> entrantCancelledArrayList;
    private ArrayList<Entrant> entrantWaitedListArrayList;
    private SignedUpListAdapter signedUpAdapter;
    private CancelledListAdapter cancelledAdapter;
    private WaitedListedListAdapter waitedListAdapter;
    private InvitedListAdapter invitedAdapter;
    private LinearLayout invitedEntrantsListLayout, cancelledEntrantsListLayout, signedUpEntrantsListLayout, waitedListEntrantsListLayout, viewLocationLayout;
    private RecyclerView invitedEventsView, signedUpEventsView, waitedListEventsView, cancelledEntrantsView;
    private GoogleMap googleMap;
    private MapView mapView;
    private String eventId;
    private long maxCapacity, maxSampleSize;
    private ListenerRegistration waitedListReg, invitedReg, cancelledReg, signedUpReg;

    /**
     * Initializes the activity, sets up Firebase, bind views,
     * and click button listeners for QR code generation and event creation.
     *
     * @param savedInstanceState If the activity is initialized again after being shut down,
     *                           this contains the most recent data, in other case it is null.
     */
    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrants_list);
        // Initialize Firestore
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Service Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //get event id from the jump from page to query entrants
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

        /**
         * switch to signed up component to display the entrants list that have signed up
         */
        btnSwitchSignedUp.setOnClickListener(v -> showLayout(signedUpEntrantsListLayout));
        btnSwitchCancelled.setOnClickListener(v -> showLayout(cancelledEntrantsListLayout));
        btnSwitchWaitedList.setOnClickListener(v -> showLayout(waitedListEntrantsListLayout));
        btnSwitchInvited.setOnClickListener(view -> showLayout(invitedEntrantsListLayout));

        /**
         *display send notification fragment
         */
        btnSendNotification.setOnClickListener(view -> {
            NotificationFragment notificationFragment = new NotificationFragment();
            notificationFragment.show(getSupportFragmentManager(), "Send Notification");
        });

        /**
         * display sample fragment
         */
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
                }
            }
            showLayout(viewLocationLayout);
        });

        /**
         * set waited_listed arraylist
         */
        waitedListReg = db.collection("events").document(eventId).collection(COL_WAITED_LISTED).addSnapshotListener((querySnapshot, firebaseFirestoreException) -> {
            if (firebaseFirestoreException != null) {
                Log.e(TAG, "waited_listed listener error", firebaseFirestoreException);
                return;
            }
            entrantWaitedListArrayList.clear();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot snapshot : querySnapshot) {
                    String entrantId = snapshot.getString("entrant_id");
                    Timestamp registrationTime = snapshot.getTimestamp("registration_time");
                    GeoPoint location = snapshot.getGeoPoint("location");
                    String name = snapshot.getString("entrant_name");
                    Entrant newEntrant = new Entrant(name, entrantId, registrationTime, location);
                    entrantWaitedListArrayList.add(newEntrant);
                }
                waitedListAdapter.notifyDataSetChanged();
            }
        });

        /**
         * set invited arraylist
         */
        invitedReg = db.collection("events").document(eventId).collection(COL_INVITED).addSnapshotListener((querySnapshot, firebaseFirestoreException) -> {
            if (firebaseFirestoreException != null) {
                Log.e(TAG, "invited listener error", firebaseFirestoreException);
                return;
            }
            entrantInvitedArrayList.clear();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot snapshot : querySnapshot) {
                    String entrantId = snapshot.getString("entrant_id");
                    Timestamp registrationTime = snapshot.getTimestamp("registration_time");
                    GeoPoint location = snapshot.getGeoPoint("location");
                    Timestamp invitedTime = snapshot.getTimestamp("invited_time");
                    String name = snapshot.getString("entrant_name");
                    Entrant newEntrant = new Entrant(invitedTime, name, entrantId, registrationTime, location);
                    entrantInvitedArrayList.add(newEntrant);
                }
                invitedAdapter.notifyDataSetChanged();
            }
        });

        /**
         * set cancelled arraylist
         */
        cancelledReg = db.collection("events").document(eventId).collection(COL_CANCELLED).addSnapshotListener((querySnapshot, firebaseFirestoreException) -> {
            if (firebaseFirestoreException != null) {
                Log.e(TAG, "cancelled listener error", firebaseFirestoreException);
                return;
            }
            entrantCancelledArrayList.clear();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot snapshot : querySnapshot) {
                    String entrantId = snapshot.getString("entrant_id");
                    Timestamp registrationTime = snapshot.getTimestamp("registration_time");
                    GeoPoint location = snapshot.getGeoPoint("location");
                    Timestamp cancelledTime = snapshot.getTimestamp("cancelled_time");
                    Timestamp invitedTime = snapshot.getTimestamp("invited_time");
                    String name = snapshot.getString("entrant_name");
                    Entrant newEntrant = new Entrant(name, location, entrantId, cancelledTime, invitedTime, registrationTime);
                    entrantCancelledArrayList.add(newEntrant);
                }
                cancelledAdapter.notifyDataSetChanged();
            }
        });

        /**
         * set signed up arraylist
         */
        signedUpReg = db.collection("events").document(eventId).collection(COL_SIGNED_UP).addSnapshotListener((querySnapshot, firebaseFirestoreException) -> {
            if (firebaseFirestoreException != null) {
                Log.e(TAG, "signed_up listener error", firebaseFirestoreException);
                return;
            }
            entrantSignedUpArrayList.clear();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot snapshot : querySnapshot) {
                    String entrantId = snapshot.getString("entrant_id");
                    Timestamp registrationTime = snapshot.getTimestamp("registration_time");
                    GeoPoint location = snapshot.getGeoPoint("location");
                    Timestamp signedUpTime = snapshot.getTimestamp("signed_up_time");
                    Timestamp invitedTime = snapshot.getTimestamp("invited_time");
                    String name = snapshot.getString("entrant_name");
                    Entrant newEntrant = new Entrant(name, entrantId, invitedTime, registrationTime, location, signedUpTime);
                    entrantSignedUpArrayList.add(newEntrant);
                }
                signedUpAdapter.notifyDataSetChanged();
            }
        });

        //get the max capacity of the event
        db.collection("events").document(eventId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long cap = documentSnapshot.getLong("maxCapacity");
                maxCapacity = cap != null ? cap : 0L;
            }
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
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.collection(COL_WAITED_LISTED).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                Toast.makeText(this, "no entrants in the waited listed collections", Toast.LENGTH_LONG).show();
                return;
            }
            List<DocumentSnapshot> data = queryDocumentSnapshots.getDocuments();
            Collections.shuffle(data);
            maxSampleSize = min(maxCapacity - entrantSignedUpArrayList.size() - entrantInvitedArrayList.size(), entrantWaitedListArrayList.size());
            Log.d(TAG, "sampling: shuffle succeeds");
            if (maxSampleSize < sampleSize || sampleSize <= 0) {
                String errorMessage = String.format("ERROR: check failed: 0 < sample size < %d", maxSampleSize);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                return;
            }

            WriteBatch batch = db.batch();
            for (int i = 0; i < sampleSize; i++) {
                DocumentSnapshot documentSnapshot = data.get(i);
                Map<String, Object> newData = new HashMap<>();
                newData.put("entrant_id", documentSnapshot.getString("entrant_id"));
                newData.put("registration_time", documentSnapshot.getTimestamp("registration_time"));
                newData.put("invited_time", Timestamp.now());
                newData.put("entrant_name", documentSnapshot.getString("entrant_name"));
                GeoPoint loc = documentSnapshot.getGeoPoint("location");
                if (loc != null) {
                    newData.put("location", loc);
                }
                DocumentReference newRef = eventRef.collection(COL_INVITED).document();
                batch.set(newRef, newData);

                //delete the document in the waited_listed since it's belong to invited collections now
                batch.delete(documentSnapshot.getReference());
            }
            batch.commit().addOnFailureListener(e -> Log.d("sampling", "commit failed"));
        });
    }

    /**
     * send notification to specified entrants(depend on which list the organizer is browsing)
     *
     * @param content a sequence of words that will be sent to entrants
     */
    @Override
    public void sendNotification(String content) {
    }

    /**
     * Initialize view for the create event activity.
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

    // Source - https://stackoverflow.com/a/30054797
    // Posted by Ankit Khare, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-11, License - CC BY-SA 3.0

    /**
     * nsert makers of entrants' location into the map
     *
     * @param list entrant we want to show their location on the map
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


    // Source - https://stackoverflow.com/a/19806967
    // Posted by Naveed Ali, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-11, License - CC BY-SA 4.0

    /**
     * initialize google map component
     *
     * @param g instance of GoogleMap
     */
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
        if (waitedListReg != null) waitedListReg.remove();
        if (invitedReg != null) invitedReg.remove();
        if (cancelledReg != null) cancelledReg.remove();
        if (signedUpReg != null) signedUpReg.remove();
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
