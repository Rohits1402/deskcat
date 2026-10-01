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

    private static final Color C_ORANGE = Color.valueOf("F29A4B");
    private static final Color C_YELLOW = Color.valueOf("F9D848");
    private static final Color C_BLUE = Color.valueOf("8CCFE8");
    private static final Color C_IRIS_GREEN = Color.valueOf("7CC46B");
    private static final Color C_IRIS_BROWN = Color.valueOf("5B3A26");
    private static final Color C_BLACK_FUR = Color.valueOf("3A3340");
    private static final Color C_AMBER = Color.valueOf("C9861A");
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
            a.eyeStyle = 2;
            a.attackType = 2;
            a.bodyTex = PixelArt.squirtleBody();
            a.tailTex = PixelArt.squirtleTail();
            a.walkTex = frames(4, PoseArt::squirtWalk);
            a.sleepTex = frames(2, PoseArt::squirtSleep);
            a.rollTex = frames(2, PoseArt::squirtRoll);
            a.shellTex = PixelArt.tex(PoseArt.squirtShell(false));
            a.shellEyeX = 13;
            a.shellEyeY = 6;
            a.tailX = 26;
            a.eyeLX = 11;
            a.eyeRX = 20;
            a.eyeW = 3;
            a.eyeH = 4;
            a.eyeY = 16;
            a.eyeCenterX = 16.5f;
            a.eyeCenterY = 17.5f;
            a.furColor = C_BLUE;
            a.irisColor = C_IRIS_BROWN;
            a.pawTex = PixelArt.fromMap(PixelArt.PAW_SQUIRT);
        } else if ("pikachu".equals(skin)) {
            a.eyeStyle = 4;      // round beads, drawn at runtime
            a.attackType = 1;
            a.bodyTex = PixelArt.tex(PoseArt.pikaBody());
            a.tailTex = frames(3, PoseArt::pikaTail);
            a.walkTex = frames(4, PoseArt::pikaWalk);
            a.sleepTex = frames(2, PoseArt::pikaSleep);
            a.rollTex = frames(2, PoseArt::pikaRoll);
            a.tailX = 21;
            a.eyeLX = 14;
            a.eyeRX = 20;
            a.eyeW = 3;
            a.eyeH = 4;
            a.eyeY = 17;
            a.eyeCenterX = 18f;
            a.eyeCenterY = 19f;
            a.cheekLX = 13f;
            a.cheekRX = 23f;
            a.cheekY = 14.4f;
            a.furColor = C_YELLOW;
            a.irisColor = C_IRIS_GREEN;
            a.pawTex = PixelArt.fromMap(PixelArt.PAW_PIKA);
        } else if ("blackcat".equals(skin)) {
            a.eyeStyle = 0;
            a.attackType = 3;
            a.bodyTex = PixelArt.blackCatBody();
            a.tailTex = PixelArt.blackCatTail();
            a.tailX = 22;
            a.eyeLX = 9;
            a.eyeRX = 19;
            a.eyeW = 4;
            a.eyeH = 3;
            a.eyeY = 18;
            a.eyeCenterX = 15.5f;
            a.eyeCenterY = 19f;
            a.furColor = C_BLACK_FUR;
            a.irisColor = C_AMBER;
            a.pawTex = PixelArt.fromMap(PixelArt.PAW_BLACK);
        } else if ("doge".equals(skin)) {
            a.eyeStyle = 1;
            a.attackType = 4;
            a.bodyTex = PixelArt.dogeBody();
            a.tailTex = PixelArt.dogeTail();
            a.tailX = 22;
            a.eyeLX = 13;
            a.eyeRX = 19;
            a.eyeW = 3;
            a.eyeH = 2;      // a smug squint
            a.eyeY = 17;
            a.eyeCenterX = 17f;
            a.eyeCenterY = 18f;
            a.furColor = C_SHIBA;
            a.irisColor = C_IRIS_BROWN;
            a.pawTex = PixelArt.fromMap(PixelArt.PAW_DOGE);
        } else if ("goldfish".equals(skin)) {
            a.eyeStyle = 1;
            a.attackType = 5;
            a.swims = true;
            a.bodyTex = PixelArt.goldfishBody();
            a.tailTex = PixelArt.goldfishTail();
            a.oneEye = true;
            a.tailX = 0;
            a.eyeLX = 26;
            a.eyeRX = 26;
            a.eyeW = 3;
            a.eyeH = 3;
            a.eyeY = 12;
            a.eyeCenterX = 27f;
            a.eyeCenterY = 13f;
            a.furColor = C_GOLDFISH;
            a.irisColor = C_IRIS_BROWN;
            a.pawTex = PixelArt.fromMap(PixelArt.PAW_FIN);
        } else {
            a.eyeStyle = 0;
            a.attackType = 0;
            a.bodyTex = PixelArt.fromMap(PixelArt.BODY);
            a.tailTex = new Texture[] {
                    PixelArt.fromMap(PixelArt.TAIL_A),
                    PixelArt.fromMap(PixelArt.TAIL_B),
                    PixelArt.fromMap(PixelArt.TAIL_C),
            };
            a.walkTex = frames(4, PoseArt::catWalk);
            a.sleepTex = frames(2, PoseArt::catSleep);
            a.rollTex = frames(2, PoseArt::catRoll);
            a.tailX = 26;
            a.eyeLX = 7;
            a.eyeRX = 18;
            a.eyeW = 4;
            a.eyeH = 3;
            a.eyeY = 15;
            a.eyeCenterX = 14f;
            a.eyeCenterY = 16f;
            a.furColor = C_ORANGE;
            a.irisColor = C_IRIS_GREEN;
            a.pawTex = PixelArt.fromMap(PixelArt.PAW_CAT);
        }
        return a;
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
