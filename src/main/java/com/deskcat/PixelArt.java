package com.deskcat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * All sprites are authored as character maps so the whole app ships without
 * binary assets. One char = one pixel; '.' is transparent.
 */
public final class PixelArt {

    private PixelArt() {
    }

    // K outline, O orange, D dark stripe, W warm white, P inner ear pink,
    // N nose, R heart red, B zzz blue, G iris green
    private static int rgba(char c) {
        switch (c) {
            case 'K': return Color.rgba8888(Color.valueOf("26202A"));
            case 'O': return Color.rgba8888(Color.valueOf("F29A4B"));
            case 'D': return Color.rgba8888(Color.valueOf("C9702E"));
            case 'W': return Color.rgba8888(Color.valueOf("FFF4E3"));
            case 'P': return Color.rgba8888(Color.valueOf("F2A3B3"));
            case 'N': return Color.rgba8888(Color.valueOf("C75B77"));
            case 'R': return Color.rgba8888(Color.valueOf("E5626E"));
            case 'B': return Color.rgba8888(Color.valueOf("BFE3F2"));
            case 'G': return Color.rgba8888(Color.valueOf("7CC46B"));
            case 'Y': return Color.rgba8888(Color.valueOf("F9D848"));
            case 'M': return Color.rgba8888(Color.valueOf("8C5A2B"));
            case 'Q': return Color.rgba8888(Color.valueOf("E23B2E"));
            case 'T': return Color.rgba8888(Color.valueOf("4FA8A0"));
            case 'F': return Color.rgba8888(Color.valueOf("E8542F"));
            case 'A': return Color.rgba8888(Color.valueOf("8CCFE8"));
            case 'H': return Color.rgba8888(Color.valueOf("A9784C"));
            case 'C': return Color.rgba8888(Color.valueOf("F2E3B8"));
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

    /** 1x1 white pixel, tint + scale it to draw eyes and rectangles. */
    public static Texture pixel() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.drawPixel(0, 0, Color.rgba8888(Color.WHITE));
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    // Sitting tabby, front view, 28x26. Eye whites are part of the map;
    // iris/pupil/blink are drawn on top at runtime (see CatApp.EYE_* constants).
    public static final String[] BODY = {
            "............................",
            "...KK..............KK.......",
            "..KOOK............KOOK......",
            "..KOPOK..........KOPOK......",
            ".KOOPPOK........KOPPOOK.....",
            ".KOOOOOKKKKKKKKKKOOOOOK.....",
            ".KOOOOOOOOOOOOOOOOOOOOK.....",
            "KODOOOOOOOOOOOOOOOOOODOK....",
            "KODOOWWWWOOOOOOOWWWWODOK....",
            "KOOOOWWWWOOOOOOOWWWWOOOK....",
            "KOOOOWWWWOOOOOOOWWWWOOOK....",
            "KOOOOOOOOOOKNNKOOOOOOOOK....",
            "KOOOOOOOOOKWWWWKOOOOOOOK....",
            ".KOOOOOOOOKWWWWKOOOOOOK.....",
            ".KOOOOOOOOOKKKKOOOOOOOK.....",
            "..KKOOOOOOOOOOOOOOOKKK......",
            "...KOOOOOOOOOOOOOOOK........",
            "...KOOWWOOOOOOOOOOOK........",
            "..KOOWWWWOOOOOOOOOOK........",
            "..KOOWWWWOOOOOOOOOOK........",
            "..KOOOWWOOOOOOOOOOOK........",
            "..KOOOOOOODOODOOOOOK........",
            "..KOOOOOOODOODOOOOOK........",
            "..KOWWOKOOOOOOKOWWOK........",
            "..KOWWOKOOOOOOKOWWOK........",
            "..KKKKKKKKKKKKKKKKKK........",
    };

    public static final String[] TAIL_A = {
            "......KK..",
            ".....KOOK.",
            ".....KDOK.",
            ".....KOOK.",
            "....KOOK..",
            "....KDOK..",
            "....KOOK..",
            "...KOOK...",
            "...KOOK...",
            "..KOOK....",
            ".KOOK.....",
            ".KKK......",
    };

    public static final String[] TAIL_B = {
            "..........",
            "..........",
            "......KK..",
            ".....KOOK.",
            ".....KDOK.",
            "....KOOK..",
            "....KOOK..",
            "....KDOK..",
            "...KOOK...",
            "..KOOK....",
            ".KOOK.....",
            ".KKK......",
    };

    public static final String[] TAIL_C = {
            "..........",
            "..........",
            "..........",
            "....KKK...",
            "...KOOOK..",
            "...KODOK..",
            "...KOOOK..",
            "...KOOK...",
            "..KOOK....",
            "..KOOK....",
            ".KOOK.....",
            ".KKK......",
    };

    // Sitting Pikachu, front view, 28x27. Eye area is left plain yellow;
    // bead eyes with a white glint are drawn at runtime.
    public static final String[] PIKA_BODY = {
            ".KK......................KK.",
            ".KMMK..................KMMK.",
            ".KMMMK................KMMMK.",
            "..KMYYK..............KYYMK..",
            "..KYYYK..............KYYYK..",
            "...KYYYK............KYYYK...",
            "...KYYYYK..........KYYYYK...",
            "....KYYYYKKKKKKKKKKYYYYK....",
            "...KYYYYYYYYYYYYYYYYYYYYK...",
            "..KYYYYYYYYYYYYYYYYYYYYYYK..",
            "..KYYYYYYYYYYYYYYYYYYYYYYK..",
            "..KYYYYYYYYYYKKYYYYYYYYYYK..",
            "..KYYYYYYYYYYYYYYYYYYYYYYK..",
            "..KQQQYYYYYYYYYYYYYYYYQQQK..",
            "..KQQQYYYYYYYYYYYYYYYYQQQK..",
            "..KQQQYYYYYYYYYYYYYYYYQQQK..",
            "...KYYYYYYYYYYYYYYYYYYYYK...",
            "....KKYYYYYYYYYYYYYYYYKK....",
            "....KYYYYYYYYYYYYYYYYYYK....",
            "...KYYYYYYYYYYYYYYYYYYYYK...",
            "...KYKYYYYYYYYYYYYYYYYKYK...",
            "...KYKYYYYYYYYYYYYYYYYKYK...",
            "...KYYYYYYYYYYYYYYYYYYYYK...",
            "...KYYYYYYYYYYYYYYYYYYYYK...",
            "..KYYYYKYYYYYYYYYYYYKYYYYK..",
            "..KYYYYKYYYYYYYYYYYYKYYYYK..",
            "..KKKKKKKKKKKKKKKKKKKKKKKK..",
    };

    // Lightning-bolt tail, 12x14, three wag frames (upright / lean left / lean right)
    public static final String[] PIKA_TAIL_A = {
            "......KKKKK.",
            ".....KYYYYK.",
            "....KYYYYK..",
            "...KYYYYK...",
            "...KYYYYYK..",
            "....KYYYYYK.",
            ".....KYYYYK.",
            "....KYYYYK..",
            "...KYYYYK...",
            "..KYYYYK....",
            "..KMMYK.....",
            ".KMMMK......",
            ".KMMK.......",
            ".KKK........",
    };

    public static final String[] PIKA_TAIL_B = {
            ".....KKKKK..",
            "....KYYYYK..",
            "...KYYYYK...",
            "..KYYYYK....",
            "..KYYYYYK...",
            "...KYYYYYK..",
            "....KYYYYK..",
            "....KYYYYK..",
            "...KYYYYK...",
            "..KYYYYK....",
            "..KMMYK.....",
            ".KMMMK......",
            ".KMMK.......",
            ".KKK........",
    };

    public static final String[] PIKA_TAIL_C = {
            ".......KKKKK",
            "......KYYYYK",
            ".....KYYYYK.",
            "....KYYYYK..",
            "....KYYYYYK.",
            ".....KYYYYYK",
            "......KYYYYK",
            "....KYYYYK..",
            "...KYYYYK...",
            "..KYYYYK....",
            "..KMMYK.....",
            ".KMMMK......",
            ".KMMK.......",
            ".KKK........",
    };

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

    // --- procedural sprite builder ------------------------------------------
    // Bodies built from overlapping ellipses with an automatic outline pass —
    // the same construction as the approved preview art, so shapes stay round.

    private static void ell(int[] g, int gw, float cx, float cy, float rx,
            float ry, int color) {
        int gh = g.length / gw;
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                float dx = (x - cx) / rx, dy = (y - cy) / ry;
                if (dx * dx + dy * dy <= 1f) {
                    g[y * gw + x] = color;
                }
            }
        }
    }

