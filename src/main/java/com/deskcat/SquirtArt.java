package com.deskcat;

import static com.deskcat.PathArt.*;

/**
 * Squirtle in the {@link PathArt} format: a big round head, a brown shell
 * whose cream rim frames a yellow plated belly, stubby limbs and a curled
 * tail. The shell pose keeps one arched opening low on the front (arm and
 * leg holes made it read as a frowning face). Personal-use fan art (see
 * CLAUDE.md).
 */
final class SquirtArt {

    private SquirtArt() {
    }

    static final Palette SQUIRT = new Palette()
            .set(FUR, "8CCFE8").set(FUR_FAR, "6DB2D0")
            .set(ACC1, "A0703E").set(ACC1_FAR, "7A522B")      // shell, seams
            .set(ACC2, "F4E6C0")                                 // shell rim
            .set(ACC3, "F2D27E").set(ACC4, "C9A24E")            // belly, plates
            .set(IRIS, "5B3A26").set(RING, "2A2230")
            .set(MOUTH, "2A2230").set(LINE, "2A2230").set(SEP, "6DB2D0");

    // ============================================================ SITTING

    static final String SIT_HEAD = "M10 3H20V4H23V5H25V6H26V7H27V9H28V16H27V17H26V18H25V19H23V20H20V21H10V20H7V19H5V18H4V17H3V16H2V9H3V7H4V6H5V5H7V4H10Z";
    static final String SIT_SHELL = "M9 17H21V18H24V19H25V20H26V27H25V28H24V29H22V30H8V29H6V28H5V27H4V20H5V19H6V18H9Z";
    static final String SIT_RIM = "M9 18H21V19H23V20H24V21H25V28H24V29H23V30H7V29H6V28H5V21H6V20H7V19H9Z";
    static final String SIT_BELLY = "M10 19H20V20H22V21H23V28H22V29H20V30H10V29H8V28H7V21H8V20H10Z";
    static final String SIT_ARM = "M3 20H7V22H6V23H2V21H3Z";
    static final String SIT_FOOT = "M5 27H11V30H4V28H5Z";

    /**
     * Sitting, 30x30. Eyes are runtime outlined irises (eyeStyle 2, 3x4:
     * cols 8-10 / 19-21, rows 10-13, so FBO x 10 / 21, y 16).
     */
    static PoseArt.Grid sitBody() {
        Canvas c = new Canvas(30, 30);
        fill(c, SIT_SHELL, ACC1);
        fill(c, SIT_RIM, ACC2);
        fill(c, SIT_BELLY, ACC3);
        paint(c, 0, 23, 30, 1, ACC3, ACC4);  // plastron plates
        paint(c, 0, 26, 30, 1, ACC3, ACC4);
        paint(c, 14, 20, 2, 10, ACC3, ACC4);
        fillOverPair(c, SIT_ARM, FUR);
        fillOverPair(c, SIT_FOOT, FUR);
        fillOver(c, SIT_HEAD, FUR);
        // a wide, happy grin
        px(c, 11, 15, MOUTH);
        px(c, 18, 15, MOUTH);
        rect(c, 12, 16, 6, 1, MOUTH);
        return render(c, SQUIRT);
    }

    static final String SIT_TAIL = "M5 6H9V7H11V8H12V13H11V14H10V15H9V16H8V17H7V18H6V19H5V20H4V22H0V20H1V19H2V18H3V17H4V16H5V15H6V14H5V13H3V12H2V8H3V7H5Z";

    /** The curled tail behind the right hip; 3 wag frames, 12x23. */
    static PoseArt.Grid sitTail(int frame) {
        Canvas c = new Canvas(12, 23);
        fill(c, SIT_TAIL, FUR);
        // the curl's inner swirl
        rect(c, 5, 8, 4, 1, FUR_FAR);
        px(c, 8, 9, FUR_FAR);
        rect(c, 5, 10, 4, 1, FUR_FAR);
        px(c, 4, 9, FUR_FAR);
        shiftRows(c, 0, 12, frame == 1 ? 1 : (frame == 2 ? -1 : 0));
        return render(c, SQUIRT);
    }

    // =============================================================== WALK

    static final String W_SHELL = "M11 6H18V7H21V8H22V9H23V15H22V16H6V15H5V9H6V8H8V7H11Z";
    static final String W_HEAD = "M22 4H28V5H30V6H31V12H30V13H28V14H22V13H20V12H19V6H20V5H22Z";
    static final String W_TAIL = "M2 9H6V10H7V13H8V14H9V15H4V14H3V13H2V12H1V10H2Z";

