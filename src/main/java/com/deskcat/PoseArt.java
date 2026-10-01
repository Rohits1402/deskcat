package com.deskcat;

/**
 * Whole-frame poses for the original three skins (side-view walk cycles,
 * sleep, belly-up roll, Squirtle's shell) plus Pikachu's procedural body and
 * bolt tail.
 *
 * Pure Java with no libGDX dependency, so the scratch preview tool can render
 * exactly the pixels that ship; {@link PixelArt#tex(Grid)} turns a grid into a
 * Texture. Colours are RGBA8888, the format Pixmap expects.
 *
 * Pose frames have their eyes baked in (walk, sleep, roll, shell); only the
 * sitting bodies keep runtime-drawn eyes for cursor tracking.
 */
final class PoseArt {

    private PoseArt() {
    }

    /** A w x h grid of RGBA8888 pixels; 0 is transparent. Row 0 is the top. */
    static final class Grid {
        final int w, h;
        final int[] px;

        Grid(int w, int h) {
            this.w = w;
            this.h = h;
            px = new int[w * h];
        }

        void set(int x, int y, int c) {
            if (x >= 0 && y >= 0 && x < w && y < h) {
                px[y * w + x] = c;
            }
        }

        int get(int x, int y) {
            return x < 0 || y < 0 || x >= w || y >= h ? 0 : px[y * w + x];
        }
    }

    static int rgba(String hex) {
        return (Integer.parseInt(hex, 16) << 8) | 0xFF;
    }

    // shared
    static final int K = rgba("26202A");
    static final int WHITE = rgba("FFFFFF");
    static final int STEAM = rgba("E9E4EE");
    // cat - identical to the sitting tabby's palette
    static final int CO = rgba("F29A4B"), CD = rgba("C9702E"), CW = rgba("FFF4E3");
    static final int CP = rgba("F2A3B3"), CN = rgba("C75B77"), CG = rgba("7CC46B");
    // pikachu
    static final int PK = rgba("4A2E16");       // warm brown outline
    static final int PY = rgba("F9D848"), PYL = rgba("FDE87A"), PYD = rgba("DFB93B");
    static final int PM = rgba("8C5A2B"), PQ = rgba("E23B2E"), PQD = rgba("B92C22");
    static final int PBK = rgba("2A1D12");      // ear tips
    // squirtle
    static final int SA = rgba("8CCFE8"), SAD = rgba("6FB4CF");
    static final int SH = rgba("A9784C"), SHD = rgba("85582F"), SC = rgba("F2E3B8");
    static final int SI = rgba("5B3A26");       // iris brown

    // ---------------------------------------------------------------- helpers

