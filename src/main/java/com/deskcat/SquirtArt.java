package com.deskcat;

import static com.deskcat.Gait.*;
import static com.deskcat.PathArt.*;

/**
 * Squirtle in the {@link PathArt} format, drawn to its real proportions: a
 * round head about half its height, big outlined eyes with reddish-brown
 * irises (sockets baked in; eyeStyle 5 moves the pupils), a wide open
 * smile, a brown shell whose cream rim frames a plated yellow belly, stubby
 * limbs and a small curled tail. Shading, a crown highlight and a navy
 * outline give it volume. The shell pose keeps one arched opening low on
 * the front (arm and leg holes, or curved seams over it, read as a
 * frowning face). Personal-use fan art (see CLAUDE.md).
 */
final class SquirtArt {

    private SquirtArt() {
    }

    static final Palette SQUIRT = new Palette()
            .set(FUR, "8CCFE8").set(FUR_FAR, "6AAFCF")         // skin, shade
            .set(CREAM, "C4E9F7")                                // highlight
            .set(ACC1, "A86F3A").set(ACC1_FAR, "6E4622")        // shell, seams
            .set(ACC2_FAR, "8A5A2E").set(CREAM_FAR, "C88E58")   // shell shade, shine
            .set(ACC2, "F4E6C0")                                 // shell rim
            .set(ACC3, "F4D27A").set(ACC4, "CFA44E")            // belly, plates
            .set(IRIS, "7A3528").set(RING, "22313F")
            .set(MOUTH, "22313F").set(INNER_EAR, "E8707A")      // mouth, tongue
            .set(LINE, "22313F").set(SEP, "6AAFCF")
            .outline("22313F");

    // ============================================================ SITTING

    static final String SIT_HEAD = "M11 5H19V6H21V7H23V8H24V10H25V16H24V17H23V18H21V19H19V20H11V19H9V18H7V17H6V16H5V10H6V8H7V7H9V6H11Z";
    static final String SIT_EYE_RING = "M7 9H11V10H12V14H11V15H7V14H6V10H7Z";
    static final String SIT_EYE_IRIS = "M8 10H10V11H11V13H10V14H8V13H7V11H8Z";
    static final String SIT_SHELL = "M8 16H22V17H24V18H25V19H26V29H25V30H5V29H4V19H5V18H6V17H8Z";
    static final String SIT_RIM = "M8 17H22V18H23V19H24V29H23V30H7V29H6V19H7V18H8Z";
    static final String SIT_BELLY = "M10 18H20V19H21V20H22V28H21V29H20V30H10V29H9V28H8V20H9V19H10Z";
    static final String SIT_ARM = "M4 20H8V22H7V23H6V24H2V22H3V21H4Z";
    static final String SIT_FOOT = "M5 27H11V30H4V28H5Z";

    /**
     * Sitting, 30x30. Eye sockets are baked in; CatApp draws only the
     * pupils (eyeStyle 5, iris 4x4: cols 7-10 / 19-22, rows 10-13, so FBO
     * x 9 / 21, y 16).
     */
    static PoseArt.Grid sitBody() {
        Canvas c = new Canvas(30, 30);
        fill(c, SIT_SHELL, ACC1);
        paint(c, 23, 17, 7, 13, ACC1, ACC2_FAR);     // shell in shade on the right
        fill(c, SIT_RIM, ACC2);
        fill(c, SIT_BELLY, ACC3);
        paint(c, 9, 22, 12, 1, ACC3, ACC4);          // plastron plates
        paint(c, 8, 25, 14, 1, ACC3, ACC4);
        paint(c, 14, 19, 2, 3, ACC3, ACC4);
        fillOverPair(c, SIT_ARM, FUR);
        fillOverPair(c, SIT_FOOT, FUR);
        px(c, 6, 29, SEP);                           // toes
        px(c, 8, 29, SEP);
        px(c, 21, 29, SEP);
        px(c, 23, 29, SEP);
        fillOver(c, SIT_HEAD, FUR);
        paint(c, 21, 10, 9, 7, FUR, FUR_FAR);        // shaded right side of the head
        paint(c, 17, 17, 9, 3, FUR, FUR_FAR);
        paint(c, 10, 6, 4, 1, FUR, CREAM);           // highlight on the crown
        paint(c, 8, 7, 3, 1, FUR, CREAM);
        fillPair(c, SIT_EYE_RING, RING);
        fillPair(c, SIT_EYE_IRIS, IRIS);
        smile(c, 11, 15);
        return render(c, SQUIRT);
    }

    /** A wide open smile with a tongue, 8 wide; (x, y) is its top-left. */
    private static void smile(Canvas c, int x, int y) {
        px(c, x, y, MOUTH);
        px(c, x + 7, y, MOUTH);
        rect(c, x + 1, y + 1, 6, 1, MOUTH);
        px(c, x + 1, y + 2, MOUTH);
        rect(c, x + 2, y + 2, 4, 1, INNER_EAR);
        px(c, x + 6, y + 2, MOUTH);
        rect(c, x + 2, y + 3, 4, 1, MOUTH);
    }

