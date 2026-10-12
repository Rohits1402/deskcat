package com.deskcat;

import static com.deskcat.PathArt.*;

/**
 * The goldfish in the {@link PathArt} format, in profile facing right (head
 * on it read as a chick): a plump body with a lighter belly, a curved gill
 * line, a few scale pixels, translucent-looking fins and a forked fan tail.
 */
final class FishArt {

    private FishArt() {
    }

    static final Palette GOLDFISH = new Palette()
            .set(FUR, "FF8C3A").set(FUR_FAR, "E8701F")
            .set(CREAM, "FFC48A")                                // belly
            .set(PATTERN, "E8701F").set(SEP, "D45F1C")         // scales, gill
            .set(ACC1, "FFB060").set(ACC2, "F48A3E")            // fins, fin rays
            .set(MOUTH, "2A2230").set(LINE, "2A2230");

    static final String BODY = "M12 10H22V11H25V12H27V13H28V14H29V16H30V19H29V21H28V22H26V23H22V24H11V23H8V22H6V21H5V20H4V15H5V14H6V13H7V12H9V11H12Z";
    static final String DORSAL = "M13 5H15V6H17V7H19V8H21V9H22V10H14V9H13V8H12V6H13Z";
    static final String BELLY_FIN = "M12 24H18V25H17V26H13V25H12Z";
    static final String PEC_FIN = "M15 17H19V18H20V19H19V20H15V19H14V18H15Z";

    /**
     * The body, 32x30. One runtime bead eye (eyeStyle 1, oneEye, 3x3: cols
     * 23-25, rows 14-16, so FBO x 25, y 13).
     */
    static PoseArt.Grid body() {
        Canvas c = new Canvas(32, 30);
        fill(c, DORSAL, ACC1);
        paint(c, 15, 6, 1, 4, ACC1, ACC2);   // fin rays
        paint(c, 18, 7, 1, 3, ACC1, ACC2);
        fill(c, BELLY_FIN, ACC1);
        fill(c, BODY, FUR);
        paint(c, 0, 20, 32, 4, FUR, CREAM);  // lighter belly
        paint(c, 6, 19, 22, 1, FUR, CREAM);
        // a few deliberate scales
        int[][] scales = {{9, 13}, {13, 13}, {17, 13}, {7, 15}, {11, 15}, {15, 15},
                {9, 17}, {13, 17}, {7, 19}, {11, 19}};
        for (int[] s : scales) {
            paint(c, s[0], s[1], 2, 1, FUR, PATTERN);
        }
        int[][] gill = {{21, 12}, {20, 13}, {20, 14}, {19, 15}, {19, 16}, {19, 17},
                {19, 18}, {20, 19}, {20, 20}, {21, 21}};
        for (int[] g : gill) {
            px(c, g[0], g[1], SEP);
        }
        fillOver(c, PEC_FIN, ACC1);
        paint(c, 16, 18, 3, 1, ACC1, ACC2);
        rect(c, 28, 18, 2, 1, MOUTH);
        return render(c, GOLDFISH);
    }

    static final String TAIL = "M1 2H3V3H4V4H5V5H6V6H7V7H8V8H9V9H10V10H11V11H13V16H11V17H10V18H9V19H8V20H7V21H6V22H5V23H4V24H3V25H1V20H2V18H3V17H4V16H5V15H6V14H7V13H6V12H5V11H4V10H3V9H2V7H1Z";

    /** Forked fan tail; the lobes flutter in opposition. 3 frames, 14x26. */
    static PoseArt.Grid tail(int frame) {
        Canvas c = new Canvas(14, 26);
        fill(c, TAIL, ACC1);
        int[][] rays = {{2, 4}, {3, 6}, {4, 7}, {5, 8}, {6, 10}, {7, 11},
                {2, 22}, {3, 20}, {4, 19}, {5, 18}, {6, 16}, {7, 15}, {8, 13}, {9, 13}, {10, 13}};
        for (int[] r : rays) {
            paint(c, r[0], r[1], 1, 1, ACC1, ACC2);
        }
        int d = frame == 1 ? 1 : (frame == 2 ? -1 : 0);
        shiftRows(c, 0, 9, d);
        shiftRows(c, 18, 25, -d);
        return render(c, GOLDFISH);
    }
}
