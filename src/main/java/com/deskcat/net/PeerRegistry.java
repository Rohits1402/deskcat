package com.deskcat.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks live peers from decoded LAN messages. Pure logic (time is injected)
 * so it is unit-testable; call it only from the render thread.
 */
public class PeerRegistry {

    public static final long TIMEOUT_MS = 10_000;

    private final String selfId;
    private final Map<String, Peer> peers = new LinkedHashMap<String, Peer>();

    public PeerRegistry(String selfId) {
        this.selfId = selfId;
    }

    /** Applies one message. Returns the peer if it is new, else null. */
    public Peer onMessage(LanMsg m, long nowMs) {
        if (m == null || selfId.equals(m.id)) {
            return null;
        }
        if (m.type == LanMsg.BYE) {
            peers.remove(m.id);
            return null;
        }
        Peer p = peers.get(m.id);
        boolean isNew = p == null;
        if (isNew) {
            p = new Peer(m.id);
            peers.put(m.id, p);
        }
        p.lastSeenMs = nowMs;
        p.sameHost |= m.sameHost;
        if (m.type == LanMsg.STATE) {
            p.name = m.name;
            p.skin = m.skin;
            p.xFrac = m.xFrac;
            p.yFrac = m.yFrac;
            p.facingLeft = m.facingLeft;
            p.anim = m.anim;
        } else if (m.type == LanMsg.CHAT) {
            p.name = m.name;
            // show broadcasts and DMs addressed to me; ignore others' DMs
            if (m.target == null || m.target.isEmpty() || m.target.equals(selfId)) {
                p.bubble.show(m.text, nowMs);
            }
        }
        return isNew ? p : null;
    }

    /** Drops silent peers; returns the removed ids. */
    public List<String> prune(long nowMs) {
        List<String> removed = new ArrayList<String>();
        for (java.util.Iterator<Map.Entry<String, Peer>> it =
                peers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Peer> e = it.next();
            if (nowMs - e.getValue().lastSeenMs > TIMEOUT_MS) {
                removed.add(e.getKey());
                it.remove();
            }
        }
        return removed;
    }

    public Peer byName(String name) {
        for (Peer p : peers.values()) {
            if (p.name.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public Collection<Peer> peers() {
        return peers.values();
    }
}
