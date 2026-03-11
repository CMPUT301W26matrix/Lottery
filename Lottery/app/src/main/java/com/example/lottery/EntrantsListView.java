package com.example.lottery;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.util.EventValidationUtils;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class EntrantsListView extends AppCompatActivity implements NotificationFragment.NotificationListener, SampleFragment.SamplingListener, OnMapReadyCallback{
    private static final String TAG = "CreateEventActivity";
    private final int FINE_PERMISSION_CODE = 1;
    private Button btnSwitchSignedUp, btnSwitchCancelled, btnSwitchWaitedList, btnSendNotification, btnViewLocation, btnSampleWinners;
    private FirebaseFirestore db;
    private ArrayList<Entrant> entrantSignedUpArrayList;
    private ArrayList<Entrant> entrantCancelledArrayList;
    private ArrayList<Entrant> entrantWaitedListArrayList;
    private SignedUpListAdapter SignedUpListAdapter;
    private CancelledListAdapter CancelledListAdapter;
    private WaitedListedListAdapter WaitedListedListAdapter;
    private LinearLayout cancelledEntrantsListLayout, signedUpEntrantsListLayout ,waitedListEntrantsListLayout, viewLocationLayout;
    private RecyclerView signedUpEventsView, waitedListEventsView, cancelledEntrantsView;
    private CollectionReference entrantsRef;
    private GoogleMap googleMap;
    private MapView mapView;
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
        initializeViews();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        entrantSignedUpArrayList = new ArrayList<>();
        entrantCancelledArrayList = new ArrayList<>();
        entrantWaitedListArrayList = new ArrayList<>();
        SignedUpListAdapter = new SignedUpListAdapter(this, entrantSignedUpArrayList);
        CancelledListAdapter = new CancelledListAdapter(this, entrantCancelledArrayList);
        WaitedListedListAdapter = new WaitedListedListAdapter(this, entrantWaitedListArrayList);
        signedUpEventsView.setAdapter(SignedUpListAdapter);
        waitedListEventsView.setAdapter(WaitedListedListAdapter);
        cancelledEntrantsView.setAdapter(CancelledListAdapter);
        entrantsRef = db.collection("entrants");


        // switch to signed up component to display the entrants list that have signed up
        btnSwitchSignedUp.setOnClickListener(v -> {
            // Source - https://stackoverflow.com/a/12125545
            // Posted by nandeesh
            // Retrieved 2026-03-10, License - CC BY-SA 3.0
            signedUpEntrantsListLayout.setVisibility(View.VISIBLE);
            cancelledEntrantsListLayout.setVisibility(View.GONE);
            waitedListEntrantsListLayout.setVisibility(View.GONE);
            viewLocationLayout.setVisibility(View.GONE);
        });


        // switch to signed up component to display the entrants list that have signed up
        btnSwitchCancelled.setOnClickListener(v -> {
            cancelledEntrantsListLayout.setVisibility(View.VISIBLE);
            signedUpEntrantsListLayout.setVisibility(View.GONE);
            waitedListEntrantsListLayout.setVisibility(View.GONE);
            viewLocationLayout.setVisibility(View.GONE);
        });


        // switch to signed up component to display the entrants list that have signed up
        btnSwitchWaitedList.setOnClickListener(v -> {
            waitedListEntrantsListLayout.setVisibility(View.VISIBLE);
            cancelledEntrantsListLayout.setVisibility(View.GONE);
            signedUpEntrantsListLayout.setVisibility(View.GONE);
            viewLocationLayout.setVisibility(View.GONE);
        });


        //display send notification fragment
        btnSendNotification.setOnClickListener(view->{
            NotificationFragment notificationFragment = new NotificationFragment();
            notificationFragment.show(getSupportFragmentManager(),"Send Notification");
        });


        // display sample fragment
        btnSampleWinners.setOnClickListener(view->{
            SampleFragment sampleFragment = new SampleFragment();
            sampleFragment.show(getSupportFragmentManager(),"Sample Winners");
        });


        // switch to view location component to display the entrants on the map
        btnViewLocation.setOnClickListener(v->{
            viewLocationLayout.setVisibility(View.VISIBLE);
            cancelledEntrantsListLayout.setVisibility(View.GONE);
            waitedListEntrantsListLayout.setVisibility(View.GONE);
            signedUpEntrantsListLayout.setVisibility(View.GONE);
            if(googleMap!=null){
                if(waitedListEntrantsListLayout.getVisibility()==View.VISIBLE){
                    insertMarkers(entrantWaitedListArrayList);
                }
                if(signedUpEntrantsListLayout.getVisibility()==View.VISIBLE){
                    insertMarkers(entrantSignedUpArrayList);
                }
                if(cancelledEntrantsListLayout.getVisibility()==View.VISIBLE){
                    insertMarkers(entrantCancelledArrayList);
                }
            }
        });

        //fetch entrants list from firebase
        entrantsRef.limit(100).addSnapshotListener((value, error)-> {
            if(error!=null){
                Log.e("Firestore",error.toString());
            }if(value!=null && !value.isEmpty()){
                entrantCancelledArrayList.clear();
                entrantSignedUpArrayList.clear();
                entrantWaitedListArrayList.clear();
                for(QueryDocumentSnapshot snapshot: value){
                    String accepted_timestamp = snapshot.getString("accepted_timestamp");
                    String cancelled_timestamp = snapshot.getString("cancelled_timestamp");
                    String event_id = snapshot.getString("event_id");
                    String invitation_timestamp= snapshot.getString("invitation_timestamp");
                    String referrer_id= snapshot.getString("referrer_id");
                    String register_timestamp= snapshot.getString("register_timestamp");
                    String user_id= snapshot.getString("user_id");
                    String entrant_status = snapshot.getString("entrant_status");
                    com.google.firebase.firestore.GeoPoint location = snapshot.getGeoPoint("location");
                    if(Objects.equals(entrant_status, "signed_up")){
                        entrantSignedUpArrayList.add(new Entrant(accepted_timestamp, cancelled_timestamp, event_id, invitation_timestamp, referrer_id, register_timestamp, user_id,entrant_status, location));
                    } else if (Objects.equals(entrant_status, "waited_listed")){
                        entrantWaitedListArrayList.add(new Entrant(accepted_timestamp, cancelled_timestamp, event_id, invitation_timestamp, referrer_id, register_timestamp, user_id,entrant_status, location));
                    } else if(Objects.equals(entrant_status, "cancelled")){
                    entrantCancelledArrayList.add(new Entrant(accepted_timestamp, cancelled_timestamp, event_id, invitation_timestamp, referrer_id, register_timestamp, user_id,entrant_status, location));
                    }
                }
                SignedUpListAdapter.notifyDataSetChanged();
                CancelledListAdapter.notifyDataSetChanged();
                WaitedListedListAdapter.notifyDataSetChanged();
            }
        });
    }
    @Override
    public void sampling(int size){
        entrantsRef.whereEqualTo("entrant_status",)
    }
    @Override
    public void sendNotification(String content){

    }
    /**
     * Initialize view for the create event activity.
     */
    private void initializeViews() {
        btnSwitchWaitedList = findViewById(R.id.entrants_list_waited_list_btn);
        btnSwitchCancelled = findViewById(R.id.entrants_list_cancelled_btn);
        btnSwitchSignedUp = findViewById(R.id.entrants_list_signed_up_btn);
        btnViewLocation =findViewById(R.id.entrants_list_view_location_btn);
        btnSampleWinners =findViewById(R.id.entrants_list_sample_btn);
        btnSendNotification = findViewById(R.id.entrants_list_send_notification_btn);
        signedUpEventsView = findViewById(R.id.signed_up_events_view);
        waitedListEventsView = findViewById(R.id.waited_list_events_view);
        cancelledEntrantsView = findViewById(R.id.cancelled_entrants_view);
        cancelledEntrantsListLayout = findViewById(R.id.cancelled_entrants_list_layout);
        signedUpEntrantsListLayout = findViewById(R.id.signed_up_entrants_list_layout);
        waitedListEntrantsListLayout = findViewById(R.id.waited_list_entrants_list_layout);
        viewLocationLayout = findViewById(R.id.view_location_layout);
        mapView = findViewById(R.id.mapView);
    }

    // Source - https://stackoverflow.com/a/30054797
    // Posted by Ankit Khare, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-11, License - CC BY-SA 3.0
    private void insertMarkers(ArrayList<Entrant> list) {
        googleMap.clear();
        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < list.size(); i++) {
            Entrant entrant = list.get(i);
            com.google.firebase.firestore.GeoPoint geoLocation = entrant.getLocation();
            final LatLng position = new LatLng(geoLocation.getLatitude(), geoLocation.getLongitude());
            final MarkerOptions options = new MarkerOptions().position(position);
            googleMap.addMarker(options);
            builder.include(position);
        }
    }


    // Source - https://stackoverflow.com/a/19806967
    // Posted by Naveed Ali, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-11, License - CC BY-SA 4.0
        @Override
        public void onMapReady(GoogleMap g) {
            googleMap = g;
            googleMap.getUiSettings().setZoomControlsEnabled(true);
        }
        @Override
        public void onStart() {
        mapView.onStart();
        super.onStart();
        }
        @Override
        public void onStop(){
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
            mapView.onDestroy();
        }
        @Override
        public void onLowMemory() {
            super.onLowMemory();
            mapView.onLowMemory();
        }
}
