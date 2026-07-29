package com.deskcat;

/**
 * Timing/fade state of one speech bubble. Pure logic (time injected) shared
 * by the local pet and remote peers; rendering lives in {@link Bubbles}.
 */
public class Bubble {

    public static final long DURATION_MS = 5_000;
    public static final long FADE_MS = 500;

    private String text = "";
    private long untilMs;

    public void show(String text, long nowMs) {
        this.text = text == null ? "" : text;
        this.untilMs = nowMs + DURATION_MS;
    }

    public boolean isActive(long nowMs) {
        return !text.isEmpty() && nowMs < untilMs;
    }

    /** 1 while showing, fading to 0 over the final {@link #FADE_MS}. */
    public float alpha(long nowMs) {
        if (!isActive(nowMs)) {
            return 0f;
        }
        return Math.min(1f, (untilMs - nowMs) / (float) FADE_MS);
    }

    /** Dismiss immediately (right-click menu). */
    public void clear() {
        text = "";
        untilMs = 0;
    }

    public String text() {
        return text;
    }
}
