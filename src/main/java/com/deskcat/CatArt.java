package com.deskcat;

import static com.deskcat.PathArt.*;

/**
 * The cat skins in the {@link PathArt} format. The orange tabby and the
 * black cat are the same shapes through two palettes, so a new cat is just
 * a new palette.
 */
final class CatArt {

    private CatArt() {
    }

    /** Orange tabby: cream bib and paws, tabby stripes, green eyes. */
    static final Palette TABBY = new Palette()
            .set(FUR, "F29A4B").set(FUR_FAR, "D9823B")
            .set(CREAM, "FFF4E3").set(CREAM_FAR, "EADBC4")
            .set(INNER_EAR, "F5A9B8").set(NOSE, "E98AA0")
            .set(IRIS, "9BC64B").set(RING, "2A2230").set(PATTERN, "D46F27")
            .set(LINE, "2A2230").set(MOUTH, "2A2230").set(SEP, "D9823B");

    /**
     * Black cat: no markings, amber eyes, and a light outline and whiskers
     * so it stays readable on a dark wallpaper.
     */
    static final Palette BLACK = new Palette()
            .set(FUR, "2E2936").set(FUR_FAR, "221E29")
            .set(CREAM, "2E2936").set(CREAM_FAR, "221E29")
            .set(INNER_EAR, "A06A80").set(NOSE, "A06A80")
            .set(IRIS, "F2C14E").set(RING, "17131C").set(PATTERN, "2E2936")
            .set(LINE, "ECE8F4").set(MOUTH, "2E2936").set(SEP, "4A4358")
            .outline("ECE8F4").whisker("ECE8F4");

    // ============================================================ SITTING

    /*
     * Sitting cat, 28x30 (fits the 40x30 FBO). Head 22 wide, low wide eyes,
     * small ears, a body that flares at the haunches, cream bib, muzzle and
     * front paws. Symmetric parts are authored once and mirrored.
     */
    static final String SIT_HEAD = "M7 5H21V6H23V7H24V8H25V16H24V17H22V18H6V17H4V16H3V8H4V7H5V6H7Z";
    static final String SIT_EAR = "M5 1H7V2H8V3H9V4H10V5H11V7H4V3H5Z";
    static final String SIT_EAR_IN = "M6 2H7V3H8V4H9V5H5V3H6Z";
    static final String SIT_BODY = "M7 15H21V18H22V19H23V22H24V23H25V28H24V29H4V28H3V23H4V22H5V19H6V18H7Z";
    static final String SIT_BIB = "M11 18H17V19H18V23H17V24H11V23H10V19H11Z";
    static final String SIT_MUZZLE = "M11 13H17V14H18V16H17V17H11V16H10V14H11Z";
    static final String SIT_PAW = "M9 26H12V27H13V29H8V27H9Z";
    static final String SIT_HIND = "M4 27H7V29H4Z";
    static final String SIT_EYE_RING = "M6 8H10V9H11V13H10V14H6V13H5V9H6Z";
    static final String SIT_EYE_IRIS = "M7 9H9V10H10V12H9V13H7V12H6V10H7Z";

    /**
     * The sitting body. Eye sockets (ring + iris) are baked in; the pupil
     * and glint are drawn at runtime so the eyes can follow the cursor
     * (eyeStyle 5: iris cols 6-9 / 18-21, rows 9-12, so FBO x 8 / 20, y 17).
     */
    static PoseArt.Grid sitBody(Palette p) {
        Canvas c = new Canvas(28, 30);
        fill(c, SIT_BODY, FUR);
        fill(c, SIT_EAR, FUR);
        fillMirror(c, SIT_EAR, FUR);
        fill(c, SIT_HEAD, FUR);
        fill(c, SIT_EAR_IN, INNER_EAR);
        fillMirror(c, SIT_EAR_IN, INNER_EAR);
        // tabby markings: forehead "M", cheek bars, flank stripes
        mark(c, 11, 6, 1, 2);
        mark(c, 13, 5, 2, 2);
        mark(c, 16, 6, 1, 2);
        markPair(c, 3, 11, 2, 1);
        markPair(c, 3, 13, 2, 1);
        markPair(c, 5, 20, 2, 1);
        markPair(c, 4, 23, 2, 1);
        fill(c, SIT_BIB, CREAM);
        fill(c, SIT_MUZZLE, CREAM);
        fill(c, SIT_PAW, CREAM);
        fillMirror(c, SIT_PAW, CREAM);
        fill(c, SIT_HIND, CREAM);
        fillMirror(c, SIT_HIND, CREAM);
        px(c, 10, 28, SEP);                  // toe notches
        px(c, 17, 28, SEP);
        fill(c, SIT_EYE_RING, RING);
        fillMirror(c, SIT_EYE_RING, RING);
        fill(c, SIT_EYE_IRIS, IRIS);
        fillMirror(c, SIT_EYE_IRIS, IRIS);
        rect(c, 13, 13, 2, 1, NOSE);
        px(c, 12, 14, MOUTH);                // a small ":3" mouth
        px(c, 13, 15, MOUTH);
        px(c, 14, 15, MOUTH);
        px(c, 15, 14, MOUTH);
        return render(c, p, new int[][] {
                {0, 13}, {1, 13}, {0, 15}, {1, 15},
                {27, 13}, {26, 13}, {27, 15}, {26, 15}});
    }

