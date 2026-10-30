package com.deskcat;

import static com.deskcat.PathArt.*;

/**
 * Bulbasaur in the {@link PathArt} format A wide frog-like teal
 * head with small pointed ears, big outlined red eyes (sockets baked,
 * eyeStyle 5 moves the pupils), a wide open smile, dark-teal spots, stubby
 * legs with white claws, and the green bulb on its back - drawn as the
 * "tail" layer so it sways behind the head. Personal-use fan art.
 */
final class BulbArt {

    private BulbArt() {
    }

    static final Palette BULBA = new Palette()
            .set(FUR, "79CBB0").set(FUR_FAR, "5BAE95")          // skin, shade
            .set(CREAM, "A8E2CF")                                 // highlight
            .set(PATTERN, "3F9A80")                               // spots
            .set(ACC1, "5DB54A").set(ACC1_FAR, "3C8A34")         // bulb, leaf seams
            .set(ACC2, "92DA74").set(ACC2_FAR, "2F6E2A")         // bulb shine, bulb shade
            .set(ACC3, "FFFFFF")                                  // claws
            .set(IRIS, "C8323E").set(RING, "1F3B33")
            .set(MOUTH, "1F3B33").set(INNER_EAR, "E8707A")       // mouth, tongue
            .set(LINE, "1F3B33").set(SEP, "5BAE95")
            .outline("1F3B33");

    // ============================================================ SITTING

    static final String SIT_EAR = "M5 4H6V5H7V6H8V7H9V8H4V5H5Z";
    static final String SIT_HEAD = "M8 7H22V8H25V9H26V10H27V11H28V18H27V19H26V20H24V21H6V20H4V19H3V18H2V11H3V10H4V9H5V8H8Z";
    static final String SIT_EYE_RING = "M5 11H9V12H10V16H9V17H5V16H4V12H5Z";
    static final String SIT_EYE_IRIS = "M6 12H8V13H9V15H8V16H6V15H5V13H6Z";
    static final String SIT_BODY = "M6 20H24V21H25V22H26V27H4V22H5V21H6Z";
    static final String SIT_LEG = "M4 22H10V28H11V30H3V28H4Z";
    static final String SIT_SPOT = "M13 8H17V10H16V11H14V10H13Z";

    /** Face and body as zones; eyes: 0 open sockets, 1 happy ^^, 2 asleep. */
    static Canvas sitting(int eyes, boolean openMouth) {
        return sitting(eyes, openMouth, true);
    }

    static Canvas sitting(int eyes, boolean openMouth, boolean withBody) {
        Canvas c = new Canvas(30, 30);
        if (withBody) {
            body(c);
        }
        return head(c, eyes, openMouth);
    }

    private static void body(Canvas c) {
        fill(c, SIT_BODY, FUR);
        paint(c, 20, 19, 10, 11, FUR, FUR_FAR);      // shaded right side
        fillPair(c, SIT_LEG, FUR);                    // legs set wide, a gap between
        paint(c, 20, 19, 10, 11, FUR, FUR_FAR);
        markPair(c, 11, 23, 2, 2);                    // belly spots
        for (int x : new int[] {4, 6, 8, 21, 23, 25}) {
            px(c, x, 29, ACC3);                       // white claws
        }
    }

