package com.example.lottery.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;



/** 
 * Utility class to generate default avatar.
 */
public class AvatarUtils {

    private static final int[] AVATAR_COLORS = {
            0xFF1ABC9C, 0xFF2ECC71, 0xFF3498DB, 0xFF9B59B6, 0xFF34495E,
            0xFF16A085, 0xFF27AE60, 0xFF2980B9, 0xFF8E44AD, 0xFF2C3E50,
            0xFFF1C40F, 0xFFE67E22, 0xFFE74C3C, 0xFF95A5A6, 0xFFF39C12,
            0xFFD35400, 0xFFC0392B, 0xFF7F8C8D
    };

    /**
     * Generates a default avatar bitmap with the user's initial.
     *
     * @param name The username to derive the initial and color from.
     * @param size The size of the bitmap (width and height).
     * @return A Bitmap containing the colored circle and initial.
     */
    public static Bitmap generateDefaultAvatar(String name, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Determine color based on name hash
        int color = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];

        // Draw background circle
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint);

        // Draw initial text
        String initial = getInitial(name);
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size * 0.5f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        textPaint.getTextBounds(initial, 0, initial.length(), bounds);
        float x = size / 2f;
        float y = (size / 2f) - bounds.centerY();
        canvas.drawText(initial, x, y, textPaint);

        return bitmap;
    }

    private static String getInitial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        return name.trim().substring(0, 1).toUpperCase();
    }
}
