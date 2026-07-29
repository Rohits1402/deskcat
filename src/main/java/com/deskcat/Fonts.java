package com.deskcat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * Fonts for speech bubbles. The default libGDX 15 px bitmap font is fine at
 * small scales but turns to mush when a /size command blows it up 50x, so big
 * bubbles use a font generated at {@link #BIG_PX} from a Windows system TTF
 * (read-only; nothing is shipped or written). Falls back to the default font
 * when generation isn't possible.
 */
public final class Fonts {

    /** Native pixel size of the big-bubble font. */
    public static final int BIG_PX = 96;

    private static BitmapFont big;
    private static boolean bigTried;

    private Fonts() {
    }

    /** Must be called on the GL thread. Never returns null. */
    public static BitmapFont big(BitmapFont fallback) {
        if (!bigTried) {
            bigTried = true;
            String[] candidates = {
                    "C:/Windows/Fonts/consolab.ttf",   // Consolas bold
                    "C:/Windows/Fonts/consola.ttf",
                    "C:/Windows/Fonts/arialbd.ttf",
                    "C:/Windows/Fonts/arial.ttf",
            };
            for (String path : candidates) {
                try {
                    if (!Gdx.files.absolute(path).exists()) {
                        continue;
                    }
                    FreeTypeFontGenerator gen =
                            new FreeTypeFontGenerator(Gdx.files.absolute(path));
                    FreeTypeFontGenerator.FreeTypeFontParameter p =
                            new FreeTypeFontGenerator.FreeTypeFontParameter();
                    p.size = BIG_PX;
                    p.minFilter = Texture.TextureFilter.Linear;
                    p.magFilter = Texture.TextureFilter.Linear;
                    big = gen.generateFont(p);
                    gen.dispose();
                    break;
                } catch (Throwable t) {
                    // try the next candidate
                }
            }
        }
        return big != null ? big : fallback;
    }

    /** True when {@link #big} returned a real high-res font. */
    public static boolean hasBig() {
        return big != null;
    }

    public static void disposeAll() {
        if (big != null) {
            big.dispose();
            big = null;
        }
        bigTried = false;
    }
}
