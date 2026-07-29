package com.deskcat.net;

/**
 * Live view of one remote pet. Fields are written by the render thread while
 * draining the receive queue and read by that peer's window listener on the
 * same thread, so no synchronization is needed.
 */
public class Peer {

    public final String id;
    public String name = "";
    public String skin = "cat";
    public float xFrac, yFrac;
    public boolean facingLeft;
    public int anim = LanMsg.ANIM_IDLE;
    public long lastSeenMs;

    /** Active speech bubble; empty when silent. */
    public final com.deskcat.Bubble bubble = new com.deskcat.Bubble();

    public Peer(String id) {
        this.id = id;
    }
}
