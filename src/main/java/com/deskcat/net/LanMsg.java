package com.deskcat.net;

/**
 * One decoded LAN datagram. Which fields are meaningful depends on
 * {@link #type}: STATE fills the pose fields, CHAT fills text/target,
 * BYE only carries the id.
 */
public class LanMsg {

    public static final char STATE = 'S';
    public static final char CHAT = 'C';
    public static final char BYE = 'B';
    public static final char ACTION = 'A';

    // pet animation states carried in STATE messages
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_WALK = 1;
    public static final int ANIM_SLEEP = 2;
    public static final int ANIM_DRAG = 3;
    /** Knocked over by a shot. */
    public static final int ANIM_KO = 4;

    public char type;
    public String id;
    public String name;
    public String skin;
    /** Window position as a fraction of the sender's usable screen. */
    public float xFrac, yFrac;
    public boolean facingLeft;
    public int anim;
    public String text;
    /** Empty for broadcast chat, a peer id for a DM. */
    public String target;
    /** Bubble style carried with CHAT (see ChatCommands). */
    public float chatScale = 1f;
    public int chatEffect;
    public String chatColor = "";
    /** ACTION verb, e.g. "shoot". */
    public String action;
    /**
     * Set by the receiver when the datagram came from this same machine
     * (another instance on this PC) — such peers get no mirror window.
     */
    public boolean sameHost;
}
