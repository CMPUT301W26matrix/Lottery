package com.example.lottery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.QRCodeUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.InputStream;

/**
 * Activity for entrants to scan promotional QR codes.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Supports camera-based scanning of QR codes.</li>
 *   <li>Supports picking a QR code image from the device gallery.</li>
 *   <li>Navigates to event details upon successful QR code detection.</li>
 * </ul>
 * </p>
 */
public class EntrantQrScanActivity extends AppCompatActivity {

    private static final String TAG = "EntrantQrScanActivity";
    private String userId;
    private TextView tvNotificationBadge;
    private FirebaseFirestore db;

    /**
     * Launcher for the camera-based QR code scanner.
     */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    handleScanResult(result.getContents());
                }
            });

    /**
     * Launcher for picking an image from the gallery.
     */
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    decodeQrFromImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_qr_scan);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnOpenScanner).setOnClickListener(v -> openScanner());
        findViewById(R.id.btnPickQrFromGallery).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        setupNavigation();
        checkUnreadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadNotifications();
    }

    /**
     * Configures and opens the camera-based QR scanner.
     */
    private void openScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a promotional QR code");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    /**
     * Decodes a QR code from a provided image Uri.
     *
     * @param imageUri The Uri of the image to decode.
     */
    private void decodeQrFromImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                return;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            Result result = reader.decode(binaryBitmap);
            handleScanResult(result.getText());

        } catch (Exception e) {
            Log.e(TAG, "QR Decoding failed", e);
            Toast.makeText(this, "No QR code found in image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Processes the raw content from a QR code and navigates to the details page.
     *
     * @param contents The raw string content decoded from the QR code.
     */
    private void handleScanResult(String contents) {
        String eventId = QRCodeUtils.extractEventId(contents);
        if (eventId != null) {
            Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
            intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Invalid QR Code content", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        findViewById(R.id.nav_qr_scan).setOnClickListener(v -> {
            // Already here
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void checkUnreadNotifications() {
        if (userId == null || tvNotificationBadge == null) return;
        db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("isRead", false).get()
                .addOnSuccessListener(querySnapshot -> tvNotificationBadge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE));
    }
}
