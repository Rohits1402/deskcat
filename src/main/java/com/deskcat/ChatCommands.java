package com.deskcat;

import java.util.HashMap;
import java.util.Map;

/**
 * Chat-message commands, chainable at the start of a message:
 *
 * <pre>
 * /big /huge /small /size N   — bubble text scale (N = font px, 16 ≈ normal)
 * /shake                      — jittering text
 * /rainbow                    — hue-cycling text
 * /color red                  — named text color
 * </pre>
 *
 * e.g. {@code /big /shake /color red hello}. Everything travels with the
 * chat packet so every desktop renders the same bubble.
 */
public final class ChatCommands {

    public static final float MIN_SCALE = 0.5f;
    /** Effectively uncapped — the screen size is the real limit. */
    public static final float MAX_SCALE = 64f;
    /** Font px treated as scale 1.0 in "/size N". */
    static final float BASE_SIZE = 16f;

    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_SHAKE = 1;
    public static final int EFFECT_RAINBOW = 2;

    private static final Map<String, String> COLORS = new HashMap<String, String>();
    static {
        COLORS.put("red", "E5312E");
        COLORS.put("orange", "F28C28");
        COLORS.put("yellow", "E8C400");
        COLORS.put("green", "3FA34D");
        COLORS.put("cyan", "1FA8A8");
        COLORS.put("blue", "2E6BE5");
        COLORS.put("purple", "8C4FD1");
        COLORS.put("pink", "E066A6");
        COLORS.put("brown", "8C5A2B");
        COLORS.put("black", "26202A");
        COLORS.put("white", "FAFAFA");
        COLORS.put("gray", "808080");
        COLORS.put("grey", "808080");
    }

    public static final class Parsed {
        public final String text;
        public final float scale;
        public final int effect;
        /** RRGGBB hex, or "" for the default text color. */
        public final String colorHex;

        Parsed(String text, float scale, int effect, String colorHex) {
            this.text = text;
            this.scale = scale;
            this.effect = effect;
            this.colorHex = colorHex;
        }
    }

    private ChatCommands() {
    }

    public static float clampScale(float s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }

    public static Parsed parse(String raw) {
        String t = raw.trim();
        float scale = 1f;
        int effect = EFFECT_NONE;
        String colorHex = "";

        while (t.startsWith("/")) {
            int sp = t.indexOf(' ');
            String cmd = (sp < 0 ? t : t.substring(0, sp)).toLowerCase();
            String rest = sp < 0 ? "" : t.substring(sp + 1).trim();

            if (cmd.equals("/big")) {
                scale = 1.6f;
            } else if (cmd.equals("/huge")) {
                scale = 2.2f;
            } else if (cmd.equals("/small")) {
                scale = 0.65f;
            } else if (cmd.equals("/shake")) {
                effect = EFFECT_SHAKE;
            } else if (cmd.equals("/rainbow")) {
                effect = EFFECT_RAINBOW;
            } else if (cmd.equals("/size")) {
                int sp2 = rest.indexOf(' ');
                String num = sp2 < 0 ? rest : rest.substring(0, sp2);
                try {
                    scale = clampScale(Float.parseFloat(num) / BASE_SIZE);
                    rest = sp2 < 0 ? "" : rest.substring(sp2 + 1).trim();
                } catch (NumberFormatException e) {
                    break;   // "/size notanumber ..." — treat as plain text
                }
            } else if (cmd.equals("/color") || cmd.equals("/colour")) {
                int sp2 = rest.indexOf(' ');
                String name = (sp2 < 0 ? rest : rest.substring(0, sp2)).toLowerCase();
                String hex = COLORS.get(name);
                if (hex == null) {
                    break;   // unknown color — treat as plain text
                }
                colorHex = hex;
                rest = sp2 < 0 ? "" : rest.substring(sp2 + 1).trim();
            } else {
                break;       // unknown command — leave the message as typed
            }
            t = rest;
        }
        return new Parsed(t, scale, effect, colorHex);
    }
}
