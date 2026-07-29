package com.deskcat;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Pixel-style speech bubble drawn in window-pixel coordinates at the top of a
 * pet window. Shared by the local pet and remote-peer windows. Text wraps
 * onto up to {@link #MAX_LINES} lines; only past that does it get an ellipsis.
 */
public final class Bubbles {

    static final int MAX_LINES = 4;

    private static final Color C_OUTLINE = Color.valueOf("26202A");
    private static final GlyphLayout LAYOUT = new GlyphLayout();

    /** Text-width oracle, injectable so wrapping is testable without GL. */
    public interface Measurer {
        float width(String s);
    }

    private Bubbles() {
    }

    /** Back-compat: unstyled bubble. */
    public static void draw(SpriteBatch batch, BitmapFont font, Texture px,
            String text, float alpha, int winW, float topY) {
        draw(batch, font, px, text, alpha, winW, topY, 1f,
                ChatCommands.EFFECT_NONE, "", 0f);
    }

    /**
     * @param alpha    0..1 fade
     * @param winW     window width in pixels; the bubble is centered/clamped
     * @param scale    text scale from chat commands (/big, /size N …)
     * @param effect   ChatCommands.EFFECT_* (shake, rainbow)
     * @param colorHex RRGGBB text color, "" for the default
     * @param time     caller's animation clock, drives shake/rainbow
     */
    public static void draw(SpriteBatch batch, BitmapFont font, Texture px,
            String text, float alpha, int winW, float topY, float scale,
            int effect, String colorHex, float time) {
        font.getData().setScale(scale);
        try {
            Measurer m = s -> {
                LAYOUT.setText(font, s);
                return LAYOUT.width;
            };
            float maxTextW = winW - 28;
            // bigger text gets fewer lines so the bubble stays inside the window
            int maxLines = scale >= 2f ? 2 : (scale > 1.2f ? 3 : MAX_LINES);
            List<String> lines = wrap(m, text, maxTextW, maxLines);

            float lineH = font.getLineHeight();
            float tw = 0;
            for (String line : lines) {
                tw = Math.max(tw, m.width(line));
            }
            float bw = tw + 14, bh = lines.size() * lineH + 10;
            float bx = (winW - bw) / 2f;
            float by = topY - bh;

            batch.setColor(1f, 1f, 1f, 0.95f * alpha);
            batch.draw(px, bx, by, bw, bh);
            batch.setColor(C_OUTLINE.r, C_OUTLINE.g, C_OUTLINE.b, alpha);
            batch.draw(px, bx, by - 1, bw, 1);
            batch.draw(px, bx, by + bh, bw, 1);
            batch.draw(px, bx - 1, by, 1, bh);
            batch.draw(px, bx + bw, by, 1, bh);
            // tail nub pointing down at the pet
            batch.setColor(1f, 1f, 1f, 0.95f * alpha);
            batch.draw(px, winW / 2f - 3, by - 4, 6, 4);
            batch.setColor(C_OUTLINE.r, C_OUTLINE.g, C_OUTLINE.b, alpha);
            batch.draw(px, winW / 2f - 4, by - 4, 1, 4);
            batch.draw(px, winW / 2f + 3, by - 4, 1, 4);
            batch.draw(px, winW / 2f - 3, by - 5, 6, 1);

            Color base = C_OUTLINE;
            if (!colorHex.isEmpty()) {
                try {
                    base = Color.valueOf(colorHex);
                } catch (Throwable ignored) {
                    // bad hex from the wire: keep the default
                }
            }
            for (int i = 0; i < lines.size(); i++) {
                float dx = 0, dy = 0;
                Color c = base;
                if (effect == ChatCommands.EFFECT_SHAKE) {
                    dx = (float) Math.sin(time * 45f + i * 1.7f) * 1.5f * scale;
                    dy = (float) Math.cos(time * 38f + i * 2.3f) * 1.2f * scale;
                } else if (effect == ChatCommands.EFFECT_RAINBOW) {
                    c = TMP.fromHsv((time * 120f + i * 40f) % 360f, 0.8f, 0.85f);
                }
                font.setColor(c.r, c.g, c.b, alpha);
                font.draw(batch, lines.get(i),
                        bx + 7 + dx, by + bh - 4 - i * lineH + dy);
            }
            font.setColor(Color.WHITE);
            batch.setColor(Color.WHITE);
        } finally {
            font.getData().setScale(1f);   // the font is shared — always restore
        }
    }

    private static final Color TMP = new Color();

    /**
     * Greedy word wrap. Words wider than a whole line are hard-split; text
     * that would exceed maxLines is cut with an ellipsis on the last line.
     */
    static List<String> wrap(Measurer m, String text, float maxW, int maxLines) {
        List<String> lines = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            // hard-split words that could never fit on one line
            while (m.width(word) > maxW && word.length() > 1) {
                int cut = word.length() - 1;
                while (cut > 1 && m.width(word.substring(0, cut)) > maxW) {
                    cut--;
                }
                String head = word.substring(0, cut);
                if (cur.length() > 0) {
                    lines.add(cur.toString());
                    cur.setLength(0);
                }
                lines.add(head);
                word = word.substring(cut);
            }
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (m.width(candidate) <= maxW || cur.length() == 0) {
                cur.setLength(0);
                cur.append(candidate);
            } else {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        if (lines.size() > maxLines) {
            String last = lines.get(maxLines - 1);
            lines = new ArrayList<String>(lines.subList(0, maxLines));
            lines.set(maxLines - 1, fitText(m, last + "…", maxW));
        }
        return lines;
    }

    /** Truncate with an ellipsis so a single line always fits. */
    static String fitText(Measurer m, String text, float maxW) {
        if (m.width(text) <= maxW) {
            return text;
        }
        for (int len = text.length() - 1; len > 0; len--) {
            String candidate = text.substring(0, len) + "…";
            if (m.width(candidate) <= maxW) {
                return candidate;
            }
        }
        return "…";
    }
}
