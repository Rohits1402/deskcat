package com.deskcat;

/**
 * Timing/fade/style state of one speech bubble. Pure logic (time injected)
 * shared by the local pet and remote peers; rendering lives in {@link Bubbles}.
 */
public class Bubble {

    public static final long DURATION_MS = 5_000;
    public static final long FADE_MS = 500;

    private String text = "";
    private long untilMs;
    private float scale = 1f;
    private int effect;
    private String colorHex = "";

    public void show(String text, long nowMs) {
        show(text, nowMs, 1f, ChatCommands.EFFECT_NONE, "");
    }

    public void show(String text, long nowMs, float scale, int effect,
            String colorHex) {
        this.text = text == null ? "" : text;
        this.untilMs = nowMs + DURATION_MS;
        this.scale = ChatCommands.clampScale(scale);
        this.effect = effect;
        this.colorHex = colorHex == null ? "" : colorHex;
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

    public float scale() {
        return scale;
    }

    public int effect() {
        return effect;
    }

    public String colorHex() {
        return colorHex;
    }
}
