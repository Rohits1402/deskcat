package com.deskcat;

import static com.deskcat.Gait.*;
import static com.deskcat.PathArt.*;
import static com.deskcat.ReminderArt.*;

/**
 * Being carried: each pet dangles from where you grab it, with its own
 * reaction - the cat goes limp like a scruffed kitten, the black cat
 * sulks with its tail lashing, the doge paddles happily, Pikachu kicks
 * its feet, Squirtle swims in the air, Bulbasaur hangs from its bulb
 * waving its vines, the goldfish flops. A 4-frame loop in window space
 * (W x H, like {@link ReminderArt}); CatApp swings it like a pendulum
 * around the grab point and stretches it a little on pick-up.
 */
final class CarryArt {

    private CarryArt() {
    }

    static final int FRAMES = 4;
    static final float FRAME_SECS = 0.14f;
    /** Small side-to-side sway of dangling parts per frame. */
    private static final int[] SW = {0, 1, 0, -1};

    static ReminderArt.Anim carry(String skin) {
        PoseArt.Grid[] f = new PoseArt.Grid[FRAMES];
        float[] s = new float[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            f[i] = frame(skin, i).g;
            s[i] = FRAME_SECS;
        }
        return new ReminderArt.Anim(f, s);
    }

    private static F frame(String skin, int i) {
        switch (skin) {
            case "squirtle": return squirtle(i);
            case "pikachu": return pikachu(i);
            case "blackcat": return cat(CatArt.BLACK, i);
            case "doge": return doge(i);
            case "bulbasaur": return bulbasaur(i);
            case "goldfish": return goldfish(i);
            default: return cat(CatArt.TABBY, i);
        }
    }

    /** A dangling limb from (x0, y0) to (x1, y1), its last pawRows rows in pawZone. */
    private static void hang(Canvas c, float x0, float y0, float x1, float y1, int w,
            int pawZone, int pawRows) {
        Canvas m = stroke(c, w, new float[][] {{x0, y0}, {x1, y1}});
        int bottom = -1;
        for (int y = 0; y < m.h; y++) {
            for (int x = 0; x < m.w; x++) {
                if (m.get(x, y) != 0) {
                    bottom = y;
                }
            }
        }
        Canvas z = new Canvas(c.w, c.h);
        for (int y = 0; y < m.h; y++) {
            for (int x = 0; x < m.w; x++) {
                if (m.get(x, y) != 0) {
                    z.set(x, y, y > bottom - pawRows ? pawZone : FUR);
                }
            }
        }
        creasePaste(c, z);
    }

    // ============================================================= CATS

    static final String CAT_BODY = "M15 15H29V17H30V30H29V32H27V33H17V32H15V30H14V17H15Z";
    static final String CAT_BIB = "M18 17H26V19H27V24H26V25H18V24H17V19H18Z";

