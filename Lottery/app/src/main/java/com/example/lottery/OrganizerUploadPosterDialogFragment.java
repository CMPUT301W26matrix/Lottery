package com.example.lottery;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.lottery.util.PosterImageLoader;

/**
 * DialogFragment for handling event poster selection and preview.
 * Provides functionality to browse the device for images, preview selected images,
 * and enlarge them for better visibility.
 *
 * <p>Key Responsibilities include:
 * <ul>
 *   <li>Launches a system image picker to select a poster image.</li>
 *   <li>Displays a thumbnail preview of the selected image within the dialog.</li>
 *   <li>Allows users to enlarge the preview image by clicking the thumbnail. (Requirement US 02.04.01).</li>
 *   <li>Passes the selected image URI back to the calling fragment or activity using the Fragment Result API.</li>
 * </ul>
 * </p>
 *
 * <p>Communication:
 * This dialog sends results back via getParentFragmentManager().setFragmentResult("posterRequest", bundle)
 * where the bundle contains the key "posterUri".
 * </p>
 */
public class OrganizerUploadPosterDialogFragment extends DialogFragment {

    /**
     * Temporary storage for the URI of the selected image before confirmation.
     */
    private Uri tempUri = null;
    /**
     * ImageView to display the thumbnail of the selected poster.
     */
    private ImageView ivPreview;
    /**
     * Button to confirm the current image selection and close the dialog.
     */
    private Button btnSelect;
    /**
     * Container view for the preview image, used as a click target for enlargement.
     */
    private View cvPreviewContainer;
    /**
     * Hint text shown to the user after an image is selected.
     */
    private View tvHint;
    /**
     * Launcher for the GetContent activity result contract to pick an image.
     */
    private ActivityResultLauncher<String[]> pickerLauncher;

    /**
     * Initializes the ActivityResultLauncher for picking images from the device storage.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use OpenDocument so the app can persist read access across activities and restarts.
        pickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                tempUri = uri;
                updatePreviewLayout(uri);
            }
        });
    }

    /**
     * Creates and returns the dialog for this fragment.
     * Sets up view bindings and click listeners for image browsing, selection confirmation, and preview enlargement.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     * @return A new AlertDialog instance.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_organizer_upload_poster, null);

        ivPreview = view.findViewById(R.id.ivDialogPreview);
        btnSelect = view.findViewById(R.id.btnSelectConfirm);
        Button btnBrowse = view.findViewById(R.id.btnBrowseImage);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        cvPreviewContainer = view.findViewById(R.id.cvDialogPreview);
        tvHint = view.findViewById(R.id.tvPreviewHint);

        // Browse button click listener
        btnBrowse.setOnClickListener(v -> pickerLauncher.launch(new String[]{"image/*"}));

        // Cancel button click listener
        btnCancel.setOnClickListener(v -> dismiss());

        // Confirm the selection and pass it back to the Activity
        btnSelect.setOnClickListener(v -> {
            if (tempUri != null) {
                Bundle result = new Bundle();
                result.putString("posterUri", tempUri.toString());
                getParentFragmentManager().setFragmentResult("posterRequest", result);
                dismiss();
            }
        });

        /**
         * US 02.04.01 Upgrade: Enlarging preview
         * Clicking the thumbnail container opens a larger view.
         */
        cvPreviewContainer.setOnClickListener(v -> {
            if (tempUri != null) {
                showEnlargedPreview(tempUri);
            }
        });

        builder.setView(view);
        return builder.create();
    }

    /**
     * Updates the dialog UI to show the selected image thumbnail and enable the confirmation button.
     *
     * @param uri The URI of the image to display in the preview.
     */
    private void updatePreviewLayout(Uri uri) {
        PosterImageLoader.load(ivPreview, uri, R.drawable.event_placeholder);
        // Enable visual feedback and selection button
        tvHint.setVisibility(View.VISIBLE);
        btnSelect.setEnabled(true);
    }

    /**
     * Displays a simple alert dialog with a larger version of the selected image.
     * Fulfills detail requirements for US 02.04.01 regarding poster visibility.
     *
     * @param uri The URI of the image to display in an enlarged view.
     */
    private void showEnlargedPreview(Uri uri) {
        ImageView enlargedIv = new ImageView(requireContext());
        PosterImageLoader.load(enlargedIv, uri, R.drawable.event_placeholder);
        enlargedIv.setAdjustViewBounds(true);
        enlargedIv.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Poster Preview")
                .setView(enlargedIv)
                .setPositiveButton("Close", null)
                .show();
    }
}
