package com.deskcat.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class PeerRegistryTest {

    private LanMsg state(String id, String name) {
        return LanProtocol.decode(LanProtocol.encodeState(
                id, name, "cat", 0.5f, 0.5f, false, LanMsg.ANIM_IDLE));
    }

    @Test
    public void addsAndUpdatesPeers() {
        PeerRegistry reg = new PeerRegistry("me");
        assertNotNull(reg.onMessage(state("p1", "Ana"), 1000));   // new
        assertNull(reg.onMessage(state("p1", "Ana"), 2000));      // update
        assertEquals(1, reg.peers().size());
        assertEquals(2000, reg.peers().iterator().next().lastSeenMs);
    }

    @Test
    public void ignoresOwnMessages() {
        PeerRegistry reg = new PeerRegistry("me");
        assertNull(reg.onMessage(state("me", "Me"), 1000));
        assertTrue(reg.peers().isEmpty());
    }

    @Test
    public void prunesSilentPeers() {
        PeerRegistry reg = new PeerRegistry("me");
        reg.onMessage(state("p1", "Ana"), 1000);
        reg.onMessage(state("p2", "Bob"), 5000);
        List<String> removed = reg.prune(1000 + PeerRegistry.TIMEOUT_MS + 1);
        assertEquals(1, removed.size());
        assertEquals("p1", removed.get(0));
        assertEquals(1, reg.peers().size());
    }

    @Test
    public void byeRemovesPeer() {
        PeerRegistry reg = new PeerRegistry("me");
        reg.onMessage(state("p1", "Ana"), 1000);
        reg.onMessage(LanProtocol.decode(LanProtocol.encodeBye("p1")), 2000);
        assertTrue(reg.peers().isEmpty());
    }

    @Test
    public void broadcastChatSetsBubble() {
        PeerRegistry reg = new PeerRegistry("me");
        reg.onMessage(state("p1", "Ana"), 1000);
        reg.onMessage(LanProtocol.decode(
                LanProtocol.encodeChat("p1", "Ana", "hello", null)), 2000);
        Peer p = reg.peers().iterator().next();
        assertEquals("hello", p.bubble.text());
        assertTrue(p.bubble.isActive(2000));
        assertTrue(!p.bubble.isActive(2000 + com.deskcat.Bubble.DURATION_MS));
    }

    @Test
    public void dmForMeSetsBubble_dmForOthersDoesNot() {
        PeerRegistry reg = new PeerRegistry("me");
        reg.onMessage(state("p1", "Ana"), 1000);
        reg.onMessage(LanProtocol.decode(
                LanProtocol.encodeChat("p1", "Ana", "secret", "me")), 2000);
        assertEquals("secret", reg.byName("Ana").bubble.text());

        reg.onMessage(LanProtocol.decode(
                LanProtocol.encodeChat("p1", "Ana", "not for me", "p9")), 3000);
        assertEquals("secret", reg.byName("Ana").bubble.text());   // unchanged
    }

    @Test
    public void byNameIsCaseInsensitive() {
        PeerRegistry reg = new PeerRegistry("me");
        reg.onMessage(state("p1", "Ana"), 1000);
        assertNotNull(reg.byName("ana"));
        assertNull(reg.byName("bob"));
    }
}