    /**
     * Scruffed: the sitting head on top, body hanging long below it, legs
     * and tail dangling. The tabby goes limp and content; the black cat
     * sulks - ears back, eyes half-lidded, tail lashing.
     */
    private static F cat(Palette p, int f) {
        boolean grumpy = p == CatArt.BLACK;
        int ox = 8, sw = SW[f];
        Canvas c = new Canvas(W, H);
        int lash = grumpy ? sw * 3 : sw * 2;
        Canvas tail = stroke(c, 3, new float[][] {{22, 30}, {22 + sw, 36}, {22 + lash, 40}});
        for (int i = 0; i < tail.z.length; i++) {
            if (tail.z[i] != 0) {
                c.z[i] = FUR;
            }
        }
        if (!grumpy) {
            mark(c, 19, 36, 6, 1);
            mark(c, 19, 38, 8, 1);
        }
        hang(c, 18, 29, 18 - sw, 39, 4, CREAM, 2);           // hind legs
        hang(c, 26, 29, 26 - sw, 39, 4, CREAM, 2);
        Canvas b = new Canvas(W, H);
        fill(b, CAT_BODY, FUR);
        if (!grumpy) {
            mark(b, 14, 21, 2, 1);
            mark(b, 14, 24, 2, 1);
            mark(b, 28, 21, 2, 1);
            mark(b, 28, 24, 2, 1);
        }
        fill(b, CAT_BIB, CREAM);
        creasePaste(c, b);
        hang(c, 17, 19, 16 + sw, 28, 3, CREAM, 2);           // front paws, limp
        hang(c, 27, 19, 28 + sw, 28, 3, CREAM, 2);
        // the sitting head
        Canvas h = new Canvas(28, 22);
        int earDy = grumpy ? 2 : 0;                           // ears laid back
        Canvas ears = new Canvas(28, 22);
        fillPair(ears, CatArt.SIT_EAR, FUR);
        fillPair(ears, CatArt.SIT_EAR_IN, INNER_EAR);
        paste(h, ears, 0, earDy, false);
        fill(h, CatArt.SIT_HEAD, FUR);
        mark(h, 11, 6, 1, 2);
        mark(h, 13, 5, 2, 2);
        mark(h, 16, 6, 1, 2);
        markPair(h, 3, 11, 2, 1);
        markPair(h, 3, 13, 2, 1);
        fill(h, CatArt.SIT_MUZZLE, CREAM);
        rect(h, 13, 13, 2, 1, NOSE);
        int mouth = grumpy ? LINE : MOUTH;
        if (grumpy) {                                         // a flat, unamused mouth
            rect(h, 12, 15, 4, 1, mouth);
        } else {
            px(h, 12, 14, mouth);
            px(h, 13, 15, mouth);
            px(h, 14, 15, mouth);
            px(h, 15, 14, mouth);
        }
        for (int ex : new int[] {5, 17}) {
            if (grumpy) {                                     // half-lidded glare
                fill(h, CatArt.SIT_EYE_RING, RING, ex - 5, 0, false);
                fill(h, CatArt.SIT_EYE_IRIS, IRIS, ex - 5, 0, false);
                rect(h, ex, 8, 6, 3, FUR);
                rect(h, ex, 10, 6, 1, LINE);
                rect(h, ex + 2, 11, 2, 1, PUPIL);
            } else {                                          // content, limp
                rect(h, ex + 1, 11, 4, 1, LINE);
                px(h, ex, 10, LINE);
                px(h, ex + 5, 10, LINE);
            }
        }
        Canvas hc = new Canvas(W, H);
        paste(hc, h, ox, 0, false);
        creasePaste(c, hc);
        int[][] wh = {{ox, 13}, {ox + 1, 13}, {ox, 15}, {ox + 1, 15},
                {ox + 27, 13}, {ox + 26, 13}, {ox + 27, 15}, {ox + 26, 15}};
        return new F().put(render(c, p, wh), 0, 0);
    }

    // ============================================================== DOGE

    static final String DOG_BODY = "M15 18H29V20H30V31H29V33H27V34H17V33H15V31H14V20H15Z";
    static final String DOG_CHEST = "M18 20H26V24H25V27H24V28H20V27H19V24H18Z";

