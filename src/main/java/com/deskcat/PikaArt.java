package com.deskcat;

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

    static final String W_EAR = "M17 1H19V2H20V4H21V5H22V6H23V7H24V8H25V9H22V8H21V7H20V6H19V5H18V3H17Z";
    static final String W_FAR_EAR = "M25 1H26V2H27V5H28V7H24V2H25Z";
    static final String W_HEAD = "M21 7H27V8H29V9H30V10H31V14H30V16H29V17H27V18H21V17H19V16H18V14H17V10H18V9H19V8H21Z";
    static final String W_BODY = "M10 15H20V16H22V17H23V18H24V21H23V22H22V23H8V22H7V21H6V18H7V17H8V16H10Z";
    static final String W_TAIL = "M0 3H6V4H7V6H6V8H7V9H8V10H7V12H8V13H9V14H10V15H11V16H12V17H8V16H7V15H6V14H5V13H4V12H3V11H2V10H4V9H3V7H2V6H1V5H0Z";

    /** On all fours facing right, ears swept back; 4 frames of a scamper, 32x28. */
    static PoseArt.Grid walk(int frame) {
        Canvas c = new Canvas(32, 28);
        int[] adx = {1, 0, -1, 0}, alift = {0, 0, 0, 1};
        int[] bdx = {-1, 0, 1, 0}, blift = {0, 1, 0, 0};
        fill(c, W_TAIL, FUR);
        paint(c, 0, 14, 14, 3, FUR, ACC2);
        paint(c, 4, 3, 3, 3, FUR, FUR_FAR);
        shiftRows(c, 0, 9, new int[] {0, 1, 0, -1}[frame]);
        leg(c, 21 + bdx[frame], blift[frame], false);
        leg(c, 10 + adx[frame], alift[frame], false);
        fill(c, W_FAR_EAR, FUR_FAR);
        paint(c, 0, 1, 32, 3, FUR_FAR, ACC1_FAR);
        fill(c, W_BODY, FUR);
        mark(c, 12, 15, 2, 2);                       // brown back stripes
        mark(c, 16, 15, 2, 2);
        paint(c, 0, 21, 32, 2, FUR, FUR_FAR);        // shaded underside
        fill(c, W_EAR, FUR);
        paint(c, 17, 1, 5, 3, FUR, ACC1);
        fillOver(c, W_HEAD, FUR);
        paint(c, 21, 8, 4, 1, FUR, ACC4);
        paint(c, 19, 16, 10, 2, FUR, FUR_FAR);       // under the jaw
        leg(c, 19 + adx[frame], alift[frame], true);
        leg(c, 8 + bdx[frame], blift[frame], true);
        rect(c, 26, 10, 2, 3, PUPIL);
        px(c, 26, 10, GLINT);
        rect(c, 23, 13, 3, 2, ACC3);                 // cheek, below and behind the eye
        px(c, 30, 11, NOSE);
        px(c, 29, 13, MOUTH);
        px(c, 28, 14, MOUTH);
        return render(c, PIKA);
    }

    private static void leg(Canvas c, int x, int lift, boolean near) {
        rect(c, x, 21, 3, 6 - lift, near ? FUR : FUR_FAR);
    }

    // ============================================================== SLEEP

    static final String L_BODY = "M13 7H23V8H25V9H26V10H27V11H28V16H27V17H26V18H25V19H12V18H11V17H10V16H9V10H10V9H11V8H13Z";
    static final String L_BODY_IN = "M13 6H23V7H25V8H26V9H27V10H28V16H27V17H26V18H25V19H12V18H11V17H10V16H9V9H10V8H11V7H13Z";
    static final String L_HEAD = "M5 6H11V7H13V8H14V9H15V14H14V16H13V17H12V18H4V17H3V16H2V14H1V9H2V8H3V7H5Z";
    static final String L_EAR = "M13 0H15V2H14V4H13V5H12V6H11V7H6V6H7V5H9V4H10V3H11V2H12V1H13Z";
    static final String L_FAR_EAR = "M18 2H20V4H18V5H16V6H14V7H11V6H12V5H14V4H16V3H18Z";
    static final String L_TAIL = "M26 1H31V3H30V5H29V6H28V8H27V9H24V8H25V5H27V4H26V3H25V2H26Z";

    /** On its tummy, chin on its paws, ears flopped back; 2 breaths, 32x20. */
    static PoseArt.Grid sleep(int frame) {
        Canvas c = new Canvas(32, 20);
        fill(c, L_TAIL, FUR);
        paint(c, 0, 7, 32, 2, FUR, ACC2);
        fill(c, frame == 0 ? L_BODY : L_BODY_IN, FUR);
        int top = frame == 0 ? 7 : 6;
        mark(c, 17, top, 2, 2);                      // back stripes
        mark(c, 21, top + 1, 2, 2);
        paint(c, 0, 16, 32, 3, FUR, FUR_FAR);        // shaded underside
        fillOver(c, L_FAR_EAR, FUR_FAR);
        paint(c, 17, 2, 4, 2, FUR_FAR, ACC1_FAR);
        fillOver(c, L_HEAD, FUR);
        fillOver(c, L_EAR, FUR);
        paint(c, 11, 0, 5, 3, FUR, ACC1);
        paint(c, 4, 7, 4, 1, FUR, ACC4);             // crown highlight
        rect(c, 3, 18, 3, 1, FUR);                   // paws under the chin
        rect(c, 10, 18, 3, 1, FUR);
        for (int ex : new int[] {3, 10}) {           // closed, contented eyes
            px(c, ex, 11, LINE);
            px(c, ex + 1, 12, LINE);
            px(c, ex + 2, 11, LINE);
        }
        rect(c, 1, 13, 2, 2, ACC3);                  // cheeks
        rect(c, 13, 13, 2, 2, ACC3);
        rect(c, 7, 13, 2, 1, NOSE);
        px(c, 6, 14, MOUTH);
        px(c, 9, 14, MOUTH);
        px(c, 7, 15, MOUTH);
        px(c, 8, 15, MOUTH);
        return render(c, PIKA);
    }

    // =============================================================== ROLL

    static final String R_BODY = "M16 12H26V13H28V14H29V18H28V19H26V20H16V19H14V18H13V14H14V13H16Z";
    static final String R_HEAD = "M7 8H12V9H14V10H15V11H16V16H15V17H14V18H12V19H7V18H5V17H4V16H3V11H4V10H5V9H7Z";
    static final String R_EAR = "M3 15H6V17H5V18H4V19H3V20H1V17H2V16H3Z";
    static final String R_FAR_EAR = "M1 11H4V12H5V14H2V13H1Z";
    static final String R_TAIL = "M29 4H34V6H33V7H32V9H31V11H30V13H29V15H26V11H27V9H28V5H29Z";

    /**
     * On its back, head in profile facing the sky (an upside-down face read
     * as a frown and the hanging ears as boots), ears along the ground,
     * paws paddling; 2 frames, 35x22.
     */
    static PoseArt.Grid roll(int frame) {
        Canvas c = new Canvas(35, 22);
        fill(c, R_FAR_EAR, FUR_FAR);
        paint(c, 0, 11, 3, 3, FUR_FAR, ACC1_FAR);
        int[][] legs = {{15, 9, 0, 2}, {18, 9, 1, 2}, {22, 7, 1, 3}, {25, 7, 0, 3}};
        for (int[] l : legs) {
            int y = l[1] - ((l[2] + frame) % 2);
            boolean near = l[0] == 15 || l[0] == 25;
            rect(c, l[0], y, l[3], 13 - y, near ? FUR : FUR_FAR);
        }
        fill(c, R_TAIL, FUR);
        paint(c, 0, 12, 35, 3, FUR, ACC2);
        fill(c, R_BODY, FUR);
        paint(c, 0, 18, 35, 2, FUR, FUR_FAR);        // its back, on the ground
        fill(c, R_EAR, FUR);
        paint(c, 0, 18, 6, 2, FUR, ACC1);
        fillOver(c, R_HEAD, FUR);
        paint(c, 7, 9, 3, 1, FUR, ACC4);             // highlight
        px(c, 10, 8, NOSE);                          // snout to the sky
        rect(c, 11, 9, 2, 1, MOUTH);                 // open, delighted
        px(c, 13, 10, MOUTH);
        px(c, 12, 10, INNER_EAR);
        px(c, 5, 12, LINE);                          // happy closed eye
        rect(c, 6, 11, 2, 1, LINE);
        px(c, 8, 12, LINE);
        rect(c, 9, 13, 3, 2, ACC3);                  // cheek
        return render(c, PIKA);
    }
}
