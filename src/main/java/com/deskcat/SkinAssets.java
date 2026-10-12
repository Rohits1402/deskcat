package com.deskcat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

/**
 * Per-skin textures and geometry, shared between the local pet and remote
 * peers' pets (LAN). Instances are cached per skin name; textures are created
 * on the main GL context and shared across windows, so dispose only once via
 * {@link #disposeAll()} at shutdown.
 */
public class SkinAssets {

    // eye styles: 0 = iris on white patch (cat), 1 = solid bead (pikachu),
    // 2 = outlined iris block (squirtle)
    public int eyeStyle;
    // 0 = paw swipe (cat), 1 = thunderbolt (pikachu), 2 = water gun
    // (squirtle), 3 = hiss + swipe (black cat), 4 = dig (doge),
    // 5 = puff up (goldfish)
    public int attackType;
    /** Free-swims the whole screen instead of patrolling the floor. */
    public boolean swims;
    /** Profile view: draw only the left eye (eyeRX mirrors eyeLX as a fallback). */
    public boolean oneEye;
    /**
     * Whole-frame poses with eyes baked in, replacing body + tail + eyes while
     * they play: a side-view walk (facing right), sleep and belly-up roll.
     * Null for skins that don't have them.
     */
    public Texture[] walkTex, sleepTex, rollTex;
    /** Squirtle withdrawn into its shell, plus where its peeking eyes go. */
    public Texture shellTex;
    public int shellEyeX, shellEyeY;
    /** Cheek centres in FBO units, for Pikachu's sparks. */
    public float cheekLX, cheekRX, cheekY;
    public Texture bodyTex;
    public Texture[] tailTex;
    public Texture pawTex;
    public int tailX;
    public int eyeLX, eyeRX, eyeW, eyeH, eyeY;
    public float eyeCenterX, eyeCenterY;
    public Color furColor, irisColor;
    /** Closed-eye line; null means the usual dark outline. */
    public Color lineColor;

    private static final Color C_ORANGE = Color.valueOf("F29A4B");
    private static final Color C_YELLOW = Color.valueOf("F7D23E");
    private static final Color C_BLUE = Color.valueOf("8CCFE8");
    private static final Color C_IRIS_GREEN = Color.valueOf("7CC46B");
    private static final Color C_IRIS_BROWN = Color.valueOf("5B3A26");
    private static final Color C_BLACK_FUR = Color.valueOf("2E2936");
    private static final Color C_BLACK_LINE = Color.valueOf("ECE8F4");
    private static final Color C_SQUIRT_IRIS = Color.valueOf("7A3528");
    private static final Color C_NAVY = Color.valueOf("22313F");
    private static final Color C_AMBER = Color.valueOf("F2C14E");
    private static final Color C_SHIBA = Color.valueOf("D9A066");
    private static final Color C_GOLDFISH = Color.valueOf("FF8C3A");

    private static final Map<String, SkinAssets> CACHE = new HashMap<String, SkinAssets>();

    /** Must be called on the GL thread of the main window. */
    public static SkinAssets get(String skin) {
        String key = skin == null ? "cat" : skin.toLowerCase();
        SkinAssets a = CACHE.get(key);
        if (a == null) {
            a = load(key);
            CACHE.put(key, a);
        }
        return a;
    }