    /** Held up under its front legs: hind legs paddling, tail wagging, tongue out. */
    private static F doge(int f) {
        int ox = 7, oy = -1, sw = SW[f];
        Canvas c = new Canvas(W, H);
        // curled tail wagging out to one side
        Canvas t = stroke(c, 3, new float[][] {{26, 32}, {31, 33 + sw}, {33, 30 + sw}, {31, 28 + sw}});
        for (int i = 0; i < t.z.length; i++) {
            if (t.z[i] != 0) {
                c.z[i] = FUR;
            }
        }
        rect(c, 31, 29 + sw, 2, 2, CREAM);
        int kick = f % 2 == 0 ? 2 : 0;                       // paddling, one leg then the other
        hang(c, 18, 31, 17, 40 - kick, 4, CREAM, 2);
        hang(c, 26, 31, 27, 38 + kick, 4, CREAM, 2);
        Canvas b = new Canvas(W, H);
        fill(b, DOG_BODY, FUR);
        fill(b, DOG_CHEST, CREAM);
        creasePaste(c, b);
        // front legs held out to the sides
        hang(c, 17, 22, 9, 20 - sw, 4, CREAM, 0);
        hang(c, 27, 22, 35, 20 + sw, 4, CREAM, 0);
        rect(c, 7, 19 - sw, 3, 3, CREAM);
        rect(c, 34, 19 + sw, 3, 3, CREAM);
        Canvas h = new Canvas(30, 23);
        fillPair(h, DogArt.SIT_EAR, FUR);
        fill(h, DogArt.SIT_HEAD, FUR);
        fillPair(h, DogArt.SIT_EAR_IN, INNER_EAR);
        fillPair(h, DogArt.SIT_CHEEK, CREAM);
        fill(h, DogArt.SIT_MUZZLE, CREAM);
        fill(h, DogArt.SIT_NOSE, NOSE);
        rect(h, 9, 9, 2, 1, CREAM);
        rect(h, 19, 9, 2, 1, CREAM);
        mark(h, 14, 7, 2, 1);
        for (int ex : new int[] {8, 19}) {                   // happy ^ ^
            rect(h, ex, 11, 3, 1, LINE);
            px(h, ex - 1, 12, LINE);
            px(h, ex + 3, 12, LINE);
        }
        rect(h, 13, 16, 4, 1, MOUTH);                        // open grin, tongue out
        rect(h, 14, 17, 2, 3, INNER_EAR);
        Canvas hc = new Canvas(W, H);
        paste(hc, h, ox, oy, false);
        creasePaste(c, hc);
        return new F().put(render(c, DogArt.SHIBA), 0, 0);
    }

    // =========================================================== PIKACHU

    static final String PIKA_HANG_BODY = "M8 20H22V21H23V23H24V30H23V32H21V33H9V32H7V30H6V23H7V21H8Z";

    /**
     * Lifted up under the arms like a held baby: the sitting head on top, a
     * longer body hanging below, both little arms stretched straight up past
     * the cheeks, feet dangling toes-down and kicking, bolt tail swinging.
     */
    private static F pikachu(int f) {
        int ox = 7, oy = 1, sw = SW[f];
        F out = new F();
        out.put(PikaArt.sitTail(f % 3), 26 + sw, 14);
        Canvas c = new Canvas(W, H);
        int kick = f % 2 == 0 ? 2 : 0;
        int[][] legs = {{ox + 11, ox + 10 - sw, 37 - kick}, {ox + 18, ox + 19 - sw, 35 + kick}};
        for (int[] l : legs) {
            hang(c, l[0], 30 + oy, l[1], l[2], 3, FUR, 0);
            rect(c, l[1] - 1, l[2], 3, 3, FUR);              // big feet, toes down
            px(c, l[1] - 1, l[2] + 2, SEP);
            px(c, l[1] + 1, l[2] + 2, SEP);
        }
        Canvas b = new Canvas(W, H);
        fill(b, PIKA_HANG_BODY, FUR, ox, oy, false);
        paint(b, ox + 21, 22 + oy, 4, 12, FUR, FUR_FAR);
        paint(b, ox + 6, 27 + oy, 2, 1, FUR, PATTERN);       // back stripes peeking round
        paint(b, ox + 6, 30 + oy, 2, 1, FUR, PATTERN);
        creasePaste(c, b);
        Canvas ears = new Canvas(30, 30);
        fillPair(ears, PikaArt.SIT_EAR, FUR);
        paint(ears, 0, 0, 30, 3, FUR, ACC1);
        shiftRows(ears, 0, 4, sw);                          // ear tips wobble
        paste(c, ears, ox, oy, false);
        fillOver(c, PikaArt.SIT_HEAD, FUR, ox, oy, false);
        paint(c, ox + 25, 15 + oy, 5, 6, FUR, FUR_FAR);
        paint(c, ox + 10, 10 + oy, 4, 1, FUR, ACC4);
        paint(c, ox + 8, 11 + oy, 3, 1, FUR, ACC4);
        Canvas cheeks = new Canvas(30, 30);
        fillPair(cheeks, PikaArt.SIT_CHEEK, ACC3);
        paste(c, cheeks, ox, oy, false);
        rect(c, ox + 14, 17 + oy, 2, 1, NOSE);
        for (int ex : new int[] {8, 18}) {
            rect(c, ox + ex, 14 + oy, 4, 1, LINE);
            px(c, ox + ex - 1, 15 + oy, LINE);
            px(c, ox + ex + 4, 15 + oy, LINE);
        }
        rect(c, ox + 12, 18 + oy, 6, 1, MOUTH);
        rect(c, ox + 13, 19 + oy, 4, 1, INNER_EAR);
        rect(c, ox + 13, 20 + oy, 4, 1, MOUTH);
        // arms straight up beside the head, where the hands hold it
        limbOver(c, stroke(c, 3, new float[][] {{ox + 6, 24 + oy}, {ox + 1, 20 + oy}, {ox + 1, 12 + oy}}), FUR);
        limbOver(c, stroke(c, 3, new float[][] {{ox + 23, 24 + oy}, {ox + 28, 20 + oy}, {ox + 28, 12 + oy}}), FUR);
        return out.put(render(c, PikaArt.PIKA), 0, 0);
    }

