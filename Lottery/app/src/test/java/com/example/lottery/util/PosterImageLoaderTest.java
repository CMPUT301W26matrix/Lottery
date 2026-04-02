package com.example.lottery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.util.Base64;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

/**
 * Tests for {@link PosterImageLoader}.
 * Verifies Base64 detection, decoding, and Robolectric-side loading behavior.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PosterImageLoaderTest {

    private Method isBase64Method;
    private Method decodeBase64Method;

    @Before
    public void setUp() throws Exception {
        isBase64Method = PosterImageLoader.class.getDeclaredMethod("isBase64", String.class);
        isBase64Method.setAccessible(true);
        decodeBase64Method = PosterImageLoader.class.getDeclaredMethod("decodeBase64", String.class);
        decodeBase64Method.setAccessible(true);
    }

    // US 02.04.01: Verify isBase64 detects JPEG data URI prefix
    @Test
    public void testIsBase64WithDataImageJpegPrefix() throws Exception {
        assertTrue((boolean) isBase64Method.invoke(null, "data:image/jpeg;base64,/9j/4AAQ"));
    }

    // US 02.04.01: Verify isBase64 detects PNG data URI prefix
    @Test
    public void testIsBase64WithDataImagePngPrefix() throws Exception {
        assertTrue((boolean) isBase64Method.invoke(null, "data:image/png;base64,iVBORw0KGgo"));
    }

    // US 02.04.01: Verify isBase64 returns false for HTTP URLs
    @Test
    public void testIsBase64WithHttpUrl() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, "https://example.com/poster.jpg"));
    }

    // US 02.04.01: Verify isBase64 returns false for content URIs
    @Test
    public void testIsBase64WithContentUri() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, "content://media/external/images/123"));
    }

    // US 02.04.01: Verify isBase64 returns false for empty string
    @Test
    public void testIsBase64WithEmptyString() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, ""));
    }

    // US 02.04.01: Verify isBase64 returns false for plain text
    @Test
    public void testIsBase64WithPlainText() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, "just some plain text"));
    }

    // US 02.04.01: Verify decodeBase64 correctly decodes data with URI prefix
    @Test
    public void testDecodeBase64WithDataPrefix() throws Exception {
        byte[] original = {1, 2, 3, 4, 5};
        String encoded = "data:image/jpeg;base64,"
                + Base64.encodeToString(original, Base64.NO_WRAP);
        byte[] decoded = (byte[]) decodeBase64Method.invoke(null, encoded);
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], decoded[i]);
        }
    }

    // US 02.04.01: Verify decodeBase64 correctly decodes raw Base64 without prefix
    @Test
    public void testDecodeBase64WithoutPrefix() throws Exception {
        byte[] original = {10, 20, 30};
        String encoded = Base64.encodeToString(original, Base64.NO_WRAP);
        byte[] decoded = (byte[]) decodeBase64Method.invoke(null, encoded);
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
    }

    // US 03.06.01: Verify load does not crash when source is null
    @Test
    public void testLoadWithNullSource() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        PosterImageLoader.load(imageView, null, R.drawable.event_placeholder);
    }

    // US 03.06.01: Verify load does not crash when source is empty
    @Test
    public void testLoadWithEmptyString() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        PosterImageLoader.load(imageView, "", R.drawable.event_placeholder);
    }

    // US 03.06.01: Verify load does not crash when ImageView is null
    @Test
    public void testLoadWithNullImageView() {
        PosterImageLoader.load(null, "data:image/jpeg;base64,/9j/4AAQ", R.drawable.event_placeholder);
    }

    // US 02.04.01: Verify load handles Base64-encoded poster image
    @Test
    public void testLoadWithBase64String() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        byte[] fakeImage = {0, 1, 2, 3, 4, 5};
        String base64 = "data:image/jpeg;base64," + Base64.encodeToString(fakeImage, Base64.DEFAULT);
        PosterImageLoader.load(imageView, base64, R.drawable.event_placeholder);
    }

    // US 02.04.01: Verify load handles HTTP URL poster image source
    @Test
    public void testLoadWithHttpUrl() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        PosterImageLoader.load(imageView, "https://example.com/poster.jpg", R.drawable.event_placeholder);
    }
}
