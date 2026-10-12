package com.deskcat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The hand-drawn sprite format every skin is built in:
 *
 * - each body part is a hand-authored rectilinear outline in SVG path
 *   syntax ({@code M7 5H21V6...Z}), filled into a grid of colour zones;
 * - markings are small rects clipped to one zone, so they never spill onto
 *   other parts or the background;
 * - a part drawn in front of others gets a soft crease where it overlaps
 *   them ({@link #fillOver}), instead of a hard inner outline;
 * - one uniform outline is grown around the finished silhouette (an
 *   8-neighbour dilation, so steps and corners stay solid);
 * - a {@link Palette} turns zones into colours, so one drawing serves
 *   several colourways.
 *
 * Pure Java - a scratch preview renders the exact shipping pixels.
 */
final class PathArt {

    private PathArt() {
    }

    // colour zones; skins give the generic ones (FUR, CREAM, ACC*) their own
    // meaning through their palette
    static final int FUR = 1, FUR_FAR = 2, CREAM = 3, CREAM_FAR = 4, INNER_EAR = 5,
            NOSE = 6, IRIS = 7, RING = 8, PUPIL = 9, GLINT = 10, PATTERN = 11,
            LINE = 12, MOUTH = 13, SEP = 14, ACC1 = 15, ACC2 = 16, ACC3 = 17,
            ACC4 = 18, ACC1_FAR = 19, ACC2_FAR = 20;
    private static final int ZONES = 21;

    /** Zone -> colour (RGBA8888), plus the outline and whisker colours. */
    static final class Palette {
        final int[] zone = new int[ZONES];
        int outline, whisker;

        Palette() {
            zone[PUPIL] = PoseArt.rgba("120F16");
            zone[GLINT] = PoseArt.rgba("FFFFFF");
            outline = whisker = PoseArt.rgba("2A2230");
        }

        Palette set(int z, String hex) {
            zone[z] = PoseArt.rgba(hex);
            return this;
        }

        Palette outline(String hex) {
            outline = PoseArt.rgba(hex);
            return this;
        }

        Palette whisker(String hex) {
            whisker = PoseArt.rgba(hex);
            return this;
        }

        int colour(int z) {
            return zone[z];
        }
    }

    /** A zone grid; row 0 is the top. */
    static final class Canvas {
        final int w, h;
        final int[] z;

        Canvas(int w, int h) {
            this.w = w;
            this.h = h;
            z = new int[w * h];
        }

        void set(int x, int y, int zone) {
            if (x >= 0 && y >= 0 && x < w && y < h) {
                z[y * w + x] = zone;
            }
        }

        int get(int x, int y) {
            return x < 0 || y < 0 || x >= w || y >= h ? 0 : z[y * w + x];
        }
    }

    private static final Pattern CMD = Pattern.compile("([MHVZ])\\s*(-?\\d+)?(?:[\\s,]+(-?\\d+))?");

    /** Vertices of an absolute M/H/V/Z path, in pixel-edge coordinates. */
    static int[][] parse(String d) {
        List<int[]> pts = new ArrayList<int[]>();
        int x = 0, y = 0;
        Matcher m = CMD.matcher(d);
        while (m.find()) {
            char c = m.group(1).charAt(0);
            if (c == 'M') {
                x = Integer.parseInt(m.group(2));
                y = Integer.parseInt(m.group(3));
            } else if (c == 'H') {
                x = Integer.parseInt(m.group(2));
            } else if (c == 'V') {
                y = Integer.parseInt(m.group(2));
            } else {
                continue;
            }
            pts.add(new int[] {x, y});
        }
        return pts.toArray(new int[0][]);
    }

    /** Fill a path's interior (pixel centres, even-odd) with a zone. */
    static void fill(Canvas c, String d, int zone) {
        fill(c, d, zone, 0, 0, false);
    }

    /** Fill the path mirrored left-right across the canvas width. */
    static void fillMirror(Canvas c, String d, int zone) {
        fill(c, d, zone, 0, 0, true);
    }

    /** fill() and fillMirror(): a symmetric pair of parts. */
    static void fillPair(Canvas c, String d, int zone) {
        fill(c, d, zone, 0, 0, false);
        fill(c, d, zone, 0, 0, true);
    }

    static void fill(Canvas c, String d, int zone, int dx, int dy, boolean mirror) {
        Canvas m = mask(c, d, dx, dy, mirror);
        for (int i = 0; i < m.z.length; i++) {
            if (m.z[i] != 0) {
                c.z[i] = zone;
            }
        }
    }

    /** 1 where the (shifted, optionally mirrored) path covers a pixel centre. */
    private static Canvas mask(Canvas c, String d, int dx, int dy, boolean mirror) {
        int[][] p = parse(d);
        Canvas m = new Canvas(c.w, c.h);
        for (int py = 0; py < c.h; py++) {
            for (int px = 0; px < c.w; px++) {
                double tx = px + 0.5 - dx, ty = py + 0.5 - dy;
                if (mirror) {
                    tx = c.w - (px + 0.5) - dx;
                }
                boolean in = false;
                for (int i = 0, j = p.length - 1; i < p.length; j = i++) {
                    double xi = p[i][0], yi = p[i][1], xj = p[j][0], yj = p[j][1];
                    if ((yi > ty) != (yj > ty)
                            && tx < (xj - xi) * (ty - yi) / (yj - yi) + xi) {
                        in = !in;
                    }
                }
                if (in) {
                    m.z[py * c.w + px] = 1;
                }
            }
        }
        return m;
    }

    /**
     * Fill a part that lies in front of what is already drawn: where its
     * edge meets earlier parts it gets a soft crease (SEP), so overlapping
     * parts stay readable without a hard internal outline.
     */
    static void fillOver(Canvas c, String d, int zone) {
        fillOver(c, d, zone, 0, 0, false);
    }

    /** fillOver() and its mirror image. */
    static void fillOverPair(Canvas c, String d, int zone) {
        fillOver(c, d, zone, 0, 0, false);
        fillOver(c, d, zone, 0, 0, true);
    }

    static void fillOver(Canvas c, String d, int zone, int dx, int dy, boolean mirror) {
        Canvas m = mask(c, d, dx, dy, mirror);
        int[] before = c.z.clone();
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                if (m.get(x, y) == 0) {
                    continue;
                }
                boolean crease = false;
                for (int[] o : n) {
                    int nx = x + o[0], ny = y + o[1];
                    if (m.get(nx, ny) == 0 && nx >= 0 && ny >= 0 && nx < c.w && ny < c.h
                            && before[ny * c.w + nx] != 0) {
                        crease = true;
                    }
                }
                c.set(x, y, crease ? SEP : zone);
            }
        }
    }

    static void rect(Canvas c, int x, int y, int w, int h, int zone) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                c.set(xx, yy, zone);
            }
        }
    }

    /** Paint a rect, but only over pixels currently in zone {@code over}. */
    static void paint(Canvas c, int x, int y, int w, int h, int over, int zone) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                if (c.get(xx, yy) == over) {
                    c.set(xx, yy, zone);
                }
            }
        }
    }

    /** paint() and its left-right mirror image. */
    static void paintPair(Canvas c, int x, int y, int w, int h, int over, int zone) {
        paint(c, x, y, w, h, over, zone);
        paint(c, c.w - x - w, y, w, h, over, zone);
    }

    /** A marking: only paints where there is already (near-side) fur. */
    static void mark(Canvas c, int x, int y, int w, int h) {
        paint(c, x, y, w, h, FUR, PATTERN);
    }

    /** mark() and its left-right mirror image. */
    static void markPair(Canvas c, int x, int y, int w, int h) {
        mark(c, x, y, w, h);
        mark(c, c.w - x - w, y, w, h);
    }

    static void px(Canvas c, int x, int y, int zone) {
        c.set(x, y, zone);
    }

    /** px() and its mirror image. */
    static void pxPair(Canvas c, int x, int y, int zone) {
        c.set(x, y, zone);
        c.set(c.w - 1 - x, y, zone);
    }

    /** Shift a band of rows sideways - a cheap, clean tail wag. */
    static void shiftRows(Canvas c, int y0, int y1, int dx) {
        if (dx == 0) {
            return;
        }
        for (int y = y0; y <= y1; y++) {
            int[] row = new int[c.w];
            for (int x = 0; x < c.w; x++) {
                int sx = x - dx;
                row[x] = sx >= 0 && sx < c.w ? c.z[y * c.w + sx] : 0;
            }
            System.arraycopy(row, 0, c.z, y * c.w, c.w);
        }
    }

    /**
     * Colourise, grow the uniform outline (8-neighbour dilation, like an SVG
     * feMorphology dilate of radius 1), then add whiskers on top.
     */
    static PoseArt.Grid render(Canvas c, Palette p, int[][] whiskers) {
        PoseArt.Grid g = new PoseArt.Grid(c.w, c.h);
        for (int i = 0; i < c.z.length; i++) {
            if (c.z[i] != 0) {
                g.px[i] = p.colour(c.z[i]);
            }
        }
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                if (c.get(x, y) != 0) {
                    continue;
                }
                boolean edge = false;
                for (int oy = -1; oy <= 1 && !edge; oy++) {
                    for (int ox = -1; ox <= 1 && !edge; ox++) {
                        edge = c.get(x + ox, y + oy) != 0;
                    }
                }
                if (edge) {
                    g.set(x, y, p.outline);
                }
            }
        }
        if (whiskers != null) {
            for (int[] w : whiskers) {
                g.set(w[0], w[1], p.whisker);
            }
        }
        return g;
    }

    static PoseArt.Grid render(Canvas c, Palette p) {
        return render(c, p, null);
    }

    /** A 4x4 rounded paw/fin for kneading, swiping, grooming and digging. */
    static PoseArt.Grid paw(Palette p, int fillZone) {
        PoseArt.Grid g = new PoseArt.Grid(4, 4);
        int fill = p.colour(fillZone);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                boolean corner = (x == 0 || x == 3) && (y == 0 || y == 3);
                boolean edge = x == 0 || x == 3 || y == 0 || y == 3;
                if (!corner) {
                    g.set(x, y, edge ? p.outline : fill);
                }
            }
        }
        return g;
    }
}
