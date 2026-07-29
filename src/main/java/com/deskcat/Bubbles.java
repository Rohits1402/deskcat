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

    /**
     * @param alpha 0..1 fade
     * @param winW  window width in pixels; the bubble is centered and clamped
     */
    public static void draw(SpriteBatch batch, BitmapFont font, Texture px,
            String text, float alpha, int winW, float topY) {
        Measurer m = s -> {
            LAYOUT.setText(font, s);
            return LAYOUT.width;
        };
        float maxTextW = winW - 28;
        List<String> lines = wrap(m, text, maxTextW, MAX_LINES);

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

        font.setColor(C_OUTLINE.r, C_OUTLINE.g, C_OUTLINE.b, alpha);
        for (int i = 0; i < lines.size(); i++) {
            font.draw(batch, lines.get(i), bx + 7, by + bh - 4 - i * lineH);
        }
        font.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
    }

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
