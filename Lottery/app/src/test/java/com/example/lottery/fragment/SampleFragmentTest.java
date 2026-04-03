package com.example.lottery.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for SampleFragment factory methods and argument passing.
 * Covers US 02.05.02: Sample a specified number of attendees from the waiting list.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SampleFragmentTest {

    // US 02.05.02: newInstance() without arguments creates fragment with no default size
    @Test
    public void testNewInstance_noArgs_hasNullArguments() {
        SampleFragment fragment = SampleFragment.newInstance();
        assertNull(fragment.getArguments());
    }

    // US 02.05.02: newInstance(long) stores the default size in arguments
    @Test
    public void testNewInstanceWithDefaultSize_setsCorrectArg() {
        SampleFragment fragment = SampleFragment.newInstance(5);
        Bundle args = fragment.getArguments();

        assertNotNull(args);
        assertTrue(args.containsKey(SampleFragment.ARG_DEFAULT_SIZE));
        assertEquals(5L, args.getLong(SampleFragment.ARG_DEFAULT_SIZE));
    }

    // US 02.05.02: A default size of 0 is stored correctly (edge case: no available slots)
    @Test
    public void testNewInstanceWithZeroDefaultSize_setsZero() {
        SampleFragment fragment = SampleFragment.newInstance(0);
        Bundle args = fragment.getArguments();

        assertNotNull(args);
        assertEquals(0L, args.getLong(SampleFragment.ARG_DEFAULT_SIZE));
    }

    // US 02.05.02: Large default size is stored correctly
    @Test
    public void testNewInstanceWithLargeDefaultSize_setsCorrectly() {
        SampleFragment fragment = SampleFragment.newInstance(1000);
        Bundle args = fragment.getArguments();

        assertNotNull(args);
        assertEquals(1000L, args.getLong(SampleFragment.ARG_DEFAULT_SIZE));
    }

    // US 02.05.02: No-arg factory and parameterized factory produce distinct argument states
    @Test
    public void testFactoryMethodsProduceDistinctBundles() {
        SampleFragment noArgs = SampleFragment.newInstance();
        SampleFragment withArgs = SampleFragment.newInstance(7);

        assertNull(noArgs.getArguments());
        assertNotNull(withArgs.getArguments());
        assertEquals(7L, withArgs.getArguments().getLong(SampleFragment.ARG_DEFAULT_SIZE));
    }
}
