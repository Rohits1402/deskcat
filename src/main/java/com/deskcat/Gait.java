package com.deskcat;

import static com.deskcat.PathArt.*;

/**
 * Walk-cycle machinery shared by every skin's 8-frame walk, plus helpers
 * for composing poses out of the sitting drawing.
 *
 * Quadrupeds use a lateral-sequence walk: near hind, near fore, far hind,
 * far fore, a quarter cycle apart. Each leg is a jointed limb posed per
 * phase: 0 contact (reaching forward), 1 plant, 2 under, 3 push, 4 push-off
 * (heel up), 5 lift, 6 swing under, 7 swing forward. Bipeds step heel to
 * toe on short legs with big feet. In the stance phases a foot moves back
 * about 2 px a frame, so at CatApp's walk frame rate it stays planted
 * while the window moves. The body dips after each contact (BOB), the head
 * follows a frame later, and tails sway.
 */
final class Gait {

    private Gait() {
    }

    /** Paw x offset from the shoulder/hip, and paw lift (rows), per phase. */
    static final float[] PAW_X = {5, 3, 1, -1, -3, -3, 0, 4};
    static final int[] LIFT = {0, 0, 0, 0, 0, 3, 3, 1};
    /** Hind legs swing more compactly: the hock folds above the lifted paw. */
    static final float[] HIND_PAW_X = {4, 2, 0, -2, -4, -2, 1, 4};
    static final int[] HIND_LIFT = {0, 0, 0, 0, 0, 2, 3, 1};
    /** Biped foot x offset from the hip and lift, per phase. */
    static final float[] FOOT_X = {4, 2, 0, -2, -4, -2, 1, 4};
    static final int[] FOOT_LIFT = {0, 0, 0, 0, 0, 1, 2, 1};

    /** Body bob (rows down) per frame: dips after each contact. */
    static final int[] BOB = {0, 1, 1, 0, 0, 1, 1, 0};
    /** Tail-tip sway per frame. */
    static final int[] SWAY = {0, 1, 1, 1, 0, -1, -1, -1};
    /** Near arm swing (bipeds), opposite the near leg. */
    static final int[] ARM_DX = {-1, -1, 0, 1, 1, 1, 0, -1};

    /** The head (and ears) lag the body by a frame, so they nod. */
    static int headBob(int f) {
        return BOB[(f + 7) % 8];
    }

    /**
     * Rasterise a limb through joints {x, y} (y increasing), one span per
     * row centred on the polyline; widths[i] is the width on segment i.
     */
    static Canvas limb(Canvas c, float[][] j, int[] widths) {
        Canvas m = new Canvas(c.w, c.h);
        for (int s = 0; s + 1 < j.length; s++) {
            float x0 = j[s][0], y0 = j[s][1], x1 = j[s + 1][0], y1 = j[s + 1][1];
            int w = widths[s];
            for (int y = Math.round(y0); y <= Math.round(y1); y++) {
                float t = y1 == y0 ? 1 : (y - y0) / (y1 - y0);
                float cx = x0 + (x1 - x0) * Math.max(0, Math.min(1, t));
                int left = Math.round(cx - w / 2f);
                for (int x = left; x < left + w; x++) {
                    m.set(x, y, 1);
                }
            }
        }
        return m;
    }

    /** A short thick front leg in phase p, shoulder (sx, sy) to ground row gy. */
    static Canvas front(Canvas c, float sx, int sy, int gy, int p) {
        float px = sx + PAW_X[p] * 0.8f;
        int py = gy - LIFT[p] * 2 / 3;
        float wx = sx + (px - sx) * 0.7f;
        int wy = py - 2;
        if (p >= 5) {
            wx = px + 1;
            wy = py - 1;
        }
        return limb(c, new float[][] {{sx, sy}, {wx, wy}, {px, py}}, new int[] {4, 3});
    }

