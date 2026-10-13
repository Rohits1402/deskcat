package com.deskcat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Texture helpers plus the small particle sprites (hearts, Zzz, sparks,
 * notes, drops, dirt, bubbles), authored as character maps: one char = one
 * pixel, '.' is transparent. The pets themselves are drawn in
 * {@link PathArt}; there are no image assets.
 */
public final class PixelArt {

    private PixelArt() {
    }

    // K outline, W warm white, R heart red, B zzz blue, Y spark yellow,
    // A water blue, X/Z earth brown / dark (thrown dirt), G/D leaf green / vein
    private static int rgba(char c) {
        switch (c) {
            case 'K': return Color.rgba8888(Color.valueOf("26202A"));
            case 'W': return Color.rgba8888(Color.valueOf("FFF4E3"));
            case 'R': return Color.rgba8888(Color.valueOf("E5626E"));
            case 'B': return Color.rgba8888(Color.valueOf("BFE3F2"));
            case 'Y': return Color.rgba8888(Color.valueOf("F9D848"));
            case 'A': return Color.rgba8888(Color.valueOf("8CCFE8"));
            case 'X': return Color.rgba8888(Color.valueOf("B57A42"));
            case 'Z': return Color.rgba8888(Color.valueOf("2E2622"));
            case 'G': return Color.rgba8888(Color.valueOf("7FD25E"));
            case 'D': return Color.rgba8888(Color.valueOf("2F6E2A"));
            default:
                throw new IllegalArgumentException("unknown palette char '" + c + "'");
        }
    }

    public static Texture fromMap(String[] rows) {
        int h = rows.length;
        int w = rows[0].length();
        for (int i = 0; i < h; i++) {
            if (rows[i].length() != w) {
                throw new IllegalArgumentException(
                        "map row " + i + " is " + rows[i].length() + " chars, expected " + w);
            }
        }
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = rows[y].charAt(x);
                if (c != '.') {
                    pm.drawPixel(x, y, rgba(c));
                }
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    /** Texture from a {@link PoseArt} grid (already RGBA8888). */
    static Texture tex(PoseArt.Grid g) {
        Pixmap pm = new Pixmap(g.w, g.h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                if (g.px[y * g.w + x] != 0) {
                    pm.drawPixel(x, y, g.px[y * g.w + x]);
                }
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    /** 1x1 white pixel, tint + scale it to draw eyes and rectangles. */
    public static Texture pixel() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.drawPixel(0, 0, Color.rgba8888(Color.WHITE));
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    public static final String[] HEART = {
            ".KK.KK.",
            "KRRKRRK",
            "KRRRRRK",
            ".KRRRK.",
            "..KRK..",
            "...K...",
    };

    public static final String[] ZZZ = {
            "BBBB",
            "..B.",
            ".B..",
            "BBBB",
    };

    public static final String[] WATER_DROP = {
            ".A.",
            "AWA",
            ".A.",
    };

    /** Leaf flung off Bulbasaur's vine as it cracks. */
    public static final String[] LEAF = {
            ".GG",
            "GDG",
            "GG.",
    };

    public static final String[] SPARK = {
            ".Y.",
            "YWY",
            ".Y.",
    };

    public static final String[] NOTE = {
            "....K.",
            "....K.",
            "....K.",
            "....K.",
            ".BBBK.",
            "BBBBK.",
            ".BBB..",
    };

    public static final String[] ALERT = {
            "RRR",
            "RRR",
            "RRR",
            "RRR",
            "RRR",
            "...",
            "RRR",
            "RRR",
    };

    /** Clod of earth thrown by the shiba while digging. */
    public static final String[] DIRT = {
            ".X.",
            "XZX",
            ".X.",
    };

    /** Bubble released by the goldfish. */
    public static final String[] BUBBLE = {
            ".AA.",
            "AWWA",
            "AWWA",
            ".AA.",
    };
}