    private static Canvas head(Canvas c, int eyes, boolean openMouth) {
        // the head sits HY rows low so the bulb rises well above it
        int y = HY;
        fill(c, SIT_EAR, FUR, 0, y, false);
        fill(c, SIT_EAR, FUR, 0, y, true);
        fillOver(c, SIT_HEAD, FUR, 0, y, false);
        paint(c, 23, 11 + y, 7, 9, FUR, FUR_FAR);
        paint(c, 9, 8 + y, 4, 1, FUR, CREAM);         // crown highlight
        paint(c, 6, 9 + y, 3, 1, FUR, CREAM);
        fill(c, SIT_SPOT, PATTERN, 0, y, false);      // forehead spot
        markPair(c, 2, 17 + y, 2, 2);                 // cheek spots
        if (eyes == 0) {
            // white eyes: the red iris and pupil are drawn over them at
            // runtime (eyeStyle 6) so they can follow the cursor. A socket
            // filled solid red read as bloodshot.
            fill(c, SIT_EYE_RING, RING, 0, y, false);
            fill(c, SIT_EYE_RING, RING, 0, y, true);
            fill(c, SIT_EYE_IRIS, GLINT, 0, y, false);
            fill(c, SIT_EYE_IRIS, GLINT, 0, y, true);
        } else {
            for (int ex : new int[] {5, 21}) {
                if (eyes == 1) {
                    rect(c, ex, 13 + y, 4, 1, LINE); // ^ ^
                    px(c, ex - 1, 14 + y, LINE);
                    px(c, ex + 4, 14 + y, LINE);
                } else {
                    rect(c, ex, 15 + y, 4, 1, LINE); // asleep
                    px(c, ex - 1, 14 + y, LINE);
                    px(c, ex + 4, 14 + y, LINE);
                }
            }
        }
        // a wide smile, open with a tongue
        px(c, 9, 17 + y, MOUTH);
        px(c, 20, 17 + y, MOUTH);
        rect(c, 10, 18 + y, 10, 1, MOUTH);
        if (openMouth) {
            px(c, 11, 19 + y, MOUTH);
            rect(c, 12, 19 + y, 6, 1, INNER_EAR);
            px(c, 18, 19 + y, MOUTH);
            rect(c, 12, 20 + y, 6, 1, MOUTH);
        }
        return c;
    }

    /** How far the head sits below its drawn position. */
    static final int HY = 2;

    /**
     * Sitting, 30x30. Eye sockets baked; CatApp draws the pupils (eyeStyle
     * 5, iris 4x4: cols 5-8 / 21-24, rows 14-17, so FBO x 7 / 23, y 12).
     */
    static PoseArt.Grid sitBody() {
        return render(sitting(0, true), BULBA);
    }

    // =============================================================== BULB

    static final String BULB = "M11 0H13V1H14V2H15V3H17V4H19V5H20V6H21V7H22V8H23V13H22V14H21V15H3V14H2V13H1V8H2V7H3V6H4V5H5V4H7V3H9V2H10V1H11Z";

    /** The bulb as zones on a 24x16 canvas: leaf seams, shine, shade. */
    static Canvas bulb() {
        Canvas b = new Canvas(24, 16);
        fill(b, BULB, ACC1);
        paint(b, 15, 6, 9, 10, ACC1, ACC2_FAR);      // shade on the right
        int[][] seams = {{11, 2}, {11, 3}, {11, 4}, {12, 5}, {12, 6}, {12, 7},
                {7, 4}, {6, 5}, {6, 6}, {5, 7}, {5, 8}, {5, 9}, {5, 10},
                {16, 4}, {17, 5}, {17, 6}, {18, 7}, {18, 8}, {18, 9}, {18, 10}};
        for (int[] s : seams) {
            paint(b, s[0], s[1], 1, 1, ACC1, ACC1_FAR);
            paint(b, s[0], s[1], 1, 1, ACC2_FAR, ACC1_FAR);
        }
        paint(b, 8, 4, 2, 1, ACC1, ACC2);            // shine
        paint(b, 7, 5, 2, 2, ACC1, ACC2);
        paint(b, 9, 2, 1, 2, ACC1, ACC2);
        return b;
    }

    /** The bulb behind the head; it breathes and sways. 3 frames, 24x30. */
    static PoseArt.Grid sitTail(int frame) {
        Canvas c = new Canvas(24, 30);
        Canvas b = bulb();
        int lift = frame == 1 ? -1 : 0;
        for (int y = 0; y < b.h; y++) {
            for (int x = 0; x < b.w; x++) {
                if (b.z[y * b.w + x] != 0) {
                    c.set(x, y + 1 + lift, b.z[y * b.w + x]);
                }
            }
        }
        shiftRows(c, 0, 5, frame == 2 ? 1 : 0);
        return render(c, BULBA);
    }

    /** Copy zones from src into dst at (ox, oy). */
    static void stamp(Canvas dst, Canvas src, int ox, int oy) {
        for (int y = 0; y < src.h; y++) {
            for (int x = 0; x < src.w; x++) {
                int z = src.z[y * src.w + x];
                if (z != 0) {
                    dst.set(ox + x, oy + y, z);
                }
            }
        }
    }

    // ============================================================== HAPPY

