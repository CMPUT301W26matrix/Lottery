package com.example.lottery;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class EntrantsListView extends AppCompatActivity{
    private static final String TAG = "CreateEventActivity";
    private Button btnSwitchSignedUp, btnSwitchCancelled, btnSwitchWaitedList, btnSendNotification, btnViewLocation, btnSampleWinners;
    private FirebaseFirestore db;
    private ArrayList<Entrant> entrantSignedUpArrayList;
    private ArrayList<Entrant> entrantCancelledArrayList;
    private ArrayList<Entrant> entrantWaitedListArrayList;
    private SignedUpListAdapter SignedUpListAdapter;
    private CancelledListAdapter CancelledListAdapter;
    private WaitedListedListAdapter WaitedListedListAdapter;
    private LinearLayout cancelledEntrantsListLayout, signedUpEntrantsListLyaout ,waitedListEntrantsListLayout;
    private RecyclerView signedUpEventsView, waitedListEventsView, cancelledEntrantsView;
    private CollectionReference entrantsRef;
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
            cancelledEntrantsListLayout.setVisibility(View.GONE);
            waitedListEntrantsListLayout.setVisibility(View.GONE);

        });
        // switch to signed up component to display the entrants list that have signed up
        btnSwitchCancelled.setOnClickListener(v -> {
            signedUpEntrantsListLyaout.setVisibility(View.GONE);
            waitedListEntrantsListLayout.setVisibility(View.GONE);
        });
        // switch to signed up component to display the entrants list that have signed up
        btnSwitchWaitedList.setOnClickListener(v -> {
            cancelledEntrantsListLayout.setVisibility(View.GONE);
            signedUpEntrantsListLyaout.setVisibility(View.GONE);
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
                    if(Objects.equals(entrant_status, "signed_up")){
                        entrantSignedUpArrayList.add(new Entrant(accepted_timestamp, cancelled_timestamp, event_id, invitation_timestamp, referrer_id, register_timestamp, user_id,entrant_status));
                    } else if (Objects.equals(entrant_status, "waited_listed")){
                        entrantWaitedListArrayList.add(new Entrant(accepted_timestamp, cancelled_timestamp, event_id, invitation_timestamp, referrer_id, register_timestamp, user_id,entrant_status));
                    } else if(Objects.equals(entrant_status, "cancelled")){
                    entrantCancelledArrayList.add(new Entrant(accepted_timestamp, cancelled_timestamp, event_id, invitation_timestamp, referrer_id, register_timestamp, user_id,entrant_status));
                    }
                }
                SignedUpListAdapter.notifyDataSetChanged();
                CancelledListAdapter.notifyDataSetChanged();
                WaitedListedListAdapter.notifyDataSetChanged();
            }
        });
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
        signedUpEntrantsListLyaout = findViewById(R.id.signed_up_entrants_list_layout);
        waitedListEntrantsListLayout = findViewById(R.id.waited_list_entrants_list_layout);
    }
}
