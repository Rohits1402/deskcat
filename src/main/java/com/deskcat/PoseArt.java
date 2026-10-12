package com.deskcat;

/**
 * The plain RGBA8888 pixel grid every skin is drawn into (see
 * {@link PathArt}), plus the overheat steam puff. Pure Java with no libGDX
 * dependency, so the scratch preview renders exactly the pixels that ship;
 * {@link PixelArt#tex(Grid)} turns a grid into a Texture.
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

    static final int STEAM = rgba("E9E4EE");

    /** A small puff of steam for the overheat effect, 5x4. */
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