    static final String SIT_TAIL = "M3 7H6V8H7V9H8V10H9V17H8V18H7V19H6V20H5V21H4V22H0V21H1V20H2V19H3V18H4V17H5V11H4V9H3V7Z";

    /** The curling tail behind the right haunch; 3 wag frames, 12x23. */
    static PoseArt.Grid sitTail(Palette p, int frame) {
        Canvas c = new Canvas(12, 23);
        fill(c, SIT_TAIL, FUR, 1, 0, false);
        mark(c, 6, 11, 4, 1);
        mark(c, 6, 14, 4, 1);
        mark(c, 4, 8, 3, 1);
        shiftRows(c, 0, 10, frame == 1 ? 1 : (frame == 2 ? -1 : 0));
        return render(c, p, null);
    }

    /** A 4x4 paw for kneading, swiping and grooming. */
    static PoseArt.Grid paw(Palette p) {
        return PathArt.paw(p, p == BLACK ? FUR : CREAM);
    }

    // =============================================================== WALK

    static final String W_FAR_EAR = "M22 2H23V3H24V5H22Z";
    static final String W_HEAD = "M22 4H28V5H29V6H30V11H29V12H28V13H22V12H21V11H20V6H21V5H22Z";
    static final String W_NEAR_EAR = "M26 1H27V5H23V4H24V3H25V2H26Z";
    static final String W_BODY = "M9 10H21V11H23V12H24V17H23V18H21V19H9V18H7V17H6V12H7V11H9Z";
    static final String W_CHEST = "M19 13H23V14H24V17H23V18H19V17H18V14H19Z";
    static final String W_MUZZLE = "M26 9H29V10H30V12H29V13H26V12H25V10H26Z";
    static final String W_EYE_RING = "M23 5H26V6H27V9H26V10H23V9H22V6H23Z";
    static final String W_TAIL = "M4 3H7V4H6V5H5V9H6V11H7V12H8V13H5V12H4V11H3V9H2V5H3V4H4Z";

    /** Side-view walk, facing right; 4 frames of a trot, 32x24. */
    static PoseArt.Grid walk(Palette p, int frame) {
        Canvas c = new Canvas(32, 24);
        // diagonal pairs swing together: A = near-front + far-back
        int[] adx = {1, 0, -1, 0}, alift = {0, 0, 0, 1};
        int[] bdx = {-1, 0, 1, 0}, blift = {0, 1, 0, 0};
        fill(c, W_TAIL, FUR);
        mark(c, 2, 6, 3, 1);
        mark(c, 3, 10, 3, 1);
        shiftRows(c, 0, 7, new int[] {0, 1, 0, -1}[frame]);
        leg(c, 21 + bdx[frame], blift[frame], false);   // far front
        leg(c, 10 + adx[frame], alift[frame], false);   // far back
        fill(c, W_FAR_EAR, FUR_FAR);
        fill(c, W_BODY, FUR);
        fill(c, W_HEAD, FUR);
        fill(c, W_NEAR_EAR, FUR);
        px(c, 26, 2, INNER_EAR);
        px(c, 25, 3, INNER_EAR);
        px(c, 26, 3, INNER_EAR);
        mark(c, 10, 10, 1, 3);
        mark(c, 13, 10, 1, 3);
        mark(c, 16, 10, 1, 3);
        mark(c, 21, 5, 1, 2);
        fill(c, W_CHEST, CREAM);
        fill(c, W_MUZZLE, CREAM);
        leg(c, 19 + adx[frame], alift[frame], true);    // near front
        leg(c, 8 + bdx[frame], blift[frame], true);     // near back
        fill(c, W_EYE_RING, RING);
        rect(c, 23, 6, 3, 3, IRIS);
        rect(c, 24, 7, 2, 2, PUPIL);
        px(c, 24, 7, GLINT);
        px(c, 29, 10, NOSE);
        px(c, 28, 12, MOUTH);
        return render(c, p, new int[][] {{31, 11}, {31, 13}});
    }

    /** A leg hanging from the body; lift raises the paw one row. */
    private static void leg(Canvas c, int x, int lift, boolean near) {
        int w = near ? 3 : 2;
        rect(c, x, 17, w, 5 - lift, near ? FUR : FUR_FAR);
        rect(c, x, 22 - lift, w, 1, near ? CREAM : CREAM_FAR);
    }

    // ============================================================== SLEEP