    /** A thigh-heavy hind leg: thigh, knee forward, hock back, paw. */
    static Canvas hind(Canvas c, float hx, int hy, int gy, int p) {
        float px = hx + HIND_PAW_X[p] * 0.8f;
        int py = gy - HIND_LIFT[p] * 2 / 3;
        float kx = hx + (px - hx) * 0.4f, ky = hy + 2;
        float ox = px - 1, oy = py - 2;
        if (p == 4) {
            ox = px - 2;
        }
        if (oy <= ky) {
            oy = ky + 1;
        }
        return limb(c, new float[][] {{hx, hy}, {kx, ky}, {ox, oy}, {px, py}}, new int[] {6, 4, 3});
    }

    /**
     * A short biped leg from the hip to a foot with the toes forward; on
     * push-off (phase 4) the heel lifts and only the toes touch.
     */
    static Canvas footLeg(Canvas c, float hx, int hy, int gy, int p, int legW, int footW) {
        float fx = hx + FOOT_X[p];
        int sole = gy - FOOT_LIFT[p];
        float ax = hx + (fx - hx) * 0.6f;
        Canvas m = limb(c, new float[][] {{hx, hy}, {ax, sole - 2}}, new int[] {legW});
        int x0 = Math.round(fx - legW / 2f);
        if (p == 4) {
            for (int x = x0; x < x0 + 2; x++) {
                m.set(x, sole - 1, 1);              // heel up
            }
            for (int x = x0 + 1; x < x0 + footW; x++) {
                m.set(x, sole, 1);                  // on the toes
            }
        } else {
            for (int y = sole - 1; y <= sole; y++) {
                for (int x = x0; x < x0 + footW; x++) {
                    m.set(x, y, 1);
                }
            }
        }
        return m;
    }

    /** Draw a limb mask (far legs in shade) with a one-row paw/foot. */
    static void draw(Canvas c, Canvas m, boolean near, int pawZone) {
        draw(c, m, near, pawZone, 1);
    }

    /**
     * Draw a limb mask: far legs flat shade, near legs creased where they
     * overlap what is already drawn; the bottom pawRows rows are pawZone.
     */
    static void draw(Canvas c, Canvas m, boolean near, int pawZone, int pawRows) {
        int zone = near ? FUR : FUR_FAR;
        int bottom = -1;
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                if (m.get(x, y) != 0) {
                    bottom = y;
                }
            }
        }
        int[] before = c.z.clone();
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                if (m.get(x, y) == 0) {
                    continue;
                }
                boolean crease = false;
                if (near) {
                    for (int[] o : n) {
                        int nx = x + o[0], ny = y + o[1];
                        if (m.get(nx, ny) == 0 && nx >= 0 && ny >= 0 && nx < c.w && ny < c.h
                                && before[ny * c.w + nx] != 0) {
                            crease = true;
                        }
                    }
                }
                c.set(x, y, y > bottom - pawRows ? pawZone : (crease ? SEP : zone));
            }
        }
    }

    /** A part in front with its own thin LINE outline where it overlaps others. */
    static void outlined(Canvas c, String d, int zone, int dx, int dy) {
        Canvas m = new Canvas(c.w, c.h);
        fill(m, d, 1, dx, dy, false);
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                if (m.get(x, y) != 0 || c.get(x, y) == 0) {
                    continue;
                }
                for (int[] o : n) {
                    if (m.get(x + o[0], y + o[1]) != 0) {
                        c.set(x, y, LINE);
                    }
                }
            }
        }
        fill(c, d, zone, dx, dy, false);
    }

    /** Copy zones from src into dst at (ox, oy), optionally upside down. */
    static void paste(Canvas dst, Canvas src, int ox, int oy, boolean flipV) {
        for (int y = 0; y < src.h; y++) {
            for (int x = 0; x < src.w; x++) {
                int z = src.z[(flipV ? src.h - 1 - y : y) * src.w + x];
                if (z != 0) {
                    dst.set(ox + x, oy + y, z);
                }
            }
        }
    }

    /** Lay `front` over c, creasing its fur where it meets what is behind. */
    static void creasePaste(Canvas c, Canvas front) {
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
    }
}