    // ========================================================== SQUIRTLE

    /** Swimming in the air: arms stroking, legs kicking, a worried "o" and a sweat drop. */
    private static F squirtle(int f) {
        int ox = 7, sw = SW[f];
        Palette p = SquirtArt.SQUIRT;
        F out = new F();
        out.put(SquirtArt.sitTail(f % 3), 32 + sw, 16);
        Canvas c = new Canvas(W, H);
        int kick = f % 2 == 0 ? 2 : 0;
        hang(c, ox + 9, 26, ox + 7, 37 - kick, 4, FUR, 0);
        hang(c, ox + 20, 26, ox + 22, 35 + kick, 4, FUR, 0);
        fill(c, SquirtArt.SIT_SHELL, ACC1, ox, 0, false);
        paint(c, ox + 23, 17, 7, 13, ACC1, ACC2_FAR);
        fill(c, SquirtArt.SIT_RIM, ACC2, ox, 0, false);
        fill(c, SquirtArt.SIT_BELLY, ACC3, ox, 0, false);
        paint(c, ox + 9, 22, 12, 1, ACC3, ACC4);
        paint(c, ox + 8, 25, 14, 1, ACC3, ACC4);
        paint(c, ox + 14, 19, 2, 3, ACC3, ACC4);
        // a breaststroke: one arm up while the other sweeps down
        boolean up = f < 2;
        limbOver(c, stroke(c, 4, new float[][] {{ox + 6, 21}, {ox, up ? 14 : 26}}), FUR);
        limbOver(c, stroke(c, 4, new float[][] {{ox + 23, 21}, {ox + 29, up ? 26 : 14}}), FUR);
        fillOver(c, SquirtArt.SIT_HEAD, FUR, ox, 0, false);
        paint(c, ox + 21, 10, 9, 7, FUR, FUR_FAR);
        paint(c, ox + 10, 6, 4, 1, FUR, CREAM);
        paint(c, ox + 8, 7, 3, 1, FUR, CREAM);
        Canvas eyes = new Canvas(30, 30);
        fillPair(eyes, SquirtArt.SIT_EYE_RING, RING);
        fillPair(eyes, SquirtArt.SIT_EYE_IRIS, IRIS);
        paste(c, eyes, ox, 0, false);
        rect(c, ox + 8, 12, 2, 2, PUPIL);                    // eyes wide, looking down
        rect(c, ox + 20, 12, 2, 2, PUPIL);
        px(c, ox + 8, 12, GLINT);
        px(c, ox + 20, 12, GLINT);
        rect(c, ox + 14, 16, 2, 2, MOUTH);                   // a worried "o"
        out.put(render(c, p), 0, 0);
        Canvas w = water();
        bigDrop(w, ox + 26, 4 + (f % 2));                   // sweat drop
        return out.put(render(w, PROP), 0, 0);
    }