    static final String SIT_TAIL = "M4 7H8V8H9V9H10V12H9V13H7V14H6V16H5V18H4V22H0V18H1V16H2V14H3V12H2V9H3V8H4Z";

    /** The small curled tail behind the right hip; 3 wag frames, 12x23. */
    static PoseArt.Grid sitTail(int frame) {
        Canvas c = new Canvas(12, 23);
        fill(c, SIT_TAIL, FUR);
        swirl(c, 4, 9);
        paint(c, 8, 9, 2, 3, FUR, FUR_FAR);
        shiftRows(c, 0, 12, frame == 1 ? 1 : (frame == 2 ? -1 : 0));
        return render(c, SQUIRT);
    }

    /** The tail's spiral, a small ring of shade; (x, y) is its top-left. */
    private static void swirl(Canvas c, int x, int y) {
        int[][] s = {{1, 0}, {2, 0}, {3, 1}, {2, 2}, {1, 2}, {0, 1}};
        for (int[] p : s) {
            paint(c, x + p[0], y + p[1], 1, 1, FUR, FUR_FAR);
        }
    }

    // =============================================================== WALK

    static final String W_HEAD = "M16 1H24V2H26V3H27V4H28V5H29V12H28V13H27V14H25V15H16V14H14V13H13V12H12V4H13V3H14V2H16Z";
    static final String W_RING = "M23 5H26V6H27V10H26V11H23V10H22V6H23Z";
    static final String W_IRIS = "M24 6H26V10H24V9H23V7H24Z";
    static final String W_BODY = "M14 13H23V14H24V21H23V22H22V23H21V24H14V23H13V22H12V21H11V16H12V15H13V14H14Z";
    static final String W_SHELL = "M9 12H15V13H16V14H17V23H16V24H14V25H9V24H7V23H6V22H5V15H6V14H7V13H9Z";
    static final String W_ARM = "M17 15H20V16H21V18H22V19H18V17H17Z";
    static final String W_TAIL = "M2 14H5V15H6V17H7V19H8V21H5V20H3V19H2V18H1V15H2Z";

    /**
     * Walking upright facing right, the way Squirtle does: head as big as
     * the sitting head with the same socketed eye, shell on its back with a
     * cream rim, plastron in front, arm swinging, legs stepping heel to toe,
     * tail curling up behind; 8 frames, 30x30.
     */
    static PoseArt.Grid walk(int f) {
        Canvas c = new Canvas(30, 30);
        int b = BOB[f], hb = headBob(f);
        int gy = 28;
        fill(c, W_TAIL, FUR, 0, 1 + b, false);
        int[][] swirl = {{2, 16}, {3, 16}, {4, 17}, {3, 18}, {2, 17}};
        for (int[] s : swirl) {
            paint(c, s[0], s[1] + b, 1, 1, FUR, FUR_FAR);
        }
        shiftRows(c, 0, 18 + b, SWAY[f] > 0 ? 1 : (SWAY[f] < 0 ? -1 : 0));
        draw(c, footLeg(c, 13, 23 + b, gy, (f + 4) % 8, 4, 5), false, FUR_FAR);
        draw(c, footLeg(c, 17, 23 + b, gy, f, 4, 5), true, FUR);
        fill(c, W_BODY, ACC3, 0, b, false);
        paint(c, 17, 16 + b, 7, 1, ACC3, ACC4);      // plastron plates
        paint(c, 17, 19 + b, 7, 1, ACC3, ACC4);
        fill(c, W_SHELL, ACC1, 0, b, false);
        paint(c, 5, 21 + b, 12, 4, ACC1, ACC2_FAR);  // underside in shade
        paint(c, 5, 17 + b, 10, 1, ACC1, ACC1_FAR);  // scute seams
        paint(c, 10, 13 + b, 1, 4, ACC1, ACC1_FAR);
        paint(c, 8, 18 + b, 1, 3, ACC1, ACC1_FAR);
        paint(c, 12, 18 + b, 1, 3, ACC1, ACC1_FAR);
        paint(c, 8, 13 + b, 2, 1, ACC1, CREAM_FAR);  // shine
        paint(c, 15, 13 + b, 2, 11, ACC1, ACC2);     // cream rim along the front edge
        paint(c, 15, 13 + b, 2, 11, ACC2_FAR, ACC2);
        fillOver(c, W_HEAD, FUR, 0, hb, false);
        paint(c, 12, 12 + hb, 17, 3, FUR, FUR_FAR);  // under the jaw
        paint(c, 15, 2 + hb, 5, 1, FUR, CREAM);      // crown highlight
        paint(c, 14, 3 + hb, 2, 1, FUR, CREAM);
        fill(c, W_RING, RING, 0, hb, false);
        fill(c, W_IRIS, IRIS, 0, hb, false);
        rect(c, 24, 7 + hb, 2, 2, PUPIL);
        px(c, 24, 7 + hb, GLINT);
        px(c, 22, 11 + hb, MOUTH);                   // open smile with a tongue
        rect(c, 23, 12 + hb, 4, 1, MOUTH);
        rect(c, 24, 11 + hb, 3, 1, INNER_EAR);
        px(c, 27, 11 + hb, MOUTH);
        px(c, 28, 10 + hb, MOUTH);
        outlined(c, W_ARM, FUR, ARM_DX[f], 1 + b);
        return render(c, SQUIRT);
    }

