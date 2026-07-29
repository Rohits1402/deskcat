package com.deskcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BubblesFitTextTest {

    /** 7 px per char, like a simple monospace font. */
    private static final Bubbles.Measurer MONO = s -> s.length() * 7f;

    @Test
    public void shortTextUnchanged() {
        assertEquals("hi", Bubbles.fitText(MONO, "hi", 100f));
    }

    @Test
    public void exactFitUnchanged() {
        assertEquals("12345", Bubbles.fitText(MONO, "12345", 35f));
    }

    @Test
    public void longTextTruncatedWithEllipsisAndFits() {
        String fitted = Bubbles.fitText(MONO, "hello wonderful world", 70f);
        assertTrue(fitted.endsWith("…"));
        assertTrue(MONO.width(fitted) <= 70f);
        assertEquals("hello wonderful world".substring(0, 9) + "…", fitted);
    }

    @Test
    public void tinyLimitDegradesToEllipsis() {
        assertEquals("…", Bubbles.fitText(MONO, "hello", 7f));
        assertEquals("…", Bubbles.fitText(MONO, "hello", 0f));
    }
}