    static final String L_BODY = "M13 4H21V5H24V6H25V7H26V9H27V14H26V15H25V16H24V17H8V16H7V15H6V8H7V7H8V6H10V5H13Z";
    static final String L_BODY_IN = "M13 3H21V4H24V5H25V6H26V8H27V14H26V15H25V16H24V17H8V16H7V15H6V7H7V6H8V5H10V4H13Z";
    static final String L_TAIL = "M13 14H24V15H25V17H12V15H13Z";
    static final String L_HEAD = "M3 7H10V8H11V9H12V15H11V16H10V17H3V16H2V15H1V9H2V8H3Z";
    static final String L_EAR_L = "M3 4H4V5H5V6H6V7H3Z";
    static final String L_EAR_R = "M9 4H10V7H7V6H8V5H9Z";

    /** Curled up asleep, tail wrapped round the front; 2 breaths, 30x18. */
    static PoseArt.Grid sleep(Palette p, int frame) {
        Canvas c = new Canvas(30, 18);
        fill(c, frame == 0 ? L_BODY : L_BODY_IN, FUR);
        int top = frame == 0 ? 4 : 3;
        mark(c, 14, top, 1, 3);
        mark(c, 17, top, 1, 3);
        mark(c, 20, top + 1, 1, 3);
        mark(c, 23, top + 2, 1, 2);
        fillOver(c, L_TAIL, FUR);
        mark(c, 16, 15, 1, 2);
        mark(c, 20, 15, 1, 2);
        fillOver(c, L_HEAD, FUR);
        fill(c, L_EAR_L, FUR);
        fill(c, L_EAR_R, FUR);
        for (int[] q : new int[][] {{3, 5}, {3, 6}, {4, 6}, {9, 5}, {9, 6}, {8, 6}}) {
            px(c, q[0], q[1], INNER_EAR);
        }
        mark(c, 5, 8, 1, 1);                 // a little forehead stripe
        mark(c, 7, 8, 1, 1);
        rect(c, 3, 13, 6, 2, CREAM);         // muzzle
        rect(c, 4, 15, 4, 1, CREAM);
        rect(c, 5, 13, 2, 1, NOSE);
        for (int ex : new int[] {2, 7}) {    // closed, contented eyes
            px(c, ex, 11, LINE);
            px(c, ex + 1, 12, LINE);
            px(c, ex + 2, 11, LINE);
        }
        return render(c, p, null);
    }

    // =============================================================== ROLL

    static final String R_HEAD = "M3 7H9V8H10V9H11V15H10V16H9V17H3V16H2V15H1V9H2V8H3Z";
    static final String R_EAR_L = "M2 16H6V17H5V18H4V19H2Z";
    static final String R_EAR_R = "M7 16H11V19H9V18H8V17H7Z";
    static final String R_BODY = "M13 8H25V9H27V10H28V14H27V15H25V16H13V15H11V14H10V10H11V9H13Z";
    static final String R_BELLY = "M15 9H23V10H24V12H23V13H15V12H14V10H15Z";

    /** Belly-up with paws in the air, delighted; 2 wiggle frames, 32x20. */
    static PoseArt.Grid roll(Palette p, int frame) {
        Canvas c = new Canvas(32, 20);
        // front paws folded on the chest, hind legs up in the air;
        // alternate ones paddle up and down
        int[][] legs = {{13, 7, 0}, {16, 7, 1}, {21, 5, 1}, {24, 5, 0}};
        for (int[] l : legs) {
            int y = l[1] - ((l[2] + frame) % 2);
            boolean near = l[0] == 13 || l[0] == 24;
            rect(c, l[0], y, 2, 9 - y, near ? FUR : FUR_FAR);
            rect(c, l[0], y, 2, 1, near ? CREAM : CREAM_FAR);
        }
        rect(c, 27, 13, 3, 1, FUR);          // tail along the ground, tip up
        rect(c, 29, 10, 2, 3, FUR);
        rect(c, 30, 9, 1, 1, FUR);
        fill(c, R_BODY, FUR);
        fill(c, R_BELLY, CREAM);
        mark(c, 11, 12, 2, 1);
        mark(c, 25, 11, 2, 1);
        mark(c, 12, 14, 2, 1);
        mark(c, 24, 13, 2, 1);
        fill(c, R_EAR_L, FUR);
        fill(c, R_EAR_R, FUR);
        fillOver(c, R_HEAD, FUR);
        px(c, 3, 17, INNER_EAR);
        px(c, 9, 17, INNER_EAR);
        rect(c, 4, 8, 4, 2, CREAM);          // chin up, upside down
        rect(c, 5, 10, 2, 1, NOSE);
        mark(c, 5, 15, 1, 1);
        for (int ex : new int[] {2, 7}) {    // happy ^ ^ eyes, flipped over
            px(c, ex, 12, LINE);
            px(c, ex + 1, 13, LINE);
            px(c, ex + 2, 12, LINE);
        }
        return render(c, p, null);
    }
}
