package com.deskcat;

import static com.deskcat.Gait.*;
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

    /*
     * Side view on the sitting design's proportions: the head is as tall as
     * the sitting head (~14 rows) with the same triangle ears, big socketed
     * eye, cream muzzle and stripes; a round chunky body with a cream bib;
     * short thick legs; a thick tail. 36x30, ground row 28.
     */
    static final String C_HEAD = "M23 4H30V5H32V6H33V7H34V14H33V16H32V17H30V18H23V17H21V16H20V15H19V7H20V6H21V5H23Z";
    static final String C_MUZZLE = "M30 11H34V12H35V14H34V15H30V14H29V12H30Z";
    static final String C_EAR = "M27 1H28V2H29V3H30V4H31V5H24V4H25V3H26V2H27Z";
    static final String C_EAR_IN = "M27 2H28V3H29V4H26V3H27Z";
    static final String C_FAR_EAR = "M21 2H22V3H23V4H24V5H20V4H21Z";
    static final String C_RING = "M27 7H30V8H31V12H30V13H27V12H26V8H27Z";
    static final String C_IRIS = "M28 8H30V12H28V11H27V9H28Z";
    static final String C_BODY = "M10 12H21V13H24V14H26V15H27V17H28V21H27V22H26V23H24V24H9V23H7V22H6V21H5V16H6V15H7V14H8V13H10Z";
    static final String C_CHEST = "M23 14H27V15H28V20H27V21H23V20H22V15H23Z";
    static final String C_TAIL = "M2 4H5V6H4V9H5V10H6V11H8V12H9V13H10V14H5V13H4V12H3V11H2V10H1V5H2Z";

    /** Walking, facing right; 8 frames of a lateral-sequence walk, 36x30. */
    static PoseArt.Grid walk(Palette p, int f) {
        Canvas c = new Canvas(36, 30);
        int b = BOB[f], hb = headBob(f);
        int gy = 28;
        fill(c, C_TAIL, FUR, 0, b, false);
        mark(c, 1, 7 + b, 3, 1);
        mark(c, 1, 10 + b, 3, 1);
        shiftRows(c, 0, 8 + b, SWAY[f]);
        // legs first, so they come out from under the body
        draw(c, hind(c, 14, 20 + b, gy, (f + 4) % 8), false, CREAM_FAR);   // far hind
        draw(c, front(c, 25, 20 + b, gy, (f + 2) % 8), false, CREAM_FAR);  // far fore
        draw(c, hind(c, 10, 20 + b, gy, f), true, CREAM);                  // near hind
        draw(c, front(c, 21, 20 + b, gy, (f + 6) % 8), true, CREAM);       // near fore
        fill(c, C_FAR_EAR, FUR_FAR, 0, hb, false);
        fill(c, C_BODY, FUR, 0, b, false);
        mark(c, 11, 12 + b, 1, 3);
        mark(c, 14, 12 + b, 1, 3);
        mark(c, 17, 12 + b, 1, 3);
        fill(c, C_CHEST, CREAM, 0, b, false);
        fillOver(c, C_HEAD, FUR, 0, hb, false);
        fill(c, C_EAR, FUR, 0, hb, false);
        fill(c, C_EAR_IN, INNER_EAR, 0, hb, false);
        mark(c, 25, 4 + hb, 1, 2);                   // forehead stripes
        mark(c, 23, 5 + hb, 1, 2);
        mark(c, 20, 11 + hb, 2, 1);                  // cheek stripes
        mark(c, 20, 13 + hb, 2, 1);
        fill(c, C_MUZZLE, CREAM, 0, hb, false);
        fill(c, C_RING, RING, 0, hb, false);
        fill(c, C_IRIS, IRIS, 0, hb, false);
        rect(c, 28, 9 + hb, 2, 2, PUPIL);
        px(c, 28, 9 + hb, GLINT);
        px(c, 33, 11 + hb, NOSE);
        px(c, 34, 12 + hb, NOSE);
        px(c, 33, 14 + hb, MOUTH);
        return render(c, p, new int[][] {{35, 12 + hb}, {35, 14 + hb}});
    }

    // ============================================================== SLEEP

    /**
     * The sitting head (ears, stripes, muzzle, nose) on a 28x19 canvas in
     * sitting coordinates, eyes closed: happy arcs (^) or sleepy curves.
     */
    private static Canvas closedHead(boolean happy) {
        Canvas h = new Canvas(28, 19);
        fillPair(h, SIT_EAR, FUR);
        fill(h, SIT_HEAD, FUR);
        fillPair(h, SIT_EAR_IN, INNER_EAR);
        mark(h, 11, 6, 1, 2);
        mark(h, 13, 5, 2, 2);
        mark(h, 16, 6, 1, 2);
        markPair(h, 3, 11, 2, 1);
        markPair(h, 3, 13, 2, 1);
        fill(h, SIT_MUZZLE, CREAM);
        rect(h, 13, 13, 2, 1, NOSE);
        px(h, 12, 14, MOUTH);
        px(h, 13, 15, MOUTH);
        px(h, 14, 15, MOUTH);
        px(h, 15, 14, MOUTH);
        for (int ex : new int[] {5, 17}) {
            if (happy) {                        // ^ ^
                rect(h, ex + 1, 10, 4, 1, LINE);
                px(h, ex, 11, LINE);
                px(h, ex + 5, 11, LINE);
            } else {                            // contented, closed
                rect(h, ex + 1, 11, 4, 1, LINE);
                px(h, ex, 10, LINE);
                px(h, ex + 5, 10, LINE);
            }
        }
        return h;
    }

    static final String LOAF = "M21 5H31V6H33V7H35V8H36V18H35V19H34V20H18V19H17V18H16V8H17V7H18V6H21Z";
    static final String LOAF_IN = "M21 4H31V5H33V6H35V7H36V18H35V19H34V20H18V19H17V18H16V7H17V6H18V5H21Z";
    static final String LOAF_TAIL = "M30 16H35V17H36V19H35V20H21V19H20V18H21V17H30Z";

    /** Curled up: the sitting head resting on its paws, loaf and tail behind; 2 breaths, 36x21. */
    static PoseArt.Grid sleep(Palette p, int frame) {
        Canvas c = new Canvas(36, 21);
        fill(c, frame == 0 ? LOAF : LOAF_IN, FUR);
        int top = frame == 0 ? 5 : 4;
        mark(c, 24, top, 1, 3);
        mark(c, 27, top, 1, 3);
        mark(c, 30, top + 1, 1, 3);
        mark(c, 33, top + 3, 1, 3);
        fillOver(c, LOAF_TAIL, FUR);
        mark(c, 25, 17, 1, 3);
        mark(c, 29, 17, 1, 3);
        mark(c, 33, 16, 1, 3);
        Canvas front = new Canvas(36, 21);
        paste(front, closedHead(false), 0, -1, false);
        creasePaste(c, front);
        rect(c, 7, 17, 4, 2, CREAM);                 // front paws under the chin
        rect(c, 15, 17, 4, 2, CREAM);
        px(c, 9, 18, SEP);
        px(c, 17, 18, SEP);
        return render(c, p, new int[][] {{0, 12}, {1, 12}, {0, 14}, {1, 14}});
    }

    // =============================================================== ROLL

    /** The whole sitting cat (tail, body, face) as zones, delighted. 34x30. */
    private static Canvas delighted() {
        Canvas c = new Canvas(34, 30);
        Canvas t = new Canvas(12, 23);
        fill(t, SIT_TAIL, FUR, 1, 0, false);
        mark(t, 6, 11, 4, 1);
        mark(t, 6, 14, 4, 1);
        mark(t, 4, 8, 3, 1);
        paste(c, t, 21, 7, false);
        Canvas b = new Canvas(28, 30);
        fill(b, SIT_BODY, FUR);
        paste(b, closedHead(true), 0, 0, false);
        markPair(b, 5, 20, 2, 1);
        markPair(b, 4, 23, 2, 1);
        fill(b, SIT_BIB, CREAM);
        fillPair(b, SIT_PAW, CREAM);
        fillPair(b, SIT_HIND, CREAM);
        px(b, 10, 28, SEP);
        px(b, 17, 28, SEP);
        // mouth open with delight, over the sitting ":3"
        rect(b, 12, 14, 4, 3, CREAM);
        px(b, 12, 14, MOUTH);
        rect(b, 13, 15, 2, 1, INNER_EAR);
        px(b, 15, 14, MOUTH);
        rect(b, 13, 16, 2, 1, MOUTH);
        paste(c, b, 0, 0, false);
        return c;
    }

    /**
     * Rolled onto its back: the sitting cat turned upside down, paws in the
     * air with pink toe beans, paddling; 2 frames, 34x30.
     */
    static PoseArt.Grid roll(Palette p, int frame) {
        Canvas c = new Canvas(34, 30);
        paste(c, delighted(), 0, 0, true);
        for (int x : new int[] {9, 18}) {
            rect(c, x, 2, 2, 1, INNER_EAR);
        }
        px(c, 5, 1, INNER_EAR);
        px(c, 23, 1, INNER_EAR);
        shiftRows(c, 0, 2, frame == 0 ? 0 : 1);
        return render(c, p);
    }
}