    private static SkinAssets load(String skin) {
        SkinAssets a = new SkinAssets();
        if ("squirtle".equals(skin)) {
            a.eyeStyle = 5;      // baked sockets, moving pupils
            a.attackType = 2;
            a.bodyTex = PixelArt.tex(SquirtArt.sitBody());
            a.tailTex = frames(3, SquirtArt::sitTail);
            a.walkTex = frames(4, SquirtArt::walk);
            a.sleepTex = frames(2, SquirtArt::sleep);
            a.rollTex = frames(2, SquirtArt::roll);
            a.shellTex = PixelArt.tex(SquirtArt.shell(false, 0));
            a.shellEyeX = 13;
            a.shellEyeY = 6;
            a.tailX = 25;
            a.eyeLX = 9;
            a.eyeRX = 21;
            a.eyeW = 4;
            a.eyeH = 4;
            a.eyeY = 16;
            a.eyeCenterX = 17f;
            a.eyeCenterY = 18f;
            a.furColor = C_BLUE;
            a.irisColor = C_SQUIRT_IRIS;
            a.lineColor = C_NAVY;
            a.pawTex = PixelArt.tex(PathArt.paw(SquirtArt.SQUIRT, PathArt.FUR));
        } else if ("pikachu".equals(skin)) {
            a.eyeStyle = 4;      // round beads, drawn at runtime
            a.attackType = 1;
            a.bodyTex = PixelArt.tex(PikaArt.sitBody());
            a.tailTex = frames(3, PikaArt::sitTail);
            a.walkTex = frames(4, PikaArt::walk);
            a.sleepTex = frames(2, PikaArt::sleep);
            a.rollTex = frames(2, PikaArt::roll);
            a.tailX = 23;
            a.eyeLX = 10;
            a.eyeRX = 20;
            a.eyeW = 4;
            a.eyeH = 5;
            a.eyeY = 12;
            a.eyeCenterX = 17f;
            a.eyeCenterY = 14.5f;
            a.cheekLX = 7.5f;
            a.cheekRX = 26.5f;
            a.cheekY = 10f;
            a.furColor = C_YELLOW;
            a.irisColor = C_IRIS_GREEN;
            a.pawTex = PixelArt.tex(PathArt.paw(PikaArt.PIKA, PathArt.FUR));
        } else if ("blackcat".equals(skin)) {
            catArt(a, CatArt.BLACK);
            a.attackType = 3;
            a.furColor = C_BLACK_FUR;
            a.irisColor = C_AMBER;
            a.lineColor = C_BLACK_LINE;   // light, like its outline
        } else if ("doge".equals(skin)) {
            a.eyeStyle = 1;
            a.attackType = 4;
            a.bodyTex = PixelArt.tex(DogArt.sitBody());
            a.tailTex = frames(3, DogArt::sitTail);
            a.walkTex = frames(4, DogArt::walk);
            a.sleepTex = frames(2, DogArt::sleep);
            a.rollTex = frames(2, DogArt::roll);
            a.tailX = 26;
            a.eyeLX = 10;
            a.eyeRX = 21;
            a.eyeW = 3;
            a.eyeH = 2;      // a smug squint
            a.eyeY = 17;
            a.eyeCenterX = 17f;
            a.eyeCenterY = 18f;
            a.furColor = C_SHIBA;
            a.irisColor = C_IRIS_BROWN;
            a.pawTex = PixelArt.tex(PathArt.paw(DogArt.SHIBA, PathArt.CREAM));
        } else if ("goldfish".equals(skin)) {
            a.eyeStyle = 1;
            a.attackType = 5;
            a.swims = true;
            a.bodyTex = PixelArt.tex(FishArt.body());
            a.tailTex = frames(3, FishArt::tail);
            a.oneEye = true;
            a.tailX = 0;
            a.eyeLX = 25;
            a.eyeRX = 25;
            a.eyeW = 3;
            a.eyeH = 3;
            a.eyeY = 13;
            a.eyeCenterX = 26.5f;
            a.eyeCenterY = 14.5f;
            a.furColor = C_GOLDFISH;
            a.irisColor = C_IRIS_BROWN;
            a.pawTex = PixelArt.tex(PathArt.paw(FishArt.GOLDFISH, PathArt.ACC1));
        } else {
            catArt(a, CatArt.TABBY);
            a.attackType = 0;
            a.furColor = C_ORANGE;
            a.irisColor = C_IRIS_GREEN;
        }
        return a;
    }

    /**
     * The hand-drawn cat art (CatArt), shared by every cat palette: sitting
     * body with baked eye sockets, wagging tail, walk / sleep / roll poses.
     */
    private static void catArt(SkinAssets a, PathArt.Palette p) {
        a.eyeStyle = 5;
        a.bodyTex = PixelArt.tex(CatArt.sitBody(p));
        a.tailTex = frames(3, f -> CatArt.sitTail(p, f));
        a.walkTex = frames(4, f -> CatArt.walk(p, f));
        a.sleepTex = frames(2, f -> CatArt.sleep(p, f));
        a.rollTex = frames(2, f -> CatArt.roll(p, f));
        a.pawTex = PixelArt.tex(CatArt.paw(p));
        a.tailX = 23;
        a.eyeLX = 8;
        a.eyeRX = 20;
        a.eyeW = 4;
        a.eyeH = 4;
        a.eyeY = 17;
        a.eyeCenterX = 16f;
        a.eyeCenterY = 19f;
    }

    private static Texture[] frames(int n, IntFunction<PoseArt.Grid> make) {
        Texture[] t = new Texture[n];
        for (int i = 0; i < n; i++) {
            t[i] = PixelArt.tex(make.apply(i));
        }
        return t;
    }

    private static void dispose(Texture[] ts) {
        if (ts != null) {
            for (Texture t : ts) {
                t.dispose();
            }
        }
    }

    public static void disposeAll() {
        for (SkinAssets a : CACHE.values()) {
            a.bodyTex.dispose();
            for (Texture t : a.tailTex) {
                t.dispose();
            }
            a.pawTex.dispose();
            dispose(a.walkTex);
            dispose(a.sleepTex);
            dispose(a.rollTex);
            if (a.shellTex != null) {
                a.shellTex.dispose();
            }
        }
        CACHE.clear();
    }
}