    /** Side view facing right, shell on its back; 4 frames of a waddle, 32x24. */
    static PoseArt.Grid walk(int frame) {
        Canvas c = new Canvas(32, 24);
        int[] adx = {1, 0, -1, 0}, alift = {0, 0, 0, 1};
        int[] bdx = {-1, 0, 1, 0}, blift = {0, 1, 0, 0};
        leg(c, 18 + bdx[frame], blift[frame], false);
        leg(c, 9 + adx[frame], alift[frame], false);
        fill(c, W_TAIL, FUR);
        px(c, 3, 11, FUR_FAR);
        px(c, 4, 12, FUR_FAR);
        rect(c, 7, 16, 15, 2, ACC3);         // belly under the shell
        fill(c, W_SHELL, ACC1);
        rect(c, 5, 15, 18, 1, ACC2);         // cream rim
        for (int x = 9; x <= 19; x += 5) {   // scute seams
            paint(c, x, 7, 1, 4, ACC1, ACC1_FAR);
        }
        paint(c, 6, 11, 17, 1, ACC1, ACC1_FAR);
        for (int x = 7; x <= 21; x += 5) {
            paint(c, x, 12, 1, 3, ACC1, ACC1_FAR);
        }
        fillOver(c, W_HEAD, FUR);
        leg(c, 16 + adx[frame], alift[frame], true);
        leg(c, 7 + bdx[frame], blift[frame], true);
        rect(c, 24, 6, 3, 4, RING);          // eye
        rect(c, 25, 7, 2, 2, IRIS);
        px(c, 25, 7, GLINT);
        px(c, 26, 11, MOUTH);                // smile
        rect(c, 27, 12, 3, 1, MOUTH);
        px(c, 30, 11, MOUTH);
        return render(c, SQUIRT);
    }

    private static void leg(Canvas c, int x, int lift, boolean near) {
        rect(c, x, 17, 3, 6 - lift, near ? FUR : FUR_FAR);
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
        // a plate grid, like the walking shell; curved seams above the
        // opening read as frowning brows
        paint(c, 0, 11 - lift, 30, 1, ACC1, ACC1_FAR);
        for (int x = 10; x <= 20; x += 5) {
            paint(c, x, 6 - lift, 1, 5, ACC1, ACC1_FAR);
        }
        for (int x = 7; x <= 22; x += 15) {
            paint(c, x, 12 - lift, 1, 5, ACC1, ACC1_FAR);
        }
        fill(c, SH_HOLE, MOUTH);
        paint(c, 12, 13, 6, 1, ACC1, ACC2);  // cream lip over the opening
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

    static final String R_SHELL = "M7 13H27V14H28V18H27V19H25V20H22V21H12V20H9V19H7V18H6V14H7Z";
    static final String R_BELLY = "M11 9H23V10H25V11H26V13H8V11H9V10H11Z";
    static final String R_HEAD = "M3 9H9V10H10V11H11V15H10V16H9V17H3V16H2V15H1V11H2V10H3Z";

    /** On its back on the shell, legs pedalling; 2 frames, 32x22. */
    static PoseArt.Grid roll(int frame) {
        Canvas c = new Canvas(32, 22);
        int[][] legs = {{11, 5, 0}, {14, 5, 1}, {20, 5, 1}, {23, 5, 0}};
        for (int[] l : legs) {
            int y = l[1] - ((l[2] + frame) % 2);
            boolean near = l[0] == 11 || l[0] == 23;
            rect(c, l[0], y, 2, 10 - y, near ? FUR : FUR_FAR);
        }
        rect(c, 27, 10, 3, 3, FUR);          // tail curling up
        px(c, 28, 11, FUR_FAR);
        fill(c, R_BELLY, ACC3);
        for (int x = 13; x <= 22; x += 3) {
            paint(c, x, 10, 1, 3, ACC3, ACC4);
        }
        fill(c, R_SHELL, ACC1);
        paint(c, 0, 13, 32, 1, ACC1, ACC2);  // rim
        paint(c, 10, 17, 14, 1, ACC1, ACC1_FAR);
        paint(c, 15, 15, 1, 2, ACC1, ACC1_FAR);
        paint(c, 20, 15, 1, 2, ACC1, ACC1_FAR);
        fillOver(c, R_HEAD, FUR);
        px(c, 3, 11, LINE);                  // happy eye
        px(c, 4, 10, LINE);
        px(c, 5, 11, LINE);
        px(c, 7, 13, MOUTH);                 // grin
        rect(c, 8, 14, 2, 1, MOUTH);
        return render(c, SQUIRT);
    }
}
