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
 * Unit tests for {@link PosterImageLoader}.
 * Verifies Base64 detection and decoding logic used for poster image rendering.
 *
 * <p>Covers User Stories:</p>
 * <ul>
 *   <li>US 02.04.01: As an organizer, I want to upload an event poster so entrants
 *       can see what the event looks like. (poster rendering from Base64)</li>
 *   <li>US 03.06.01: As an administrator, I want to browse all uploaded event posters.
 *       (poster thumbnail loading)</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
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

    // --- isBase64() detection tests ---

    // US 02.04.01: JPEG poster stored as data URI should be recognized as Base64
    @Test
    public void testIsBase64WithDataImageJpegPrefix() throws Exception {
        assertTrue((boolean) isBase64Method.invoke(null, "data:image/jpeg;base64,/9j/4AAQ"));
    }

    // US 02.04.01: PNG poster stored as data URI should be recognized as Base64
    @Test
    public void testIsBase64WithDataImagePngPrefix() throws Exception {
        assertTrue((boolean) isBase64Method.invoke(null, "data:image/png;base64,iVBORw0KGgo"));
    }

    // US 02.04.01: HTTP URLs should not be mistaken for Base64 poster data
    @Test
    public void testIsBase64WithHttpUrl() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, "https://example.com/poster.jpg"));
    }

    // US 02.04.01: Content URIs should not be mistaken for Base64 poster data
    @Test
    public void testIsBase64WithContentUri() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, "content://media/external/images/123"));
    }

    // US 02.04.01: Empty string should not be detected as Base64
    @Test
    public void testIsBase64WithEmptyString() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, ""));
    }

    // US 02.04.01: Arbitrary text should not be detected as Base64
    @Test
    public void testIsBase64WithPlainText() throws Exception {
        assertEquals(false, isBase64Method.invoke(null, "just some plain text"));
    }

    // --- decodeBase64() decoding tests ---

    // US 02.04.01: Base64 data URI with prefix should decode to original bytes
    @Test
    public void testDecodeBase64WithDataPrefix() throws Exception {
        byte[] original = {1, 2, 3, 4, 5};
        String encoded = "data:image/jpeg;base64," + Base64.encodeToString(original, Base64.DEFAULT);
        byte[] decoded = (byte[]) decodeBase64Method.invoke(null, encoded);
        assertNotNull("Decoded bytes should not be null", decoded);
        assertEquals(original.length, decoded.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], decoded[i]);
        }
    }

    // US 02.04.01: Raw Base64 string without data URI prefix should still decode
    @Test
    public void testDecodeBase64WithoutPrefix() throws Exception {
        byte[] original = {10, 20, 30};
        String encoded = Base64.encodeToString(original, Base64.DEFAULT);
        byte[] decoded = (byte[]) decodeBase64Method.invoke(null, encoded);
        assertNotNull("Decoded bytes should not be null", decoded);
        assertEquals(original.length, decoded.length);
    }

    // --- load() integration tests ---

    // US 03.06.01: Loading with null source should show placeholder without crashing
    @Test
    public void testLoadWithNullSource() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        PosterImageLoader.load(imageView, null, R.drawable.event_placeholder);
    }

    // US 03.06.01: Loading with empty string should show placeholder without crashing
    @Test
    public void testLoadWithEmptyString() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        PosterImageLoader.load(imageView, "", R.drawable.event_placeholder);
    }

    // US 03.06.01: Loading with null ImageView should be a safe no-op
    @Test
    public void testLoadWithNullImageView() {
        PosterImageLoader.load(null, "data:image/jpeg;base64,/9j/4AAQ", R.drawable.event_placeholder);
    }

    // US 02.04.01: Loading a Base64-encoded poster should not throw
    @Test
    public void testLoadWithBase64String() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        byte[] fakeImage = {0, 1, 2, 3, 4, 5};
        String base64 = "data:image/jpeg;base64," + Base64.encodeToString(fakeImage, Base64.DEFAULT);
        PosterImageLoader.load(imageView, base64, R.drawable.event_placeholder);
    }

    // US 02.04.01: Loading a remote URL poster should not throw (Glide handles fetch)
    @Test
    public void testLoadWithHttpUrl() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        ImageView imageView = new ImageView(context);
        PosterImageLoader.load(imageView, "https://example.com/poster.jpg", R.drawable.event_placeholder);
    }
}
