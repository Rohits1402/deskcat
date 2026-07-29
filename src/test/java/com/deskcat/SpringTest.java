package com.deskcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpringTest {

    @Test
    public void settlesAtTarget() {
        Spring s = new Spring(160f, 12f, 0.7f, 1.5f);
        for (int i = 0; i < 600; i++) {
            s.update(1.2f, 1 / 60f);
        }
        assertEquals(1.2f, s.value(), 0.01f);
    }

    @Test
    public void kickWobblesThenDecays() {
        Spring s = new Spring(160f, 12f, 0.7f, 1.5f);
        s.kick(5f);
        float peak = 1f;
        for (int i = 0; i < 600; i++) {
            peak = Math.max(peak, s.update(1f, 1 / 60f));
        }
        assertTrue("kick should overshoot", peak > 1.05f);
        assertEquals(1f, s.value(), 0.01f);
    }

    @Test
    public void staysWithinClampBounds() {
        Spring s = new Spring(160f, 12f, 0.7f, 1.5f);
        s.kick(100f);
        for (int i = 0; i < 600; i++) {
            float v = s.update(1f, 1 / 60f);
            assertTrue(v >= 0.7f && v <= 1.5f);
        }
    }
}
