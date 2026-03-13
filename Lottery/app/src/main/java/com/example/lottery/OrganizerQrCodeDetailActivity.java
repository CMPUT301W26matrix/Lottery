package com.example.lottery;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lottery.util.QRCodeUtils;

/**
 * Activity for displaying the large QR code for a specific event.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Displays the event title and its associated QR code.</li>
 *   <li>Uses {@link QRCodeUtils} to render the QR code bitmap from provided content.</li>
 *   <li>Handles navigation back to the event selection list.</li>
 * </ul>
 * </p>
 */
public class OrganizerQrCodeDetailActivity extends AppCompatActivity {

    /** Intent extra key for the event title. */
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    /** Intent extra key for the QR code raw content. */
    public static final String EXTRA_QR_CONTENT = "extra_qr_content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_qr_code_detail);

        // Setup Toolbar with back navigation
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Retrieve data from Intent
        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        String qrContent = getIntent().getStringExtra(EXTRA_QR_CONTENT);

        TextView tvTitle = findViewById(R.id.tvDetailEventTitle);
        ImageView ivQrCode = findViewById(R.id.ivQrCodeLarge);

        tvTitle.setText(eventTitle != null ? eventTitle : "Event QR Code");

        // Prepare content to encode; fallback to a test string if content is missing
        String contentToEncode = (qrContent != null && !qrContent.isEmpty()) 
                ? qrContent 
                : "Test QR Content for " + eventTitle;

        // Generate and display the QR Code
        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(contentToEncode);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        }
    }
}