    /**
     * Petted until delighted: Bulbasaur can't roll onto its bulb, so it
     * beams instead - ^^ eyes, mouth open, a little hop with the bulb
     * bouncing. Bulb and body together, 2 frames, 34x30.
     */
    static PoseArt.Grid happy(int frame) {
        Canvas c = new Canvas(34, 30);
        int hop = frame == 0 ? 0 : -1;
        stamp(c, bulb(), 3, 1 + hop * 2);
        stamp(c, sitting(1, true), 0, hop);
        return render(c, BULBA);
    }

    // ============================================================== SLEEP

    static final String SB = "M8 16H22V17H23V18H24V25H23V26H7V25H6V18H7V17H8Z";

    /**
     * Asleep: the sitting head (ears and all) lowered onto its front paws,
     * a rounded body behind, the bulb rising and falling above. 30x28.
     */
    static PoseArt.Grid sleep(int frame) {
        Canvas c = new Canvas(30, 28);
        stamp(c, bulb(), 3, frame == 0 ? 2 : 1);
        fill(c, SB, FUR);
        paint(c, 18, 16, 10, 11, FUR, FUR_FAR);
        Canvas front = new Canvas(30, 28);
        stamp(front, sitting(2, false, false), 0, 2);
        int[] before = c.z.clone();
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                int z = front.get(x, y);
                if (z == 0) {
                    continue;
                }
                boolean edge = false;
                for (int[] o : n) {
                    int nx = x + o[0], ny = y + o[1];
                    if (front.get(nx, ny) == 0 && nx >= 0 && ny >= 0 && nx < c.w && ny < c.h
                            && before[ny * c.w + nx] != 0) {
                        edge = true;
                    }
                }
                c.set(x, y, edge && z == FUR ? SEP : z);
            }
        }
        // outline the head against the bulb, as the separate sitting layers are
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                int z = before[y * c.w + x];
                if (front.get(x, y) != 0 || (z != ACC1 && z != ACC1_FAR && z != ACC2 && z != ACC2_FAR)) {
                    continue;
                }
                boolean touches = false;
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        touches |= front.get(x + ox, y + oy) != 0;
                    }
                }
                if (touches) {
                    c.set(x, y, LINE);
                }
            }
        }
        // chin resting on the front paws
        rect(c, 4, 23, 7, 3, FUR);
        rect(c, 19, 23, 7, 3, FUR);
        pxPair(c, 10, 23, SEP);
        for (int x : new int[] {5, 7, 9, 20, 22, 24}) {
            px(c, x, 25, ACC3);
        }
        return render(c, BULBA);
    }

    // =============================================================== WALK

    /*
     * Side view built on Bulbasaur's real profile: a big frog-like head as
     * tall as the sitting head with the mouth running back under the eye; a
     * short squat body slung low between four short splayed legs; the bulb
     * as a rounded onion on its back, tip leaning back, leaf seams curving
     * down from the tip. 39x30, ground row 28.
     */
    static final String W_HEAD = "M25 8H33V9H35V10H36V11H37V12H38V19H37V20H36V21H34V22H24V21H22V20H21V19H20V12H21V11H22V10H23V9H25Z";
    static final String W_EAR = "M24 5H25V6H26V7H27V9H22V8H23V6H24Z";
    static final String W_FAR_EAR = "M21 6H22V7H23V9H20V7H21Z";
    static final String W_RING = "M30 11H33V12H34V16H33V17H30V16H29V12H30Z";
    static final String W_IRIS = "M31 12H33V16H31V15H30V13H31Z";
    static final String W_BODY = "M12 13H21V14H23V15H24V16H25V23H24V24H22V25H11V24H9V23H8V16H9V15H10V14H12Z";
    static final String W_BULB = "M9 1H11V2H12V3H14V4H16V5H18V6H19V7H20V8H21V10H22V13H21V14H20V15H18V16H7V15H5V14H4V12H3V8H4V6H5V5H6V4H7V3H8V2H9Z";

    /** White claws on the front of a leg's foot row. */
    static void claws(Canvas c, Canvas leg) {
        int bottom = -1;
        for (int y = 0; y < leg.h; y++) {
            for (int x = 0; x < leg.w; x++) {
                if (leg.get(x, y) != 0) {
                    bottom = y;
                }
            }
        }
        int right = -1;
        for (int x = 0; x < leg.w; x++) {
            if (leg.get(x, bottom) != 0) {
                right = x;
            }
        }
        if (right >= 0) {
            px(c, right, bottom, ACC3);
            px(c, right - 2, bottom, ACC3);
        }
    }

    /** The side-view bulb as zones, dy rows down: seams from the tip, shine, shade. */
    static void sideBulb(Canvas c, int dy, int sway) {
        Canvas b = new Canvas(c.w, c.h);
        fill(b, W_BULB, ACC1, 0, dy, false);
        paint(b, 15, 9 + dy, 8, 8, ACC1, ACC2_FAR);    // shade, lower right
        paint(b, 3, 13 + dy, 20, 3, ACC1, ACC2_FAR);
        int[][] seams = {{10, 3}, {10, 4}, {11, 5}, {11, 6}, {12, 7}, {12, 8}, {13, 9}, {13, 10},
                {13, 11}, {13, 12}, {13, 13}, {8, 4}, {7, 5}, {6, 6}, {6, 7}, {5, 8}, {5, 9}, {5, 10},
                {5, 11}, {14, 5}, {16, 6}, {17, 7}, {18, 8}, {18, 9}, {19, 10}, {19, 11}, {19, 12}};
        for (int[] s : seams) {
            paint(b, s[0], s[1] + dy, 1, 1, ACC1, ACC1_FAR);
            paint(b, s[0], s[1] + dy, 1, 1, ACC2_FAR, ACC1_FAR);
        }
        paint(b, 8, 3 + dy, 2, 1, ACC1, ACC2);         // shine on the upper left
        paint(b, 7, 4 + dy, 1, 3, ACC1, ACC2);
        shiftRows(b, 0, 5 + dy, sway);
        for (int i = 0; i < b.z.length; i++) {
            if (b.z[i] != 0) {
                c.z[i] = b.z[i];
            }
        }
    }

    /** Walking on four short splayed legs, facing right. 8 frames, 39x30. */
    static PoseArt.Grid walk(int f) {
        Canvas c = new Canvas(39, 30);
        int b = Gait.BOB[f], hb = Gait.BOB[(f + 7) % 8];
        int gy = 28;
        Canvas fh = Gait.front(c, 13, 21 + b, gy, (f + 4) % 8);
        Canvas ff = Gait.front(c, 25, 21 + b, gy, (f + 2) % 8);
        Canvas nh = Gait.front(c, 10, 21 + b, gy, f);
        Canvas nf = Gait.front(c, 22, 21 + b, gy, (f + 6) % 8);
        Gait.draw(c, fh, false, FUR_FAR);
        Gait.draw(c, ff, false, FUR_FAR);
        Gait.draw(c, nh, true, FUR);
        Gait.draw(c, nf, true, FUR);
        for (Canvas leg : new Canvas[] {fh, ff, nh, nf}) {
            claws(c, leg);
        }
        fill(c, W_BODY, FUR, 0, b, false);
        paint(c, 0, 22 + b, 39, 3, FUR, FUR_FAR);      // shaded underside
        mark(c, 10, 18 + b, 3, 2);                     // body spots
        mark(c, 15, 20 + b, 2, 2);
        sideBulb(c, b, Gait.SWAY[f] > 0 ? 1 : 0);
        fill(c, W_FAR_EAR, FUR_FAR, 0, hb, false);
        fillOver(c, W_HEAD, FUR, 0, hb, false);
        fill(c, W_EAR, FUR, 0, hb, false);
        paint(c, 20, 19 + hb, 19, 3, FUR, FUR_FAR);    // under the jaw
        paint(c, 25, 9 + hb, 5, 1, FUR, CREAM);        // crown highlight
        rect(c, 28, 9 + hb, 3, 2, PATTERN);            // forehead spot
        mark(c, 22, 14 + hb, 2, 2);                    // cheek spot
        fill(c, W_RING, RING, 0, hb, false);
        fill(c, W_IRIS, GLINT, 0, hb, false);              // white eye, red iris ahead
        rect(c, 31, 13 + hb, 2, 3, IRIS);
        rect(c, 31, 14 + hb, 2, 1, PUPIL);
        px(c, 31, 13 + hb, GLINT);
        // the long smile runs back under the eye; open, with a tongue
        px(c, 37, 17 + hb, MOUTH);
        rect(c, 27, 18 + hb, 10, 1, MOUTH);
        px(c, 26, 17 + hb, MOUTH);
        rect(c, 30, 19 + hb, 5, 1, INNER_EAR);
        rect(c, 31, 20 + hb, 3, 1, MOUTH);
        return render(c, BULBA);
    }
}
