package com.example.lottery.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.Glide;

/**
 * Centralizes poster image loading so screens can render either local URIs,
 * remote download URLs, or Base64 encoded strings.
 */
public final class PosterImageLoader {

    private PosterImageLoader() {
    }

    /**
     * Loads a poster image into the provided ImageView using Glide with a placeholder fallback.
     *
     * @param imageView        The target ImageView.
     * @param imageSource      A remote URL, file/content URI string, Uri instance, or Base64 string.
     * @param placeholderResId Drawable used when no image is available or loading fails.
     */
    public static void load(ImageView imageView, Object imageSource, @DrawableRes int placeholderResId) {
        if (imageView == null) {
            return;
        }

        Context context = imageView.getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
        }

        Object model = imageSource;
        if (imageSource instanceof String) {
            String str = ((String) imageSource).trim();
            if (str.isEmpty()) {
                model = null;
            } else if (isBase64(str)) {
                model = decodeBase64(str);
            }
        } else if (imageSource instanceof Uri && Uri.EMPTY.equals(imageSource)) {
            model = null;
        }

        Glide.with(imageView)
                .load(model)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .into(imageView);
    }

    private static boolean isBase64(String str) {
        return str.startsWith("data:image");
    }

    private static byte[] decodeBase64(String base64Str) {
        try {
            String pureBase64 = base64Str;
            if (base64Str.contains(",")) {
                pureBase64 = base64Str.split(",")[1];
            }
            return Base64.decode(pureBase64, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }
}
