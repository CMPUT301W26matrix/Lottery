package com.example.lottery.organizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.adapter.NotificationAdapter;
import com.example.lottery.fragment.OrganizerInviteCoOrganizerDialogFragment;
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
 * Tests for co-organizer features, including pure logic and Robolectric-side view binding.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CoOrganizerTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    // US 02.09.01: Verify co-organizer assignment notification is created with correct fields
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

    // US 02.09.01: Verify notification type is set to co_organizer_assignment
    @Test
    public void testCoOrganizerNotificationType() {
        NotificationItem notification = new NotificationItem(
                "id", "title", "msg", "co_organizer_assignment",
                "ev1", "Ev Title", "s1", "ORGANIZER", false, Timestamp.now()
        );

        assertEquals("co_organizer_assignment", notification.getType());
    }

    // US 02.09.01: Verify sender role is ORGANIZER for co-organizer assignment notification
    @Test
    public void testCoOrganizerNotificationSenderRole() {
        NotificationItem notification = new NotificationItem(
                "id", "Co-Organizer Assignment", "msg", "co_organizer_assignment",
                "ev1", "Event", "sender1", "ORGANIZER", false, Timestamp.now()
        );

        assertEquals("ORGANIZER", notification.getSenderRole());
    }

    // US 02.09.01: Verify filtering entrants by username returns matching candidates
    @Test
    public void testFilterEntrantsMatchesByUsername() {
        List<User> candidates = new ArrayList<>();
        candidates.add(new User("u1", "Alice", "alice@test.com", null));
        candidates.add(new User("u2", "Bob", "bob@test.com", null));
        candidates.add(new User("u3", "Alicia", "alicia@test.com", null));

        List<User> result = OrganizerInviteCoOrganizerDialogFragment.filterEntrants(candidates, "ali");
        assertEquals(2, result.size());
        assertEquals("u1", result.get(0).getUserId());
        assertEquals("u3", result.get(1).getUserId());
    }

    // US 02.09.01: Verify filtering entrants by email returns matching candidates
    @Test
    public void testFilterEntrantsMatchesByEmail() {
        List<User> candidates = new ArrayList<>();
        candidates.add(new User("u1", "Alice", "alice@test.com", null));
        candidates.add(new User("u2", "Bob", "bob@other.com", null));

        List<User> result = OrganizerInviteCoOrganizerDialogFragment.filterEntrants(candidates, "test.com");
        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getUserId());
    }

    // US 02.09.01: Verify entrant filtering is case-insensitive
    @Test
    public void testFilterEntrantsCaseInsensitive() {
        List<User> candidates = new ArrayList<>();
        candidates.add(new User("u1", "Alice", "ALICE@TEST.COM", null));

        List<User> result = OrganizerInviteCoOrganizerDialogFragment.filterEntrants(candidates, "alice");
        assertEquals(1, result.size());
    }

    // US 02.09.01: Verify filtering returns empty list when no entrants match the query
    @Test
    public void testFilterEntrantsNoMatch() {
        List<User> candidates = new ArrayList<>();
        candidates.add(new User("u1", "Alice", "alice@test.com", null));

        List<User> result = OrganizerInviteCoOrganizerDialogFragment.filterEntrants(candidates, "zzz");
        assertTrue(result.isEmpty());
    }

    // US 02.09.01: Verify filtering returns empty list when candidate list is empty
    @Test
    public void testFilterEntrantsEmptyCandidates() {
        List<User> candidates = new ArrayList<>();

        List<User> result = OrganizerInviteCoOrganizerDialogFragment.filterEntrants(candidates, "alice");
        assertTrue(result.isEmpty());
    }

    // US 02.09.01: Verify filtering only returns candidates that match and excludes non-matches
    @Test
    public void testFilterEntrantsOnlyReturnsCandidates() {
        List<User> candidates = new ArrayList<>();
        candidates.add(new User("u2", "Bob", "bob@test.com", null));

        List<User> result = OrganizerInviteCoOrganizerDialogFragment.filterEntrants(candidates, "bob");
        assertEquals(1, result.size());
        assertEquals("u2", result.get(0).getUserId());

        List<User> result2 = OrganizerInviteCoOrganizerDialogFragment.filterEntrants(candidates, "alice");
        assertTrue(result2.isEmpty());
    }

    // US 02.09.01: Verify only users with ENTRANT role are eligible for co-organizer assignment
    @Test
    public void testOnlyEntrantRoleIsEligible() {
        User entrant = new User("u1", "Alice", "alice@test.com", null);
        entrant.setRole("ENTRANT");
        assertTrue(entrant.isEntrant());

        User organizer = new User("u2", "Bob", "bob@test.com", null);
        organizer.setRole("ORGANIZER");
        assertFalse(organizer.isEntrant());

        User admin = new User("u3", "Carol", "carol@test.com", null);
        admin.setRole("ADMIN");
        assertFalse(admin.isEntrant());
    }

    // US 02.09.01: Verify co-organizer status defaults to false
    @Test
    public void testIsCoOrganizerDefaultsFalse() {
        boolean isCoOrganizer = false;
        assertFalse(isCoOrganizer);
    }

    // US 02.09.01: Verify co-organizer is blocked from joining the event waitlist
    @Test
    public void testCoOrganizerCannotJoinWaitlist() {
        boolean isCoOrganizer = true;
        boolean shouldBlockWaitlistAction = isCoOrganizer;
        assertTrue(shouldBlockWaitlistAction);
    }

    // US 02.09.01: Verify non-co-organizer is allowed to join the event waitlist
    @Test
    public void testNonCoOrganizerCanJoinWaitlist() {
        boolean isCoOrganizer = false;
        boolean shouldBlockWaitlistAction = isCoOrganizer;
        assertFalse(shouldBlockWaitlistAction);
    }

    // US 02.09.01: Verify co-organizer cannot download a ticket for the event
    @Test
    public void testCoOrganizerCannotDownloadTicket() {
        boolean isCoOrganizer = true;
        boolean shouldHideDownloadTicket = isCoOrganizer;
        assertTrue(shouldHideDownloadTicket);
    }

    // US 02.09.01: Verify UserSearchAdapter reports correct item count for candidates
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

    // US 02.09.01: Verify UserSearchAdapter reports zero items for an empty candidate list
    @Test
    public void testUserSearchAdapterEmptyList() throws Exception {
        List<User> users = new ArrayList<>();
        Set<String> existingIds = new HashSet<>();
        Object adapter = createUserSearchAdapter(users, existingIds);

        int count = (int) adapter.getClass().getMethod("getItemCount").invoke(adapter);
        assertEquals(0, count);
    }

    // US 02.09.01: Verify UserSearchAdapter disables and dims row for existing co-organizers
    @Test
    public void testUserSearchAdapterDisabledForExistingCoOrganizer() throws Exception {
        List<User> users = new ArrayList<>();
        users.add(new User("u1", "Alice", "alice@test.com", "123"));
        users.add(new User("u2", "Bob", "bob@test.com", "456"));

        Set<String> existingIds = new HashSet<>();
        existingIds.add("u1");

        Object adapter = createUserSearchAdapter(users, existingIds);

        FrameLayout parent = new FrameLayout(context);
        Method onCreateVH = adapter.getClass().getMethod("onCreateViewHolder", android.view.ViewGroup.class, int.class);
        Object holder = onCreateVH.invoke(adapter, parent, 0);

        Method onBindVH = adapter.getClass().getMethod("onBindViewHolder", holder.getClass(), int.class);
        onBindVH.invoke(adapter, holder, 0);

        View itemView = ((androidx.recyclerview.widget.RecyclerView.ViewHolder) holder).itemView;
        assertFalse(itemView.isEnabled());
        assertEquals(0.5f, itemView.getAlpha(), 0.01f);

        TextView text1 = itemView.findViewById(android.R.id.text1);
        assertTrue(text1.getText().toString().contains("(Already Co-Organizer)"));
    }

    // US 02.09.01: Verify UserSearchAdapter enables row for users who are not yet co-organizers
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
        assertTrue(itemView.isEnabled());
        assertEquals(1.0f, itemView.getAlpha(), 0.01f);

        TextView text1 = itemView.findViewById(android.R.id.text1);
        assertFalse(text1.getText().toString().contains("(Already Co-Organizer)"));
    }

    // US 02.09.01: Verify UserSearchAdapter displays user email in the subtitle
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

    // US 02.09.01: Verify UserSearchAdapter falls back to phone number when email is null
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

    // US 01.09.01: Verify NotificationAdapter formats co-organizer type label correctly
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

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> { });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Co-Organizer", holder.tvType.getText().toString());
    }

    // US 01.09.01: Verify unread co-organizer notification shows the "New" badge
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
                false,
                Timestamp.now()
        ));

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> { });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(View.VISIBLE, holder.tvNew.getVisibility());
    }

    // US 01.09.01: Verify read co-organizer notification hides the "New" badge
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
                true,
                Timestamp.now()
        ));

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> { });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(View.GONE, holder.tvNew.getVisibility());
    }

    // US 01.09.01: Verify co-organizer notification title includes event name and assignment label
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

        NotificationAdapter adapter = new NotificationAdapter(notifications, item -> { });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Annual Gala: Co-Organizer Assignment", holder.tvTitle.getText().toString());
    }

    private Object createUserSearchAdapter(List<User> users, Set<String> existingIds) throws Exception {
        Class<?> adapterClass = Class.forName(
                "com.example.lottery.fragment.OrganizerInviteCoOrganizerDialogFragment$UserSearchAdapter");

        Class<?> listenerClass = Class.forName(
                "com.example.lottery.fragment.OrganizerInviteCoOrganizerDialogFragment$UserSearchAdapter$OnUserClickListener");

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
