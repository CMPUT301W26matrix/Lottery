package com.example.lottery.util;

import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.Glide;

/**
 * Centralizes poster image loading so screens can render either local URIs or remote download URLs.
 */
public final class PosterImageLoader {

    private PosterImageLoader() {
    }

    /**
     * Loads a poster image into the provided ImageView using Glide with a placeholder fallback.
     *
     * @param imageView The target ImageView.
     * @param imageSource A remote URL, file/content URI string, or Uri instance.
     * @param placeholderResId Drawable used when no image is available or loading fails.
     */
    public static void load(ImageView imageView, Object imageSource, @DrawableRes int placeholderResId) {
        if (imageView == null) {
            return;
        }

        Object model = imageSource;
        if (imageSource instanceof String && ((String) imageSource).trim().isEmpty()) {
            model = null;
        } else if (imageSource instanceof Uri && Uri.EMPTY.equals(imageSource)) {
            model = null;
        }

        Glide.with(imageView)
                .load(model)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .into(imageView);
    }
}
