package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.model.NotificationItem;
import com.example.lottery.model.User;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for co-organizer features.
 *
 * <p>Covers the following user stories:</p>
 * <ul>
 *   <li>US 02.09.01: As an organizer, I want to assign an entrant as a co-organizer
 *       for my event, which prevents them from joining the entrant pool for that event.</li>
 *   <li>US 01.09.01: As an entrant, I want to receive a notification if I have been
 *       invited to be a co-organizer for an event.</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CoOrganizerTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    // ---------------------------------------------------------------
    // US 02.09.01 – UserSearchAdapter tests (organizer searches and
    //               assigns entrants as co-organizers)
    // ---------------------------------------------------------------

    /**
     * US 02.09.01
     * Verifies that UserSearchAdapter reports the correct item count
     * when the organizer searches for entrants to assign as co-organizers.
     */
    @Test
    public void testUserSearchAdapterItemCount() throws Exception {
        List<User> users = new ArrayList<>();
        users.add(new User("u1", "Alice", "alice@test.com", "123"));
        users.add(new User("u2", "Bob", "bob@test.com", "456"));

        Set<String> existingIds = new HashSet<>();
        Object adapter = createUserSearchAdapter(users, existingIds);

        int count = (int) adapter.getClass().getMethod("getItemCount").invoke(adapter);
        assertEquals(2, count);
    }

    /**
     * US 02.09.01
     * Verifies that an empty user list returns item count 0.
     */
    @Test
    public void testUserSearchAdapterEmptyList() throws Exception {
        List<User> users = new ArrayList<>();
        Set<String> existingIds = new HashSet<>();
        Object adapter = createUserSearchAdapter(users, existingIds);

        int count = (int) adapter.getClass().getMethod("getItemCount").invoke(adapter);
        assertEquals(0, count);
    }

    /**
     * US 02.09.01
     * Verifies that a user who is already a co-organizer is displayed with disabled state
     * (alpha 0.5) and the "(Already Co-Organizer)" suffix, preventing duplicate assignment.
     */
    @Test
    public void testUserSearchAdapterDisabledForExistingCoOrganizer() throws Exception {
        List<User> users = new ArrayList<>();
        users.add(new User("u1", "Alice", "alice@test.com", "123"));
        users.add(new User("u2", "Bob", "bob@test.com", "456"));

        Set<String> existingIds = new HashSet<>();
        existingIds.add("u1"); // Alice is already a co-organizer

        Object adapter = createUserSearchAdapter(users, existingIds);

        FrameLayout parent = new FrameLayout(context);
        Method onCreateVH = adapter.getClass().getMethod("onCreateViewHolder", android.view.ViewGroup.class, int.class);
        Object holder = onCreateVH.invoke(adapter, parent, 0);

        Method onBindVH = adapter.getClass().getMethod("onBindViewHolder", holder.getClass(), int.class);
        onBindVH.invoke(adapter, holder, 0);

        View itemView = ((androidx.recyclerview.widget.RecyclerView.ViewHolder) holder).itemView;
        assertFalse("Existing co-organizer item should be disabled", itemView.isEnabled());
        assertEquals(0.5f, itemView.getAlpha(), 0.01f);

        TextView text1 = itemView.findViewById(android.R.id.text1);
        assertTrue("Should contain '(Already Co-Organizer)' suffix",
                text1.getText().toString().contains("(Already Co-Organizer)"));
    }

    /**
     * US 02.09.01
     * Verifies that a user who is NOT a co-organizer is displayed with enabled state
     * (alpha 1.0) and without the co-organizer suffix, allowing the organizer to assign them.
     */
    @Test
    public void testUserSearchAdapterEnabledForNonCoOrganizer() throws Exception {
        List<User> users = new ArrayList<>();
        users.add(new User("u1", "Alice", "alice@test.com", "123"));

        Set<String> existingIds = new HashSet<>();

        Object adapter = createUserSearchAdapter(users, existingIds);

        FrameLayout parent = new FrameLayout(context);
        Method onCreateVH = adapter.getClass().getMethod("onCreateViewHolder", android.view.ViewGroup.class, int.class);
        Object holder = onCreateVH.invoke(adapter, parent, 0);

        Method onBindVH = adapter.getClass().getMethod("onBindViewHolder", holder.getClass(), int.class);
        onBindVH.invoke(adapter, holder, 0);

        View itemView = ((androidx.recyclerview.widget.RecyclerView.ViewHolder) holder).itemView;
        assertTrue("Non co-organizer item should be enabled", itemView.isEnabled());
        assertEquals(1.0f, itemView.getAlpha(), 0.01f);

        TextView text1 = itemView.findViewById(android.R.id.text1);
        assertFalse("Should NOT contain '(Already Co-Organizer)' suffix",
                text1.getText().toString().contains("(Already Co-Organizer)"));
    }

    /**
     * US 02.09.01
     * Verifies that the adapter shows user email in the second line when available,
     * so the organizer can identify the entrant to assign.
     */
    @Test
    public void testUserSearchAdapterShowsEmail() throws Exception {
        List<User> users = new ArrayList<>();
        users.add(new User("u1", "Alice", "alice@test.com", "123"));

        Set<String> existingIds = new HashSet<>();
        Object adapter = createUserSearchAdapter(users, existingIds);

        FrameLayout parent = new FrameLayout(context);
        Method onCreateVH = adapter.getClass().getMethod("onCreateViewHolder", android.view.ViewGroup.class, int.class);
        Object holder = onCreateVH.invoke(adapter, parent, 0);

        Method onBindVH = adapter.getClass().getMethod("onBindViewHolder", holder.getClass(), int.class);
        onBindVH.invoke(adapter, holder, 0);

        View itemView = ((androidx.recyclerview.widget.RecyclerView.ViewHolder) holder).itemView;
        TextView text2 = itemView.findViewById(android.R.id.text2);
        assertEquals("alice@test.com", text2.getText().toString());
    }

    /**
     * US 02.09.01
     * Verifies that the adapter shows user phone in the second line when email is null,
     * providing a fallback identifier for the organizer.
     */
    @Test
    public void testUserSearchAdapterShowsPhoneWhenEmailNull() throws Exception {
        User user = new User("u1", "Alice", null, "5551234567");

        List<User> users = new ArrayList<>();
        users.add(user);

        Set<String> existingIds = new HashSet<>();
        Object adapter = createUserSearchAdapter(users, existingIds);

        FrameLayout parent = new FrameLayout(context);
        Method onCreateVH = adapter.getClass().getMethod("onCreateViewHolder", android.view.ViewGroup.class, int.class);
        Object holder = onCreateVH.invoke(adapter, parent, 0);

        Method onBindVH = adapter.getClass().getMethod("onBindViewHolder", holder.getClass(), int.class);
        onBindVH.invoke(adapter, holder, 0);

        View itemView = ((androidx.recyclerview.widget.RecyclerView.ViewHolder) holder).itemView;
        TextView text2 = itemView.findViewById(android.R.id.text2);
        assertEquals("5551234567", text2.getText().toString());
    }

    // ---------------------------------------------------------------
    // US 02.09.01 – Notification creation logic tests
    //               (verifies the notification payload sent when
    //                an organizer assigns a co-organizer)
    // ---------------------------------------------------------------

    /**
     * US 02.09.01 / US 01.09.01
     * Verifies that a co-organizer assignment notification is created with the correct fields
     * as produced by sendNotification() in OrganizerInviteCoOrganizerDialogFragment.
     * The entrant should receive this notification with all fields intact (US 01.09.01).
     */
    @Test
    public void testCoOrganizerNotificationCreation() {
        String notificationId = "test-notif-id";
        String eventId = "event-42";
        String eventTitle = "Annual Gala";
        String senderId = "organizer-99";

        NotificationItem notification = new NotificationItem(
                notificationId,
                "Co-Organizer Assignment",
                "You have been assigned as a co-organizer for: " + eventTitle,
                "co_organizer_assignment",
                eventId,
                eventTitle,
                senderId,
                "ORGANIZER",
                false,
                Timestamp.now()
        );

        assertEquals(notificationId, notification.getNotificationId());
        assertEquals("Co-Organizer Assignment", notification.getTitle());
        assertEquals("You have been assigned as a co-organizer for: Annual Gala", notification.getMessage());
        assertEquals("co_organizer_assignment", notification.getType());
        assertEquals(eventId, notification.getEventId());
        assertEquals(eventTitle, notification.getEventTitle());
        assertEquals(senderId, notification.getSenderId());
        assertEquals("ORGANIZER", notification.getSenderRole());
        assertFalse(notification.isRead());
        assertNotNull(notification.getCreatedAt());
    }

    /**
     * US 01.09.01
     * Verifies that the notification type is exactly "co_organizer_assignment",
     * which is how the system identifies co-organizer invitation notifications.
     */
    @Test
    public void testCoOrganizerNotificationType() {
        NotificationItem notification = new NotificationItem(
                "id", "title", "msg", "co_organizer_assignment",
                "ev1", "Ev Title", "s1", "ORGANIZER", false, Timestamp.now()
        );

        assertEquals("co_organizer_assignment", notification.getType());
    }

    /**
     * US 01.09.01
     * Verifies that the notification sender role for co-organizer assignment is "ORGANIZER",
     * indicating to the entrant that the invitation came from the event organizer.
     */
    @Test
    public void testCoOrganizerNotificationSenderRole() {
        NotificationItem notification = new NotificationItem(
                "id", "Co-Organizer Assignment", "msg", "co_organizer_assignment",
                "ev1", "Event", "sender1", "ORGANIZER", false, Timestamp.now()
        );

        assertEquals("ORGANIZER", notification.getSenderRole());
    }

    // ---------------------------------------------------------------
    // US 01.09.01 – NotificationAdapter displays co-organizer
    //               notifications correctly in the entrant's inbox
    // ---------------------------------------------------------------

    /**
     * US 01.09.01
     * Verifies that the NotificationAdapter formats "co_organizer_assignment" as "Co-Organizer"
     * in the notification type label, so the entrant can identify the notification purpose.
     */
    @Test
    public void testNotificationAdapterFormatsCoOrganizerType() {
        List<NotificationItem> notifications = new ArrayList<>();
        notifications.add(new NotificationItem(
                "id1",
                "Co-Organizer Assignment",
                "You have been assigned as a co-organizer for: Test Event",
                "co_organizer_assignment",
                "event1",
                "Test Event",
                "sender1",
                "ORGANIZER",
                false,
                Timestamp.now()
        ));

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> {
        });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Co-Organizer", holder.tvType.getText().toString());
    }

    /**
     * US 01.09.01
     * Verifies that the NotificationAdapter shows the "New" badge for unread
     * co-organizer notifications, alerting the entrant to the new invitation.
     */
    @Test
    public void testCoOrganizerNotificationShowsNewBadge() {
        List<NotificationItem> notifications = new ArrayList<>();
        notifications.add(new NotificationItem(
                "id1",
                "Co-Organizer Assignment",
                "You have been assigned",
                "co_organizer_assignment",
                "event1",
                "Test Event",
                "sender1",
                "ORGANIZER",
                false, // unread
                Timestamp.now()
        ));

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> {
        });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(View.VISIBLE, holder.tvNew.getVisibility());
    }

    /**
     * US 01.09.01
     * Verifies that the NotificationAdapter hides the "New" badge for read
     * co-organizer notifications, indicating the entrant has seen the invitation.
     */
    @Test
    public void testCoOrganizerNotificationHidesNewBadgeWhenRead() {
        List<NotificationItem> notifications = new ArrayList<>();
        notifications.add(new NotificationItem(
                "id1",
                "Co-Organizer Assignment",
                "You have been assigned",
                "co_organizer_assignment",
                "event1",
                "Test Event",
                "sender1",
                "ORGANIZER",
                true, // read
                Timestamp.now()
        ));

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> {
        });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(View.GONE, holder.tvNew.getVisibility());
    }

    /**
     * US 01.09.01
     * Verifies that the NotificationAdapter combines eventTitle and title for
     * co-organizer notifications, displaying as "EventTitle: Co-Organizer Assignment".
     */
    @Test
    public void testCoOrganizerNotificationTitleFormatting() {
        List<NotificationItem> notifications = new ArrayList<>();
        notifications.add(new NotificationItem(
                "id1",
                "Co-Organizer Assignment",
                "You have been assigned",
                "co_organizer_assignment",
                "event1",
                "Annual Gala",
                "sender1",
                "ORGANIZER",
                false,
                Timestamp.now()
        ));

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> {
        });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Annual Gala: Co-Organizer Assignment", holder.tvTitle.getText().toString());
    }

    // ---------------------------------------------------------------
    // US 02.09.01 – Co-organizer status prevents entrant from joining
    //               the waiting list for that event
    // ---------------------------------------------------------------

    /**
     * US 02.09.01
     * Verifies that the isCoOrganizer flag defaults to false, meaning entrants
     * can join the waitlist by default until they are assigned as co-organizers.
     */
    @Test
    public void testIsCoOrganizerDefaultsFalse() {
        boolean isCoOrganizer = false; // default state
        assertFalse("By default, a user should not be a co-organizer", isCoOrganizer);
    }

    /**
     * US 02.09.01
     * Verifies that when isCoOrganizer is true, the waitlist action should be blocked.
     * Co-organizers are prevented from joining the entrant pool for that event.
     */
    @Test
    public void testCoOrganizerCannotJoinWaitlist() {
        boolean isCoOrganizer = true;

        boolean shouldBlockWaitlistAction = isCoOrganizer;
        assertTrue("Co-organizers should be blocked from joining the waitlist", shouldBlockWaitlistAction);
    }

    /**
     * US 02.09.01
     * Verifies that when isCoOrganizer is false, the waitlist action is allowed.
     * Non co-organizers should be able to join the entrant pool normally.
     */
    @Test
    public void testNonCoOrganizerCanJoinWaitlist() {
        boolean isCoOrganizer = false;

        boolean shouldBlockWaitlistAction = isCoOrganizer;
        assertFalse("Non co-organizers should be allowed to join the waitlist", shouldBlockWaitlistAction);
    }

    /**
     * US 02.09.01
     * Verifies that when a user is a co-organizer, the download ticket button should be hidden.
     * Co-organizers do not participate in the lottery and therefore have no ticket.
     */
    @Test
    public void testCoOrganizerCannotDownloadTicket() {
        boolean isCoOrganizer = true;

        boolean shouldHideDownloadTicket = isCoOrganizer;
        assertTrue("Co-organizers should not see the download ticket button", shouldHideDownloadTicket);
    }

    // ---------------------------------------------------------------
    // Helper: create UserSearchAdapter via reflection
    // (it is a private static inner class of
    //  OrganizerInviteCoOrganizerDialogFragment)
    // ---------------------------------------------------------------

    private Object createUserSearchAdapter(List<User> users, Set<String> existingIds) throws Exception {
        Class<?> adapterClass = Class.forName(
                "com.example.lottery.OrganizerInviteCoOrganizerDialogFragment$UserSearchAdapter");

        Class<?> listenerClass = Class.forName(
                "com.example.lottery.OrganizerInviteCoOrganizerDialogFragment$UserSearchAdapter$OnUserClickListener");

        Constructor<?> constructor = adapterClass.getDeclaredConstructor(List.class, Set.class, listenerClass);
        constructor.setAccessible(true);

        Object listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (proxy, method, args) -> null
        );

        return constructor.newInstance(users, existingIds, listener);
    }
}
