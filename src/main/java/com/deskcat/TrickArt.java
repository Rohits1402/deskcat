package com.deskcat;

import static com.deskcat.PathArt.*;
import static com.deskcat.ReminderArt.*;

/**
 * Click tricks and idle fidgets, one of each per skin, in the same
 * window-space frame format as {@link ReminderArt}. Tricks that travel
 * (the doge's zoomies, Squirtle's spin, the goldfish's leap) carry
 * per-frame window offsets in their {@link ReminderArt.Anim}.
 */
final class TrickArt {

    private TrickArt() {
    }

    static Anim click(String skin) {
        switch (skin) {
            case "squirtle": return squirtleSpin();
            case "pikachu": return pikaBolt();
            case "blackcat": return blackCatMug();
            case "doge": return dogeZoomies();
            case "bulbasaur": return razorLeaf();
            case "goldfish": return fishLeap();
            default: return catYarn();
        }
    }

    static Anim fidget(String skin) {
        switch (skin) {
            case "squirtle": return squirtlePolish();
            case "pikachu": return pikaCheekRub();
            case "blackcat": return blackCatWash();
            case "doge": return dogeScratch();
            case "bulbasaur": return bulbaDroop();
            case "goldfish": return fishBubble();
            default: return catLick();
        }
    }

    /** Fed up with being clicked over and over. */
    static Anim annoyed(String skin) {
        switch (skin) {
            case "squirtle": return squirtleSulk();
            case "pikachu": return pikaPout();
            case "blackcat": return catSnub(CatArt.BLACK);
            case "doge": return dogeSulk();
            case "bulbasaur": return bulbaCross();
            case "goldfish": return fishSnub();
            default: return catSnub(CatArt.TABBY);
        }
    }

    // ============================================================ HELPERS

    private static final String[] ANGER = {
            ".R.R.",
            "RR.RR",
            ".....",
            "RR.RR",
            ".R.R.",
    };
    private static final char[] ANGER_KEYS = {'R'};
    private static final int[] ANGER_COLS = {PoseArt.rgba("E8473A")};

    /** The anime anger mark; big alternates its size so it throbs. */
    static void anger(F f, int x, int y, boolean big) {
        if (big) {
            f.map(ANGER, x, y, ANGER_KEYS, ANGER_COLS);
        } else {
            f.map(new String[] {"R.R", "...", "R.R"}, x + 1, y + 1, ANGER_KEYS, ANGER_COLS);
        }
    }

    /** A little "hmph" puff of air. */
    static void huff(F f, int x, int y) {
        f.put(PoseArt.steam(), x, y);
    }

