package com.deskcat;

import static com.deskcat.Gait.*;
import static com.deskcat.PathArt.*;

/**
 * The doge (a Shiba Inu) in the {@link PathArt} format. What reads as doge
 * rather than fox: a wide face with puffy cream cheeks, small upright ears,
 * cream eyebrow dots, a smug squint, and the tail curled over its back.
 * Poses reuse the cat's body shapes ({@link CatArt}) with a dog's head.
 */
final class DogArt {

    private DogArt() {
    }

    static final Palette SHIBA = new Palette()
            .set(FUR, "D9A066").set(FUR_FAR, "BF8850")
            .set(CREAM, "F7E6CC").set(CREAM_FAR, "E3CFB2")
            .set(INNER_EAR, "F7E6CC").set(NOSE, "2E2622").set(MOUTH, "2E2622")
            .set(PATTERN, "B57A42").set(LINE, "2A2230").set(SEP, "C08850");

    // ============================================================ SITTING

    static final String SIT_EAR = "M7 2H8V3H9V4H10V5H11V6H12V9H4V7H5V5H6V3H7Z";
    static final String SIT_EAR_IN = "M7 4H8V5H9V6H10V8H6V7H7Z";
    static final String SIT_HEAD = "M10 6H20V7H23V8H25V9H26V10H27V14H28V15H29V17H28V18H27V19H25V20H22V21H8V20H5V19H3V18H2V17H1V15H2V14H3V10H4V9H5V8H7V7H10Z";
    static final String SIT_CHEEK = "M2 14H8V15H10V16H11V17H12V18H13V19H14V20H15V21H8V20H5V19H3V18H2V17H1V15H2Z";
    static final String SIT_MUZZLE = "M12 12H18V13H19V17H18V18H12V17H11V13H12Z";
    static final String SIT_NOSE = "M13 12H17V14H16V15H14V14H13Z";
    static final String SIT_BODY = "M8 19H22V20H23V22H24V24H25V26H26V30H4V26H5V24H6V22H7V20H8Z";
    static final String SIT_CHEST = "M11 20H19V23H18V25H17V26H13V25H12V23H11Z";
    static final String SIT_LEG = "M7 23H11V30H7Z";

    /**
     * Sitting shiba, 30x30. The eyes are runtime beads (eyeStyle 1, 3x2:
     * cols 8-10 / 19-21, rows 11-12, so FBO x 10 / 21, y 17).
     */
    static PoseArt.Grid sitBody() {
        Canvas c = new Canvas(30, 30);
        fill(c, SIT_BODY, FUR);
        fillPair(c, SIT_EAR, FUR);
        fill(c, SIT_HEAD, FUR);
        fillPair(c, SIT_EAR_IN, INNER_EAR);
        fillPair(c, SIT_CHEEK, CREAM);
        fill(c, SIT_MUZZLE, CREAM);
        fill(c, SIT_NOSE, NOSE);
        px(c, 14, 15, MOUTH);
        px(c, 15, 15, MOUTH);
        px(c, 13, 16, MOUTH);
        px(c, 16, 16, MOUTH);
        rect(c, 9, 9, 2, 1, CREAM);          // eyebrow dots
        rect(c, 19, 9, 2, 1, CREAM);
        mark(c, 14, 7, 2, 1);                // a darker brow ridge
        fill(c, SIT_CHEST, CREAM);
        fillOver(c, SIT_LEG, FUR, 0, 0, false);
        fillOver(c, SIT_LEG, FUR, 0, 0, true);
        paintPair(c, 7, 27, 4, 3, FUR, CREAM);   // white socks
        rect(c, 4, 28, 2, 2, CREAM);             // hind paws
        rect(c, 24, 28, 2, 2, CREAM);
        return render(c, SHIBA);
    }

    static final String SIT_TAIL = "M4 3H8V4H10V5H11V6H12V10H11V11H10V12H9V13H8V14H7V15H6V16H5V17H4V22H0V17H1V16H2V15H3V14H4V13H5V11H3V10H2V9H1V5H2V4H4Z";
    static final String SIT_TAIL_IN = "M4 5H8V6H9V8H8V9H4V8H3V6H4Z";