    // ============================================================== SHELL

    static final String SH_DOME = "M11 4H19V5H22V6H24V7H25V8H26V12H27V17H26V18H4V17H3V12H4V8H5V7H6V6H8V5H11Z";
    static final String SH_RIM = "M3 17H27V18H28V20H27V21H3V20H2V18H3Z";
    static final String SH_HOLE = "M12 14H18V15H19V17H11V15H12Z";

    /**
     * Tucked into its shell, 30x22. The opening is where CatApp draws the
     * peeking eyes (shellEyeX 13, shellEyeY 6 = row 15). Asleep, closed
     * lids show in the dark; lift raises the dome a row for breathing.
     */
    static PoseArt.Grid shell(boolean asleep, int lift) {
        Canvas c = new Canvas(30, 22);
        fill(c, SH_RIM, ACC2);
        fill(c, SH_DOME, ACC1, 0, -lift, false);
        paint(c, 21, 4 - lift, 9, 14, ACC1, ACC2_FAR);   // right side in shade
        paint(c, 3, 15 - lift, 27, 3, ACC1, ACC2_FAR);   // and the lower edge
        // a plate grid; curved seams above the opening read as frowning brows
        int[] z = {ACC1, ACC2_FAR};
        for (int k : z) {
            paint(c, 0, 11 - lift, 30, 1, k, ACC1_FAR);
            for (int x = 10; x <= 20; x += 5) {
                paint(c, x, 6 - lift, 1, 5, k, ACC1_FAR);
            }
            for (int x = 7; x <= 22; x += 15) {
                paint(c, x, 12 - lift, 1, 5, k, ACC1_FAR);
            }
        }
        paint(c, 8, 7 - lift, 2, 1, ACC1, CREAM_FAR);   // shine
        paint(c, 7, 8 - lift, 1, 2, ACC1, CREAM_FAR);
        fill(c, SH_HOLE, MOUTH);
        paint(c, 12, 13, 6, 1, ACC1, ACC2);              // cream lip over the opening
        paint(c, 12, 13, 6, 1, ACC2_FAR, ACC2);
        if (asleep) {
            rect(c, 13, 15, 2, 1, FUR);
            rect(c, 16, 15, 2, 1, FUR);
        }
        return render(c, SQUIRT);
    }

    /** Asleep in the shell; the dome rises and falls. 2 frames. */
    static PoseArt.Grid sleep(int frame) {
        return shell(true, frame);
    }

    // =============================================================== ROLL

    /** Rolled onto its back: the sitting Squirtle upside down, happy, feet paddling; 2 frames, 36x27. */
    static PoseArt.Grid roll(int frame) {
        Canvas s = new Canvas(36, 30);
        Canvas t = new Canvas(12, 23);
        fill(t, SIT_TAIL, FUR);
        paste(s, t, 23, 7, false);
        Canvas b = new Canvas(30, 30);
        fill(b, SIT_SHELL, ACC1);
        paint(b, 23, 17, 7, 13, ACC1, ACC2_FAR);
        fill(b, SIT_RIM, ACC2);
        fill(b, SIT_BELLY, ACC3);
        paint(b, 9, 22, 12, 1, ACC3, ACC4);
        paint(b, 8, 25, 14, 1, ACC3, ACC4);
        paint(b, 14, 19, 2, 3, ACC3, ACC4);
        fillOverPair(b, SIT_ARM, FUR);
        fillOverPair(b, SIT_FOOT, FUR);
        fillOver(b, SIT_HEAD, FUR);
        paint(b, 21, 10, 9, 7, FUR, FUR_FAR);
        paint(b, 17, 17, 9, 3, FUR, FUR_FAR);
        paint(b, 10, 6, 4, 1, FUR, CREAM);
        paint(b, 8, 7, 3, 1, FUR, CREAM);
        for (int ex : new int[] {7, 19}) {
            rect(b, ex, 11, 4, 1, LINE);             // ^ ^
            px(b, ex - 1, 12, LINE);
            px(b, ex + 4, 12, LINE);
        }
        smile(b, 11, 15);
        paste(s, b, 0, 0, false);
        Canvas f = new Canvas(36, 30);
        paste(f, s, 0, 0, true);
        Canvas c = new Canvas(36, 27);
        paste(c, f, 0, 1, false);
        shiftRows(c, 1, 3, frame == 0 ? 0 : 1);
        return render(c, SQUIRT);
    }
}