    /** The frame mirrored left-right (a pet turning round). */
    static F mirror(F f) {
        F m = new F();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                m.g.set(W - 1 - x, y, f.g.get(x, y));
            }
        }
        return m;
    }

    /** The frame turned a quarter turn clockwise `quarters` times about its centre. */
    static F rotate(F f, int quarters) {
        F r = new F();
        double cx = (W - 1) / 2.0, cy = (H - 1) / 2.0;
        int q = ((quarters % 4) + 4) % 4;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double dx = x - cx, dy = y - cy, sx, sy;
                if (q == 1) {
                    sx = dy;
                    sy = -dx;
                } else if (q == 2) {
                    sx = -dx;
                    sy = -dy;
                } else if (q == 3) {
                    sx = -dy;
                    sy = dx;
                } else {
                    sx = dx;
                    sy = dy;
                }
                r.g.set(x, y, f.g.get((int) Math.round(cx + sx), (int) Math.round(cy + sy)));
            }
        }
        return r;
    }

    /** Shift one rectangle of the frame sideways (an ear twitch). */
    static F shiftRect(F f, int x0, int y0, int x1, int y1, int dx) {
        PoseArt.Grid g = f.g;
        int[] src = g.px.clone();
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                g.set(x, y, 0);
            }
            for (int x = x0; x <= x1; x++) {
                int c = src[y * W + x];
                if (c != 0) {
                    g.set(x + dx, y, c);
                }
            }
        }
        return f;
    }

    /** Darken every non-outline pixel: singed. */
    static F soot(F f, int outline) {
        for (int i = 0; i < f.g.px.length; i++) {
            int c = f.g.px[i];
            if (c == 0 || c == outline) {
                continue;
            }
            int r = (c >>> 24) & 0xFF, gg = (c >>> 16) & 0xFF, b = (c >>> 8) & 0xFF;
            f.g.px[i] = ((r * 11 / 20) << 24) | ((gg * 11 / 20) << 16) | ((b * 11 / 20) << 8) | 0xFF;
        }
        return f;
    }

    /** A limb mask laid in fur with its last pawRows rows in pawZone, as its own outlined layer. */
    private static PoseArt.Grid leg(Palette p, float[][] pts, int w, int pawZone, int pawRows) {
        Canvas c = new Canvas(W, H);
        Canvas m = stroke(c, w, pts);
        int bottom = -1, top = H;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (m.get(x, y) != 0) {
                    bottom = Math.max(bottom, y);
                    top = Math.min(top, y);
                }
            }
        }
        boolean pawUp = pts[pts.length - 1][1] < pts[0][1];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (m.get(x, y) != 0) {
                    boolean paw = pawUp ? y < top + pawRows : y > bottom - pawRows;
                    c.set(x, y, paw ? pawZone : FUR);
                }
            }
        }
        return render(c, p);
    }

    private static void star(F f, int x, int y) {
        spark(f, x, y);
    }

    // ============================================================== CAT

    static final Palette YARN = new Palette()
            .set(FUR, "E25A7A").set(FUR_FAR, "B23A58").set(CREAM, "F7A8BC")
            .outline("2A2230");
    static final String BALL = "M3 1H7V2H8V3H9V7H8V8H7V9H3V8H2V7H1V3H2V2H3Z";

    /** A ball of yarn, 10x10; phase 0-2 turns its strands as it rolls. */
    static PoseArt.Grid yarn(int phase) {
        Canvas c = new Canvas(10, 10);
        fill(c, BALL, FUR);
        int[][][] strands = {
                {{2, 6}, {3, 5}, {4, 4}, {5, 3}, {6, 2}, {4, 7}, {5, 6}, {6, 5}, {7, 4}},
                {{2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4}, {7, 4}, {3, 6}, {4, 6}, {5, 6}, {6, 6}},
                {{3, 2}, {4, 3}, {5, 4}, {6, 5}, {7, 6}, {2, 4}, {3, 5}, {4, 6}, {5, 7}}};
        for (int[] s : strands[phase % 3]) {
            paint(c, s[0], s[1], 1, 1, FUR, FUR_FAR);
        }
        paint(c, 3, 2, 2, 1, FUR, CREAM);
        return render(c, YARN);
    }

    /** The tabby sitting with its paw at (px, py) in body coordinates. */
    private static F catArm(int px, int py, int eyes, int tail) {
        Palette p = CatArt.TABBY;
        F f = new F();
        f.put(CatArt.sitTail(p, tail), 25, H - 23);
        f.put(render(catSitArm(px, py), p, CAT_WHISKERS), 4, H - 30);
        eyes(f, CAT_EYE, eyes);
        return f;
    }

    private static Anim catYarn() {
        Palette p = CatArt.TABBY;
        int gy = GROUND_Y_YARN;
        Seq s = new Seq();
        int[] in = {44, 40, 36};
        for (int i = 0; i < in.length; i++) {
            s.add(catSit(p, RIGHT, i % 3).put(yarn(i), in[i], gy), 0.09f);
        }
        s.add(catSit(p, RIGHT, 0).put(yarn(0), 32, gy), 0.35f);
        s.add(catArm(26, 13, RIGHT, 0).put(yarn(0), 32, gy), 0.18f);
        s.add(catArm(30, 20, RIGHT, 1).put(yarn(1), 35, gy), 0.08f);   // bat!
        s.add(catArm(27, 15, RIGHT, 1).put(yarn(2), 38, gy), 0.1f);
        s.add(catSit(p, RIGHT, 2).put(yarn(0), 41, gy), 0.1f);
        s.add(catSit(p, RIGHT, 2).put(yarn(2), 38, gy), 0.1f);          // bounces back
        s.add(catSit(p, RIGHT, 1).put(yarn(1), 35, gy), 0.1f);
        s.add(catSit(p, RIGHT, 0).put(yarn(0), 33, gy), 0.25f);
        s.add(catArm(25, 12, RIGHT, 0).put(yarn(0), 33, gy), 0.18f);
        s.add(catArm(31, 19, RIGHT, 1).put(yarn(1), 36, gy - 4), 0.07f); // a big swat
        s.add(catArm(28, 15, RIGHT, 2).put(yarn(2), 39, gy - 9), 0.07f);
        s.add(catSit(p, RIGHT, 2).put(yarn(0), 42, gy - 6), 0.07f);
        s.add(catSit(p, RIGHT, 1).put(yarn(1), 44, gy - 1), 0.07f);
        s.add(catSit(p, RIGHT, 0), 0.35f);
        s.add(catSit(p, HAPPY, 0), 0.45f);
        s.add(catSit(p, OPEN, 0), 0.2f);
        return s.anim();
    }

    private static final int GROUND_Y_YARN = H - 10;

    private static Anim catLick() {
        Palette p = CatArt.TABBY;
        Seq s = new Seq();
        s.add(catSit(p, OPEN, 0), 0.3f);
        for (int i = 0; i < 4; i++) {                        // ear flicks
            F f = catSit(p, OPEN, 0);
            if (i % 2 == 0) {
                shiftRect(f, 7, 12, 15, 17, -1);
            }
            s.add(f, 0.08f);
        }
        s.add(catSit(p, OPEN, 1), 0.2f);
        for (int i = 0; i < 6; i++) {                        // licks a paw
            F f = catArm(16, 15 + i % 2, CLOSED, i % 3);
            if (i % 2 == 0) {
                f.rect(17, 28, 2, 2, p.colour(INNER_EAR));
            }
            s.add(f, 0.16f);
        }
        s.add(catSit(p, HAPPY, 0), 0.4f);
        s.add(catSit(p, OPEN, 0), 0.2f);
        return s.anim();
    }

    // ========================================================= BLACK CAT

    static final Palette MUG = new Palette()
            .set(FUR, "F4F1EA").set(FUR_FAR, "CFC9BD")           // mug, shade
            .set(ACC1, "D9534F")                                  // band
            .set(PATTERN, "5A3A22")                               // coffee
            .set(ACC2, "A8743F").set(ACC2_FAR, "7E542B")          // shelf
            .set(CREAM, "FFFFFF")
            .outline("2A2230");

    static PoseArt.Grid mug() {
        Canvas c = new Canvas(10, 9);
        rect(c, 1, 1, 5, 7, FUR);
        rect(c, 2, 1, 3, 1, PATTERN);
        rect(c, 5, 2, 1, 6, FUR_FAR);
        rect(c, 1, 4, 5, 1, ACC1);
        px(c, 2, 3, CREAM);
        px(c, 6, 3, FUR);                                     // handle
        px(c, 7, 3, FUR);
        px(c, 7, 4, FUR);
        px(c, 7, 5, FUR);
        px(c, 6, 5, FUR);
        return render(c, MUG);
    }

    /** The mug tipped over, falling rim first to the right. */
    static PoseArt.Grid mugTipped() {
        PoseArt.Grid m = mug();
        PoseArt.Grid r = new PoseArt.Grid(m.h, m.w);
        for (int y = 0; y < m.h; y++) {
            for (int x = 0; x < m.w; x++) {
                r.set(m.h - 1 - y, x, m.get(x, y));
            }
        }
        return r;
    }

    static PoseArt.Grid shelf() {
        Canvas c = new Canvas(14, 4);
        rect(c, 1, 1, 12, 1, ACC2);
        rect(c, 1, 2, 12, 1, ACC2_FAR);
        return render(c, MUG);
    }

    private static Anim blackCatMug() {
        Palette p = CatArt.BLACK;
        PoseArt.Grid shelf = shelf(), mug = mug();
        int sx = 30, sy = 29, my = 22;
        Seq s = new Seq();
        s.add(catSit(p, SMUG, 0).put(shelf, sx, sy).put(mug, 31, my), 0.6f);    // staring at you
        s.add(blackCatArm(25, 13, SMUG, 0).put(shelf, sx, sy).put(mug, 31, my), 0.3f);
        int[] push = {33, 35, 37};
        for (int i = 0; i < push.length; i++) {               // slowly... pushing it
            s.add(blackCatArm(25 + (push[i] - 31), 13, SMUG, i % 3).put(shelf, sx, sy)
                    .put(mug, push[i], my), 0.38f);
        }
        s.add(blackCatArm(31, 13, SMUG, 0).put(shelf, sx, sy).put(mugTipped(), 37, my + 1), 0.14f);
        s.add(catSit(p, SMUG, 1).put(shelf, sx, sy).put(mugTipped(), 36, 30), 0.08f);
        s.add(catSit(p, SMUG, 2).put(shelf, sx, sy).put(mugTipped(), 35, 34), 0.07f);
        // smash: shards, a coffee splash
        int white = MUG.colour(FUR), red = MUG.colour(ACC1), coffee = MUG.colour(PATTERN);
        F smash = catSit(p, SMUG, 0).put(shelf, sx, sy);
        smash.rect(34, 40, 9, 1, coffee).rect(36, 39, 4, 1, coffee);
        int[][] shards = {{33, 36}, {36, 34}, {40, 35}, {42, 37}, {38, 37}, {35, 38}};
        for (int i = 0; i < shards.length; i++) {
            smash.rect(shards[i][0], shards[i][1], 2, 1, i % 3 == 0 ? red : white);
        }
        s.add(smash, 0.18f);
        F settle = catSit(p, SMUG, 1).put(shelf, sx, sy);
        settle.rect(34, 40, 9, 1, coffee).rect(36, 39, 4, 1, coffee);
        int[][] lying = {{33, 39}, {37, 38}, {41, 39}, {39, 40}};
        for (int i = 0; i < lying.length; i++) {
            settle.rect(lying[i][0], lying[i][1], 2, 1, i % 2 == 0 ? red : white);
        }
        s.add(settle, 0.7f);
        s.add(catSit(p, HAPPY, 0), 0.4f);
        s.add(catSit(p, OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim blackCatWash() {
        Palette p = CatArt.BLACK;
        Seq s = new Seq();
        s.add(catSit(p, OPEN, 0), 0.3f);
        int[][] strokes = {{12, 8}, {14, 13}, {12, 8}, {14, 13}, {11, 7}, {14, 13}};
        for (int i = 0; i < strokes.length; i++) {             // washes its face and ear
            s.add(blackCatArm(strokes[i][0], strokes[i][1], CLOSED, i % 3), 0.17f);
        }
        F flick = catSit(p, CLOSED, 0);
        shiftRect(flick, 7, 12, 15, 17, -1);
        s.add(flick, 0.1f);
        s.add(catSit(p, HAPPY, 0), 0.4f);
        s.add(catSit(p, OPEN, 0), 0.2f);
        return s.anim();
    }

    // ============================================================== DOGE

    private static F dogeRun(int f, boolean left) {
        F r = new F().put(DogArt.walk(f), 3, GROUND - 28);
        int line = DogArt.SHIBA.outline;
        int tail = left ? 40 : 1;                              // speed lines trail behind
        r.rect(tail, 24, 2, 1, line).rect(tail + (left ? 1 : 0), 28, 2, 1, line)
                .rect(tail, 32, 2, 1, line);
        int puff = PoseArt.rgba("D8D2C8");
        r.rect(left ? 38 : 3, 39 - f % 2, 3, 2, puff);         // dust
        return left ? mirror(r) : r;
    }

    private static Anim dogeZoomies() {
        Seq s = new Seq();
        s.add(dogeSit(OPEN, 0), 0.2f);
        s.add(dogeStand(), 0.1f);
        int x = 0, f = 0;
        for (; x < 36; x += 4, f++) {
            s.add(dogeRun(f % 8, false), 0.05f, x, -(f % 2));
        }
        s.add(mirror(dogeStand()), 0.07f, x, 0);
        for (; x > -36; x -= 4, f++) {
            s.add(dogeRun(f % 8, true), 0.05f, x, -(f % 2));
        }
        s.add(dogeStand(), 0.07f, x, 0);
        for (; x < 0; x += 4, f++) {
            s.add(dogeRun(f % 8, false), 0.05f, x, -(f % 2));
        }
        // flops over, belly up, panting
        for (int i = 0; i < 6; i++) {
            F flop = new F().put(DogArt.roll(i % 2), 4, H - 30);
            Canvas w = water();
            drop(w, 8 + (i % 2), 18 + i);                       // sweat
            drop(w, 34, 20 + i);
            s.add(flop.put(render(w, PROP), 0, 0), 0.18f, 0, 0);
        }
        s.add(dogeSit(HAPPY, 0), 0.45f);
        s.add(dogeSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim dogeScratch() {
        Palette p = DogArt.SHIBA;
        Seq s = new Seq();
        s.add(dogeSit(OPEN, 0), 0.3f);
        int line = p.outline;
        for (int i = 0; i < 10; i++) {
            F f = dogeSit(HAPPY, i % 3);
            f.shiftRows(12, 33, -1);                            // head tilted into it
            int k = i % 2 == 0 ? 0 : 3;
            f.put(leg(p, new float[][] {{10, 38}, {6, 31}, {5, 22 + k}}, 4, CREAM, 2), 0, 0);
            if (i % 2 == 0) {
                f.rect(1, 20, 1, 2, line).rect(2, 25, 1, 2, line);
            }
            s.add(f, 0.08f);
        }
        F after = dogeSit(HAPPY, 0);
        after.rect(18, 30, 2, 2, p.colour(INNER_EAR));         // tongue out, satisfied
        s.add(after, 0.4f);
        s.add(dogeSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    // =========================================================== PIKACHU

    static final Palette BOLT = new Palette()
            .set(FUR, "FFE54A").set(CREAM, "FFFFFF").outline("3A2A1E");

    static PoseArt.Grid bolt(int jitter) {
        Canvas c = new Canvas(W, H);
        float[][] pts = {{21, 0}, {16 + jitter, 4}, {23, 8}, {17 - jitter, 12}, {21, 15}, {19, 19}};
        Canvas m = stroke(c, 3, pts);
        Canvas core = stroke(c, 1, pts);
        for (int i = 0; i < c.z.length; i++) {
            if (m.z[i] != 0) {
                c.z[i] = core.z[i] != 0 ? CREAM : FUR;
            }
        }
        return render(c, BOLT);
    }

    private static Anim pikaBolt() {
        Palette p = PikaArt.PIKA;
        int glow = PoseArt.rgba("FFF6A0");
        Seq s = new Seq();
        s.add(pikaSit(OPEN, 0), 0.2f);
        int[][] sparks = {{3, 26}, {32, 25}, {5, 33}, {33, 32}, {12, 20}, {26, 21}};
        for (int i = 0; i < 6; i++) {                         // charging up
            F f = pikaSit(CLOSED, i % 3).shift(0, 1);
            spark(f, sparks[i % 6][0], sparks[i % 6][1]);
            spark(f, sparks[(i + 3) % 6][0], sparks[(i + 3) % 6][1]);
            s.add(f, 0.1f);
        }
        for (int i = 0; i < 5; i++) {                         // the strike, with a flash
            F f = pikaMouth(pikaSit(CLOSED, 0));
            if (i % 2 == 0) {
                for (int k = 0; k < 12; k++) {
                    double a = Math.PI * 2 * k / 12;
                    for (int r = 17; r < 21; r++) {
                        int x = 19 + (int) Math.round(Math.cos(a) * r);
                        int y = 27 + (int) Math.round(Math.sin(a) * r * 0.8);
                        if (f.g.get(x, y) == 0) {
                            f.set(x, y, glow);
                        }
                    }
                }
            }
            f.put(bolt(i % 2), 0, 0);
            s.add(f, 0.07f);
        }
        for (int i = 0; i < 4; i++) {                         // frazzled, smoking
            F f = soot(pikaSit(XED, i % 3), p.outline);
            f.put(PoseArt.steam(), 13, 9 - i * 2);
            f.put(PoseArt.steam(), 23, 11 - i * 2);
            s.add(f, 0.22f);
        }
        for (int i = 0; i < 4; i++) {                         // shakes it off
            F f = pikaSit(CLOSED, i % 3);
            f.shiftRows(12, 34, i % 2 == 0 ? 2 : -2);
            s.add(f, 0.07f);
        }
        F proud = pikaMouth(pikaSit(HAPPY, 0));
        spark(proud, 4, 28);
        spark(proud, 33, 27);
        s.add(proud, 0.5f);
        s.add(pikaSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim pikaCheekRub() {
        Palette p = PikaArt.PIKA;
        PoseArt.Grid paw = PathArt.paw(p, FUR);
        Seq s = new Seq();
        s.add(pikaSit(OPEN, 0), 0.3f);
        for (int i = 0; i < 8; i++) {
            int k = i % 2;
            F f = pikaSit(HAPPY, i % 3).put(paw, 9, 29 + k).put(paw, 25, 30 - k);
            if (i % 3 == 0) {
                spark(f, 5, 27 + k);
                spark(f, 31, 28 - k);
            }
            s.add(f, 0.13f);
        }
        s.add(pikaMouth(pikaSit(HAPPY, 0)), 0.4f);
        s.add(pikaSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    // ========================================================== SQUIRTLE

    private static F shellFrame(int k) {
        F f = new F().put(SquirtArt.shell(false, 0), 4, GROUND - 20);
        if (k == 1) {
            f.shiftRows(21, 33, 2);
        } else if (k == 2) {
            f = mirror(f);
        } else if (k == 3) {
            f.shiftRows(21, 33, -2);
        }
        int line = SquirtArt.SQUIRT.outline;
        for (int y : new int[] {27, 31, 35}) {                 // spin lines
            f.rect(0, y + k % 2, 3, 1, line).rect(40, y + 1 - k % 2, 3, 1, line);
        }
        return f;
    }

    private static Anim squirtleSpin() {
        Seq s = new Seq();
        s.add(squirtSit(OPEN, 0), 0.15f);
        s.add(new F().put(SquirtArt.shell(false, 0), 4, GROUND - 20), 0.15f);
        int[] travel = {0, 3, 6, 9, 12, 14, 14, 12, 9, 6, 3, 0, 0, 0};
        for (int i = 0; i < travel.length; i++) {
            s.add(shellFrame(i % 4), 0.05f, travel[i], 0);
        }
        // pops out dizzy, stars circling its head
        for (int i = 0; i < 9; i++) {
            F f = squirtSit(DIZZY, i % 3);
            f.shiftRows(12, 34, i % 2 == 0 ? 1 : -1);
            for (int k = 0; k < 3; k++) {
                double a = i * 0.7 + k * Math.PI * 2 / 3;
                star(f, 18 + (int) Math.round(Math.cos(a) * 10), 11 + (int) Math.round(Math.sin(a) * 3));
            }
            s.add(f, 0.12f);
        }
        s.add(squirtSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim squirtlePolish() {
        Palette p = SquirtArt.SQUIRT;
        Seq s = new Seq();
        s.add(squirtSit(OPEN, 0), 0.3f);
        for (int i = 0; i < 8; i++) {
            int k = i % 2 == 0 ? 0 : 3;
            F f = squirtSit(HAPPY, i % 3);
            f.put(leg(p, new float[][] {{26, 33}, {31, 31 + k}}, 3, FUR, 0), 0, 0);
            if (i % 2 == 1) {
                spark(f, 31, 27 + (i % 4));                     // shiny!
            }
            s.add(f, 0.13f);
        }
        F shine = squirtSit(HAPPY, 0);
        spark(shine, 30, 26);
        spark(shine, 6, 30);
        s.add(shine, 0.4f);
        s.add(squirtSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    // ========================================================= BULBASAUR

    /** A spinning razor leaf, 7x7; turn 0-3. */
    static PoseArt.Grid razor(int turn) {
        Canvas c = new Canvas(7, 7);
        int[][] leaf = {{4, 1}, {5, 1}, {3, 2}, {4, 2}, {5, 2}, {2, 3}, {3, 3}, {4, 3},
                {1, 4}, {2, 4}, {3, 4}, {1, 5}, {2, 5}};
        for (int[] q : leaf) {
            int x = q[0], y = q[1];
            int rx = turn % 2 == 0 ? x : 6 - x, ry = turn >= 2 ? 6 - y : y;
            c.set(rx, ry, ACC1);
        }
        for (int[] q : new int[][] {{2, 4}, {3, 3}, {4, 2}}) {
            int rx = turn % 2 == 0 ? q[0] : 6 - q[0], ry = turn >= 2 ? 6 - q[1] : q[1];
            c.set(rx, ry, ACC1_FAR);
        }
        return render(c, BulbArt.BULBA);
    }

    private static Anim razorLeaf() {
        Seq s = new Seq();
        s.add(bulbaSit(OPEN, 0), 0.2f);
        F charge = bulbaSit(OPEN, 1);
        spark(charge, 12, 12);
        spark(charge, 27, 13);
        s.add(charge, 0.18f);
        int[][] dirs = {{-5, -2}, {5, -2}, {0, -5}, {-6, 1}, {6, 1}, {-3, -4}, {3, -4}};
        int[] start = {0, 0, 0, 2, 2, 4, 4};
        for (int i = 0; i < 10; i++) {
            F f = bulbaSit(OPEN, i % 2 == 0 ? 1 : 0);
            for (int k = 0; k < dirs.length; k++) {
                int t = i - start[k];
                if (t >= 0) {
                    f.put(razor(i + k), 16 + dirs[k][0] * t, 10 + dirs[k][1] * t);
                }
            }
            s.add(f, 0.06f);
        }
        s.add(bulbaSit(HAPPY, 0), 0.45f);
        s.add(bulbaSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    private static F bulbaBulbAt(int eyes, int bulbFrame, int dy) {
        F f = new F();
        f.put(BulbArt.sitTail(bulbFrame), 7, H - 30 + dy);
        f.put(BulbArt.sitBody(), 4, H - 30);
        ReminderArt.eyes(f, BULBA_EYE, eyes);
        return f;
    }

    private static Anim bulbaDroop() {
        Seq s = new Seq();
        s.add(bulbaSit(OPEN, 0), 0.3f);
        s.add(bulbaBulbAt(CLOSED, 0, 1), 0.3f);                 // the bulb droops, sleepy
        s.add(bulbaBulbAt(CLOSED, 0, 2), 0.45f);
        s.add(bulbaBulbAt(OPEN, 1, -1), 0.12f);                 // and perks right up
        s.add(bulbaBulbAt(OPEN, 2, 0), 0.1f);
        s.add(bulbaBulbAt(OPEN, 0, 0), 0.1f);
        s.add(bulbaBulbAt(OPEN, 2, 0), 0.1f);
        F sparkle = bulbaSit(HAPPY, 1);
        spark(sparkle, 11, 12);
        spark(sparkle, 26, 10);
        s.add(sparkle, 0.4f);
        s.add(bulbaSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    // ========================================================== GOLDFISH

    private static Anim fishLeap() {
        Seq s = new Seq();
        s.add(fish(OPEN, 0, false, 0), 0.15f);
        s.add(fish(CLOSED, 1, false, 0).bend(arc(-2)).shift(0, 1), 0.12f);   // crouch
        s.add(rotate(fish(OPEN, 2, false, 0), 3), 0.07f, 0, -8);              // nose up
        s.add(rotate(fish(OPEN, 0, false, 0), 3), 0.07f, 0, -16);
        s.add(rotate(fish(HAPPY, 1, false, 0), 2), 0.1f, 0, -22);             // the flip
        s.add(rotate(fish(HAPPY, 2, false, 0), 2), 0.1f, 0, -24);
        s.add(rotate(fish(OPEN, 0, false, 0), 1), 0.07f, 0, -16);             // nose down
        s.add(rotate(fish(OPEN, 1, false, 0), 1), 0.07f, 0, -8);
        int[][][] splash = {
                {{4, 30}, {37, 29}, {10, 25}, {31, 24}},
                {{2, 26}, {39, 25}, {8, 20}, {33, 19}},
                {{1, 31}, {40, 30}, {7, 27}, {34, 26}}};
        for (int i = 0; i < 3; i++) {
            Canvas w = water();
            for (int[] d : splash[i]) {
                bigDrop(w, d[0], d[1]);
            }
            rect(w, 6, 40, 32, 1, FUR);                         // the splash ring
            rect(w, 9, 39, 26, 1, FUR);
            s.add(fish(HAPPY, i, false, 0).put(render(w, PROP), 0, 0), 0.12f);
        }
        s.add(fish(HAPPY, 0, false, 0), 0.35f);
        s.add(fish(OPEN, 0, false, 0), 0.2f);
        return s.anim();
    }

    private static Anim fishBubble() {
        int mouth = PoseArt.rgba("2A2230");
        Seq s = new Seq();
        s.add(fish(OPEN, 0, false, 0), 0.3f);
        s.add(fish(OPEN, 1, false, 0).rect(32, 29, 2, 2, mouth), 0.15f);
        F small = fish(OPEN, 2, false, 0).rect(32, 29, 2, 2, mouth);
        small.rect(35, 28, 2, 2, BUBBLE_A).set(35, 28, BUBBLE_W);
        s.add(small, 0.2f);
        int[][] rise = {{35, 25}, {36, 20}, {35, 15}, {36, 10}};
        for (int i = 0; i < rise.length; i++) {
            F f = fish(i == 0 ? OPEN : HAPPY, i % 3, false, 0);
            bubble(f, rise[i][0], rise[i][1]);
            s.add(f, 0.18f);
        }
        F pop = fish(HAPPY, 0, false, 0);
        spark(pop, 36, 7);
        s.add(pop, 0.2f);
        s.add(fish(OPEN, 0, false, 0), 0.2f);
        return s.anim();
    }

    // ========================================================== ANNOYED

    static PoseArt.Grid flipH(PoseArt.Grid g) {
        PoseArt.Grid r = new PoseArt.Grid(g.w, g.h);
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                r.set(g.w - 1 - x, y, g.get(x, y));
            }
        }
        return r;
    }

    /** The sitting cat seen from behind: no face, stripes down its back. */
    static PoseArt.Grid catBack(Palette p) {
        Canvas c = new Canvas(28, 30);
        fill(c, CatArt.SIT_BODY, FUR);
        fillPair(c, CatArt.SIT_EAR, FUR);
        fill(c, CatArt.SIT_HEAD, FUR);
        mark(c, 9, 6, 10, 1);
        mark(c, 7, 9, 14, 1);
        mark(c, 6, 12, 16, 1);
        mark(c, 8, 19, 12, 1);
        mark(c, 7, 22, 14, 1);
        mark(c, 6, 25, 16, 1);
        fillPair(c, CatArt.SIT_HIND, CREAM);
        return render(c, p);
    }

    /** Glares (the black cat hisses, fangs out) with its tail lashing, then turns its back. */
    private static Anim catSnub(Palette p) {
        boolean black = p == CatArt.BLACK;
        Seq s = new Seq();
        for (int i = 0; i < 6; i++) {
            F f = catSit(p, ANGRY, i % 3);
            if (black) {                                        // hiss
                int pink = p.colour(INNER_EAR), white = PoseArt.rgba("FFFFFF");
                f.rect(16, 26, 4, 3, pink).set(16, 26, white).set(19, 26, white);
            }
            anger(f, 30, 11, i % 2 == 0);
            s.add(f, 0.15f);
        }
        PoseArt.Grid back = catBack(p);
        for (int i = 0; i < 5; i++) {                           // hmph.
            F f = new F().put(flipH(CatArt.sitTail(p, i % 3)), -1, H - 23).put(back, 4, H - 30);
            anger(f, 30, 11, i % 2 == 0);
            if (i == 1) {
                huff(f, 29, 21);
            }
            s.add(f, 0.3f);
        }
        s.add(catSit(p, ANGRY, 0), 0.25f);                      // a glance back
        s.add(catSit(p, OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim dogeSulk() {
        Seq s = new Seq();
        for (int i = 0; i < 4; i++) {
            F f = dogeSit(ANGRY, i % 3);
            anger(f, 31, 12, i % 2 == 0);
            s.add(f, 0.15f);
        }
        for (int i = 0; i < 6; i++) {                           // flops down, ignoring you
            F f = new F().put(DogArt.sleep(i % 2), 3, H - 23);
            anger(f, 33, 14, i % 2 == 0);
            if (i % 3 == 1) {
                huff(f, 9, 26 - i);
            }
            s.add(f, 0.3f);
        }
        s.add(dogeSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim pikaPout() {
        Palette p = PikaArt.PIKA;
        Seq s = new Seq();
        for (int i = 0; i < 6; i++) {                           // glaring, cheeks crackling
            F f = pikaSit(ANGRY, i % 3);
            spark(f, i % 2 == 0 ? 4 : 5, 28 + i % 2);
            spark(f, i % 2 == 0 ? 32 : 31, 29 - i % 2);
            anger(f, 31, 12, i % 2 == 0);
            s.add(f, 0.13f);
        }
        for (int i = 0; i < 5; i++) {                           // arms folded, pouting
            F f = pikaSit(ANGRY, i % 3);
            f.put(leg(p, new float[][] {{11, 35}, {25, 37}}, 3, FUR, 0), 0, 0);
            f.put(leg(p, new float[][] {{26, 35}, {12, 37}}, 3, FUR, 0), 0, 0);
            anger(f, 31, 12, i % 2 == 0);
            if (i == 2) {
                huff(f, 19, 24);
            }
            s.add(f, 0.25f);
        }
        s.add(pikaSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim squirtleSulk() {
        Seq s = new Seq();
        s.add(squirtSit(ANGRY, 0), 0.2f);
        int white = PoseArt.rgba("FFFFFF");
        for (int i = 0; i < 8; i++) {                           // won't come out
            F f = new F().put(SquirtArt.shell(false, 0), 4, GROUND - 20);
            f.set(16, 36, white).set(17, 37, white).set(21, 36, white).set(20, 37, white);
            if (i % 4 == 1) {
                f.shiftRows(21, 41, 1);
            }
            anger(f, 26, 15, i % 2 == 0);
            if (i % 3 == 2) {
                huff(f, 10, 17 - i % 2);
            }
            s.add(f, 0.2f);
        }
        s.add(squirtSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    /** Both vines crossed over its chest like folded arms. */
    static PoseArt.Grid crossedVines() {
        Canvas c = new Canvas(W, H);
        for (float[][] v : new float[][][] {
                {{11, 19}, {12, 28}, {17, 34}, {27, 35}},
                {{27, 19}, {26, 28}, {21, 34}, {11, 35}}}) {
            Canvas m = stroke(c, 2, v);
            for (int i = 0; i < m.z.length; i++) {
                if (m.z[i] != 0) {
                    c.z[i] = ACC1;
                }
            }
        }
        return render(c, BulbArt.BULBA);
    }

    private static Anim bulbaCross() {
        Seq s = new Seq();
        for (int i = 0; i < 3; i++) {
            F f = bulbaSit(ANGRY, i % 2);
            anger(f, 31, 10, i % 2 == 0);
            s.add(f, 0.15f);
        }
        PoseArt.Grid vines = crossedVines();
        for (int i = 0; i < 6; i++) {
            F f = bulbaSit(ANGRY, i % 3 == 0 ? 1 : 0).put(vines, 0, 0);
            anger(f, 31, 10, i % 2 == 0);
            if (i == 2) {
                huff(f, 19, 6);
            }
            s.add(f, 0.25f);
        }
        s.add(bulbaSit(OPEN, 0), 0.2f);
        return s.anim();
    }

    private static Anim fishSnub() {
        Seq s = new Seq();
        for (int i = 0; i < 4; i++) {
            F f = fish(ANGRY, i % 3, false, 0);
            anger(f, 33, 12, i % 2 == 0);
            s.add(f, 0.15f);
        }
        for (int i = 0; i < 7; i++) {                           // turns tail on you, flicking it
            F f = mirror(fish(OPEN, i % 3, false, 0));
            anger(f, 6, 12, i % 2 == 0);
            if (i % 3 == 1) {
                bubble(f, 5, 24 - i);
            }
            s.add(f, 0.2f);
        }
        s.add(fish(OPEN, 0, false, 0), 0.2f);
        return s.anim();
    }

    private static final int BUBBLE_A = PoseArt.rgba("8CCFE8");
    private static final int BUBBLE_W = PoseArt.rgba("FFF4E3");
}
