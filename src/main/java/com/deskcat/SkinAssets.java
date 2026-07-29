package com.deskcat;

import java.util.HashMap;
import java.util.Map;

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
    // 0 = startle only (cat), 1 = thunderbolt (pikachu), 2 = water gun (squirtle)
    public int attackType;
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
            a.eyeStyle = 1;
            a.attackType = 1;
            a.bodyTex = PixelArt.fromMap(PixelArt.PIKA_BODY);
            a.tailTex = new Texture[] {
                    PixelArt.fromMap(PixelArt.PIKA_TAIL_A),
                    PixelArt.fromMap(PixelArt.PIKA_TAIL_B),
                    PixelArt.fromMap(PixelArt.PIKA_TAIL_C),
            };
            a.tailX = 27;
            a.eyeLX = 9;
            a.eyeRX = 20;
            a.eyeW = 3;
            a.eyeH = 3;
            a.eyeY = 15;
            a.eyeCenterX = 15.5f;
            a.eyeCenterY = 16f;
            a.furColor = C_YELLOW;
            a.irisColor = C_IRIS_GREEN;
            a.pawTex = PixelArt.fromMap(PixelArt.PAW_PIKA);
        } else {
            a.eyeStyle = 0;
            a.attackType = 0;
            a.bodyTex = PixelArt.fromMap(PixelArt.BODY);
            a.tailTex = new Texture[] {
                    PixelArt.fromMap(PixelArt.TAIL_A),
                    PixelArt.fromMap(PixelArt.TAIL_B),
                    PixelArt.fromMap(PixelArt.TAIL_C),
            };
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

    public static void disposeAll() {
        for (SkinAssets a : CACHE.values()) {
            a.bodyTex.dispose();
            for (Texture t : a.tailTex) {
                t.dispose();
            }
            a.pawTex.dispose();
        }
        CACHE.clear();
    }
}