    /** The curled tail peeking out beside the hip; 3 wag frames, 12x23. */
    static PoseArt.Grid sitTail(int frame) {
        Canvas c = new Canvas(12, 23);
        fill(c, SIT_TAIL, FUR);
        fill(c, SIT_TAIL_IN, CREAM);
        px(c, 5, 6, FUR);                    // the curl's centre
        px(c, 6, 7, FUR);
        shiftRows(c, 0, 10, frame == 1 ? 1 : (frame == 2 ? -1 : 0));
        return render(c, SHIBA);
    }

    // =============================================================== WALK

    /*
     * Side view on the sitting doge's proportions: big head with cream
     * cheeks and muzzle, small upright ears, eyebrow dot, chunky body with a
     * cream underside, short thick legs in white socks, tail curled over the
     * back. 38x30, ground row 28.
     */
    static final String D_HEAD = "M23 4H29V5H31V6H32V7H33V14H32V16H31V17H29V18H23V17H21V16H20V15H19V7H20V6H21V5H23Z";
    static final String D_MUZZLE = "M29 10H34V11H36V14H35V15H33V16H29V15H28V11H29Z";
    static final String D_CHEEK = "M22 12H29V13H31V14H32V16H31V17H29V18H24V17H23V16H22V15H21V13H22Z";
    static final String D_EAR = "M28 1H29V2H30V4H31V5H25V4H26V3H27V2H28Z";
    static final String D_EAR_IN = "M27 3H29V4H27Z";
    static final String D_FAR_EAR = "M22 2H23V3H24V5H21V3H22Z";
    static final String D_TAIL = "M8 7H12V8H13V12H12V13H8V12H7V11H6V9H7V8H8Z";

    /** Walking, facing right; 8 frames, 38x30. */
    static PoseArt.Grid walk(int f) {
        Canvas c = new Canvas(38, 30);
        int b = BOB[f], hb = headBob(f);
        int gy = 28;
        draw(c, hind(c, 14, 20 + b, gy, (f + 4) % 8), false, CREAM_FAR, 2);
        draw(c, front(c, 25, 20 + b, gy, (f + 2) % 8), false, CREAM_FAR, 2);
        draw(c, hind(c, 10, 20 + b, gy, f), true, CREAM, 2);
        draw(c, front(c, 21, 20 + b, gy, (f + 6) % 8), true, CREAM, 2);
        fill(c, D_FAR_EAR, FUR_FAR, 0, hb, false);
        fill(c, CatArt.C_BODY, FUR, 0, b, false);
        paint(c, 0, 22 + b, 38, 2, FUR, CREAM);       // cream underside
        fill(c, CatArt.C_CHEST, CREAM, 0, b, false);
        // the curled tail on its back, wagging
        Canvas t = new Canvas(38, 30);
        fill(t, D_TAIL, FUR, 0, b, false);
        rect(t, 8, 9 + b, 3, 2, CREAM);
        px(t, 9, 9 + b, FUR);
        shiftRows(t, 0, 13 + b, new int[] {0, 1, 1, 0, 0, -1, -1, 0}[f]);
        paste(c, t, 0, 0, false);
        fillOver(c, D_HEAD, FUR, 0, hb, false);
        fill(c, D_EAR, FUR, 0, hb, false);
        fill(c, D_EAR_IN, INNER_EAR, 0, hb, false);
        fill(c, D_CHEEK, CREAM, 0, hb, false);
        fill(c, D_MUZZLE, CREAM, 0, hb, false);
        mark(c, 24, 5 + hb, 2, 1);                   // darker brow
        rect(c, 28, 8 + hb, 2, 2, PUPIL);            // eye
        px(c, 28, 8 + hb, GLINT);
        rect(c, 28, 6 + hb, 2, 1, CREAM);            // eyebrow dot
        rect(c, 34, 10 + hb, 2, 2, NOSE);
        px(c, 33, 13 + hb, MOUTH);
        px(c, 32, 14 + hb, MOUTH);
        return render(c, SHIBA);
    }

    // ============================================================== SLEEP

