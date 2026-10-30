package com.deskcat;

import static com.deskcat.Gait.*;
import static com.deskcat.PathArt.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The reminder animations: each skin stretches its own way (stretch
 * reminder) and gets a drink its own way (water reminder). Frames are whole
 * pictures in window space - W x H world units, row 0 at the top, the
 * ground on the last row - so props like a water bowl or a rain cloud fit
 * beside and above the pet. Sitting frames are the shipping sitting art
 * with the runtime eyes baked in; new poses are drawn in the same
 * {@link PathArt} format. Pure Java, so a scratch preview shows exactly
 * the pixels that ship.
 */
final class ReminderArt {

    private ReminderArt() {
    }

    static final int W = 44, H = 42;
    static final int GROUND = H - 1;

    /** Frames and how long each one shows. */
    static final class Anim {
        final PoseArt.Grid[] frames;
        final float[] secs;
        /**
         * Where the window should be, per frame, in world units from where
         * the animation started (null = it stays put): zoomies, leaps.
         */
        final int[] dx, dy;

        Anim(PoseArt.Grid[] frames, float[] secs) {
            this(frames, secs, null, null);
        }

        Anim(PoseArt.Grid[] frames, float[] secs, int[] dx, int[] dy) {
            this.frames = frames;
            this.secs = secs;
            this.dx = dx;
            this.dy = dy;
        }

        /** Index of the frame showing t seconds in (the last past the end). */
        int index(float t) {
            for (int i = 0; i < frames.length; i++) {
                t -= secs[i];
                if (t < 0f) {
                    return i;
                }
            }
            return frames.length - 1;
        }

        float duration() {
            float t = 0f;
            for (float s : secs) {
                t += s;
            }
            return t;
        }

        /** The frame showing t seconds in (the last one past the end). */
        PoseArt.Grid at(float t) {
            for (int i = 0; i < frames.length; i++) {
                t -= secs[i];
                if (t < 0f) {
                    return frames[i];
                }
            }
            return frames[frames.length - 1];
        }
    }

    static Anim stretch(String skin) {
        switch (skin) {
            case "squirtle": return squirtleStretch();
            case "pikachu": return pikaStretch();
            case "blackcat": return blackCatStretch();
            case "doge": return dogeStretch();
            case "bulbasaur": return bulbaStretch();
            case "goldfish": return fishStretch();
            default: return catStretch();
        }
    }

    static Anim water(String skin) {
        switch (skin) {
            case "squirtle": return squirtleWater();
            case "pikachu": return pikaWater();
            case "blackcat": return blackCatWater();
            case "doge": return dogeWater();
            case "bulbasaur": return bulbaWater();
            case "goldfish": return fishWater();
            default: return catWater();
        }
    }

    // ============================================================ FRAMES

    /** A frame being composed: rendered grids layered back to front. */
    static final class F {
        final PoseArt.Grid g = new PoseArt.Grid(W, H);

        F put(PoseArt.Grid s, int x, int y) {
            for (int yy = 0; yy < s.h; yy++) {
                for (int xx = 0; xx < s.w; xx++) {
                    int c = s.px[yy * s.w + xx];
                    if (c != 0) {
                        g.set(x + xx, y + yy, c);
                    }
                }
            }
            return this;
        }

        F set(int x, int y, int c) {
            g.set(x, y, c);
            return this;
        }

        F rect(int x, int y, int w, int h, int c) {
            for (int yy = y; yy < y + h; yy++) {
                for (int xx = x; xx < x + w; xx++) {
                    g.set(xx, yy, c);
                }
            }
            return this;
        }

        /** A char-map sprite (PixelArt style); '.' is clear. */
        F map(String[] rows, int x, int y, char[] keys, int[] cols) {
            for (int yy = 0; yy < rows.length; yy++) {
                for (int xx = 0; xx < rows[yy].length(); xx++) {
                    char ch = rows[yy].charAt(xx);
                    for (int k = 0; k < keys.length; k++) {
                        if (keys[k] == ch) {
                            g.set(x + xx, y + yy, cols[k]);
                        }
                    }
                }
            }
            return this;
        }

        F copy() {
            F f = new F();
            System.arraycopy(g.px, 0, f.g.px, 0, g.px.length);
            return f;
        }

