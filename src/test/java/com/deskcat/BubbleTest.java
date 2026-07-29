package com.deskcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BubbleTest {

    @Test
    public void inactiveUntilShown() {
        Bubble b = new Bubble();
        assertFalse(b.isActive(0));
        assertEquals(0f, b.alpha(0), 0f);
    }

    @Test
    public void activeForDurationThenExpires() {
        Bubble b = new Bubble();
        b.show("hi", 1000);
        assertTrue(b.isActive(1000));
        assertTrue(b.isActive(1000 + Bubble.DURATION_MS - 1));
        assertFalse(b.isActive(1000 + Bubble.DURATION_MS));
    }

    @Test
    public void alphaFullThenFadesToZero() {
        Bubble b = new Bubble();
        b.show("hi", 0);
        assertEquals(1f, b.alpha(0), 0f);
        assertEquals(1f, b.alpha(Bubble.DURATION_MS - Bubble.FADE_MS), 0f);
        assertEquals(0.5f, b.alpha(Bubble.DURATION_MS - Bubble.FADE_MS / 2), 0.01f);
        assertEquals(0f, b.alpha(Bubble.DURATION_MS), 0f);
    }

    @Test
    public void newMessageResetsTimer() {
        Bubble b = new Bubble();
        b.show("one", 0);
        b.show("two", 4000);
        assertTrue(b.isActive(4000 + Bubble.DURATION_MS - 1));
        assertEquals("two", b.text());
    }

    @Test
    public void clearDismissesImmediately() {
        Bubble b = new Bubble();
        b.show("hi", 0);
        b.clear();
        assertFalse(b.isActive(1));
        assertEquals("", b.text());
    }

    @Test
    public void emptyOrNullTextNeverActive() {
        Bubble b = new Bubble();
        b.show("", 0);
        assertFalse(b.isActive(1));
        b.show(null, 0);
        assertFalse(b.isActive(1));
    }
}
