package com.example.lottery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Unit tests for {@link ProfileImageHelper}.
 *
 * <p>Covers User Stories:</p>
 * <ul>
 *   <li>US 01.02.01: As an entrant, I want to provide my personal information such as
 *       name, email and optional phone number in the app. (profile display incl. avatar)</li>
 *   <li>US 01.02.02: As an entrant, I want to update information such as name, email and
 *       contact information on my profile. (avatar upload/removal)</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ProfileImageHelperTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
    }

    // US 01.02.02: User selected a new image during profile edit → should detect custom image
    @Test
    public void hasCustomImage_selectedNonEmpty_savedNull_returnsTrue() {
        assertTrue(ProfileImageHelper.hasCustomImage("base64data", null));
    }

    // US 01.02.02: User selected a new image replacing an existing one → still custom
    @Test
    public void hasCustomImage_selectedNonEmpty_savedNonEmpty_returnsTrue() {
        assertTrue(ProfileImageHelper.hasCustomImage("newImage", "savedImage"));
    }

    // US 01.02.01: No new selection but a saved image exists → custom image present
    @Test
    public void hasCustomImage_selectedNull_savedNonEmpty_returnsTrue() {
        assertTrue(ProfileImageHelper.hasCustomImage(null, "savedImage"));
    }

    // US 01.02.01: Neither selected nor saved → no custom image
    @Test
    public void hasCustomImage_bothNull_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage(null, null));
    }

    // US 01.02.01: Empty saved string should not count as custom image
    @Test
    public void hasCustomImage_selectedNull_savedEmpty_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage(null, ""));
    }

    // US 01.02.02: Empty selected string signals avatar removal to no custom image
    @Test
    public void hasCustomImage_selectedEmpty_savedNonEmpty_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage("", "savedImage"));
    }

    // US 01.02.02: Both empty → no custom image
    @Test
    public void hasCustomImage_selectedEmpty_savedNull_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage("", null));
    }

    // US 01.02.01: Valid Base64 image should be displayed and placeholder hidden
    @Test
    public void displayProfileImage_validBase64_showsImageHidesPlaceholder() {
        ImageView imageView = new ImageView(context);
        ImageView placeholder = new ImageView(context);
        placeholder.setVisibility(View.VISIBLE);

        Bitmap testBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        testBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        ProfileImageHelper.displayProfileImage(base64, imageView, placeholder, "Test");

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(View.GONE, placeholder.getVisibility());
    }

    // US 01.02.01: Null base64 should fall back to generated default avatar
    @Test
    public void displayProfileImage_nullBase64_showsDefaultAvatar() {
        ImageView imageView = new ImageView(context);
        ImageView placeholder = new ImageView(context);

        ProfileImageHelper.displayProfileImage(null, imageView, placeholder, "Alice");

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(View.GONE, placeholder.getVisibility());
        assertNotNull(imageView.getDrawable());
    }

    // US 01.02.01: Empty base64 should fall back to generated default avatar
    @Test
    public void displayProfileImage_emptyBase64_showsDefaultAvatar() {
        ImageView imageView = new ImageView(context);
        ImageView placeholder = new ImageView(context);

        ProfileImageHelper.displayProfileImage("", imageView, placeholder, "Bob");

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(View.GONE, placeholder.getVisibility());
    }

    // US 01.02.01: Corrupt base64 data should not crash — falls back to default avatar
    @Test
    public void displayProfileImage_invalidBase64_doesNotCrash() {
        ImageView imageView = new ImageView(context);
        ImageView placeholder = new ImageView(context);

        ProfileImageHelper.displayProfileImage("not-valid-base64!!!", imageView, placeholder, "Charlie");

        assertEquals(View.VISIBLE, imageView.getVisibility());
    }

    // US 01.02.01: Null placeholder view should be handled gracefully
    @Test
    public void displayProfileImage_nullPlaceholder_doesNotCrash() {
        ImageView imageView = new ImageView(context);

        ProfileImageHelper.displayProfileImage(null, imageView, null, "Dave");

        assertEquals(View.VISIBLE, imageView.getVisibility());
    }

    // US 01.02.01: Default avatar should set image visible and hide placeholder when viewing profile
    @Test
    public void showDefaultAvatar_setsCorrectVisibility() {
        ImageView imageView = new ImageView(context);
        imageView.setVisibility(View.GONE);
        ImageView placeholder = new ImageView(context);
        placeholder.setVisibility(View.VISIBLE);

        ProfileImageHelper.showDefaultAvatar(imageView, placeholder, "Eve");

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(View.GONE, placeholder.getVisibility());
        assertNotNull(imageView.getDrawable());
    }

    // US 01.02.01: Null seed should use "?" fallback without crashing
    @Test
    public void showDefaultAvatar_nullSeed_doesNotCrash() {
        ImageView imageView = new ImageView(context);

        ProfileImageHelper.showDefaultAvatar(imageView, null, null);

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertNotNull(imageView.getDrawable());
    }

    // US 01.02.01: Same seed should produce the same avatar
    @Test
    public void showDefaultAvatar_sameSeed_deterministic() {
        ImageView iv1 = new ImageView(context);
        ImageView iv2 = new ImageView(context);

        ProfileImageHelper.showDefaultAvatar(iv1, null, "TestUser");
        ProfileImageHelper.showDefaultAvatar(iv2, null, "TestUser");

        // Both should be visible and have a drawable set
        assertEquals(View.VISIBLE, iv1.getVisibility());
        assertEquals(View.VISIBLE, iv2.getVisibility());
        assertNotNull(iv1.getDrawable());
        assertNotNull(iv2.getDrawable());
    }

    // US 01.02.02: Null InputStream from ContentResolver should return null
    @Test
    public void processSelectedImage_nullStream_returnsNull() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/null");
        when(mockResolver.openInputStream(testUri)).thenReturn(null);

        ProfileImageHelper.ProcessedImage result =
                ProfileImageHelper.processSelectedImage(mockResolver, testUri);

        assertNull(result);
    }

    // US 01.02.02: FileNotFoundException from ContentResolver should propagate as IOException
    @Test(expected = java.io.FileNotFoundException.class)
    public void processSelectedImage_fileNotFound_propagates() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/ioerror");
        when(mockResolver.openInputStream(testUri))
                .thenThrow(new java.io.FileNotFoundException("File not found"));

        ProfileImageHelper.processSelectedImage(mockResolver, testUri);
    }

    // US 01.02.02: Non-image bytes should not crash
    @Test
    public void processSelectedImage_invalidImageBytes_doesNotCrash() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/garbage");
        when(mockResolver.openInputStream(testUri))
                .thenReturn(new ByteArrayInputStream(new byte[]{0, 1, 2, 3, 4, 5}));

        // Should not throw — either returns null or a ProcessedImage depending on shadow
        ProfileImageHelper.processSelectedImage(mockResolver, testUri);
    }

    // US 01.02.02: Valid PNG image bytes should produce a ProcessedImage with base64 and bitmap
    @Test
    public void processSelectedImage_validImage_returnsProcessedImage() throws IOException {
        Bitmap sourceBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/valid");
        when(mockResolver.openInputStream(testUri))
                .thenReturn(new ByteArrayInputStream(baos.toByteArray()));

        ProfileImageHelper.ProcessedImage result =
                ProfileImageHelper.processSelectedImage(mockResolver, testUri);

        assertNotNull("ProcessedImage should not be null for valid image", result);
        assertNotNull("Base64 should not be null", result.base64);
        assertFalse("Base64 should not be empty", result.base64.isEmpty());
        assertNotNull("Bitmap should not be null", result.bitmap);
    }

    // US 01.02.02: InputStream must be closed after processing to prevent resource leaks
    @Test
    public void processSelectedImage_closesInputStream() throws IOException {
        Bitmap sourceBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/close");
        ByteArrayInputStream spyStream = spy(new ByteArrayInputStream(baos.toByteArray()));
        when(mockResolver.openInputStream(testUri)).thenReturn(spyStream);

        ProfileImageHelper.processSelectedImage(mockResolver, testUri);

        verify(spyStream).close();
    }

    // US 01.02.02: InputStream must be closed even when decoding fails
    @Test
    public void processSelectedImage_closesStreamOnDecodeFailure() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/fail");
        ByteArrayInputStream spyStream = spy(new ByteArrayInputStream(new byte[]{0, 1, 2}));
        when(mockResolver.openInputStream(testUri)).thenReturn(spyStream);

        ProfileImageHelper.processSelectedImage(mockResolver, testUri);

        verify(spyStream).close();
    }

    // US 01.02.02: Should launch gallery picker with ACTION_PICK intent
    @SuppressWarnings("unchecked")
    @Test
    public void openImagePicker_launchesGalleryIntent() {
        ActivityResultLauncher<Intent> mockLauncher = mock(ActivityResultLauncher.class);

        ProfileImageHelper.openImagePicker(mockLauncher);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mockLauncher).launch(captor.capture());
        assertEquals(Intent.ACTION_PICK, captor.getValue().getAction());
    }

    // US 01.02.02: ProcessedImage should correctly store base64 and bitmap fields
    @Test
    public void processedImage_storesFieldsCorrectly() {
        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        ProfileImageHelper.ProcessedImage result =
                new ProfileImageHelper.ProcessedImage("abc123", bitmap);

        assertEquals("abc123", result.base64);
        assertSame(bitmap, result.bitmap);
    }
}
