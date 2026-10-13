package com.deskcat;

import static com.deskcat.Gait.*;
import static com.deskcat.PathArt.*;

/**
 * Pikachu in the {@link PathArt} format, drawn to its official proportions:
 * an onion-shaped head (narrow crown, widest at the cheeks), long leaf
 * ears with black tips, a pear body with arms tucked under the chin, big
 * feet, and a stepped bolt tail with a brown base. A shade tone, a crown
 * highlight and a dark-brown outline give it volume. Personal-use fan art
 * (see CLAUDE.md).
 */
final class PikaArt {

    private PikaArt() {
    }

    static final Palette PIKA = new Palette()
            .set(FUR, "F7D23E").set(FUR_FAR, "DEAD24")         // body, shade
            .set(ACC4, "FCE98A")                                 // highlight
            .set(ACC1, "2E2420").set(ACC1_FAR, "241B17")        // ear tips
            .set(ACC2, "8E5A2D")                                 // tail base
            .set(ACC3, "E8473A")                                 // cheeks
            .set(PATTERN, "8E5A2D")                              // back stripes
            .set(INNER_EAR, "E87A82")                            // tongue
            .set(NOSE, "3A2A1E").set(MOUTH, "3A2A1E")
            .set(SEP, "D9A720").set(LINE, "3A2A1E")
            .outline("3A2A1E");

    // ============================================================ SITTING

    static final String SIT_EAR = "M5 0H6V2H7V4H8V6H9V8H10V10H11V11H12V12H8V11H7V10H6V8H5V5H4V1H5Z";
    static final String SIT_HEAD = "M11 9H19V10H21V11H23V12H24V13H25V14H26V16H27V20H26V21H25V22H23V23H7V22H5V21H4V20H3V16H4V14H5V13H6V12H7V11H9V10H11Z";
    static final String SIT_CHEEK = "M4 18H7V19H8V21H7V22H5V21H3V19H4Z";
    static final String SIT_BODY = "M8 20H22V21H23V22H24V24H25V28H24V29H23V30H7V29H6V28H5V24H6V22H7V21H8Z";
    static final String SIT_ARM = "M7 24H11V25H12V26H13V27H9V26H8V25H7Z";
    static final String SIT_FOOT = "M6 27H12V30H5V28H6Z";

    /**
     * Sitting, 30x30. Eyes are runtime round beads (eyeStyle 4, 4x5: cols
     * 8-11 / 18-21, rows 13-17, so FBO x 10 / 20, y 12); cheeks are centred
     * on FBO (7.5, 10) and (26.5, 10) for the sparks.
     */
    static PoseArt.Grid sitBody() {
        Canvas c = new Canvas(30, 30);
        fill(c, SIT_BODY, FUR);
        paint(c, 0, 23, 30, 1, FUR, FUR_FAR);        // shadow under the chin
        paint(c, 22, 23, 8, 7, FUR, FUR_FAR);        // shaded right flank
        fillPair(c, SIT_EAR, FUR);
        paint(c, 0, 0, 30, 3, FUR, ACC1);            // black ear tips
        fill(c, SIT_HEAD, FUR);
        paint(c, 25, 15, 5, 6, FUR, FUR_FAR);        // shaded right cheekbone
        paint(c, 23, 21, 4, 2, FUR, FUR_FAR);
        paint(c, 10, 10, 4, 1, FUR, ACC4);           // highlight on the crown
        paint(c, 8, 11, 3, 1, FUR, ACC4);
        fillPair(c, SIT_CHEEK, ACC3);
        rect(c, 14, 17, 2, 1, NOSE);
        px(c, 12, 18, MOUTH);                        // "w" mouth
        px(c, 13, 19, MOUTH);
        px(c, 14, 18, MOUTH);
        px(c, 15, 18, MOUTH);
        px(c, 16, 19, MOUTH);
        px(c, 17, 18, MOUTH);
        fillOverPair(c, SIT_FOOT, FUR);
        fillPair(c, SIT_ARM, FUR_FAR);               // arms tucked in the chin's shadow
        rect(c, 11, 26, 2, 1, FUR);                  // paws catching the light
        rect(c, 17, 26, 2, 1, FUR);
        px(c, 8, 29, SEP);                           // toes
        px(c, 10, 29, SEP);
        px(c, 19, 29, SEP);
        px(c, 21, 29, SEP);
        return render(c, PIKA);
    }

    static final String SIT_TAIL = "M7 0H13V1H14V3H13V4H12V5H11V6H10V7H9V8H10V9H11V11H10V12H9V13H8V15H9V16H8V17H7V18H6V19H5V20H4V21H3V22H0V20H1V19H2V18H3V17H4V16H5V14H4V13H3V12H4V11H7V9H6V8H5V7H4V5H5V3H6V1H7Z";

