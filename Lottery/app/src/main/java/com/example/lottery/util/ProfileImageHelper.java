package com.example.lottery.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;

import com.example.lottery.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared helper for profile image operations.
 * Used by AdminProfileActivity, EntrantProfileActivity, and OrganizerProfileActivity.
 */
public final class ProfileImageHelper {

    private static final int AVATAR_SIZE = 200;
    private static final int JPEG_QUALITY = 70;

    private ProfileImageHelper() {
    }

    /**
     * Displays a profile image from a Base64 string, or falls back to a generated default avatar.
     */
    public static void displayProfileImage(String base64String, ImageView imageView,
                                           ImageView placeholderView, String seed) {
        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(base64String, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    imageView.setVisibility(View.VISIBLE);
                    if (placeholderView != null) placeholderView.setVisibility(View.GONE);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        showDefaultAvatar(imageView, placeholderView, seed);
    }

    /**
     * Shows a deterministic, initial-based default avatar.
     */
    public static void showDefaultAvatar(ImageView imageView, ImageView placeholderView, String seed) {
        Bitmap avatar = AvatarUtils.generateDefaultAvatar(seed != null ? seed : "?", AVATAR_SIZE);
        imageView.setImageBitmap(avatar);
        imageView.setVisibility(View.VISIBLE);
        if (placeholderView != null) placeholderView.setVisibility(View.GONE);
    }

    /**
     * Processes a picked image URI: decodes, scales to avatar size, compresses to JPEG,
     * and returns both the Base64 string and the scaled bitmap.
     * Properly closes the InputStream via try-with-resources.
     *
     * @return a ProcessedImage on success, or null if decoding fails.
     * @throws IOException if the content resolver cannot open the URI.
     */
    public static ProcessedImage processSelectedImage(ContentResolver contentResolver, Uri imageUri)
            throws IOException {
        try (InputStream inputStream = contentResolver.openInputStream(imageUri)) {
            if (inputStream == null) return null;

            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) return null;

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                    originalBitmap, AVATAR_SIZE, AVATAR_SIZE, true);
            if (scaledBitmap != originalBitmap) {
                originalBitmap.recycle();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            return new ProcessedImage(base64, scaledBitmap);
        }
    }

    /**
     * Shows avatar options: if no image exists, runs onPickImage directly;
     * otherwise shows a bottom sheet with "Change photo" and "Remove photo" options.
     */
    public static void showAvatarOptions(Activity activity, boolean hasImage,
                                         Runnable onPickImage, Runnable onRemoveImage) {
        if (!hasImage) {
            onPickImage.run();
        } else {
            BottomSheetDialog dialog = new BottomSheetDialog(activity);
            View view = activity.getLayoutInflater().inflate(R.layout.layout_avatar_options_sheet, null);

            view.findViewById(R.id.ll_change_photo).setOnClickListener(v -> {
                onPickImage.run();
                dialog.dismiss();
            });

            view.findViewById(R.id.ll_remove_photo).setOnClickListener(v -> {
                onRemoveImage.run();
                dialog.dismiss();
            });

            dialog.setContentView(view);
            dialog.show();
        }
    }

    /**
     * Launches the system image picker.
     */
    public static void openImagePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcher.launch(intent);
    }

    /**
     * Determines whether a custom profile image currently exists,
     * considering both the in-progress selection and the persisted state.
     */
    public static boolean hasCustomImage(String selectedImageBase64, String savedImageBase64) {
        return (selectedImageBase64 != null && !selectedImageBase64.isEmpty())
                || (selectedImageBase64 == null
                && savedImageBase64 != null && !savedImageBase64.isEmpty());
    }

    /**
     * Holds the result of processing a picked image.
     */
    public static class ProcessedImage {
        public final String base64;
        public final Bitmap bitmap;

        public ProcessedImage(String base64, Bitmap bitmap) {
            this.base64 = base64;
            this.bitmap = bitmap;
        }
    }
}
