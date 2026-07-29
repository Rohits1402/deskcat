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

    // pet animation states carried in STATE messages
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_WALK = 1;
    public static final int ANIM_SLEEP = 2;
    public static final int ANIM_DRAG = 3;

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
    /**
     * Set by the receiver when the datagram came from this same machine
     * (another instance on this PC) — such peers get no mirror window.
     */
    public boolean sameHost;
}
