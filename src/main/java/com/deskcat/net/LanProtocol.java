package com.deskcat.net;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire format: pipe-delimited fields with backslash escaping, prefixed with a
 * magic+version field so foreign datagrams on the port are ignored.
 *
 * <pre>
 * DC1|S|id|name|skin|xFrac|yFrac|facing|anim
 * DC1|C|id|name|text|target
 * DC1|B|id
 * </pre>
 */
public final class LanProtocol {

    public static final String MAGIC = "DC1";
    /** Fits any sane message; datagrams beyond this are truncated by recv. */
    public static final int MAX_PACKET = 1400;

    private LanProtocol() {
    }

    public static String encodeState(String id, String name, String skin,
            float xFrac, float yFrac, boolean facingLeft, int anim) {
        return join(MAGIC, String.valueOf(LanMsg.STATE), id, name, skin,
                String.valueOf(xFrac), String.valueOf(yFrac),
                facingLeft ? "1" : "0", String.valueOf(anim));
    }

    public static String encodeChat(String id, String name, String text,
            String target) {
        return join(MAGIC, String.valueOf(LanMsg.CHAT), id, name, text,
                target == null ? "" : target);
    }

    public static String encodeBye(String id) {
        return join(MAGIC, String.valueOf(LanMsg.BYE), id);
    }

    /** Returns null for anything malformed or non-DeskCat. */
    public static LanMsg decode(String raw) {
        if (raw == null) {
            return null;
        }
        List<String> f = split(raw);
        if (f.size() < 3 || !MAGIC.equals(f.get(0)) || f.get(1).length() != 1) {
            return null;
        }
        LanMsg m = new LanMsg();
        m.type = f.get(1).charAt(0);
        m.id = f.get(2);
        if (m.id.isEmpty()) {
            return null;
        }
        try {
            switch (m.type) {
                case LanMsg.STATE:
                    if (f.size() < 9) {
                        return null;
                    }
                    m.name = f.get(3);
                    m.skin = f.get(4);
                    m.xFrac = clamp01(Float.parseFloat(f.get(5)));
                    m.yFrac = clamp01(Float.parseFloat(f.get(6)));
                    m.facingLeft = "1".equals(f.get(7));
                    m.anim = Integer.parseInt(f.get(8));
                    return m;
                case LanMsg.CHAT:
                    if (f.size() < 6) {
                        return null;
                    }
                    m.name = f.get(3);
                    m.text = f.get(4);
                    m.target = f.get(5);
                    return m;
                case LanMsg.BYE:
                    return m;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            String s = fields[i] == null ? "" : fields[i];
            for (int j = 0; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '\\') {
                    sb.append("\\\\");
                } else if (c == '|') {
                    sb.append("\\p");
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static List<String> split(String raw) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (esc) {
                cur.append(c == 'p' ? '|' : c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '|') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
