package com.deskcat.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LanProtocolTest {

    @Test
    public void stateRoundTrip() {
        String raw = LanProtocol.encodeState("id-1", "Rajat", "squirtle",
                0.25f, 0.75f, true, LanMsg.ANIM_WALK);
        LanMsg m = LanProtocol.decode(raw);
        assertNotNull(m);
        assertEquals(LanMsg.STATE, m.type);
        assertEquals("id-1", m.id);
        assertEquals("Rajat", m.name);
        assertEquals("squirtle", m.skin);
        assertEquals(0.25f, m.xFrac, 1e-6);
        assertEquals(0.75f, m.yFrac, 1e-6);
        assertTrue(m.facingLeft);
        assertEquals(LanMsg.ANIM_WALK, m.anim);
    }

    @Test
    public void chatRoundTripWithEscaping() {
        String nasty = "hi | there \\ friend |\\| ok";
        String raw = LanProtocol.encodeChat("id-2", "A|B\\C", nasty, "peer-9");
        LanMsg m = LanProtocol.decode(raw);
        assertNotNull(m);
        assertEquals(LanMsg.CHAT, m.type);
        assertEquals("A|B\\C", m.name);
        assertEquals(nasty, m.text);
        assertEquals("peer-9", m.target);
    }

    @Test
    public void broadcastChatHasEmptyTarget() {
        LanMsg m = LanProtocol.decode(
                LanProtocol.encodeChat("id", "n", "hello", null));
        assertNotNull(m);
        assertEquals("", m.target);
    }

    @Test
    public void chatStyleRoundTrip() {
        LanMsg m = LanProtocol.decode(LanProtocol.encodeChat(
                "id", "n", "hey", null, 2.5f, 1, "E5312E"));
        assertNotNull(m);
        assertEquals(2.5f, m.chatScale, 1e-6);
        assertEquals(1, m.chatEffect);
        assertEquals("E5312E", m.chatColor);
    }

    @Test
    public void legacyChatWithoutStyleGetsDefaults() {
        LanMsg m = LanProtocol.decode("DC1|C|id|n|hello|");
        assertNotNull(m);
        assertEquals(1f, m.chatScale, 1e-6);
        assertEquals(0, m.chatEffect);
        assertEquals("", m.chatColor);
    }

    @Test
    public void actionRoundTrip() {
        LanMsg m = LanProtocol.decode(LanProtocol.encodeAction(
                "id-4", "Ana", "shoot", "id-9"));
        assertNotNull(m);
        assertEquals(LanMsg.ACTION, m.type);
        assertEquals("shoot", m.action);
        assertEquals("id-9", m.target);

        LanMsg all = LanProtocol.decode(LanProtocol.encodeAction(
                "id-4", "Ana", "shoot", null));
        assertNotNull(all);
        assertEquals("", all.target);
    }

    @Test
    public void byeRoundTrip() {
        LanMsg m = LanProtocol.decode(LanProtocol.encodeBye("id-3"));
        assertNotNull(m);
        assertEquals(LanMsg.BYE, m.type);
        assertEquals("id-3", m.id);
    }

    @Test
    public void fractionsAreClamped() {
        LanMsg m = LanProtocol.decode(LanProtocol.encodeState(
                "id", "n", "cat", -3f, 42f, false, 0));
        assertNotNull(m);
        assertEquals(0f, m.xFrac, 1e-6);
        assertEquals(1f, m.yFrac, 1e-6);
        assertFalse(m.facingLeft);
    }

    @Test
    public void rejectsGarbage() {
        assertNull(LanProtocol.decode(null));
        assertNull(LanProtocol.decode(""));
        assertNull(LanProtocol.decode("hello world"));
        assertNull(LanProtocol.decode("XX9|S|id|n|cat|0|0|0|0"));   // wrong magic
        assertNull(LanProtocol.decode("DC1|S|id|n|cat|zero|0|0|0")); // bad float
        assertNull(LanProtocol.decode("DC1|S|id"));                  // truncated
        assertNull(LanProtocol.decode("DC1|Z|id"));                  // unknown type
        assertNull(LanProtocol.decode("DC1|S||n|cat|0|0|0|0"));      // empty id
    }
}