    // ========================================================= BULBASAUR

    /**
     * Hanging from your cursor by its own vines: both vines rise from the
     * bulb and wrap the grab point, the body dangles below, legs kicking,
     * beaming.
     */
    private static F bulbasaur(int f) {
        int ox = 7, oy = 8, sw = SW[f];
        Canvas c = new Canvas(W, H);
        Canvas v = new Canvas(W, H);
        for (float[][] vine : new float[][][] {
                {{14, 12}, {16, 6}, {20, 2}},
                {{30, 12}, {28, 6}, {24, 2}}}) {
            Canvas m = stroke(v, 2, vine);
            for (int i = 0; i < m.z.length; i++) {
                if (m.z[i] != 0) {
                    v.z[i] = ACC1_FAR;
                }
            }
        }
        rect(v, 20, 1, 5, 2, ACC1_FAR);                      // wrapped round the cursor
        px(v, 21, 1, ACC1);
        px(v, 23, 2, ACC1);
        paste(c, v, 0, 0, false);
        paste(c, BulbArt.bulb(), ox + 3, oy + 1, false);
        int kick = f % 2 == 0 ? 2 : 0;
        Canvas legs = new Canvas(W, H);
        int[][] ls = {{ox + 7, ox + 6 - sw, 38 - kick}, {ox + 22, ox + 23 - sw, 37 + kick}};
        for (int[] l : ls) {
            Canvas m = stroke(legs, 5, new float[][] {{l[0], 33}, {l[1], l[2]}});
            for (int i = 0; i < m.z.length; i++) {
                if (m.z[i] != 0) {
                    legs.z[i] = FUR;
                }
            }
        }
        paste(c, legs, 0, 0, false);
        for (int[] l : ls) {
            for (int dx : new int[] {-2, 0, 2}) {
                px(c, l[1] + dx, l[2] + 2, ACC3);            // white claws
            }
        }
        Canvas b = new Canvas(30, 30);
        fill(b, BulbArt.SIT_BODY, FUR);
        paint(b, 20, 19, 10, 11, FUR, FUR_FAR);
        markPair(b, 11, 23, 2, 2);
        paste(b, BulbArt.sitting(1, true, false), 0, 0, false);   // head over the body
        Canvas hc = new Canvas(W, H);
        paste(hc, b, ox, oy, false);
        creasePaste(c, hc);
        return new F().put(render(c, BulbArt.BULBA), 0, 0);
    }

    // ========================================================== GOLDFISH

    /** Out of the water and flopping: bending, gasping, flicking off drops. */
    private static F goldfish(int f) {
        int mouth = PoseArt.rgba("2A2230");
        int amp = new int[] {4, 0, -4, 0}[f];
        F out = ReminderArt.fish(OPEN, f % 3, f % 2 == 1, 0).shift(0, -6);
        if (f % 2 == 0) {
            out.rect(32, 23, 2, 2, mouth);                   // gasping
        }
        out.bend(arc(amp));
        Canvas w = water();
        int[][] drops = {{6, 14}, {40, 18}, {9, 30}, {38, 32}};
        drop(w, drops[f][0], drops[f][1]);
        drop(w, drops[(f + 2) % 4][0], drops[(f + 2) % 4][1] + 2);
        return out.put(render(w, PROP), 0, 0);
    }
}