    /** The sitting doge's head on a 30x22 canvas, eyes closed: happy or asleep. */
    private static Canvas closedHead(boolean happy) {
        Canvas h = new Canvas(30, 22);
        fillPair(h, SIT_EAR, FUR);
        fill(h, SIT_HEAD, FUR);
        fillPair(h, SIT_EAR_IN, INNER_EAR);
        fillPair(h, SIT_CHEEK, CREAM);
        fill(h, SIT_MUZZLE, CREAM);
        fill(h, SIT_NOSE, NOSE);
        px(h, 14, 15, MOUTH);
        px(h, 15, 15, MOUTH);
        px(h, 13, 16, MOUTH);
        px(h, 16, 16, MOUTH);
        rect(h, 9, 9, 2, 1, CREAM);                  // eyebrow dots
        rect(h, 19, 9, 2, 1, CREAM);
        mark(h, 14, 7, 2, 1);
        for (int ex : new int[] {8, 19}) {
            if (happy) {
                rect(h, ex, 11, 3, 1, LINE);
                px(h, ex - 1, 12, LINE);
                px(h, ex + 3, 12, LINE);
            } else {
                rect(h, ex, 12, 3, 1, LINE);
            }
        }
        return h;
    }

    static final String LOAF = "M26 7H34V8H36V9H37V20H36V21H35V22H25V21H24V20H23V9H24V8H26Z";
    static final String LOAF_IN = "M26 6H34V7H36V8H37V20H36V21H35V22H25V21H24V20H23V8H24V7H26Z";
    static final String LOAF_TAIL = "M30 3H35V4H36V5H37V8H36V9H35V10H30V9H29V8H28V4H30Z";

    /** Curled up: the sitting head on its paws, loaf behind, curled tail on top; 2 breaths, 38x23. */
    static PoseArt.Grid sleep(int frame) {
        Canvas c = new Canvas(38, 23);
        int lift = frame == 0 ? 2 : 1;
        fill(c, frame == 0 ? LOAF : LOAF_IN, FUR);
        paint(c, 0, 20, 38, 2, FUR, CREAM);          // cream underside
        fillOver(c, LOAF_TAIL, FUR, 0, lift, false);
        rect(c, 31, 5 + lift, 3, 2, CREAM);
        px(c, 32, 5 + lift, FUR);
        Canvas front = new Canvas(38, 23);
        paste(front, closedHead(false), 0, -2, false);
        creasePaste(c, front);
        rect(c, 8, 19, 4, 2, CREAM);                 // paws under the chin
        rect(c, 18, 19, 4, 2, CREAM);
        px(c, 10, 20, SEP);
        px(c, 20, 20, SEP);
        return render(c, SHIBA);
    }

    // =============================================================== ROLL

    /** The whole sitting doge (tail, body, face) as zones, grinning. 36x30. */
    private static Canvas delighted() {
        Canvas c = new Canvas(36, 30);
        Canvas t = new Canvas(12, 23);
        fill(t, SIT_TAIL, FUR);
        fill(t, SIT_TAIL_IN, CREAM);
        px(t, 5, 6, FUR);
        px(t, 6, 7, FUR);
        paste(c, t, 24, 7, false);
        Canvas b = new Canvas(30, 30);
        fill(b, SIT_BODY, FUR);
        paste(b, closedHead(true), 0, 0, false);
        rect(b, 13, 16, 4, 1, MOUTH);                // open grin
        rect(b, 14, 17, 2, 1, INNER_EAR);
        fill(b, SIT_CHEST, CREAM);
        fillOver(b, SIT_LEG, FUR, 0, 0, false);
        fillOver(b, SIT_LEG, FUR, 0, 0, true);
        paintPair(b, 7, 27, 4, 3, FUR, CREAM);
        rect(b, 4, 28, 2, 2, CREAM);
        rect(b, 24, 28, 2, 2, CREAM);
        paste(c, b, 0, 0, false);
        return c;
    }

    /** Rolled onto its back: the sitting doge upside down, socks paddling; 2 frames, 36x30. */
    static PoseArt.Grid roll(int frame) {
        Canvas f = new Canvas(36, 30);
        paste(f, delighted(), 0, 0, true);
        Canvas c = new Canvas(36, 30);
        paste(c, f, 0, 1, false);                    // its paws reach the sitting frame's last row
        shiftRows(c, 1, 3, frame == 0 ? 0 : 1);
        return render(c, SHIBA);
    }
}