    /** The stepped bolt tail behind the right hip; 3 wag frames, 15x24. */
    static PoseArt.Grid sitTail(int frame) {
        Canvas c = new Canvas(15, 24);
        fill(c, SIT_TAIL, FUR, 0, 1, false);
        paint(c, 0, 19, 15, 5, FUR, ACC2);          // brown base
        paint(c, 9, 4, 6, 4, FUR, FUR_FAR);         // shade on the flag
        shiftRows(c, 0, 9, frame == 1 ? 1 : (frame == 2 ? -1 : 0));
        return render(c, PIKA);
    }

    // =============================================================== WALK

    /*
     * Walking upright, with the sitting Pikachu's proportions: a head as tall
     * as the sitting head (14 rows), big oval eye, round red cheek, long
     * black-tipped ears swept back, short body, big feet, bolt tail. 32x30.
     */
    static final String W_TAIL = "M0 3H6V4H7V6H6V8H7V9H8V10H7V12H8V13H9V14H10V15H11V16H12V17H8V16H7V15H6V14H5V13H4V12H3V11H2V10H4V9H3V7H2V6H1V5H0Z";
    static final String W_HEAD = "M16 6H25V7H27V8H28V9H29V10H30V17H29V18H28V19H26V20H16V19H14V18H13V17H12V9H13V8H14V7H16Z";
    static final String W_EAR = "M8 0H10V1H11V2H12V3H13V4H14V5H15V6H16V7H18V8H19V9H15V8H14V7H12V6H11V5H10V4H9V2H8Z";
    static final String W_FAR_EAR = "M20 0H22V2H23V7H19V1H20Z";
    static final String W_EYE = "M24 9H25V10H26V13H25V14H24V13H23V10H24Z";
    static final String W_BODY = "M14 17H23V18H24V19H25V24H24V25H23V26H13V25H12V24H11V20H12V19H13V18H14Z";

    /** Walking upright facing right: ears lag a beat, arm swings, tail sways; 8 frames, 32x30. */
    static PoseArt.Grid walk(int f) {
        Canvas c = new Canvas(32, 30);
        int b = BOB[f], hb = headBob(f), eb = BOB[(f + 6) % 8];
        int gy = 28;
        fill(c, W_TAIL, FUR, 0, 6 + b, false);
        paint(c, 0, 20 + b, 13, 3, FUR, ACC2);
        paint(c, 4, 9 + b, 3, 3, FUR, FUR_FAR);
        shiftRows(c, 0, 13 + b, SWAY[f]);
        draw(c, footLeg(c, 15, 24 + b, gy, (f + 4) % 8, 3, 5), false, FUR_FAR);
        draw(c, footLeg(c, 19, 24 + b, gy, f, 3, 5), true, FUR);
        fill(c, W_BODY, FUR, 0, b, false);
        paint(c, 11, 20 + b, 3, 1, FUR, PATTERN);       // brown back stripes
        paint(c, 11, 23 + b, 2, 1, FUR, PATTERN);
        paint(c, 19, 23 + b, 6, 3, FUR, FUR_FAR);       // belly in shade
        fill(c, W_FAR_EAR, FUR_FAR, 0, 1 + eb, false);
        paint(c, 18, 0, 6, 4 + eb, FUR_FAR, ACC1_FAR);
        fill(c, W_EAR, FUR, 0, 1 + eb, false);
        paint(c, 0, 0, 13, 4 + eb, FUR, ACC1);
        fillOver(c, W_HEAD, FUR, 0, hb, false);
        paint(c, 16, 7 + hb, 5, 1, FUR, ACC4);          // crown highlight
        fill(c, W_EYE, PUPIL, 0, hb, false);
        px(c, 23, 10 + hb, GLINT);                      // glint top-left, like the sitting eyes
        px(c, 23, 11 + hb, GLINT);
        rect(c, 18, 14 + hb, 4, 3, ACC3);               // round cheek
        px(c, 18, 14 + hb, FUR);
        px(c, 21, 16 + hb, FUR);
        px(c, 29, 11 + hb, NOSE);
        px(c, 28, 14 + hb, MOUTH);
        px(c, 27, 15 + hb, MOUTH);
        int ax = 20 + ARM_DX[f], ay = 20 + b;           // arm in shade, hanging forward
        rect(c, ax, ay, 2, 2, FUR_FAR);
        rect(c, ax + 1, ay + 2, 2, 1, FUR_FAR);
        rect(c, ax + 1, ay + 3, 2, 1, FUR);
        return render(c, PIKA);
    }

    // ============================================================== SLEEP

