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
    // J/L/S black-cat fur base/sheen/shade, E amber eye
    // U/V/X shiba tan/cream/shade, Z dark nose, I goldfish orange
    // 1 black-cat rim (lighter than K so it reads on dark wallpaper), 2 peach
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
            case 'J': return Color.rgba8888(Color.valueOf("3A3340"));
            case 'L': return Color.rgba8888(Color.valueOf("5C5368"));
            case 'S': return Color.rgba8888(Color.valueOf("2B2531"));
            case 'E': return Color.rgba8888(Color.valueOf("F2C14E"));
            case 'U': return Color.rgba8888(Color.valueOf("D9A066"));
            case 'V': return Color.rgba8888(Color.valueOf("F7E6CC"));
            case 'X': return Color.rgba8888(Color.valueOf("B57A42"));
            case 'Z': return Color.rgba8888(Color.valueOf("2E2622"));
            case 'I': return Color.rgba8888(Color.valueOf("FF8C3A"));
            case '1': return Color.rgba8888(Color.valueOf("6B6178"));
            case '2': return Color.rgba8888(Color.valueOf("FFB877"));
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
        return toTexture(g.px, g.w);
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

    // ---------- shared helper for the newer skins ----------

    /** Upward triangle; lean offsets the apex sideways (ears, fins). */
    private static void tri(int[] g, int gw, float cx, float baseY,
            float halfW, float h, float lean, int c) {
        int gh = g.length / gw;
        int steps = Math.round(h);
        for (int j = 0; j <= steps; j++) {
            float t = h <= 0f ? 0f : j / h;
            int y = Math.round(baseY - j);
            if (y < 0 || y >= gh) {
                continue;
            }
            int x0 = Math.round(cx + lean * t - halfW * (1 - t));
            int x1 = Math.round(cx + lean * t + halfW * (1 - t));
            for (int x = x0; x <= x1; x++) {
                if (x >= 0 && x < gw) {
                    g[y * gw + x] = c;
                }
            }
        }
    }

    /** Sitting black cat, 28x28 - slimmer than the tabby, amber eyes. */
    public static Texture blackCatBody() {
        int rim = rgba('1'), k = rgba('K'), j = rgba('J'), l = rgba('L');
        int s = rgba('S'), e = rgba('E'), n = rgba('N');
        int gw = 28;
        int[] g = new int[gw * 28];
        ell(g, gw, 14f, 22.5f, 7.0f, 5.4f, j);      // haunches
        ell(g, gw, 14f, 17.5f, 4.6f, 5.2f, j);      // upright chest
        ell(g, gw, 11f, 24.0f, 1.9f, 3.6f, j);      // front legs
        ell(g, gw, 17f, 24.0f, 1.9f, 3.6f, j);
        ell(g, gw, 14f, 9.0f, 5.9f, 5.1f, j);       // head
        ell(g, gw, 10.2f, 11.2f, 2.3f, 2.1f, j);    // cheek tufts
        ell(g, gw, 17.8f, 11.2f, 2.3f, 2.1f, j);
        tri(g, gw, 9.8f, 5.6f, 2.5f, 4.4f, -1.2f, j);   // ears
        tri(g, gw, 18.2f, 5.6f, 2.5f, 4.4f, 1.2f, j);
        ell(g, gw, 14f, 6.4f, 4.4f, 1.7f, l);       // sheen on the skull
        ell(g, gw, 14f, 15.4f, 3.4f, 1.5f, l);      // sheen on the shoulders
        ell(g, gw, 11f, 26.6f, 2.2f, 1.5f, s);      // paws
        ell(g, gw, 17f, 26.6f, 2.2f, 1.5f, s);
        tri(g, gw, 9.8f, 4.6f, 1.0f, 2.3f, -0.9f, s);   // inner ear
        tri(g, gw, 18.2f, 4.6f, 1.0f, 2.3f, 0.9f, s);
        // a rim lighter than the usual outline, or a black cat vanishes
        // against a dark wallpaper and only its eyes are left floating
        outlinePass(g, gw, rim);
        for (int y = 7; y <= 9; y++) {              // amber eye sockets
            for (int x = 7; x <= 10; x++) {
                g[y * gw + x] = e;
            }
            for (int x = 17; x <= 20; x++) {
                g[y * gw + x] = e;
            }
        }
        g[12 * gw + 13] = n;                        // nose
        g[12 * gw + 14] = n;
        g[13 * gw + 13] = k;                        // philtrum
        g[14 * gw + 11] = k;                        // mouth corners
        g[14 * gw + 16] = k;
        int[][] wh = {{5, 12}, {4, 11}, {5, 14}, {4, 15},
                      {22, 12}, {23, 11}, {22, 14}, {23, 15}};
        for (int[] w : wh) {                        // whiskers stay hairline
            g[w[1] * gw + w[0]] = rim;
        }
        return toTexture(g, gw);
    }

    private static Texture blackCatTailFrame(float sweep) {
        int rim = rgba('1'), j = rgba('J');
        int gw = 12;
        int[] g = new int[gw * 17];
        for (int i = 0; i <= 12; i++) {             // bows out, tip hooks in
            float t = i / 12f;
            float r = 2.0f - t * 0.75f;
            float x = 2.6f + t * (3.6f + sweep)
                    + (float) Math.sin(t * Math.PI) * 2.4f;
            ell(g, gw, x, 15f - i * 1.1f, r, r, j);
        }
        outlinePass(g, gw, rim);
        return toTexture(g, gw);
    }

    /** Three sweep frames for the black cat tail. */
    public static Texture[] blackCatTail() {
        return new Texture[] {
                blackCatTailFrame(0f),
                blackCatTailFrame(1.1f),
                blackCatTailFrame(-0.9f),
        };
    }

    /**
     * Sitting shiba, 30x28. What reads as doge rather than fox: a wide face
     * with puffy cream cheeks, small upright ears, raised cream eyebrow spots
     * and smug squinting eyes (eyeH 2, set in SkinAssets).
     */
    public static Texture dogeBody() {
        int k = rgba('K'), u = rgba('U'), v = rgba('V'), z = rgba('Z');
        int gw = 30;
        int[] g = new int[gw * 28];
        ell(g, gw, 15f, 22.0f, 7.4f, 5.8f, u);      // body
        ell(g, gw, 15f, 17.0f, 5.0f, 5.0f, u);      // chest
        ell(g, gw, 15f, 22.5f, 4.0f, 5.0f, v);      // cream bib
        ell(g, gw, 11f, 25.0f, 2.2f, 2.6f, v);      // front legs
        ell(g, gw, 19f, 25.0f, 2.2f, 2.6f, v);
        tri(g, gw, 10.6f, 6.4f, 2.3f, 4.0f, -0.2f, u);  // small upright ears
        tri(g, gw, 19.4f, 6.4f, 2.3f, 4.0f, 0.2f, u);
        ell(g, gw, 15f, 10.0f, 7.2f, 5.4f, u);      // wide head
        ell(g, gw, 8.8f, 12.8f, 3.0f, 2.6f, u);     // puffy cheeks
        ell(g, gw, 21.2f, 12.8f, 3.0f, 2.6f, u);
        ell(g, gw, 9.4f, 13.0f, 2.8f, 2.4f, v);     // urajiro: cream cheeks
        ell(g, gw, 20.6f, 13.0f, 2.8f, 2.4f, v);
        ell(g, gw, 15f, 13.4f, 3.6f, 2.6f, v);      // and muzzle
        tri(g, gw, 10.6f, 5.8f, 1.0f, 2.2f, -0.1f, v);  // cream inner ears
        tri(g, gw, 19.4f, 5.8f, 1.0f, 2.2f, 0.1f, v);
        outlinePass(g, gw, k);
        g[7 * gw + 11] = v;                         // raised eyebrow spots
        g[7 * gw + 12] = v;
        g[7 * gw + 18] = v;
        g[7 * gw + 19] = v;
        ell(g, gw, 15f, 12.2f, 1.5f, 1.1f, z);      // nose
        g[14 * gw + 15] = k;                        // philtrum
        int[][] smirk = {{12, 16}, {13, 16}, {14, 16}, {15, 16},
                         {16, 16}, {17, 15}, {18, 14}};
        for (int[] m : smirk) {                     // the smug doge curve
            g[m[1] * gw + m[0]] = k;
        }
        return toTexture(g, gw);
    }

    private static Texture dogeTailFrame(float sweep) {
        int k = rgba('K'), u = rgba('U'), v = rgba('V');
        int gw = 13;
        int[] g = new int[gw * 16];
        for (int i = 0; i <= 14; i++) {             // curls up and over
            double a = Math.PI * (0.15 + 0.78 * (i / 14.0)) + sweep * 0.06;
            float cx = 8.6f + (float) (Math.cos(a) * -4.6);
            float cy = 12.5f - (float) (Math.sin(a) * 7.4);
            float r = 2.5f - (i / 14f) * 0.9f;
            ell(g, gw, cx, cy, r, r, i > 9 ? v : u);
        }
        outlinePass(g, gw, k);
        return toTexture(g, gw);
    }

    /** Three wag frames for the curled shiba tail. */
    public static Texture[] dogeTail() {
        return new Texture[] {
                dogeTailFrame(0f),
                dogeTailFrame(1.4f),
                dogeTailFrame(-1.4f),
        };
    }

    /**
     * Goldfish in profile, facing right, 32x24. Head-on it read as a chick
     * (the dorsal fin became a comb, the side fins wings); fish are
     * recognised side-on, so this one has a single eye (eyeRX = -1).
     */
    public static Texture goldfishBody() {
        int k = rgba('K'), i = rgba('I'), f = rgba('F'), o = rgba('O');
        int peach = rgba('2');
        int gw = 32;
        int[] g = new int[gw * 24];
        tri(g, gw, 17f, 8.5f, 4.2f, 5.6f, -3.2f, o);    // dorsal sail, swept back
        ell(g, gw, 14f, 18.6f, 2.6f, 1.8f, o);      // anal fin
        ell(g, gw, 21f, 19.0f, 2.2f, 1.6f, o);      // pelvic fin
        ell(g, gw, 19f, 13.0f, 9.5f, 6.4f, i);      // body, head to the right
        ell(g, gw, 19.5f, 16.4f, 7.2f, 2.6f, peach);    // pale belly
        ell(g, gw, 17.5f, 9.4f, 7.0f, 2.2f, f);     // deeper orange back
        outlinePass(g, gw, k);
        ell(g, gw, 18.5f, 14.4f, 2.6f, 1.3f, o);    // pectoral fin
        int[][] gill = {{22, 10}, {21, 11}, {21, 12}, {21, 13}, {22, 14}};
        for (int[] q : gill) {
            g[q[1] * gw + q[0]] = f;
        }
        int[][] scales = {{14, 12}, {16, 14}, {12, 14}};
        for (int[] q : scales) {
            g[q[1] * gw + q[0]] = f;
        }
        g[12 * gw + 28] = k;                        // mouth
        g[13 * gw + 28] = k;
        return toTexture(g, gw);
    }

    private static Texture goldfishTailFrame(float wave) {
        int k = rgba('K'), o = rgba('O'), f = rgba('F');
        int gw = 14, gh = 22;
        int[] g = new int[gw * gh];
        for (int x = 0; x < gw; x++) {              // forked fan, root on the right
            float t = (gw - 1 - x) / (float) (gw - 1);
            float half = 1.6f + t * 6.4f;
            float cy = 11f + wave * t * 2.5f;
            for (int y = 0; y < gh; y++) {
                float d = Math.abs(y - cy);
                boolean fork = t > 0.65f && d < (t - 0.65f) * 10f;
                if (d <= half && !fork) {
                    g[y * gw + x] = t > 0.5f ? o : f;
                }
            }
        }
        outlinePass(g, gw, k);
        return toTexture(g, gw);
    }

    /** Three wave frames for the goldfish fan tail. */
    public static Texture[] goldfishTail() {
        return new Texture[] {
                goldfishTailFrame(0f),
                goldfishTailFrame(0.8f),
                goldfishTailFrame(-0.8f),
        };
    }

    public static final String[] PAW_BLACK = {
            ".KK.",
            "KJJK",
            "KSSK",
            ".KK.",
    };

    public static final String[] PAW_DOGE = {
            ".KK.",
            "KVVK",
            "KVVK",
            ".KK.",
    };

    public static final String[] PAW_FIN = {
            ".KK.",
            "KOOK",
            "KOOK",
            ".KK.",
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
