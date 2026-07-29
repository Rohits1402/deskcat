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

    @Test
    public void wrapShortTextSingleLine() {
        assertEquals(java.util.Arrays.asList("hi there"),
                Bubbles.wrap(MONO, "hi there", 100f, 4));
    }

    @Test
    public void wrapBreaksAtWordsAndEveryLineFits() {
        java.util.List<String> lines =
                Bubbles.wrap(MONO, "the quick brown fox jumps over", 80f, 4);
        assertTrue(lines.size() > 1);
        for (String line : lines) {
            assertTrue(MONO.width(line) <= 80f);
        }
        assertEquals("the quick brown fox jumps over",
                String.join(" ", lines));
    }

    @Test
    public void wrapHardSplitsOversizedWords() {
        java.util.List<String> lines =
                Bubbles.wrap(MONO, "abcdefghijklmnop", 35f, 4);
        assertTrue(lines.size() > 1);
        for (String line : lines) {
            assertTrue(MONO.width(line) <= 35f);
        }
        assertEquals("abcdefghijklmnop", String.join("", lines));
    }

    @Test
    public void wrapCapsLinesWithEllipsis() {
        java.util.List<String> lines = Bubbles.wrap(MONO,
                "one two three four five six seven eight nine ten", 35f, 3);
        assertEquals(3, lines.size());
        assertTrue(lines.get(2).endsWith("…"));
        assertTrue(MONO.width(lines.get(2)) <= 35f);
    }

    @Test
    public void wrapEmptyTextGivesOneEmptyLine() {
        assertEquals(java.util.Arrays.asList(""),
                Bubbles.wrap(MONO, "", 35f, 4));
    }
}
