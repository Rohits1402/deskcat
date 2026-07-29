package com.deskcat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Pixel-style speech bubble drawn in window-pixel coordinates at the top of a
 * pet window. Shared by the local pet and remote-peer windows.
 */
public final class Bubbles {

    private static final Color C_OUTLINE = Color.valueOf("26202A");
    private static final GlyphLayout LAYOUT = new GlyphLayout();

    /** Text-width oracle, injectable so fitting is testable without GL. */
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
        String shown = fitText(font, text, winW - 28);
        LAYOUT.setText(font, shown);
        float tw = LAYOUT.width, th = LAYOUT.height;
        float bw = tw + 14, bh = th + 12;
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
        font.draw(batch, shown, bx + 7, by + bh - 5);
        font.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
    }

    private static String fitText(BitmapFont font, String text, float maxW) {
        return fitText(s -> {
            LAYOUT.setText(font, s);
            return LAYOUT.width;
        }, text, maxW);
    }

    /** Truncate with an ellipsis so the bubble always fits the window. */
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