    static void ell(Grid g, double cx, double cy, double rx, double ry, int c) {
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                double dx = (x - cx) / rx, dy = (y - cy) / ry;
                if (dx * dx + dy * dy <= 1.0) {
                    g.set(x, y, c);
                }
            }
        }
    }

    /** Ellipse with its own dark rim so overlapping parts stay readable. */
    static void ellRim(Grid g, double cx, double cy, double rx, double ry,
            int fill, int rim) {
        ell(g, cx, cy, rx + 0.9, ry + 0.9, rim);
        ell(g, cx, cy, rx, ry, fill);
    }

    /** Recolour pixels of one colour inside an ellipse (shading). */
    static void shade(Grid g, double cx, double cy, double rx, double ry,
            int from, int to) {
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                double dx = (x - cx) / rx, dy = (y - cy) / ry;
                if (dx * dx + dy * dy <= 1.0 && g.get(x, y) == from) {
                    g.set(x, y, to);
                }
            }
        }
    }

    /** Triangle with its apex up (h > 0) or down (h < 0); lean shifts the apex. */
    static void tri(Grid g, double cx, double baseY, double halfW, double h,
            double lean, int c) {
        int steps = (int) Math.round(Math.abs(h));
        for (int j = 0; j <= steps; j++) {
            double t = steps == 0 ? 0 : j / (double) steps;
            int y = (int) Math.round(baseY - Math.signum(h) * j);
            long x0 = Math.round(cx + lean * t - halfW * (1 - t));
            long x1 = Math.round(cx + lean * t + halfW * (1 - t));
            for (long x = x0; x <= x1; x++) {
                g.set((int) x, y, c);
            }
        }
    }

    /** Thick straight stroke, tapering from r0 to r1. */
    static void seg(Grid g, double x0, double y0, double x1, double y1,
            double r0, double r1, int c) {
        for (double t = 0; t <= 1.0001; t += 0.04) {
            double r = r0 + (r1 - r0) * t;
            ell(g, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r, r, c);
        }
    }

    /** Quadratic curve stroked with blobs. */
    static void curve(Grid g, double x0, double y0, double x1, double y1,
            double x2, double y2, double r0, double r1, int c) {
        for (double t = 0; t <= 1.0001; t += 0.025) {
            double mt = 1 - t;
            double x = mt * mt * x0 + 2 * mt * t * x1 + t * t * x2;
            double y = mt * mt * y0 + 2 * mt * t * y1 + t * t * y2;
            double r = r0 + (r1 - r0) * t;
            ell(g, x, y, r, r, c);
        }
    }

    static void poly(Grid g, double[][] pts, int c) {
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                boolean in = false;
                for (int i = 0, j = pts.length - 1; i < pts.length; j = i++) {
                    double xi = pts[i][0], yi = pts[i][1];
                    double xj = pts[j][0], yj = pts[j][1];
                    if ((yi > y) != (yj > y)
                            && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                        in = !in;
                    }
                }
                if (in) {
                    g.set(x, y, c);
                }
            }
        }
    }

    /** 1px outline in the transparent pixels around the silhouette. */
    static void outline(Grid g, int c) {
        int[] src = g.px.clone();
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                if (src[y * g.w + x] != 0) {
                    continue;
                }
                if (op(src, g, x - 1, y) || op(src, g, x + 1, y)
                        || op(src, g, x, y - 1) || op(src, g, x, y + 1)) {
                    g.set(x, y, c);
                }
            }
        }
    }

    private static boolean op(int[] a, Grid g, int x, int y) {
        return x >= 0 && y >= 0 && x < g.w && y < g.h && a[y * g.w + x] != 0;
    }

    /** Leg phase helpers for a trot: returns {footDx, footLift}. */
    static double[] stride(double phase, double reach, double lift) {
        double a = Math.PI * 2 * phase;
        return new double[] {Math.sin(a) * reach, Math.max(0, Math.cos(a)) * lift};
    }

    // ================================================================== CAT

    /** Side-view tabby walk, facing right. 4 frames, 32x24. */
    static Grid catWalk(int frame) {
        double phase = frame / 4.0;
        Grid g = new Grid(32, 24);
        double bob = Math.abs(Math.sin(phase * Math.PI * 2)) * 0.8;
        double by = 12.5 + bob;
        double tsw = Math.sin(phase * Math.PI * 2) * 1.6;
        // tail up, gently swaying
        curve(g, 6, by - 1, 2 + tsw, by - 5, 3.5 + tsw, by - 10.5, 1.8, 1.1, CO);
        catLegs(g, phase + 0.5, by, CD, false);
        ell(g, 14, by, 8.2, 4.2, CO);                   // body
        ell(g, 20.2, by - 0.6, 4.4, 4.0, CO);           // shoulders
        for (int i = 0; i < 3; i++) {                   // back stripes
            g.set(10 + i * 4, (int) (by - 3.4), CD);
            g.set(10 + i * 4, (int) (by - 2.4), CD);
        }
        ell(g, 15, by + 2.9, 5.6, 1.5, CW);             // belly
        catLegs(g, phase, by, CO, true);
        double hx = 24.6, hy = by - 4.4;
        tri(g, hx - 2.0, hy - 2.4, 1.7, 3.4, -0.9, CO);  // far ear
        tri(g, hx + 1.8, hy - 2.4, 1.6, 3.4, 0.8, CO);   // near ear
        ell(g, hx, hy, 4.4, 4.0, CO);                    // head
        tri(g, hx + 1.8, hy - 3.0, 0.7, 1.8, 0.6, CP);
        ell(g, hx + 2.4, hy + 1.8, 2.4, 1.5, CW);        // muzzle
        outline(g, K);
        g.set((int) hx + 4, (int) hy + 1, CN);           // nose
        g.set((int) hx + 1, (int) hy - 1, CG);           // eye: green iris
        g.set((int) hx + 1, (int) hy, CG);
        g.set((int) hx + 2, (int) hy - 1, K);            // slit pupil
        g.set((int) hx + 2, (int) hy, K);
        return g;
    }

    private static void catLegs(Grid g, double phase, double by, int c,
            boolean near) {
        int off = near ? 0 : -1;
        double[] f = stride(phase, 2.2, 1.3);
        double[] b = stride(phase + 0.5, 2.2, 1.3);
        double fx = 20.5 + off, bx = 9 + off;
        seg(g, fx, by + 2, fx + f[0], by + 7.6 - f[1], 1.4, 1.1, c);
        ell(g, fx + f[0] + 0.4, by + 8.0 - f[1], 1.6, 1.0, near ? CW : c);
        seg(g, bx, by + 2, bx + b[0], by + 7.6 - b[1], 1.7, 1.1, c);
        ell(g, bx + b[0] + 0.4, by + 8.0 - b[1], 1.6, 1.0, near ? CW : c);
    }

    /** Curled-up sleeping tabby, 2 breathing frames, 30x18. */
    static Grid catSleep(int frame) {
        Grid g = new Grid(30, 18);
        double breath = frame * 0.5;
        double cy = 11 - breath * 0.6;
        ell(g, 15.5, cy, 9.6, 5.0 + breath, CO);        // curled body
        for (int i = 0; i < 3; i++) {
            g.set(14 + i * 4, (int) (cy - 3.6), CD);
            g.set(14 + i * 4, (int) (cy - 2.6), CD);
        }
        curve(g, 24, cy + 1.5, 20, cy + 5.6, 11, cy + 4.6, 1.7, 1.2, CO);
        for (int i = 0; i < 2; i++) {                   // tail rings
            g.set(18 - i * 4, (int) (cy + 5), CD);
        }
        double hx = 8.5, hy = cy + 0.6;
        tri(g, hx - 2.8, hy - 2.6, 1.8, 3.2, -1.0, CO);  // ears
        tri(g, hx + 2.6, hy - 2.6, 1.8, 3.2, 1.0, CO);
        ell(g, hx, hy, 4.8, 4.0, CO);                    // tucked head
        tri(g, hx + 2.6, hy - 3.2, 0.7, 1.6, 0.8, CP);
        ell(g, hx + 1.2, hy + 2.0, 2.6, 1.6, CW);        // muzzle on paws
        outline(g, K);
        for (int i = -2; i <= 2; i++) {                  // closed eye, a soft arc
            g.set((int) hx - 1 + i, (int) hy - (i == 0 ? 1 : 0), K);
        }
        g.set((int) hx + 3, (int) hy + 1, CN);
        return g;
    }

    /** Belly-up tabby for long petting: paws in the air. 2 frames, 32x20. */
    static Grid catRoll(int frame) {
        Grid g = new Grid(32, 20);
        double w = frame == 0 ? 0 : 1;
        double by = 13;
        // paws up in the air, wiggling between frames
        double[] lx = {12, 15.5, 20, 23.5};
        for (int i = 0; i < 4; i++) {
            double sway = (i % 2 == 0 ? 1 : -1) * w;
            seg(g, lx[i], by - 2, lx[i] + sway, by - 7.5, 1.4, 1.1, CO);
            ell(g, lx[i] + sway, by - 8.2, 1.5, 1.1, CW);
        }
        curve(g, 27, by + 1, 30, by - 1, 29.5 + w, by - 4.5, 1.6, 1.0, CO);
        ell(g, 17.5, by, 9.0, 4.6, CO);                 // body on its back
        ell(g, 17.5, by - 1.6, 6.4, 2.6, CW);           // the belly
        double hx = 7, hy = by + 0.6;
        tri(g, hx - 2.4, hy + 3.0, 1.7, -3.2, -0.8, CO);  // ears point down
        tri(g, hx + 1.6, hy + 3.0, 1.7, -3.2, 0.8, CO);
        ell(g, hx, hy, 4.4, 4.0, CO);
        ell(g, hx + 0.4, hy - 2.0, 2.6, 1.5, CW);        // upside-down muzzle
        outline(g, K);
        for (int s = -1; s <= 1; s += 2) {               // happy closed eyes
            int ex = (int) hx + s * 2;
            g.set(ex - 1, (int) hy + 1, K);
            g.set(ex, (int) hy + 2, K);
            g.set(ex + 1, (int) hy + 1, K);
        }
        g.set((int) hx, (int) hy - 2, CN);
        return g;
    }

    // ============================================================== PIKACHU

    /**
     * Sitting Pikachu, 32x30, from the reference-matched v6 preview: big round
     * head on a small body, tall dark-tipped ears, low red cheeks. Eyes are
     * NOT drawn here - they're round beads drawn at runtime so they can track
     * the cursor (eyeStyle 4).
     */
    static Grid pikaBody() {
        Grid g = new Grid(32, 30);
        double bx = 16, hy = 12;
        ell(g, bx, 23.6, 5.4, 4.6, PY);                  // pear body
        shade(g, bx, 25.4, 5.0, 2.8, PY, PYD);
        ellRim(g, bx - 2.8, 27.6, 2.2, 1.3, PY, PK);     // stubby feet
        ellRim(g, bx + 2.8, 27.6, 2.2, 1.3, PY, PK);
        ellRim(g, bx - 5.3, 23.6, 1.5, 1.9, PY, PK);     // stubby arms
        ellRim(g, bx + 5.3, 23.6, 1.5, 1.9, PY, PK);
        ellRim(g, bx, hy, 7.0, 6.0, PY, PK);             // big round head
        shade(g, bx, hy - 3.0, 6.0, 2.2, PY, PYL);
        pikaEar(g, bx - 3.4, hy - 5.0, 1.9, bx - 6.0, hy - 11.2, 1.0);
        pikaEar(g, bx + 3.4, hy - 5.0, 1.9, bx + 6.0, hy - 11.2, 1.0);
        ellRim(g, bx - 5.0, hy + 2.6, 1.8, 1.6, PQ, PK); // cheeks
        ellRim(g, bx + 5.0, hy + 2.6, 1.8, 1.6, PQ, PK);
        shade(g, bx - 5.0, hy + 3.2, 1.5, 0.8, PQ, PQD);
        shade(g, bx + 5.0, hy + 3.2, 1.5, 0.8, PQ, PQD);
        int nx = (int) bx, ny = (int) hy + 1;            // nose + open smile
        g.set(nx, ny, PK);
        g.set(nx - 2, ny + 2, PK);
        g.set(nx + 2, ny + 2, PK);
        g.set(nx - 1, ny + 3, PK);
        g.set(nx, ny + 3, PK);
        g.set(nx + 1, ny + 3, PK);
        return g;
    }

    /** Tapered ear quad with a dark tip over its top 40%. */
    static void pikaEar(Grid g, double bx0, double by0, double hw0,
            double bx1, double by1, double hw1) {
        poly(g, new double[][] {{bx0 - hw0 - 0.9, by0 + 0.6}, {bx0 + hw0 + 0.9, by0 + 0.6},
                {bx1 + hw1 + 0.9, by1 - 0.9}, {bx1 - hw1 - 0.9, by1 - 0.9}}, PK);
        poly(g, new double[][] {{bx0 - hw0, by0}, {bx0 + hw0, by0},
                {bx1 + hw1, by1}, {bx1 - hw1, by1}}, PY);
        double t = 0.6;
        double mx = bx0 + (bx1 - bx0) * t, my = by0 + (by1 - by0) * t;
        double mw = hw0 + (hw1 - hw0) * t;
        poly(g, new double[][] {{mx - mw, my}, {mx + mw, my},
                {bx1 + hw1, by1}, {bx1 - hw1, by1}}, PBK);
    }

    /** The big slab lightning-bolt tail, its root at the bottom-left. 14x19. */
    static Grid pikaTail(int frame) {
        Grid g = new Grid(14, 19);
        double lean = frame == 0 ? 0 : (frame == 1 ? 1.0 : -1.0);
        boltShape(g, 1.2, 17.6, lean, 0.92);
        outline(g, PK);
        return g;
    }

    /** Lower stroke, a kick back, then a broad flag; brown at the root. */
    static void boltShape(Grid g, double ox, double oy, double lean, double s) {
        poly(g, new double[][] {{ox - 0.5, oy + 0.6}, {ox + 5.6 * s, oy - 2.2 * s},
                {ox + 5.6 * s, oy - 5.6 * s}, {ox - 0.5, oy - 2.6 * s}}, PY);
        poly(g, new double[][] {{ox + 1.2 * s, oy - 4.6 * s}, {ox + 5.6 * s, oy - 5.6 * s},
                {ox + 6.4 * s, oy - 9.4 * s}, {ox + 1.2 * s, oy - 7.6 * s}}, PY);
        poly(g, new double[][] {{ox + 3.4 * s, oy - 8.6 * s},
                {ox + 10.4 * s + lean, oy - 10.0 * s},
                {ox + 10.4 * s + lean, oy - 16.4 * s},
                {ox + 3.4 * s, oy - 11.6 * s}}, PY);
        ell(g, ox + 0.4, oy - 0.2, 2.2, 1.8, PM);
    }

    /** Side-view Pikachu walk, facing right. 4 frames, 32x30. */
    static Grid pikaWalk(int frame) {
        double phase = frame / 4.0;
        Grid g = new Grid(32, 30);
        double bob = Math.abs(Math.sin(phase * Math.PI * 2)) * 0.8;
        double by = 21.5 + bob;
        boltShape(g, 3.6, by + 1.4, Math.sin(phase * Math.PI * 2) * 0.8, 0.86);
        pikaLegs(g, phase + 0.5, by, PYD);
        ell(g, 12.5, by, 5.4, 4.4, PY);                  // small body
        for (int i = 0; i < 2; i++) {                    // brown back stripes
            for (int x = 10 + i * 4; x < 13 + i * 4; x++) {
                g.set(x, (int) (by - 3.2), PM);
                g.set(x, (int) (by - 2.2), PM);
            }
        }
        pikaLegs(g, phase, by, PY);
        double hx = 21.5, hy = by - 6.0;
        // long ears sweeping up and back, so they read as ears, not a tuft
        pikaEar(g, hx - 1.4, hy - 4.2, 1.7, hx - 5.6, hy - 14.0, 0.9);
        pikaEar(g, hx + 1.8, hy - 4.4, 1.6, hx - 0.6, hy - 14.6, 0.9);
        ellRim(g, hx, hy, 5.8, 5.2, PY, PK);             // big head up front
        shade(g, hx, hy - 2.8, 4.8, 1.9, PY, PYL);
        // cheek sits back on the face, clear of the mouth, so it can't
        // read as an open red mouth
        ellRim(g, hx - 0.6, hy + 1.4, 1.3, 1.2, PQ, PK);
        outline(g, PK);
        ell(g, hx + 2.6, hy - 1.4, 1.5, 1.8, PBK);       // bead eye
        g.set((int) Math.round(hx + 2.2), (int) Math.round(hy - 2.4), WHITE);
        g.set((int) hx + 5, (int) hy, PK);               // snout
        g.set((int) hx + 4, (int) hy + 2, PK);           // small smile
        g.set((int) hx + 3, (int) hy + 2, PK);
        return g;
    }

    private static void pikaLegs(Grid g, double phase, double by, int c) {
        double[] f = stride(phase, 1.8, 1.2);
        double[] b = stride(phase + 0.5, 1.8, 1.2);
        seg(g, 16.5, by + 2.6, 16.5 + f[0], by + 6.6 - f[1], 1.6, 1.4, c);
        ell(g, 17.0 + f[0], by + 7.0 - f[1], 2.0, 1.2, c);
        seg(g, 9.5, by + 2.6, 9.5 + b[0], by + 6.6 - b[1], 1.8, 1.4, c);
        ell(g, 10.0 + b[0], by + 7.0 - b[1], 2.0, 1.2, c);
    }

    /** Pikachu flopped on its tummy, ears down, fast asleep. 2 frames, 32x20. */
    static Grid pikaSleep(int frame) {
        Grid g = new Grid(32, 20);
        double breath = frame * 0.5;
        double cy = 14 - breath * 0.6;
        boltShape(g, 22.5, cy + 3.2, 0, 0.62);           // tail lying behind
        ell(g, 17, cy, 8.4, 4.4 + breath, PY);           // flopped body
        shade(g, 17, cy + 2.2, 8.0, 2.2, PY, PYD);
        for (int i = 0; i < 2; i++) {
            for (int x = 15 + i * 4; x < 18 + i * 4; x++) {
                g.set(x, (int) (cy - 3.4), PM);
            }
        }
        double hx = 8.5, hy = cy - 0.4;
        pikaEar(g, hx - 1.6, hy - 3.4, 1.6, hx - 6.8, hy - 6.4, 0.9);  // ears flat
        pikaEar(g, hx + 1.4, hy - 3.8, 1.6, hx + 6.6, hy - 7.0, 0.9);
        ellRim(g, hx, hy, 5.8, 5.0, PY, PK);
        ellRim(g, hx + 3.4, hy + 2.0, 1.6, 1.4, PQ, PK); // cheek
        outline(g, PK);
        for (int s = -1; s <= 1; s += 2) {               // closed eyes
            int ex = (int) hx + s * 2;
            g.set(ex - 1, (int) hy, PK);
            g.set(ex, (int) hy + 1, PK);
            g.set(ex + 1, (int) hy, PK);
        }
        g.set((int) hx, (int) hy + 3, PK);               // sleepy mouth
        return g;
    }

    /**
     * Pikachu on its back, limbs up, delighted. The head is in profile with
     * its face turned up - a flipped front-on face put both red cheeks where
     * eyes go. 2 frames, 34x22.
     */
    static Grid pikaRoll(int frame) {
        Grid g = new Grid(34, 22);
        double w = frame == 0 ? 0 : 1;
        double by = 15;
        boltShape(g, 26.5, by + 2.4, w * 0.6, 0.6);
        double[] lx = {16.5, 20, 24};
        for (int i = 0; i < 3; i++) {                    // stubby limbs up
            double sway = (i % 2 == 0 ? 1 : -1) * w;
            seg(g, lx[i], by - 2, lx[i] + sway, by - 6.4, 1.6, 1.4, PY);
        }
        ell(g, 20, by, 7.6, 4.4, PY);                    // round body, belly up
        shade(g, 20, by - 1.6, 5.4, 2.2, PY, PYL);
        double hx = 10, hy = by - 0.6;
        // ears lie flat along the ground behind the head
        pikaEar(g, hx - 3.6, hy + 2.2, 1.6, hx - 9.0, hy + 4.4, 0.9);
        pikaEar(g, hx - 3.0, hy + 0.2, 1.6, hx - 9.4, hy + 0.8, 0.9);
        ellRim(g, hx, hy, 5.6, 5.0, PY, PK);
        ellRim(g, hx + 2.6, hy - 0.6, 1.5, 1.3, PQ, PK); // the one cheek we see
        outline(g, PK);
        g.set((int) hx - 1, (int) hy - 2, PK);           // happy eye, face up
        g.set((int) hx, (int) hy - 3, PK);
        g.set((int) hx + 1, (int) hy - 2, PK);
        g.set((int) hx + 3, (int) hy - 3, PK);           // open grin
        g.set((int) hx + 4, (int) hy - 4, PK);
        g.set((int) hx + 4, (int) hy - 3, PQ);
        return g;
    }

    // ============================================================= SQUIRTLE

    /** Side-view Squirtle walk, facing right. 4 frames, 32x26. */
    static Grid squirtWalk(int frame) {
        double phase = frame / 4.0;
        Grid g = new Grid(32, 26);
        double bob = Math.abs(Math.sin(phase * Math.PI * 2)) * 0.7;
        double by = 15.5 + bob;
        double cs = Math.sin(phase * Math.PI * 2) * 0.8;
        ell(g, 4.6 + cs * 0.3, by - 2.4, 2.6, 2.4, SA);  // curly tail
        ell(g, 6.6, by + 0.4, 2.0, 1.8, SA);
        squirtLegs(g, phase + 0.5, by, SAD);
        ell(g, 15, by + 2.4, 6.4, 3.4, SC);              // plastron underside
        ell(g, 13.5, by - 1.0, 7.6, 5.6, SH);            // shell dome
        shade(g, 13.5, by + 2.0, 7.4, 2.8, SH, SHD);
        squirtLegs(g, phase, by, SA);
        ell(g, 21.5, by + 0.6, 2.2, 1.8, SA);            // front arm
        double hx = 23.6, hy = by - 4.6;
        ell(g, hx, hy, 5.0, 4.4, SA);                    // head
        outline(g, K);
        for (int i = -4; i <= 3; i++) {                  // shell scute seams
            if (Math.abs(i) != 4) {
                g.set(11 + i, (int) (by - 1), SHD);
            }
        }
        g.set(10, (int) by - 3, SHD);
        g.set(16, (int) by - 3, SHD);
        int ex = (int) hx + 1, ey = (int) hy - 2;        // big eye
        g.set(ex, ey, K);
        g.set(ex + 1, ey, K);
        g.set(ex, ey + 1, SI);
        g.set(ex + 1, ey + 1, K);
        g.set(ex, ey + 2, SI);
        g.set(ex + 1, ey + 2, SI);
        g.set(ex, ey, WHITE);                             // glint
        for (int i = 0; i < 3; i++) {                     // smile
            g.set((int) hx + 2 + i, (int) hy + 2 - (i == 2 ? 1 : 0), K);
        }
        return g;
    }

    private static void squirtLegs(Grid g, double phase, double by, int c) {
        double[] f = stride(phase, 1.8, 1.2);
        double[] b = stride(phase + 0.5, 1.8, 1.2);
        seg(g, 18, by + 3, 18 + f[0], by + 6.6 - f[1], 1.7, 1.5, c);
        ell(g, 18.4 + f[0], by + 7.2 - f[1], 2.0, 1.2, c);
        seg(g, 9.5, by + 3, 9.5 + b[0], by + 6.6 - b[1], 1.7, 1.5, c);
        ell(g, 9.9 + b[0], by + 7.2 - b[1], 2.0, 1.2, c);
    }

    /**
     * Squirtle withdrawn into its shell, seen from the front like the sitting
     * pose: domed shell, cream rim, dark openings. 30x22. Eyes peeking out of
     * the head opening are drawn at runtime (see SkinAssets.shellEye*).
     */
    static Grid squirtShell(boolean asleep) {
        Grid g = new Grid(30, 22);
        ell(g, 15, 15.6, 11.2, 4.4, SC);                  // cream plastron rim
        ell(g, 15, 12.0, 10.6, 8.2, SH);                  // shell dome
        shade(g, 15, 15.4, 10.4, 3.2, SH, SHD);
        outline(g, K);
        // hexagonal scute seams across the dome
        int[][] seams = {{15, 6}, {15, 7}, {15, 8}, {11, 8}, {12, 9}, {13, 10},
                {14, 10}, {16, 10}, {17, 10}, {18, 9}, {19, 8}, {8, 11}, {9, 12},
                {22, 11}, {21, 12}};
        for (int[] s : seams) {
            g.set(s[0], s[1], SHD);
        }
        // one arched opening low on the front where the head went in; arm
        // and leg holes are left out on purpose - with them the shell read
        // as a frowning face
        ell(g, 15, 15.0, 3.6, 1.7, K);
        for (int x = 12; x <= 18; x++) {                   // cream lip over the arch
            if (g.get(x, 13) == SH || g.get(x, 13) == SHD) {
                g.set(x, 13, SC);
            }
        }
        if (asleep) {                                      // closed eyes in the dark
            g.set(13, 15, SAD);
            g.set(14, 15, SAD);
            g.set(16, 15, SAD);
            g.set(17, 15, SAD);
        }
        return g;
    }

    /** Asleep in the shell; the dome rises and falls. 2 frames, 30x22. */
    static Grid squirtSleep(int frame) {
        Grid g = squirtShell(true);
        if (frame == 1) {                                 // breathe: lift a row
            Grid s = new Grid(g.w, g.h);
            for (int y = 0; y < g.h - 1; y++) {
                System.arraycopy(g.px, (y + 1) * g.w, s.px, y * g.w, g.w);
            }
            for (int x = 0; x < g.w; x++) {               // keep the base planted
                s.set(x, g.h - 1, g.get(x, g.h - 1));
                if (s.get(x, g.h - 2) == 0) {
                    s.set(x, g.h - 2, g.get(x, g.h - 1) == 0 ? 0 : g.get(x, g.h - 2));
                }
            }
            return s;
        }
        return g;
    }

    /** Squirtle upside down on its shell, legs pedalling. 2 frames, 32x22. */
    static Grid squirtRoll(int frame) {
        Grid g = new Grid(32, 22);
        double w = frame == 0 ? 0 : 1;
        double by = 13;
        double[] lx = {11, 14.5, 19, 22.5};
        for (int i = 0; i < 4; i++) {                     // stubby legs pedalling
            double sway = (i % 2 == 0 ? 1 : -1) * w;
            seg(g, lx[i], by - 2, lx[i] + sway, by - 6.6, 1.7, 1.5, SA);
            ell(g, lx[i] + sway, by - 7.2, 1.8, 1.2, SA);
        }
        ell(g, 17, by + 3.2, 9.0, 4.6, SH);               // shell, dome down
        shade(g, 17, by + 4.8, 8.6, 2.8, SH, SHD);
        ell(g, 17, by, 7.6, 3.0, SC);                     // plastron facing up
        ell(g, 27.5, by - 1.2, 2.0, 2.0, SA);             // tail flicking
        double hx = 6.4, hy = by + 0.2;
        ell(g, hx, hy, 4.6, 4.0, SA);                     // head in profile, face up
        outline(g, K);
        g.set((int) hx - 1, (int) hy - 1, K);             // one happy eye
        g.set((int) hx, (int) hy - 2, K);
        g.set((int) hx + 1, (int) hy - 1, K);
        for (int i = 0; i < 3; i++) {                     // a wide grin
            g.set((int) hx - 2 + i, (int) hy + 2 - (i == 0 ? 1 : 0), K);
        }
        return g;
    }

    // ============================================================= PARTICLES

    /** Little steam puff for the typing overheat. 5x4. */
    static Grid steam() {
        Grid g = new Grid(5, 4);
        int[][] p = {{1, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, {1, 2},
                {2, 2}, {3, 2}, {4, 2}, {2, 3}, {3, 3}};
        for (int[] q : p) {
            g.set(q[0], q[1], STEAM);
        }
        return g;
    }
}