    private static void outlinePass(int[] g, int gw, int outline) {
        int gh = g.length / gw;
        boolean[] edge = new boolean[g.length];
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                if (g[y * gw + x] == 0) {
                    continue;
                }
                if (x == 0 || y == 0 || x == gw - 1 || y == gh - 1
                        || g[y * gw + x - 1] == 0 || g[y * gw + x + 1] == 0
                        || g[(y - 1) * gw + x] == 0 || g[(y + 1) * gw + x] == 0) {
                    edge[y * gw + x] = true;
                }
            }
        }
        for (int i = 0; i < g.length; i++) {
            if (edge[i]) {
                g[i] = outline;
            }
        }
    }

    private static Texture toTexture(int[] g, int gw) {
        int gh = g.length / gw;
        Pixmap pm = new Pixmap(gw, gh, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                if (g[y * gw + x] != 0) {
                    pm.drawPixel(x, y, g[y * gw + x]);
                }
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    /** Sitting Squirtle, 30x27, same shapes as the approved preview. */
    public static Texture squirtleBody() {
        int k = rgba('K'), a = rgba('A'), h = rgba('H'), c = rgba('C');
        int gw = 30;
        int[] g = new int[gw * 27];
        ell(g, gw, 15f, 19.5f, 9f, 6.5f, h);      // shell
        ell(g, gw, 5.5f, 18.5f, 2.5f, 3f, a);     // arms
        ell(g, gw, 24.5f, 18.5f, 2.5f, 3f, a);
        ell(g, gw, 15f, 21f, 5.5f, 5.3f, c);      // plastron, down to the ground
        ell(g, gw, 9.5f, 24.5f, 3f, 2f, a);       // feet on top of it
        ell(g, gw, 20.5f, 24.5f, 3f, 2f, a);
        ell(g, gw, 15f, 9.5f, 8.5f, 7f, a);       // head
        outlinePass(g, gw, k);
        for (int x = 12; x <= 17; x++) {
            g[13 * gw + x] = k;                   // smile
        }
        g[12 * gw + 11] = k;
        g[12 * gw + 18] = k;
        return toTexture(g, gw);
    }

    private static Texture squirtleTailFrame(float ox, float oy) {
        int k = rgba('K'), a = rgba('A');
        int gw = 12;
        int[] g = new int[gw * 12];
        ell(g, gw, 2.5f, 9f, 3f, 2.6f, a);            // root, hidden by body
        ell(g, gw, 5.5f + ox, 5.5f + oy, 3.2f, 3f, a); // curl
        outlinePass(g, gw, k);
        int cx = Math.round(5.5f + ox), cy = Math.round(5.5f + oy);
        g[cy * gw + cx] = k;                          // spiral hint
        g[cy * gw + cx + 1] = k;
        return toTexture(g, gw);
    }

    /** Three wag frames for the curly tail. */
    public static Texture[] squirtleTail() {
        return new Texture[] {
                squirtleTailFrame(0f, 0f),
                squirtleTailFrame(0.9f, -0.7f),
                squirtleTailFrame(-0.6f, 0.8f),
        };
    }

    public static final String[] PAW_SQUIRT = {
            ".KK.",
            "KAAK",
            "KAAK",
            ".KK.",
    };

    public static final String[] WATER_DROP = {
            ".A.",
            "AWA",
            ".A.",
    };

    public static final String[] PAW_CAT = {
            ".KK.",
            "KOOK",
            "KWWK",
            ".KK.",
    };

    public static final String[] PAW_PIKA = {
            ".KK.",
            "KYYK",
            "KYYK",
            ".KK.",
    };

    public static final String[] SPARK = {
            ".Y.",
            "YWY",
            ".Y.",
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
}
