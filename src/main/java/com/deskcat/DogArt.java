package com.deskcat;

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

    static final String W_HEAD = "M21 3H27V4H28V6H29V7H31V8H32V10H31V11H29V12H22V11H21V10H20V4H21Z";
    static final String W_MUZZLE = "M25 8H31V9H32V10H31V11H29V12H23V10H24V9H25Z";
    static final String W_EAR = "M25 0H26V1H27V3H23V2H24V1H25Z";
    static final String W_FAR_EAR = "M21 1H22V2H23V3H21Z";
    static final String W_TAIL = "M7 4H11V5H12V9H11V10H7V9H6V8H5V6H6V5H7Z";

    /** Side-view trot facing right, tail curled over the back; 4 frames, 32x24. */
    static PoseArt.Grid walk(int frame) {
        Canvas c = new Canvas(32, 24);
        int[] adx = {1, 0, -1, 0}, alift = {0, 0, 0, 1};
        int[] bdx = {-1, 0, 1, 0}, blift = {0, 1, 0, 0};
        leg(c, 21 + bdx[frame], blift[frame], false);
        leg(c, 10 + adx[frame], alift[frame], false);
        fill(c, W_FAR_EAR, FUR_FAR);
        fill(c, CatArt.W_BODY, FUR);
        fill(c, W_TAIL, FUR);
        rect(c, 7, 6, 3, 2, CREAM);          // cream underside of the curl
        px(c, 8, 6, FUR);
        shiftRows(c, 0, 9, new int[] {0, 1, 0, -1}[frame]);
        fill(c, W_HEAD, FUR);
        fill(c, W_EAR, FUR);
        px(c, 25, 1, INNER_EAR);
        px(c, 24, 2, INNER_EAR);
        px(c, 25, 2, INNER_EAR);
        fill(c, CatArt.W_CHEST, CREAM);
        fill(c, W_MUZZLE, CREAM);
        leg(c, 19 + adx[frame], alift[frame], true);
        leg(c, 8 + bdx[frame], blift[frame], true);
        rect(c, 24, 5, 2, 2, PUPIL);         // eye
        px(c, 24, 5, GLINT);
        px(c, 25, 4, CREAM);                 // eyebrow dot
        rect(c, 30, 7, 2, 1, NOSE);
        px(c, 31, 8, NOSE);
        px(c, 29, 10, MOUTH);
        return render(c, SHIBA);
    }

    private static void leg(Canvas c, int x, int lift, boolean near) {
        int w = near ? 3 : 2;
        rect(c, x, 17, w, 4 - lift, near ? FUR : FUR_FAR);
        rect(c, x, 21 - lift, w, 2, near ? CREAM : CREAM_FAR);
    }

    // ============================================================== SLEEP

    /** Curled up, nose tucked under the tail; 2 breaths, 30x18. */
    static PoseArt.Grid sleep(int frame) {
        int o = 1;                           // room for the snout on the left
        Canvas c = new Canvas(31, 18);
        fill(c, frame == 0 ? CatArt.L_BODY : CatArt.L_BODY_IN, FUR, o, 0, false);
        fillOver(c, CatArt.L_TAIL, FUR, o, 0, false);
        paint(c, 12 + o, 16, 13, 1, FUR, CREAM);   // cream underside of the tail
        fillOver(c, CatArt.L_HEAD, FUR, o, 0, false);
        fill(c, CatArt.L_EAR_L, FUR, o, 0, false);
        fill(c, CatArt.L_EAR_R, FUR, o, 0, false);
        px(c, 3 + o, 6, INNER_EAR);
        px(c, 4 + o, 6, INNER_EAR);
        px(c, 8 + o, 6, INNER_EAR);
        px(c, 9 + o, 6, INNER_EAR);
        rect(c, 0, 12, 2, 3, FUR);           // snout poking out past the cheek
        rect(c, 0, 13, 7, 2, CREAM);         // muzzle and cheeks
        rect(c, 2, 15, 7, 1, CREAM);
        rect(c, 8, 13, 3, 2, CREAM);
        rect(c, 0, 12, 2, 1, NOSE);
        px(c, 3 + o, 9, CREAM);              // eyebrow dots
        px(c, 8 + o, 9, CREAM);
        for (int ex : new int[] {3, 8}) {
            px(c, ex, 11, LINE);
            px(c, ex + 1, 11, LINE);
            px(c, ex + 2, 11, LINE);
        }
        return render(c, SHIBA);
    }

    // =============================================================== ROLL

    /** Belly-up with paws in the air; 2 wiggle frames, 32x20. */
    static PoseArt.Grid roll(int frame) {
        Canvas c = new Canvas(32, 20);
        int[][] legs = {{13, 7, 0}, {16, 7, 1}, {21, 5, 1}, {24, 5, 0}};
        for (int[] l : legs) {
            int y = l[1] - ((l[2] + frame) % 2);
            boolean near = l[0] == 13 || l[0] == 24;
            rect(c, l[0], y, 2, 9 - y, near ? FUR : FUR_FAR);
            rect(c, l[0], y, 2, 2, near ? CREAM : CREAM_FAR);
        }
        rect(c, 27, 10, 3, 3, FUR);          // curled tail on the ground
        rect(c, 28, 11, 1, 1, CREAM);
        fill(c, CatArt.R_BODY, FUR);
        fill(c, CatArt.R_BELLY, CREAM);
        fill(c, CatArt.R_EAR_L, FUR);
        fill(c, CatArt.R_EAR_R, FUR);
        fillOver(c, CatArt.R_HEAD, FUR);
        px(c, 3, 17, INNER_EAR);
        px(c, 9, 17, INNER_EAR);
        rect(c, 2, 7, 8, 3, CREAM);          // muzzle and cheeks, upside down
        rect(c, 5, 7, 2, 1, NOSE);
        px(c, 5, 9, MOUTH);
        px(c, 6, 9, MOUTH);
        px(c, 3, 14, CREAM);                 // eyebrow dots
        px(c, 8, 14, CREAM);
        for (int ex : new int[] {2, 7}) {    // happy squint
            px(c, ex, 12, LINE);
            px(c, ex + 1, 13, LINE);
            px(c, ex + 2, 12, LINE);
        }
        return render(c, SHIBA);
    }
}