    /** The sitting head (ears optional), eyes closed: happy ^ or asleep. 30x23. */
    private static Canvas closedHead(boolean ears, boolean happy) {
        Canvas h = new Canvas(30, 23);
        if (ears) {
            fillPair(h, SIT_EAR, FUR);
            paint(h, 0, 0, 30, 3, FUR, ACC1);
        }
        fill(h, SIT_HEAD, FUR);
        paint(h, 25, 15, 5, 6, FUR, FUR_FAR);
        paint(h, 23, 21, 4, 2, FUR, FUR_FAR);
        paint(h, 10, 10, 4, 1, FUR, ACC4);
        paint(h, 8, 11, 3, 1, FUR, ACC4);
        fillPair(h, SIT_CHEEK, ACC3);
        rect(h, 14, 17, 2, 1, NOSE);
        for (int ex : new int[] {8, 18}) {
            rect(h, ex, happy ? 14 : 16, 4, 1, LINE);
            px(h, ex - 1, 15, LINE);
            px(h, ex + 4, 15, LINE);
        }
        if (happy) {                                    // open, delighted
            rect(h, 12, 18, 6, 1, MOUTH);
            rect(h, 13, 19, 4, 1, INNER_EAR);
            rect(h, 13, 20, 4, 1, MOUTH);
        } else {
            px(h, 13, 18, MOUTH);
            px(h, 14, 19, MOUTH);
            px(h, 15, 19, MOUTH);
            px(h, 16, 18, MOUTH);
        }
        return h;
    }

    static final String LOAF = "M24 11H34V12H36V13H37V14H38V23H37V24H36V25H21V24H20V23H19V14H20V13H21V12H24Z";
    static final String LOAF_IN = "M24 10H34V11H36V12H37V13H38V23H37V24H36V25H21V24H20V23H19V13H20V12H21V11H24Z";
    static final String LOAF_TAIL = "M32 2H37V3H38V4H37V5H36V6H35V7H34V8H35V9H34V10H33V11H30V10H31V9H32V8H31V6H33V5H32V4H31V3H32Z";

    /** Asleep: the sitting head on its paws, body behind, bolt tail up; 2 breaths, 39x27. */
    static PoseArt.Grid sleep(int frame) {
        Canvas c = new Canvas(39, 27);              // a row of headroom for the ear tips
        fill(c, LOAF_TAIL, FUR, 0, frame == 0 ? 1 : 0, false);
        paint(c, 0, 9, 39, 3, FUR, ACC2);
        fill(c, frame == 0 ? LOAF : LOAF_IN, FUR, 0, 1, false);
        int top = frame == 0 ? 12 : 11;
        mark(c, 28, top, 2, 2);                      // back stripes
        mark(c, 32, top + 1, 2, 2);
        paint(c, 19, 22, 20, 4, FUR, FUR_FAR);       // shaded underside
        Canvas front = new Canvas(39, 27);
        paste(front, closedHead(true, false), 0, 1, false);
        creasePaste(c, front);
        rect(c, 8, 24, 4, 2, FUR_FAR);               // paws under the chin
        rect(c, 18, 24, 4, 2, FUR_FAR);
        return render(c, PIKA);
    }

    // =============================================================== ROLL

    static final String R_EAR = "M7 18H10V20H9V21H8V22H7V23H6V24H2V23H3V22H4V21H5V20H6V19H7Z";

    /**
     * Rolled onto its back: the sitting Pikachu upside down, feet paddling,
     * long ears splayed along the ground (straight down they read as
     * standing on its ears); 2 frames, 36x25.
     */
    static PoseArt.Grid roll(int frame) {
        Canvas s = new Canvas(36, 30);
        Canvas t = new Canvas(15, 24);
        fill(t, SIT_TAIL, FUR, 0, 1, false);
        paint(t, 0, 19, 15, 5, FUR, ACC2);
        paint(t, 9, 4, 6, 4, FUR, FUR_FAR);
        paste(s, t, 21, 6, false);
        Canvas b = new Canvas(30, 30);
        fill(b, SIT_BODY, FUR);
        paint(b, 0, 23, 30, 1, FUR, FUR_FAR);
        paint(b, 22, 23, 8, 7, FUR, FUR_FAR);
        paste(b, closedHead(false, true), 0, 0, false);
        fillOverPair(b, SIT_FOOT, FUR);
        fillPair(b, SIT_ARM, FUR_FAR);
        rect(b, 11, 26, 2, 1, FUR);
        rect(b, 17, 26, 2, 1, FUR);
        paste(s, b, 0, 0, false);
        Canvas c = new Canvas(36, 25);
        fill(c, R_EAR, FUR, 0, 0, false);            // ears, behind the head
        fill(c, R_EAR, FUR, 7, 0, true);             // mirrored about the head's centre
        paint(c, 0, 22, 36, 2, FUR, ACC1);
        Canvas f = new Canvas(36, 30);
        paste(f, s, 0, 0, true);
        paste(c, f, 0, 1, false);
        shiftRows(c, 1, 3, frame == 0 ? 0 : 1);
        return render(c, PIKA);
    }
}
