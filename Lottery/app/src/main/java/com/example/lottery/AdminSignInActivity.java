package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Admin authentication screen.
 * <p>
 * Verifies a secret access code against admin_codes/primary_admin_code in Firestore.
 * The document must have a "code" (String) and "isActive" (Boolean) field.
 * On success, navigates to AdminBrowseEventsActivity.
 */
public class AdminSignInActivity extends AppCompatActivity {

    private static final String ADMIN_CODES = "admin_codes";
    private static final String PRIMARY_ADMIN_CODE = "primary_admin_code";

    private FirebaseFirestore db;
    private EditText adminCodeInput;
    private Button signInButton;

    /**
     * Initialize the activity and binds UI views.
     *
     * @param savedInstanceState previously saved instance state, or null if not exist.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_sign_in);

        db = FirebaseFirestore.getInstance();
        adminCodeInput = findViewById(R.id.etAdminCode);
        signInButton = findViewById(R.id.btnSignIn);

        signInButton.setOnClickListener(view -> validateAdminCode());
    }

    /**
     * Fetches the admin code from Firestore and validates it against the user's input.
     * Navigates to AdminBrowseEventsActivity on success.
     */
    private void validateAdminCode() {
        String enteredCode = adminCodeInput.getText().toString().trim();

        // Skip call if input is empty.
        if (enteredCode.isEmpty()) {
            adminCodeInput.setError(getString(R.string.error_admin_code_required));
            return;
        }

        // Prevent duplicate submissions while the request is under processing.
        signInButton.setEnabled(false);

        db.collection(ADMIN_CODES).document(PRIMARY_ADMIN_CODE)
                .get()
                .addOnCompleteListener(task -> {
                    signInButton.setEnabled(true);

                    if (!task.isSuccessful()) {
                        Toast.makeText(this, R.string.error_admin_code_lookup_failed,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot document = task.getResult();

                    // Document missing — admin code has not been configured in Firestore.
                    if (document == null || !document.exists()) {
                        Toast.makeText(this, R.string.error_admin_code_not_configured,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String storedCode = document.getString("code");
                    Boolean isActive = document.getBoolean("isActive");

                    if (storedCode == null) {
                        Toast.makeText(this, R.string.error_admin_code_not_configured,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!Boolean.TRUE.equals(isActive)) {
                        Toast.makeText(this, R.string.error_admin_code_inactive,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!storedCode.equals(enteredCode)) {
                        adminCodeInput.setError(getString(R.string.error_incorrect_admin_code));
                        return;
                    }

                    startActivity(new Intent(this, AdminBrowseEventsActivity.class));
                    finish();
                });
    }
}
