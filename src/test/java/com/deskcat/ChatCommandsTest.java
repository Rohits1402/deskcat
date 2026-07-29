package com.deskcat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChatCommandsTest {

    @Test
    public void plainTextPassesThrough() {
        ChatCommands.Parsed p = ChatCommands.parse("hello world");
        assertEquals("hello world", p.text);
        assertEquals(1f, p.scale, 0f);
        assertEquals(ChatCommands.EFFECT_NONE, p.effect);
        assertEquals("", p.colorHex);
    }

    @Test
    public void bigHugeSmall() {
        assertEquals(1.6f, ChatCommands.parse("/big hi").scale, 0f);
        assertEquals(2.2f, ChatCommands.parse("/huge hi").scale, 0f);
        assertEquals(0.65f, ChatCommands.parse("/small hi").scale, 0f);
        assertEquals("hi", ChatCommands.parse("/big hi").text);
    }

    @Test
    public void sizeIsFontPxAndClamped() {
        assertEquals(50f / 16f, ChatCommands.parse("/size 50 hi").scale, 1e-4);
        assertEquals("hi", ChatCommands.parse("/size 50 hi").text);
        assertEquals(ChatCommands.MAX_SCALE,
                ChatCommands.parse("/size 500 hi").scale, 0f);
        assertEquals(ChatCommands.MIN_SCALE,
                ChatCommands.parse("/size 1 hi").scale, 0f);
    }

    @Test
    public void effects() {
        assertEquals(ChatCommands.EFFECT_SHAKE,
                ChatCommands.parse("/shake hi").effect);
        assertEquals(ChatCommands.EFFECT_RAINBOW,
                ChatCommands.parse("/rainbow hi").effect);
    }

    @Test
    public void namedColors() {
        ChatCommands.Parsed p = ChatCommands.parse("/color red hi");
        assertEquals("E5312E", p.colorHex);
        assertEquals("hi", p.text);
        assertEquals("2E6BE5", ChatCommands.parse("/colour blue hi").colorHex);
    }

    @Test
    public void commandsChain() {
        ChatCommands.Parsed p = ChatCommands.parse("/big /shake /color red hello");
        assertEquals("hello", p.text);
        assertEquals(1.6f, p.scale, 0f);
        assertEquals(ChatCommands.EFFECT_SHAKE, p.effect);
        assertEquals("E5312E", p.colorHex);
    }

    @Test
    public void caseInsensitive() {
        assertEquals(1.6f, ChatCommands.parse("/BIG hi").scale, 0f);
        assertEquals("E5312E", ChatCommands.parse("/Color RED hi").colorHex);
    }

    @Test
    public void malformedCommandsStayAsText() {
        assertEquals("/size nan hi", ChatCommands.parse("/size nan hi").text);
        assertEquals("/color neon hi", ChatCommands.parse("/color neon hi").text);
        assertEquals("/dance hi", ChatCommands.parse("/dance hi").text);
    }

    @Test
    public void commandWithNoTextGivesEmpty() {
        assertEquals("", ChatCommands.parse("/big").text);
        assertEquals("", ChatCommands.parse("/big ").text);
    }
}
