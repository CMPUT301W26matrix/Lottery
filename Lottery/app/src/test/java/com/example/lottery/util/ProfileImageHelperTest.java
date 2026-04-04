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
 * Tests for {@link ProfileImageHelper}.
 *
 * <p>{@code ProfileImageHelper} is shared profile-image infrastructure used by
 * {@code AdminProfileActivity}, {@code EntrantProfileActivity} and
 * {@code OrganizerProfileActivity}. It is not tied to a specific live User Story —
 * the original profile-picture stories (US 01.03.01 / 01.03.02 / 01.03.03) were
 * retired from {@code project_problem_descr.md} but the shared helper remains in
 * use, so these tests cover it as infrastructure rather than as a tagged US.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ProfileImageHelperTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
    }

    // Verify hasCustomImage returns true when selected image is non-empty
    @Test
    public void hasCustomImage_selectedNonEmpty_savedNull_returnsTrue() {
        assertTrue(ProfileImageHelper.hasCustomImage("base64data", null));
    }

    // Verify hasCustomImage returns true when both selected and saved are non-empty
    @Test
    public void hasCustomImage_selectedNonEmpty_savedNonEmpty_returnsTrue() {
        assertTrue(ProfileImageHelper.hasCustomImage("newImage", "savedImage"));
    }

    // Verify hasCustomImage returns true when saved image is non-empty
    @Test
    public void hasCustomImage_selectedNull_savedNonEmpty_returnsTrue() {
        assertTrue(ProfileImageHelper.hasCustomImage(null, "savedImage"));
    }

    // Verify hasCustomImage returns false when both are null
    @Test
    public void hasCustomImage_bothNull_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage(null, null));
    }

    // Verify hasCustomImage returns false when saved is empty
    @Test
    public void hasCustomImage_selectedNull_savedEmpty_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage(null, ""));
    }

    // Verify hasCustomImage returns false when selected is empty
    @Test
    public void hasCustomImage_selectedEmpty_savedNonEmpty_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage("", "savedImage"));
    }

    // Verify hasCustomImage returns false when selected is empty and saved is null
    @Test
    public void hasCustomImage_selectedEmpty_savedNull_returnsFalse() {
        assertFalse(ProfileImageHelper.hasCustomImage("", null));
    }

    // Verify displayProfileImage shows decoded image and hides placeholder
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

    // Verify displayProfileImage shows default avatar when Base64 is null
    @Test
    public void displayProfileImage_nullBase64_showsDefaultAvatar() {
        ImageView imageView = new ImageView(context);
        ImageView placeholder = new ImageView(context);

        ProfileImageHelper.displayProfileImage(null, imageView, placeholder, "Alice");

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(View.GONE, placeholder.getVisibility());
        assertNotNull(imageView.getDrawable());
    }

    // Verify displayProfileImage shows default avatar when Base64 is empty
    @Test
    public void displayProfileImage_emptyBase64_showsDefaultAvatar() {
        ImageView imageView = new ImageView(context);
        ImageView placeholder = new ImageView(context);

        ProfileImageHelper.displayProfileImage("", imageView, placeholder, "Bob");

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertEquals(View.GONE, placeholder.getVisibility());
    }

    // Verify displayProfileImage does not crash on invalid Base64
    @Test
    public void displayProfileImage_invalidBase64_doesNotCrash() {
        ImageView imageView = new ImageView(context);
        ImageView placeholder = new ImageView(context);

        ProfileImageHelper.displayProfileImage("not-valid-base64!!!", imageView, placeholder, "Charlie");

        assertEquals(View.VISIBLE, imageView.getVisibility());
    }

    // Verify displayProfileImage does not crash when placeholder is null
    @Test
    public void displayProfileImage_nullPlaceholder_doesNotCrash() {
        ImageView imageView = new ImageView(context);

        ProfileImageHelper.displayProfileImage(null, imageView, null, "Dave");

        assertEquals(View.VISIBLE, imageView.getVisibility());
    }

    // Verify showDefaultAvatar sets correct visibility on views
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

    // Verify showDefaultAvatar does not crash when seed is null
    @Test
    public void showDefaultAvatar_nullSeed_doesNotCrash() {
        ImageView imageView = new ImageView(context);

        ProfileImageHelper.showDefaultAvatar(imageView, null, null);

        assertEquals(View.VISIBLE, imageView.getVisibility());
        assertNotNull(imageView.getDrawable());
    }

    // Verify showDefaultAvatar produces deterministic result for same seed
    @Test
    public void showDefaultAvatar_sameSeed_deterministic() {
        ImageView iv1 = new ImageView(context);
        ImageView iv2 = new ImageView(context);

        ProfileImageHelper.showDefaultAvatar(iv1, null, "TestUser");
        ProfileImageHelper.showDefaultAvatar(iv2, null, "TestUser");

        assertEquals(View.VISIBLE, iv1.getVisibility());
        assertEquals(View.VISIBLE, iv2.getVisibility());
        assertNotNull(iv1.getDrawable());
        assertNotNull(iv2.getDrawable());
    }

    // Verify processSelectedImage returns null when input stream is null
    @Test
    public void processSelectedImage_nullStream_returnsNull() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/null");
        when(mockResolver.openInputStream(testUri)).thenReturn(null);

        ProfileImageHelper.ProcessedImage result =
                ProfileImageHelper.processSelectedImage(mockResolver, testUri);

        assertNull(result);
    }

    // Verify processSelectedImage propagates FileNotFoundException
    @Test(expected = java.io.FileNotFoundException.class)
    public void processSelectedImage_fileNotFound_propagates() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/ioerror");
        when(mockResolver.openInputStream(testUri))
                .thenThrow(new java.io.FileNotFoundException("File not found"));

        ProfileImageHelper.processSelectedImage(mockResolver, testUri);
    }

    // Verify processSelectedImage does not crash on invalid image bytes
    @Test
    public void processSelectedImage_invalidImageBytes_doesNotCrash() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/garbage");
        when(mockResolver.openInputStream(testUri))
                .thenReturn(new ByteArrayInputStream(new byte[]{0, 1, 2, 3, 4, 5}));

        ProfileImageHelper.processSelectedImage(mockResolver, testUri);
    }

    // Verify processSelectedImage returns valid ProcessedImage for valid input
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

        assertNotNull(result);
        assertNotNull(result.base64);
        assertFalse(result.base64.isEmpty());
        assertNotNull(result.bitmap);
    }

    // Verify processSelectedImage closes the input stream after processing
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

    // Verify processSelectedImage closes the stream even on decode failure
    @Test
    public void processSelectedImage_closesStreamOnDecodeFailure() throws IOException {
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri testUri = Uri.parse("content://test/fail");
        ByteArrayInputStream spyStream = spy(new ByteArrayInputStream(new byte[]{0, 1, 2}));
        when(mockResolver.openInputStream(testUri)).thenReturn(spyStream);

        ProfileImageHelper.processSelectedImage(mockResolver, testUri);

        verify(spyStream).close();
    }

    // Verify openImagePicker launches a gallery pick intent
    @SuppressWarnings("unchecked")
    @Test
    public void openImagePicker_launchesGalleryIntent() {
        ActivityResultLauncher<Intent> mockLauncher = mock(ActivityResultLauncher.class);

        ProfileImageHelper.openImagePicker(mockLauncher);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mockLauncher).launch(captor.capture());
        assertEquals(Intent.ACTION_PICK, captor.getValue().getAction());
    }

    // Verify ProcessedImage stores base64 and bitmap fields correctly
    @Test
    public void processedImage_storesFieldsCorrectly() {
        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        ProfileImageHelper.ProcessedImage result =
                new ProfileImageHelper.ProcessedImage("abc123", bitmap);

        assertEquals("abc123", result.base64);
        assertSame(bitmap, result.bitmap);
    }
}