        /** Shift every column vertically by dy(x): arches and bends. */
        F bend(int[] dy) {
            int[] src = g.px.clone();
            java.util.Arrays.fill(g.px, 0);
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    int c = src[y * W + x];
                    if (c != 0) {
                        g.set(x, y + dy[x], c);
                    }
                }
            }
            return this;
        }

        /** Shift a band of rows sideways. */
        F shiftRows(int y0, int y1, int dx) {
            for (int y = Math.max(0, y0); y <= Math.min(H - 1, y1); y++) {
                int[] row = new int[W];
                for (int x = 0; x < W; x++) {
                    int sx = x - dx;
                    row[x] = sx >= 0 && sx < W ? g.px[y * W + sx] : 0;
                }
                System.arraycopy(row, 0, g.px, y * W, W);
            }
            return this;
        }

        F shift(int dx, int dy) {
            int[] src = g.px.clone();
            java.util.Arrays.fill(g.px, 0);
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int c = src[y * W + x];
                    if (c != 0) {
                        g.set(x + dx, y + dy, c);
                    }
                }
            }
            return this;
        }
    }

    static final class Seq {
        final List<PoseArt.Grid> frames = new ArrayList<PoseArt.Grid>();
        final List<Float> secs = new ArrayList<Float>();
        final List<int[]> offs = new ArrayList<int[]>();
        boolean moves;

        Seq add(F f, float s) {
            return add(f, s, 0, 0);
        }

        /** A frame shown with the window moved (dx, dy) units from the start. */
        Seq add(F f, float s, int dx, int dy) {
            frames.add(f.g);
            secs.add(s);
            offs.add(new int[] {dx, dy});
            moves |= dx != 0 || dy != 0;
            return this;
        }

        Anim anim() {
            float[] s = new float[secs.size()];
            int[] dx = new int[s.length], dy = new int[s.length];
            for (int i = 0; i < s.length; i++) {
                s[i] = secs.get(i);
                dx[i] = offs.get(i)[0];
                dy[i] = offs.get(i)[1];
            }
            PoseArt.Grid[] f = frames.toArray(new PoseArt.Grid[0]);
            return moves ? new Anim(f, s, dx, dy) : new Anim(f, s);
        }
    }

    static final int WHITE = PoseArt.rgba("FFFFFF");
    static final int PUPIL_C = PoseArt.rgba("120F16");
    static final int BEAD = PoseArt.rgba("26202A");

    // ============================================================== EYES

    /** Runtime eye geometry (as in SkinAssets), in FBO units, y up. */
    static final class Eye {
        final int style, lx, rx, w, h, y, fur, line;
        final boolean one;

        Eye(int style, int lx, int rx, int w, int h, int y, String fur, String line, boolean one) {
            this.style = style;
            this.lx = lx;
            this.rx = rx;
            this.w = w;
            this.h = h;
            this.y = y;
            this.fur = PoseArt.rgba(fur);
            this.line = PoseArt.rgba(line);
            this.one = one;
        }
    }

    static final Eye CAT_EYE = new Eye(5, 8, 20, 4, 4, 17, "F29A4B", "26202A", false);
    static final Eye BLACK_EYE = new Eye(5, 8, 20, 4, 4, 17, "2E2936", "ECE8F4", false);
    static final Eye SQUIRT_EYE = new Eye(5, 9, 21, 4, 4, 16, "8CCFE8", "22313F", false);
    static final Eye BULBA_EYE = new Eye(6, 7, 23, 4, 4, 12, "79CBB0", "1F3B33", false);
    static final Eye PIKA_EYE = new Eye(4, 10, 20, 4, 5, 12, "F7D23E", "26202A", false);
    static final Eye DOGE_EYE = new Eye(1, 10, 21, 3, 2, 17, "D9A066", "26202A", false);
    static final Eye FISH_EYE = new Eye(1, 25, 25, 3, 3, 13, "FF8C3A", "26202A", true);

    static final int OPEN = 0, CLOSED = 1, HAPPY = 2, RIGHT = 3, SMUG = 4, DIZZY = 5, XED = 6,
            ANGRY = 7;

    static final int BULBA_IRIS = PoseArt.rgba("C8323E");

    /** Bulbasaur's runtime eye (style 6): a 3x3 red iris, upright pupil, glint. */
    static void redIris(F f, int x, int y) {
        fbo(f, x, y, 3, 3, BULBA_IRIS);
        fbo(f, x + 1, y, 1, 2, PUPIL_C);
        fbo(f, x, y + 2, 1, 1, WHITE);
    }

    /** An FBO-space rect (x right, y up) onto the window frame. */
    static void fbo(F f, int x, int y, int w, int h, int c) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                f.set(2 + xx, GROUND - yy, c);
            }
        }
    }

    /** The eyes CatApp would draw: looking ahead, closed, or squeezed happy. */
    static void eyes(F f, Eye e, int mode) {
        for (int x : e.one ? new int[] {e.lx} : new int[] {e.lx, e.rx}) {
            int y = e.y;
            if (mode == OPEN || mode == RIGHT) {
                int look = mode == RIGHT ? 1 : 0;
                if (e.style == 6) {                             // red iris on white
                    redIris(f, x + Math.min(1, (x == e.lx ? 1 : 0) + look), y + 1);
                } else if (e.style == 5) {
                    fbo(f, x + 1 + look, y + 1, 2, 2, PUPIL_C);
                    fbo(f, x + 1 + look, y + 2, 1, 1, WHITE);
                } else if (e.style == 4) {
                    fbo(f, x + 1, y, e.w - 2, e.h, BEAD);
                    fbo(f, x, y + 1, e.w, e.h - 2, BEAD);
                    fbo(f, x + 1 + look, y + e.h - 3, 1, 2, WHITE);
                } else {
                    fbo(f, x, y, e.w, e.h, BEAD);
                    fbo(f, x + 1 + look, y + e.h - 1, 1, 1, WHITE);
                }
                continue;
            }
            if (mode == ANGRY) {
                // a glare: the eye as usual, then a lid slanting down toward
                // the nose, fur above it
                boolean left = e.one || x == e.lx;
                if (e.style == 6) {
                    redIris(f, x + (x == e.lx ? 1 : 0), y);
                } else if (e.style == 5) {
                    fbo(f, x + 1, y, 2, 2, PUPIL_C);
                } else if (e.style == 4) {
                    fbo(f, x + 1, y, e.w - 2, e.h, BEAD);
                    fbo(f, x, y + 1, e.w, e.h - 2, BEAD);
                } else {
                    fbo(f, x, y, e.w, e.h, BEAD);
                }
                for (int i = -1; i <= e.w; i++) {
                    int inner = left ? i + 1 : e.w - i;           // 0 at the outer corner
                    int lid = y + e.h - Math.min(e.h - 1, inner * (e.h - 1) / (e.w + 1)) - (e.style >= 5 ? 0 : 1);
                    fbo(f, x + i, lid + 1, 1, y + e.h + 2 - lid, e.fur);
                    fbo(f, x + i, lid, 1, 1, e.line);
                }
                continue;
            }
            if (mode == SMUG && e.style >= 5) {
                // half-lidded stare: pupil low and centred, lid over the top
                fbo(f, x + 1, y, 2, 2, PUPIL_C);
                fbo(f, x - 1, y + 2, e.w + 2, e.h - 1, e.fur);
                fbo(f, x, y + 2, e.w, 1, e.line);
                continue;
            }
            if (mode == DIZZY || mode == XED) {
                if (e.style >= 5) {
                    fbo(f, x, y - 1, e.w, e.h + 2, e.fur);
                    fbo(f, x - 1, y, e.w + 2, e.h, e.fur);
                } else {
                    fbo(f, x, y, e.w, e.h, e.fur);
                }
                int s = Math.min(e.w, 4);
                for (int i = 0; i < s; i++) {
                    if (mode == XED) {                         // x x
                        fbo(f, x + i, y + i, 1, 1, e.line);
                        fbo(f, x + s - 1 - i, y + i, 1, 1, e.line);
                    } else {                                   // a little spiral
                        fbo(f, x + i, y + s - 1, 1, 1, e.line);
                        fbo(f, x + s - 1, y + i, 1, 1, e.line);
                    }
                }
                if (mode == DIZZY) {
                    fbo(f, x, y, s - 1, 1, e.line);
                    fbo(f, x, y + 1, 1, Math.max(1, s - 2), e.line);
                    fbo(f, x + 1, y + 1, 1, 1, e.line);
                }
                continue;
            }
            if (e.style >= 5) {
                fbo(f, x, y - 1, e.w, e.h + 2, e.fur);
                fbo(f, x - 1, y, e.w + 2, e.h, e.fur);
            } else {
                fbo(f, x, y, e.w, e.h, e.fur);
            }
            int mid = mode == HAPPY ? y + e.h / 2 : y + e.h / 2 - 1;
            int ends = mode == HAPPY ? mid - 1 : mid + 1;
            fbo(f, x, mid, e.w, 1, e.line);
            fbo(f, x - 1, ends, 1, 1, e.line);
            fbo(f, x + e.w, ends, 1, 1, e.line);
        }
    }

    // ============================================================= PROPS

    /** Water, bowl, glass, bottle and cloud colours. */
    static final Palette PROP = new Palette()
            .set(FUR, "6EC6F2").set(FUR_FAR, "3E9AD6")          // water, deep water
            .set(CREAM, "E8F7FF")                                // shine
            .set(ACC1, "E2574C").set(ACC1_FAR, "B9433A")        // bowl
            .set(ACC2, "D7EEF8")                                 // glass
            .set(ACC3, "3A7BD5")                                 // bottle cap
            .set(ACC4, "F7F9FC").set(CREAM_FAR, "C9D3E0")       // cloud
            .outline("2A2230");

    static final String BOWL = "M1 1H13V3H12V5H11V6H3V5H2V3H1Z";

    /** A water bowl seen slightly from above, 14x7; ripple 0-2 moves the glint. */
    static PoseArt.Grid bowl(int ripple) {
        Canvas c = new Canvas(14, 7);
        fill(c, BOWL, ACC1);
        rect(c, 3, 1, 8, 1, FUR);
        paint(c, 0, 4, 14, 2, ACC1, ACC1_FAR);
        px(c, 4 + ripple * 2, 1, CREAM);
        return render(c, PROP);
    }

    /** A glass of water, 9x13; level is the water's top row (5 full, 11 empty). */
    static PoseArt.Grid glass(int level) {
        Canvas c = new Canvas(9, 13);
        rect(c, 1, 1, 7, 11, ACC2);
        if (level < 12) {
            rect(c, 2, level, 5, 12 - level, FUR);
            rect(c, 2, 11, 5, 1, FUR_FAR);
        }
        rect(c, 2, 2, 1, 8, CREAM);
        return render(c, PROP);
    }

    /** A water bottle, cap up, 7x11; level is the water's top row (4 full, 10 empty). */
    static PoseArt.Grid bottle(int level, int bubbleY) {
        Canvas c = new Canvas(7, 11);
        rect(c, 2, 1, 3, 2, ACC3);
        rect(c, 1, 3, 5, 7, ACC2);
        if (level < 10) {
            rect(c, 2, level, 3, 10 - level, FUR);
        }
        if (bubbleY >= level && bubbleY < 10) {
            px(c, 3, bubbleY, CREAM);
        }
        rect(c, 2, 4, 1, 4, CREAM);
        return render(c, PROP);
    }

    static final String CLOUD = "M6 1H11V2H14V3H16V4H18V5H19V7H18V8H2V7H1V5H2V4H4V2H6Z";

    /** A small rain cloud, 21x10. */
    static PoseArt.Grid cloud() {
        Canvas c = new Canvas(21, 10);
        fill(c, CLOUD, ACC4);
        paint(c, 0, 7, 21, 1, ACC4, CREAM_FAR);
        paint(c, 14, 5, 7, 2, ACC4, CREAM_FAR);
        return render(c, PROP);
    }

    /** Loose water: drops or a jet as zones on a window-sized canvas. */
    static Canvas water() {
        return new Canvas(W, H);
    }

    static void drop(Canvas c, int x, int y) {
        rect(c, x, y, 1, 2, FUR);
    }

    static void bigDrop(Canvas c, int x, int y) {
        px(c, x + 1, y, FUR);
        rect(c, x, y + 1, 3, 2, FUR);
        px(c, x, y + 1, CREAM);
    }

    private static final char[] SPARK_KEYS = {'Y', 'W'};
    private static final int[] SPARK_COLS = {PoseArt.rgba("F9D848"), PoseArt.rgba("FFF4E3")};
    private static final char[] LEAF_KEYS = {'G', 'D'};
    private static final int[] LEAF_COLS = {PoseArt.rgba("7FD25E"), PoseArt.rgba("2F6E2A")};
    private static final char[] BUBBLE_KEYS = {'A', 'W'};
    private static final int[] BUBBLE_COLS = {PoseArt.rgba("8CCFE8"), PoseArt.rgba("FFF4E3")};

    static void spark(F f, int x, int y) {
        f.map(PixelArt.SPARK, x, y, SPARK_KEYS, SPARK_COLS);
    }

    static void leaf(F f, int x, int y) {
        f.map(PixelArt.LEAF, x, y, LEAF_KEYS, LEAF_COLS);
    }

    static void bubble(F f, int x, int y) {
        f.map(PixelArt.BUBBLE, x, y, BUBBLE_KEYS, BUBBLE_COLS);
    }

    // ============================================================= CATS

    static F catSit(Palette p, int eyes, int tail) {
        F f = new F();
        f.put(CatArt.sitTail(p, tail), 25, H - 23);
        f.put(CatArt.sitBody(p), 4, H - 30);
        eyes(f, p == CatArt.BLACK ? BLACK_EYE : CAT_EYE, eyes);
        return f;
    }

    /** The cat's mouth on a sitting frame: 0 shut, 1 opening, 2 wide yawn, 3 tongue out. */
    static F catMouth(F f, Palette p, int open) {
        int m = p == CatArt.BLACK ? p.outline : p.colour(MOUTH);
        int pink = p.colour(INNER_EAR);
        if (open == 1) {
            f.rect(17, 26, 2, 2, m);
        } else if (open == 2) {
            f.rect(17, 26, 2, 1, m).rect(16, 27, 1, 3, m).rect(19, 27, 1, 3, m)
                    .rect(17, 27, 2, 3, pink).rect(17, 30, 2, 1, m);
        } else if (open == 3) {
            f.rect(17, 27, 2, 1, pink).set(17, 28, pink);
        }
        return f;
    }

    /** Side view, facing right, from the walk drawing's parts. */
    private static Canvas catSide(Palette p, int headDx, int headDy, boolean eyesClosed) {
        Canvas c = new Canvas(W, 30);
        int hx = headDx, hy = headDy;
        fill(c, CatArt.C_FAR_EAR, FUR_FAR, hx, hy, false);
        fillOver(c, CatArt.C_HEAD, FUR, hx, hy, false);
        fill(c, CatArt.C_EAR, FUR, hx, hy, false);
        fill(c, CatArt.C_EAR_IN, INNER_EAR, hx, hy, false);
        mark(c, 25 + hx, 4 + hy, 1, 2);
        mark(c, 23 + hx, 5 + hy, 1, 2);
        mark(c, 20 + hx, 11 + hy, 2, 1);
        mark(c, 20 + hx, 13 + hy, 2, 1);
        fill(c, CatArt.C_MUZZLE, CREAM, hx, hy, false);
        if (eyesClosed) {
            rect(c, 27 + hx, 10 + hy, 4, 1, LINE);
            px(c, 26 + hx, 9 + hy, LINE);
            px(c, 31 + hx, 9 + hy, LINE);
        } else {
            fill(c, CatArt.C_RING, RING, hx, hy, false);
            fill(c, CatArt.C_IRIS, IRIS, hx, hy, false);
            rect(c, 28 + hx, 9 + hy, 2, 2, PUPIL);
            px(c, 28 + hx, 9 + hy, GLINT);
        }
        px(c, 33 + hx, 11 + hy, NOSE);
        px(c, 34 + hx, 12 + hy, NOSE);
        px(c, 33 + hx, 14 + hy, p == CatArt.BLACK ? LINE : MOUTH);
        return c;
    }

    /** Lay a separately drawn layer over c, creasing its fur where it overlaps. */
    private static void over(Canvas c, Canvas front) {
        creasePaste(c, front);
    }

    /** Shear a canvas's columns: dy = round((x - x0) * slope). */
    private static Canvas tilt(Canvas src, int x0, float slope) {
        Canvas c = new Canvas(src.w, src.h);
        for (int x = 0; x < src.w; x++) {
            int dy = Math.round((x - x0) * slope);
            for (int y = 0; y < src.h; y++) {
                int z = src.z[y * src.w + x];
                if (z != 0) {
                    c.set(x, y + dy, z);
                }
            }
        }
        return c;
    }

    /**
     * The play bow, facing right: front legs stretched flat along the
     * ground, chest low, rump and tail high, head down on its paws, eyes
     * shut. dog swaps in the doge's head and curled tail. 44x30, ground 28.
     */
    private static Canvas bow(boolean dog, int f) {
        Canvas c = new Canvas(W, 30);
        if (!dog) {
            Canvas t = new Canvas(W, 30);
            fill(t, CatArt.C_TAIL, FUR, 1, -3, false);
            mark(t, 2, 4, 3, 1);
            mark(t, 2, 7, 3, 1);
            shiftRows(t, 0, 4, f == 1 ? 1 : 0);
            paste(c, t, 0, 0, false);
        }
        int paw = dog ? 2 : 1;
        draw(c, limb(c, new float[][] {{13, 13}, {13, 21}, {12, 28}}, new int[] {5, 4}), false, CREAM_FAR, paw);
        draw(c, limb(c, new float[][] {{10, 13}, {10, 21}, {9, 28}}, new int[] {5, 4}), true, CREAM, paw);
        Canvas b = new Canvas(W, 30);
        fill(b, CatArt.C_BODY, FUR, 0, -2, false);
        if (dog) {
            paint(b, 0, 20, W, 2, FUR, CREAM);
        } else {
            mark(b, 11, 10, 1, 3);
            mark(b, 14, 10, 1, 3);
            mark(b, 17, 10, 1, 3);
        }
        fill(b, CatArt.C_CHEST, CREAM, 0, -2, false);
        over(c, tilt(b, 14, 0.45f));                             // rump high, chest down
        if (dog) {
            Canvas t = new Canvas(W, 30);
            fill(t, DogArt.D_TAIL, FUR, -1, -6, false);
            rect(t, 7, 3, 3, 2, CREAM);
            px(t, 8, 3, FUR);
            shiftRows(t, 0, 7, f == 1 ? 1 : 0);
            over(c, t);
        }
        int reach = f == 1 ? 1 : 0;
        rect(c, 27, 25, 12 + reach, 2, FUR_FAR);                 // far foreleg
        rect(c, 37 + reach, 25, 2, 2, CREAM_FAR);
        int hx = 2, hy = 9;
        Canvas h = new Canvas(W, 30);
        if (dog) {
            fill(h, DogArt.D_FAR_EAR, FUR_FAR, hx, hy, false);
            fill(h, DogArt.D_HEAD, FUR, hx, hy, false);
            fill(h, DogArt.D_EAR, FUR, hx, hy, false);
            fill(h, DogArt.D_EAR_IN, INNER_EAR, hx, hy, false);
            fill(h, DogArt.D_CHEEK, CREAM, hx, hy, false);
            fill(h, DogArt.D_MUZZLE, CREAM, hx, hy, false);
            mark(h, 24 + hx, 5 + hy, 2, 1);
            rect(h, 27 + hx, 9 + hy, 4, 1, LINE);                // eyes shut
            rect(h, 28 + hx, 6 + hy, 2, 1, CREAM);
            rect(h, 34 + hx, 10 + hy, 2, 2, NOSE);
            px(h, 33 + hx, 13 + hy, MOUTH);
            px(h, 32 + hx, 14 + hy, MOUTH);
        } else {
            Canvas side = catSide(null, hx, hy, true);
            paste(h, side, 0, 0, false);
        }
        over(c, h);
        rect(c, 25, 27, 14 + reach, 2, FUR);                     // near foreleg, out past the chin
        rect(c, 36 + reach, 27, 3, 2, CREAM);
        px(c, 37 + reach, 28, SEP);
        return c;
    }

    /** The cat's sitting -> side transition: walk frame 0. */
    static F catStand(Palette p) {
        return new F().put(CatArt.walk(p, 0), 4, GROUND - 28);
    }

    /** A thick limb through points: w x w squares stamped along it (any angle). */
    static Canvas stroke(Canvas c, int w, float[][] pts) {
        Canvas m = new Canvas(c.w, c.h);
        for (int i = 1; i < pts.length; i++) {
            float x0 = pts[i - 1][0], y0 = pts[i - 1][1], x1 = pts[i][0], y1 = pts[i][1];
            int n = Math.max(1, Math.round(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) * 2));
            for (int k = 0; k <= n; k++) {
                int x = Math.round(x0 + (x1 - x0) * k / n - w / 2f);
                int y = Math.round(y0 + (y1 - y0) * k / n - w / 2f);
                for (int yy = y; yy < y + w; yy++) {
                    for (int xx = x; xx < x + w; xx++) {
                        m.set(xx, yy, 1);
                    }
                }
            }
        }
        return m;
    }

    /** A limb mask laid over c in one zone, creased where it crosses fur. */
    static void limbOver(Canvas c, Canvas m, int zone) {
        Canvas f = new Canvas(c.w, c.h);
        for (int i = 0; i < m.z.length; i++) {
            if (m.z[i] != 0) {
                f.z[i] = zone;
            }
        }
        creasePaste(c, f);
    }

    /**
     * The sitting cat as zones (the same drawing as CatArt.sitBody, eyes
     * left to the runtime), its right foreleg raised to (px, py) instead of
     * resting; 40x30 so the paw can reach outside the body.
     */
    static Canvas catSitArm(int px, int py) {
        Canvas c = new Canvas(40, 30);
        Canvas e = new Canvas(28, 30);                     // mirrored parts, on the body's own width
        fillMirror(e, CatArt.SIT_EAR, FUR);
        fill(c, CatArt.SIT_BODY, FUR);
        fill(c, CatArt.SIT_EAR, FUR);
        paste(c, e, 0, 0, false);
        fill(c, CatArt.SIT_HEAD, FUR);
        Canvas ei = new Canvas(28, 30);
        fillMirror(ei, CatArt.SIT_EAR_IN, INNER_EAR);
        fill(c, CatArt.SIT_EAR_IN, INNER_EAR);
        paste(c, ei, 0, 0, false);
        fill(c, CatArt.SIT_BIB, CREAM);
        fill(c, CatArt.SIT_MUZZLE, CREAM);
        fill(c, CatArt.SIT_PAW, CREAM);
        fill(c, CatArt.SIT_HIND, CREAM);
        Canvas h = new Canvas(28, 30);
        fillMirror(h, CatArt.SIT_HIND, CREAM);
        paste(c, h, 0, 0, false);
        fill(c, CatArt.SIT_EYE_RING, RING);
        fill(c, CatArt.SIT_EYE_IRIS, IRIS);
        Canvas r = new Canvas(28, 30);
        fillMirror(r, CatArt.SIT_EYE_RING, RING);
        fillMirror(r, CatArt.SIT_EYE_IRIS, IRIS);
        paste(c, r, 0, 0, false);
        rect(c, 13, 13, 2, 1, NOSE);
        px(c, 12, 14, MOUTH);                                  // the ":3" mouth
        px(c, 13, 15, MOUTH);
        px(c, 14, 15, MOUTH);
        px(c, 15, 14, MOUTH);
        px(c, 10, 28, SEP);
        // tabby markings (the black cat's palette hides them)
        mark(c, 11, 6, 1, 2);
        mark(c, 13, 5, 2, 2);
        mark(c, 16, 6, 1, 2);
        for (int[] m : new int[][] {{3, 11}, {23, 11}, {3, 13}, {23, 13}, {5, 20}, {21, 20}, {4, 23}, {22, 23}}) {
            mark(c, m[0], m[1], 2, 1);
        }
        limbOver(c, stroke(c, 4, new float[][] {{18, 21}, {px, py}}), FUR);
        return c;
    }

    private static Anim catStretch() {
        Palette p = CatArt.TABBY;
        Seq s = new Seq();
        s.add(catSit(p, OPEN, 0), 0.3f);
        s.add(catStand(p), 0.25f);
        for (int i = 0; i < 4; i++) {
            s.add(new F().put(render(bow(false, i % 2), p), 1, GROUND - 28), 0.45f);
        }
        s.add(catStand(p), 0.2f);
        s.add(catMouth(catSit(p, CLOSED, 0), p, 1), 0.2f);
        s.add(catMouth(catSit(p, CLOSED, 1), p, 2), 0.9f);
        s.add(catMouth(catSit(p, CLOSED, 2), p, 1), 0.15f);
        s.add(catSit(p, HAPPY, 0), 0.6f);
        s.add(catSit(p, OPEN, 0), 0.3f);
        return s.anim();
    }

    /**
     * The black cat's Halloween stretch, side on: legs straight and tall,
     * back arched high with its fur standing up in spikes, tail bolt
     * upright. amp is the arch height. 44x32, ground 30.
     */
    private static Canvas arch(int amp, int bristle) {
        Canvas c = new Canvas(W, 32);
        int oy = 2;
        int lift = amp > 0 ? 1 : 0;
        // tail straight up from the rump, bristling
        if (amp > 0) {
            int rump = 12 + oy - lift;
            rect(c, 5, rump - 11, 3, 12, FUR);
            for (int y = rump - 11; y < rump; y += 2) {
                px(c, 4, y + bristle, FUR);
                px(c, 8, y + 1 - bristle, FUR);
            }
        }
        // legs planted wide and straight: hind ones back, fore ones forward
        int top = 18 + oy - lift;
        draw(c, limb(c, new float[][] {{13, top}, {12, 30}}, new int[] {4}), false, CREAM_FAR);
        draw(c, limb(c, new float[][] {{24, top}, {27, 30}}, new int[] {4}), false, CREAM_FAR);
        draw(c, limb(c, new float[][] {{10, top}, {8, 30}}, new int[] {4}), true, CREAM);
        draw(c, limb(c, new float[][] {{21, top}, {23, 30}}, new int[] {4}), true, CREAM);
        Canvas b = new Canvas(W, 32);
        fill(b, CatArt.C_BODY, FUR, 0, oy - lift, false);
        fill(b, CatArt.C_CHEST, CREAM, 0, oy - lift, false);
        Canvas a = new Canvas(W, 32);
        for (int x = 0; x < W; x++) {
            int dy = x < 5 || x > 28 ? 0
                    : -Math.round(amp * (float) Math.sin(Math.PI * (x - 5) / 23.0));
            for (int y = 0; y < 32; y++) {
                int z = b.z[y * W + x];
                if (z != 0) {
                    a.set(x, y + dy, z);
                }
            }
        }
        // fur standing up along the arched back
        if (amp > 1) {
            for (int x = 7 + bristle; x <= 24; x += 2) {
                for (int y = 0; y < 32; y++) {
                    if (a.get(x, y) != 0) {
                        a.set(x, y - 1, FUR);
                        break;
                    }
                }
            }
        }
        over(c, a);
        int hd = amp > 0 ? oy + 2 : oy - 1;                 // head low, under the hump
        Canvas h = catSide(null, 0, hd, true);
        // a stretching, half-open mouth
        rect(h, 32, 13 + hd, 2, 2, INNER_EAR);
        over(c, h);
        return c;
    }

    private static Anim blackCatStretch() {
        Palette p = CatArt.BLACK;
        Seq s = new Seq();
        s.add(catSit(p, OPEN, 0), 0.3f);
        s.add(catStand(p), 0.25f);
        s.add(new F().put(render(arch(2, 0), p), 0, GROUND - 30), 0.2f);
        for (int i = 0; i < 6; i++) {
            s.add(new F().put(render(arch(6, i % 2), p), 0, GROUND - 30), 0.25f);
        }
        s.add(new F().put(render(arch(2, 0), p), 0, GROUND - 30), 0.15f);
        s.add(catStand(p), 0.2f);
        s.add(catMouth(catSit(p, CLOSED, 0), p, 1), 0.2f);
        s.add(catMouth(catSit(p, CLOSED, 1), p, 2), 0.9f);
        s.add(catMouth(catSit(p, CLOSED, 2), p, 1), 0.15f);
        s.add(catSit(p, HAPPY, 0), 0.5f);
        s.add(catSit(p, OPEN, 0), 0.3f);
        return s.anim();
    }

    /**
     * Lapping from a bowl, side on: standing, head down in the bowl, eyes
     * shut, the tongue flicking into the water. dog uses the doge.
     */
    private static Canvas lap(boolean dog, int f) {
        Canvas c = new Canvas(W, 30);
        int gy = 28;
        if (!dog) {
            Canvas t = new Canvas(W, 30);
            fill(t, CatArt.C_TAIL, FUR, 0, 1, false);
            mark(t, 1, 8, 3, 1);
            mark(t, 1, 11, 3, 1);
            shiftRows(t, 0, 9, f % 2 == 0 ? 0 : 1);
            paste(c, t, 0, 0, false);
        }
        int paw = dog ? 2 : 1;
        draw(c, hind(c, 14, 21, gy, 2), false, CREAM_FAR, paw);
        draw(c, front(c, 25, 21, gy, 3), false, CREAM_FAR, paw);
        draw(c, hind(c, 10, 21, gy, 1), true, CREAM, paw);
        draw(c, front(c, 22, 21, gy, 2), true, CREAM, paw);
        Canvas b = new Canvas(W, 30);
        fill(b, CatArt.C_BODY, FUR, 0, 1, false);
        if (dog) {
            paint(b, 0, 23, W, 2, FUR, CREAM);
        } else {
            mark(b, 11, 13, 1, 3);
            mark(b, 14, 13, 1, 3);
            mark(b, 17, 13, 1, 3);
        }
        fill(b, CatArt.C_CHEST, CREAM, 0, 1, false);
        over(c, b);
        if (dog) {
            Canvas t = new Canvas(W, 30);
            fill(t, DogArt.D_TAIL, FUR, 0, 2, false);
            rect(t, 8, 11, 3, 2, CREAM);
            px(t, 9, 11, FUR);
            shiftRows(t, 0, 15, f % 2 == 0 ? 0 : 1);
            over(c, t);
        }
        int hx = 3, hy = 7;
        Canvas h = new Canvas(W, 30);
        if (dog) {
            fill(h, DogArt.D_FAR_EAR, FUR_FAR, hx, hy, false);
            fill(h, DogArt.D_HEAD, FUR, hx, hy, false);
            fill(h, DogArt.D_EAR, FUR, hx, hy, false);
            fill(h, DogArt.D_EAR_IN, INNER_EAR, hx, hy, false);
            fill(h, DogArt.D_CHEEK, CREAM, hx, hy, false);
            fill(h, DogArt.D_MUZZLE, CREAM, hx, hy, false);
            mark(h, 24 + hx, 5 + hy, 2, 1);
            rect(h, 27 + hx, 9 + hy, 4, 1, LINE);
            rect(h, 28 + hx, 6 + hy, 2, 1, CREAM);
            rect(h, 34 + hx, 10 + hy, 2, 2, NOSE);
        } else {
            paste(h, catSide(null, hx, hy, true), 0, 0, false);
        }
        over(c, h);
        return c;
    }

    private static Anim catWater() {
        Palette p = CatArt.TABBY;
        Seq s = new Seq();
        for (int x : new int[] {44, 40, 36, 32}) {
            s.add(catSit(p, OPEN, 0).put(bowl(0), x, GROUND - 6), 0.1f);
        }
        s.add(catSit(p, OPEN, 0).put(bowl(0), 30, GROUND - 6), 0.3f);
        s.add(catStand(p).put(bowl(0), 30, GROUND - 6), 0.2f);
        for (int i = 0; i < 8; i++) {
            F f = new F().put(render(lap(false, i), p), 0, GROUND - 28)
                    .put(bowl(i % 3), 30, GROUND - 6);
            if (i % 2 == 0) {
                f.rect(37, 34, 1, 2, p.colour(INNER_EAR));       // tongue into the water
            }
            s.add(f, 0.18f);
        }
        s.add(catStand(p).put(bowl(1), 30, GROUND - 6), 0.2f);
        s.add(catMouth(catSit(p, CLOSED, 0), p, 3).put(bowl(1), 30, GROUND - 6), 0.35f);
        s.add(catSit(p, CLOSED, 1).put(bowl(1), 30, GROUND - 6), 0.15f);
        s.add(catMouth(catSit(p, CLOSED, 2), p, 3).put(bowl(1), 30, GROUND - 6), 0.35f);
        for (int x : new int[] {34, 38, 42}) {
            s.add(catSit(p, HAPPY, 0).put(bowl(0), x, GROUND - 6), 0.1f);
        }
        s.add(catSit(p, HAPPY, 0), 0.4f);
        s.add(catSit(p, OPEN, 0), 0.3f);
        return s.anim();
    }

    static final int[][] CAT_WHISKERS = {
            {0, 13}, {1, 13}, {0, 15}, {1, 15}, {27, 13}, {26, 13}, {27, 15}, {26, 15}};

    /** The black cat sitting with its paw raised to (px, py) in body coordinates. */
    static F blackCatArm(int px, int py, int eyes, int tail) {
        Palette p = CatArt.BLACK;
        F f = new F();
        f.put(CatArt.sitTail(p, tail), 25, H - 23);
        f.put(render(catSitArm(px, py), p, CAT_WHISKERS), 4, H - 30);
        eyes(f, BLACK_EYE, eyes);
        return f;
    }

    private static Anim blackCatWater() {
        Palette p = CatArt.BLACK;
        int gx = 34, gy = GROUND - 12;
        Seq s = new Seq();
        s.add(catSit(p, OPEN, 0).put(glass(5), gx, gy), 0.5f);
        s.add(blackCatArm(24, 15, OPEN, 0).put(glass(5), gx, gy), 0.25f);
        s.add(blackCatArm(29, 13, OPEN, 0).put(glass(5), gx, gy), 0.2f);
        Canvas splash = water();
        drop(splash, 35, 26);
        drop(splash, 41, 25);
        drop(splash, 38, 23);
        s.add(blackCatArm(33, 19, OPEN, 1).put(glass(6), gx, gy)
                .put(render(splash, PROP), 0, 0), 0.35f);
        Canvas drip = water();
        drop(drip, 32, 32);
        s.add(blackCatArm(28, 14, OPEN, 2).put(glass(6), gx, gy)
                .put(render(drip, PROP), 0, 0), 0.25f);
        for (int i = 0; i < 6; i++) {
            F f = blackCatArm(17, 15 + i % 2, CLOSED, i % 3).put(glass(6), gx, gy);
            if (i % 2 == 0) {
                f.rect(17, 28, 2, 2, p.colour(INNER_EAR));       // licking the wet paw
            }
            s.add(f, 0.2f);
        }
        s.add(catSit(p, HAPPY, 0).put(glass(6), gx, gy), 0.5f);
        s.add(catSit(p, HAPPY, 0), 0.3f);
        s.add(catSit(p, OPEN, 0), 0.3f);
        return s.anim();
    }

    // ============================================================== DOGE

    static F dogeSit(int eyes, int tail) {
        F f = new F();
        f.put(DogArt.sitTail(tail), 28, H - 23);
        f.put(DogArt.sitBody(), 4, H - 30);
        eyes(f, DOGE_EYE, eyes);
        return f;
    }

    static F dogeStand() {
        return new F().put(DogArt.walk(0), 3, GROUND - 28);
    }

    private static Anim dogeStretch() {
        Palette p = DogArt.SHIBA;
        Seq s = new Seq();
        s.add(dogeSit(OPEN, 0), 0.3f);
        s.add(dogeStand(), 0.25f);
        for (int i = 0; i < 4; i++) {
            s.add(new F().put(render(bow(true, i % 2), p), 0, GROUND - 28), 0.4f);
        }
        s.add(dogeStand(), 0.2f);
        // the full-body shake: body and head swing opposite ways, ears flap
        int line = p.outline;
        for (int i = 0; i < 10; i++) {
            int d = i % 2 == 0 ? 3 : -3;
            F f = dogeSit(CLOSED, i % 3);
            f.shiftRows(12, 32, d)                               // head swings
                    .shiftRows(12, 20, -d / 3)                   // ears flop behind it
                    .shiftRows(33, H - 1, -d / 3);               // body counter-swings
            for (int k = 0; k < 2; k++) {                        // motion lines
                int x = d > 0 ? 39 + k * 2 : 4 - k * 2;
                f.rect(x, 20 + k * 4, 1, 4, line);
                f.rect(43 - x, 26 + k * 4, 1, 3, line);
            }
            s.add(f, 0.08f);
        }
        s.add(dogeSit(HAPPY, 0), 0.6f);
        s.add(dogeSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    private static Anim dogeWater() {
        Palette p = DogArt.SHIBA;
        Seq s = new Seq();
        for (int x : new int[] {44, 40, 36, 32}) {
            s.add(dogeSit(OPEN, 0).put(bowl(0), x, GROUND - 6), 0.1f);
        }
        s.add(dogeStand().put(bowl(0), 30, GROUND - 6), 0.25f);
        int[][][] splashes = {
                {{40, 30}, {35, 16}, {41, 23}},                  // flung up and out of the bowl,
                {{41, 27}, {38, 13}, {30, 15}, {41, 33}},        // clear of its face
                {{40, 31}, {36, 18}, {41, 21}},
                {{41, 25}, {33, 12}, {39, 16}, {41, 32}}};
        for (int i = 0; i < 10; i++) {
            Canvas w = water();
            for (int[] d : splashes[i % 4]) {
                bigDrop(w, d[0], d[1]);
            }
            F f = new F().put(render(lap(true, i), p), 0, GROUND - 28)
                    .put(bowl(i % 3), 30, GROUND - 6).put(render(w, PROP), 0, 0);
            if (i % 2 == 0) {
                f.rect(38, 34, 2, 2, p.colour(INNER_EAR));
            }
            s.add(f, 0.15f);
        }
        s.add(dogeStand().put(bowl(1), 30, GROUND - 6), 0.2f);
        for (int i = 0; i < 3; i++) {
            Canvas w = water();
            drop(w, 17, 34 + i * 2);                            // a drip off the chin
            F f = dogeSit(HAPPY, i % 3).put(bowl(1), 30, GROUND - 6).put(render(w, PROP), 0, 0);
            f.rect(18, 30, 2, 2, p.colour(INNER_EAR));           // tongue out, pleased
            s.add(f, 0.25f);
        }
        for (int x : new int[] {34, 38, 42}) {
            s.add(dogeSit(HAPPY, 0).put(bowl(0), x, GROUND - 6), 0.1f);
        }
        s.add(dogeSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    // =========================================================== PIKACHU

    static F pikaSit(int eyes, int tail) {
        F f = new F();
        f.put(PikaArt.sitTail(tail), 25, H - 24);
        f.put(PikaArt.sitBody(), 4, H - 30);
        eyes(f, PIKA_EYE, eyes);
        return f;
    }

    /** Pikachu's open, delighted mouth on a sitting frame. */
    static F pikaMouth(F f) {
        Palette p = PikaArt.PIKA;
        return f.rect(16, 30, 6, 1, p.colour(MOUTH)).rect(17, 31, 4, 1, p.colour(INNER_EAR))
                .rect(17, 32, 4, 1, p.colour(MOUTH));
    }

    /**
     * Up on its tiptoes with both arms stretched overhead, ears straight
     * up, eyes squeezed shut, mouth open; lift 0-4 rows. The sitting
     * drawing two columns in, so the arms clear the cheeks. 34x36, ground 34.
     */
    private static Canvas pikaReach(int lift, int tremble) {
        Canvas c = new Canvas(34, 36);
        int ox = 2, dy = 5 - lift;
        for (int x : new int[] {9, 18}) {                      // legs and tiptoe feet
            if (29 + dy < 33) {
                rect(c, ox + x, 29 + dy, 3, 33 - (29 + dy), FUR_FAR);
            }
            rect(c, ox + x - 1, 33, 5, 2, FUR);
        }
        fill(c, PikaArt.SIT_BODY, FUR, ox, dy, false);
        paint(c, ox + 22, 23 + dy, 8, 7, FUR, FUR_FAR);
        Canvas ears = new Canvas(30, 36);
        fillPair(ears, PikaArt.SIT_EAR, FUR);
        paint(ears, 0, 0, 30, 3, FUR, ACC1);
        paste(c, ears, ox, dy - (lift > 2 ? 1 : 0), false);
        fillOver(c, PikaArt.SIT_HEAD, FUR, ox, dy, false);
        paint(c, ox + 25, 15 + dy, 5, 6, FUR, FUR_FAR);
        paint(c, ox + 10, 10 + dy, 4, 1, FUR, ACC4);
        paint(c, ox + 8, 11 + dy, 3, 1, FUR, ACC4);
        Canvas cheeks = new Canvas(30, 36);
        fillPair(cheeks, PikaArt.SIT_CHEEK, ACC3);
        paste(c, cheeks, ox, dy, false);
        rect(c, ox + 14, 17 + dy, 2, 1, NOSE);
        for (int ex : new int[] {8, 18}) {                     // squeezed shut
            rect(c, ox + ex, 14 + dy, 4, 1, LINE);
            px(c, ox + ex - 1, 15 + dy, LINE);
            px(c, ox + ex + 4, 15 + dy, LINE);
        }
        rect(c, ox + 12, 18 + dy, 6, 1, MOUTH);
        rect(c, ox + 13, 19 + dy, 4, 1, INNER_EAR);
        rect(c, ox + 13, 20 + dy, 4, 1, MOUTH);
        // arms from the shoulders, out past the cheeks and up overhead
        int up = Math.min(lift, 4) * 3 + tremble;
        float sy = 24 + dy, py = 24 + dy - 2 - up;
        limbOver(c, stroke(c, 3, new float[][] {{9, sy}, {3, Math.min(sy, py + 6)}, {4, py}}), FUR);
        limbOver(c, stroke(c, 3, new float[][] {{25, sy}, {31, Math.min(sy, py + 6)}, {30, py}}), FUR);
        return c;
    }

    private static Anim pikaStretch() {
        Palette p = PikaArt.PIKA;
        int oy = GROUND - 34;
        Seq s = new Seq();
        s.add(pikaSit(OPEN, 0), 0.3f);
        s.add(new F().put(PikaArt.sitTail(0), 25, H - 25).put(render(pikaReach(2, 0), p), 2, oy), 0.15f);
        s.add(new F().put(PikaArt.sitTail(1), 25, H - 27).put(render(pikaReach(3, 0), p), 2, oy), 0.15f);
        int[][] sparks = {{1, 22}, {37, 21}, {3, 29}, {35, 28}};
        for (int i = 0; i < 6; i++) {
            F f = new F().put(PikaArt.sitTail(i % 3), 25, H - 28)
                    .put(render(pikaReach(4, i % 2), p), 2, oy);
            int[] a = sparks[(i * 2) % 4], b = sparks[(i * 2 + 1) % 4];
            spark(f, a[0], a[1]);
            spark(f, b[0], b[1]);
            s.add(f, 0.22f);
        }
        s.add(new F().put(PikaArt.sitTail(0), 25, H - 25).put(render(pikaReach(2, 0), p), 2, oy), 0.15f);
        s.add(pikaMouth(pikaSit(HAPPY, 1)), 0.6f);
        s.add(pikaSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    private static Anim pikaWater() {
        Palette p = PikaArt.PIKA;
        PoseArt.Grid paw = PathArt.paw(p, FUR);
        Seq s = new Seq();
        s.add(pikaSit(OPEN, 0).put(bottle(4, -1), 15, 31).put(paw, 12, 34).put(paw, 21, 34), 0.4f);
        int[] levels = {4, 5, 5, 6, 7, 7, 8, 9, 10};
        for (int i = 0; i < levels.length; i++) {
            F f = pikaSit(CLOSED, i % 3).put(bottle(levels[i], 9 - i % 3 * 2), 15, 30)
                    .put(paw, 12, 33).put(paw, 21, 33);
            if (i % 2 == 1) {
                f.shift(0, -1);                                  // gulp
            }
            s.add(f, 0.22f);
        }
        s.add(pikaMouth(pikaSit(HAPPY, 0)).put(bottle(10, -1), 15, 32)
                .put(paw, 12, 35).put(paw, 21, 35), 0.4f);
        F ahh = pikaMouth(pikaSit(HAPPY, 1));
        spark(ahh, 3, 20);
        spark(ahh, 35, 20);
        s.add(ahh, 0.5f);
        s.add(pikaSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    // ========================================================== SQUIRTLE

    static F squirtSit(int eyes, int tail) {
        F f = new F();
        f.put(SquirtArt.sitTail(tail), 27, H - 23);
        f.put(SquirtArt.sitBody(), 4, H - 30);
        eyes(f, SQUIRT_EYE, eyes);
        return f;
    }

    /**
     * Popped out of its shell in a star: head up, arms flung up and out,
     * legs kicked down and out, eyes squeezed happy. 34x36, ground 33.
     */
    private static Canvas star(int tremble) {
        Canvas c = new Canvas(34, 36);
        int ox = 2, dy = 1;
        int t = tremble;
        // limbs first, so they come out from behind the shell
        draw(c, limb(c, new float[][] {{1 - t, 12 + dy - t}, {8, 21 + dy}}, new int[] {4}), true, FUR, 0);
        draw(c, limb(c, new float[][] {{32 + t, 12 + dy - t}, {25, 21 + dy}}, new int[] {4}), true, FUR, 0);
        draw(c, limb(c, new float[][] {{10, 26 + dy}, {4 - t, 33}}, new int[] {5}), true, FUR, 0);
        draw(c, limb(c, new float[][] {{23, 26 + dy}, {29 + t, 33}}, new int[] {5}), true, FUR, 0);
        fill(c, SquirtArt.SIT_SHELL, ACC1, ox, dy, false);
        paint(c, 25, 17 + dy, 9, 13, ACC1, ACC2_FAR);
        fill(c, SquirtArt.SIT_RIM, ACC2, ox, dy, false);
        fill(c, SquirtArt.SIT_BELLY, ACC3, ox, dy, false);
        paint(c, 11, 22 + dy, 12, 1, ACC3, ACC4);
        paint(c, 10, 25 + dy, 14, 1, ACC3, ACC4);
        paint(c, 16, 19 + dy, 2, 3, ACC3, ACC4);
        fillOver(c, SquirtArt.SIT_HEAD, FUR, ox, dy - 2, false);
        paint(c, 23, 8 + dy, 9, 7, FUR, FUR_FAR);
        paint(c, 12, 4 + dy, 4, 1, FUR, CREAM);
        paint(c, 10, 5 + dy, 3, 1, FUR, CREAM);
        for (int ex : new int[] {9, 21}) {
            rect(c, ex, 9 + dy, 4, 1, LINE);
            px(c, ex - 1, 10 + dy, LINE);
            px(c, ex + 4, 10 + dy, LINE);
        }
        int mx = 13, my = 13 + dy;                              // wide open smile
        px(c, mx, my, MOUTH);
        px(c, mx + 7, my, MOUTH);
        rect(c, mx + 1, my + 1, 6, 1, MOUTH);
        px(c, mx + 1, my + 2, MOUTH);
        rect(c, mx + 2, my + 2, 4, 1, INNER_EAR);
        px(c, mx + 6, my + 2, MOUTH);
        rect(c, mx + 2, my + 3, 4, 1, MOUTH);
        return c;
    }

    private static Anim squirtleStretch() {
        Palette p = SquirtArt.SQUIRT;
        Seq s = new Seq();
        s.add(squirtSit(OPEN, 0), 0.3f);
        s.add(new F().put(SquirtArt.shell(false, 0), 4, GROUND - 20), 0.5f);
        s.add(new F().put(SquirtArt.shell(false, 1), 4, GROUND - 20), 0.15f);
        s.add(new F().put(SquirtArt.shell(false, 0), 4, GROUND - 20), 0.25f);
        for (int i = 0; i < 6; i++) {
            s.add(new F().put(render(star(i % 2), p), 2, GROUND - 33), 0.25f);
        }
        s.add(squirtSit(HAPPY, 1), 0.6f);
        s.add(squirtSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    /** Squirtle's mouth wide open, gulping. */
    static F squirtGulp(F f) {
        Palette p = SquirtArt.SQUIRT;
        return f.rect(16, 27, 6, 4, p.colour(MOUTH)).rect(17, 28, 4, 2, p.colour(INNER_EAR));
    }

    private static Anim squirtleWater() {
        Seq s = new Seq();
        s.add(squirtGulp(squirtSit(HAPPY, 0)), 0.3f);
        for (int top : new int[] {18, 9, 1}) {                 // the jet shoots up
            Canvas w = water();
            rect(w, 18, top, 2, 27 - top, FUR);
            rect(w, 18, top, 1, 27 - top, CREAM);
            s.add(squirtGulp(squirtSit(HAPPY, 0)).put(render(w, PROP), 0, 0), 0.12f);
        }
        // a fountain: the jet bursts at the top and arcs down either side,
        // clear of the face
        int[][] arc = {{14, 1}, {11, 3}, {8, 6}, {6, 10}, {5, 15}, {4, 21}};
        for (int i = 0; i < 9; i++) {
            Canvas w = water();
            rect(w, 18, 2, 2, 25, FUR);
            rect(w, 18 + i % 2, 2, 1, 25, CREAM);
            rect(w, 16, 1, 6, 2, FUR);                          // the burst at the top
            for (int k = i % 3; k < arc.length; k += 3) {
                bigDrop(w, arc[k][0], arc[k][1]);
                bigDrop(w, 34 - arc[k][0], arc[k][1]);
            }
            s.add(squirtGulp(squirtSit(HAPPY, i % 3)).put(render(w, PROP), 0, 0), 0.13f);
        }
        int[] fall = {3, 10, 17, 23};                           // the last of it, into its mouth
        for (int y : fall) {
            Canvas w = water();
            bigDrop(w, 17, y);
            bigDrop(w, 20, y - 5);
            s.add(squirtGulp(squirtSit(HAPPY, 1)).put(render(w, PROP), 0, 0), 0.12f);
        }
        Canvas wet = water();
        drop(wet, 10, 20);
        drop(wet, 28, 22);
        s.add(squirtSit(HAPPY, 0).put(render(wet, PROP), 0, 0), 0.5f);
        s.add(squirtSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    // ========================================================= BULBASAUR

    static F bulbaSit(int eyes, int bulbFrame) {
        F f = new F();
        f.put(BulbArt.sitTail(bulbFrame), 7, H - 30);
        f.put(BulbArt.sitBody(), 4, H - 30);
        eyes(f, BULBA_EYE, eyes);
        return f;
    }

    /**
     * Both vines rising out of the bulb's flanks and reaching overhead like
     * stretching arms, tips curling in; grow 0..1, sway for the tremble.
     */
    private static PoseArt.Grid vines(float grow, int sway) {
        Canvas c = new Canvas(W, H);
        float[][] path = {{11, 21}, {8, 15}, {6, 9}, {6 + sway, 4}, {8 + sway, 1}, {11 + sway, 1}};
        // total length, then walk it up to `grow`
        float total = 0f;
        for (int i = 1; i < path.length; i++) {
            total += dist(path[i - 1], path[i]);
        }
        float left = total * grow;
        for (int i = 1; i < path.length && left > 0f; i++) {
            float seg = dist(path[i - 1], path[i]);
            float t1 = Math.min(1f, left / seg);
            int n = Math.max(1, Math.round(seg * t1 * 2));
            for (int k = 0; k <= n; k++) {
                float t = t1 * k / n;
                int x = Math.round(path[i - 1][0] + (path[i][0] - path[i - 1][0]) * t);
                int y = Math.round(path[i - 1][1] + (path[i][1] - path[i - 1][1]) * t);
                rect(c, x, y, 2, 2, ACC1);
                rect(c, 38 - x, y, 2, 2, ACC1);               // mirrored about the face
                px(c, x + 1, y + 1, ACC1_FAR);
                px(c, 38 - x, y + 1, ACC1_FAR);
            }
            left -= seg;
        }
        return render(c, BulbArt.BULBA);
    }

    private static float dist(float[] a, float[] b) {
        float dx = a[0] - b[0], dy = a[1] - b[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static F bulbaWithVines(int eyes, int bulbFrame, PoseArt.Grid v) {
        F f = new F();
        f.put(BulbArt.sitTail(bulbFrame), 7, H - 30);
        f.put(v, 0, 0);
        f.put(BulbArt.sitBody(), 4, H - 30);
        eyes(f, BULBA_EYE, eyes);
        return f;
    }

    private static Anim bulbaStretch() {
        Seq s = new Seq();
        s.add(bulbaSit(OPEN, 0), 0.3f);
        for (float g : new float[] {0.25f, 0.5f, 0.75f}) {
            s.add(bulbaWithVines(HAPPY, 0, vines(g, 0)), 0.12f);
        }
        int[][] leaves = {{3, 14}, {33, 10}, {14, 6}, {27, 16}};
        for (int i = 0; i < 6; i++) {
            F f = bulbaWithVines(HAPPY, i % 2 == 0 ? 1 : 0, vines(1f, i % 2));
            int[] a = leaves[i % 4];
            leaf(f, a[0], a[1] + i);
            leaf(f, leaves[(i + 2) % 4][0], leaves[(i + 2) % 4][1] + i);
            s.add(f, 0.25f);
        }
        for (float g : new float[] {0.6f, 0.3f}) {
            s.add(bulbaWithVines(HAPPY, 0, vines(g, 0)), 0.12f);
        }
        s.add(bulbaSit(HAPPY, 1), 0.5f);
        s.add(bulbaSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    private static Anim bulbaWater() {
        PoseArt.Grid cl = cloud();
        int cx = 9;
        Seq s = new Seq();
        s.add(bulbaSit(OPEN, 0).put(cl, cx, 0), 0.4f);
        for (int i = 0; i < 10; i++) {
            Canvas w = water();
            for (int k = 0; k < 5; k++) {
                int x = cx + 3 + k * 4;
                int y = 9 + (i + k * 2) % 4;
                drop(w, x, y);
            }
            F f = bulbaSit(i < 2 ? OPEN : HAPPY, i % 3 == 2 ? 1 : 0).put(cl, cx, 0)
                    .put(render(w, PROP), 0, 0);
            s.add(f, 0.16f);
        }
        F perk = bulbaSit(HAPPY, 1);
        spark(perk, 10, 13);
        spark(perk, 27, 11);
        s.add(perk, 0.45f);
        F perk2 = bulbaSit(HAPPY, 1);
        spark(perk2, 13, 10);
        spark(perk2, 24, 15);
        s.add(perk2, 0.45f);
        s.add(bulbaSit(OPEN, 0), 0.3f);
        return s.anim();
    }

    // ========================================================== GOLDFISH

    static final String DORSAL_FLARE = "M12 2H14V3H16V4H18V5H20V6H21V8H22V10H14V9H13V8H12V6H11V3H12Z";
    static final String PEC_FLARE = "M15 17H20V18H22V19H23V20H22V21H15V20H14V18H15Z";

    /** The goldfish body; flare spreads its fins wide. Same drawing as FishArt. */
    static PoseArt.Grid fishBody(boolean flare) {
        if (!flare) {
            return FishArt.body();
        }
        Canvas c = new Canvas(32, 30);
        fill(c, DORSAL_FLARE, ACC1);
        paint(c, 14, 3, 1, 7, ACC1, ACC2);
        paint(c, 17, 4, 1, 6, ACC1, ACC2);
        paint(c, 20, 6, 1, 4, ACC1, ACC2);
        fill(c, "M11 24H19V25H18V27H13V26H11Z", ACC1);
        fill(c, FishArt.BODY, FUR);
        paint(c, 0, 20, 32, 4, FUR, CREAM);
        paint(c, 6, 19, 22, 1, FUR, CREAM);
        int[][] scales = {{9, 13}, {13, 13}, {17, 13}, {7, 15}, {11, 15}, {15, 15},
                {9, 17}, {13, 17}, {7, 19}, {11, 19}};
        for (int[] sc : scales) {
            paint(c, sc[0], sc[1], 2, 1, FUR, PATTERN);
        }
        int[][] gill = {{21, 12}, {20, 13}, {20, 14}, {19, 15}, {19, 16}, {19, 17},
                {19, 18}, {20, 19}, {20, 20}, {21, 21}};
        for (int[] g : gill) {
            px(c, g[0], g[1], SEP);
        }
        fillOver(c, PEC_FLARE, ACC1);
        paint(c, 16, 18, 6, 1, ACC1, ACC2);
        rect(c, 28, 18, 2, 1, MOUTH);
        return render(c, FishArt.GOLDFISH);
    }

    static F fish(int eyes, int tail, boolean flare, int dx) {
        F f = new F();
        f.put(FishArt.tail(tail), 2, H - 26);
        f.put(fishBody(flare), 4, H - 30);
        eyes(f, FISH_EYE, eyes);
        return dx == 0 ? f : f.shift(dx, 0);
    }

    /** Column offsets bending the fish: amp > 0 arches its back up. */
    static int[] arc(int amp) {
        int[] dy = new int[W];
        for (int x = 0; x < W; x++) {
            float u = (x - 20) / 18f;
            dy[x] = Math.round(amp * u * u);
        }
        return dy;
    }

    private static Anim fishStretch() {
        Seq s = new Seq();
        s.add(fish(OPEN, 0, false, 0), 0.3f);
        s.add(fish(CLOSED, 1, true, 0).shift(0, -2).bend(arc(2)), 0.2f);
        for (int i = 0; i < 6; i++) {
            int amp = i % 2 == 0 ? 5 : -5;                     // bow one way, then the other
            s.add(fish(CLOSED, 1 + i % 2, true, 0).shift(0, amp > 0 ? -6 : 0).bend(arc(amp)), 0.3f);
        }
        s.add(fish(HAPPY, 1, true, 0), 0.4f);
        s.add(fish(HAPPY, 0, false, 0), 0.4f);
        s.add(fish(OPEN, 0, false, 0), 0.3f);
        return s.anim();
    }

    private static Anim fishWater() {
        int mouth = PoseArt.rgba("2A2230");
        Seq s = new Seq();
        for (int y : new int[] {0, 6, 12, 18, 24}) {
            Canvas w = water();
            bigDrop(w, 38, y);
            s.add(fish(OPEN, y % 3, false, 0).put(render(w, PROP), 0, 0), 0.16f);
        }
        Canvas w = water();
        bigDrop(w, 38, 27);
        s.add(fish(OPEN, 1, false, 2).rect(34, 29, 2, 3, mouth).put(render(w, PROP), 0, 0), 0.14f);
        s.add(fish(CLOSED, 2, false, 3).rect(35, 30, 2, 2, mouth), 0.2f);
        s.add(fish(HAPPY, 0, false, 3), 0.25f);
        int[][] bubbles = {{36, 25}, {38, 19}, {35, 13}, {37, 7}};
        for (int i = 0; i < 6; i++) {
            F f = fish(HAPPY, i % 3, false, i < 3 ? 2 : 0);
            for (int k = 0; k <= Math.min(i, 3); k++) {
                int[] b = bubbles[Math.min(3, i - k)];
                if (i - k < 4) {
                    bubble(f, b[0] - (i < 3 ? 0 : 2), b[1]);
                }
            }
            s.add(f, 0.2f);
        }
        s.add(fish(OPEN, 0, false, 0), 0.3f);
        return s.anim();
    }
}
