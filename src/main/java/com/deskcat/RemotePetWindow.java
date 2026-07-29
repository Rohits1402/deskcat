package com.deskcat;

import java.awt.Rectangle;
import java.util.function.Consumer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import com.deskcat.net.LanMsg;
import com.deskcat.net.Peer;

/**
 * View-only window for one remote peer's pet: same size and tricks as the
 * local pet window but click-through. Reads the live {@link Peer} the render
 * thread keeps updated; textures/font are shared with the main window (GL
 * contexts are shared), only the SpriteBatch is per-window.
 */
public class RemotePetWindow implements ApplicationListener {

    private static final Color C_OUTLINE = Color.valueOf("26202A");

    private final Peer peer;
    private final BitmapFont font;
    private final Texture px;
    private final Rectangle usable;
    private final Consumer<String> dmSender;

    private SpriteBatch batch;
    private OrthographicCamera unitCam, pxCam;
    private Lwjgl3Window window;
    private final GlyphLayout layout = new GlyphLayout();

    private float time, winXf = -1, winYf;
    private int lastScale;
    private boolean clickThrough;
    private float blinkIn = 3f, blinkLeft;

    // fade in on appearance; stay slightly translucent so remote pets read
    // as visitors rather than locals
    private float appear;
    private static final float BASE_ALPHA = 0.9f;
    private int tailFrame;
    private float tailTime;
    private static final int[] TAIL_CYCLE = {0, 1, 2, 1};

    public RemotePetWindow(Peer peer, BitmapFont font, Texture px,
            Rectangle usable, Consumer<String> dmSender) {
        this.peer = peer;
        this.font = font;
        this.px = px;
        this.usable = usable;
        this.dmSender = dmSender;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        unitCam = new OrthographicCamera();
        unitCam.setToOrtho(false, CatApp.UNITS_W, CatApp.UNITS_H);
        pxCam = new OrthographicCamera();
        pxCam.setToOrtho(false, CatApp.winW(), CatApp.winH());
        window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
        lastScale = CatApp.scale;
        WindowTricks.floating(window);
        // not click-through: right-click hosts the message/dismiss menu
        WindowTricks.applyExStyles(window, false);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int sx, int sy, int pointer, int button) {
                if (button != Input.Buttons.RIGHT) {
                    return false;
                }
                String name = peer.name == null || peer.name.isEmpty()
                        ? "them" : peer.name;
                PetMenu.show(window.getPositionX() + sx,
                        window.getPositionY() + sy,
                        new PetMenu.Item("Message " + name + "…", () ->
                                ChatInput.show(window.getPositionX(),
                                        window.getPositionY(),
                                        text -> Gdx.app.postRunnable(() ->
                                                dmSender.accept(text)))),
                        new PetMenu.Item("Dismiss bubble", () ->
                                Gdx.app.postRunnable(peer.bubble::clear)));
                return true;
            }
        });
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1 / 20f);
        time += dt;
        appear = Math.min(1f, appear + dt / 0.4f);
        float ga = appear * BASE_ALPHA;

        if (lastScale != CatApp.scale) {
            lastScale = CatApp.scale;
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(window.getWindowHandle(),
                    CatApp.winW(), CatApp.winH());
            pxCam.setToOrtho(false, CatApp.winW(), CatApp.winH());
        }

        moveWindow(dt);
        updateBlink(dt);
        updateTail(dt);

        SkinAssets a = SkinAssets.get(peer.skin);
        boolean walking = peer.anim == LanMsg.ANIM_WALK;
        boolean sleeping = peer.anim == LanMsg.ANIM_SLEEP;

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(unitCam.combined);
        batch.begin();

        float bob = walking ? Math.abs(MathUtils.sin(time * 9f)) * 0.8f : 0f;
        float stretch = peer.anim == LanMsg.ANIM_DRAG ? 1.15f : 1f;
        float sx = peer.facingLeft ? -1f : 1f;

        // body + tail drawn mirrored around the window center when facing left
        float cx = CatApp.UNITS_W / 2f;
        batch.setColor(1f, 1f, 1f, ga);
        Texture tail = a.tailTex[TAIL_CYCLE[tailFrame]];
        drawMirrored(tail, cx, a.tailX + 2 - cx, bob, sx, stretch,
                tail.getWidth(), tail.getHeight());
        drawMirrored(a.bodyTex, cx, 4 - cx, bob, sx, stretch,
                a.bodyTex.getWidth(), a.bodyTex.getHeight());

        boolean closed = sleeping || blinkLeft > 0f;
        drawEye(a, a.eyeLX + 2, closed, bob, sx, cx, ga);
        drawEye(a, a.eyeRX + 2, closed, bob, sx, cx, ga);

        batch.end();

        // name label (and bubble) in pixel space
        batch.setProjectionMatrix(pxCam.combined);
        batch.begin();
        String label = peer.name == null || peer.name.isEmpty() ? "?" : peer.name;
        layout.setText(font, label);
        float lx = (CatApp.winW() - layout.width) / 2f;
        float ly = CatApp.winH() - 4;
        batch.setColor(0f, 0f, 0f, 0.45f * appear);
        batch.draw(px, lx - 4, ly - layout.height - 4, layout.width + 8,
                layout.height + 8);
        batch.setColor(Color.WHITE);
        font.setColor(1f, 1f, 1f, appear);
        font.draw(batch, label, lx, ly);
        font.setColor(Color.WHITE);

        long now = System.currentTimeMillis();
        if (peer.bubble.isActive(now)) {
            Bubbles.draw(batch, font, px, peer.bubble.text(),
                    peer.bubble.alpha(now) * appear,
                    CatApp.winW(), ly - layout.height - 10);
        }
        batch.end();
    }

    private void drawMirrored(Texture tex, float cx, float relX, float bob,
            float sx, float stretch, float w, float h) {
        float x = sx > 0 ? cx + relX : cx - relX - w;
        batch.draw(tex, x, bob, w / 2f, 0f, w, h, 1f, stretch, 0f,
                0, 0, tex.getWidth(), tex.getHeight(), sx < 0, false);
    }

    private void drawEye(SkinAssets a, float eyeX, boolean closed, float bob,
            float sx, float cx, float ga) {
        float x = sx > 0 ? eyeX : 2 * cx - eyeX - a.eyeW;
        if (closed) {
            tint(a.furColor, ga);
            batch.draw(px, x, bob + a.eyeY, a.eyeW, a.eyeH);
            tint(C_OUTLINE, ga);
            batch.draw(px, x, bob + a.eyeY + 1, a.eyeW, 1);
        } else if (a.eyeStyle == 0) {
            tint(a.irisColor, ga);
            batch.draw(px, x + 1, bob + a.eyeY + 1, 2, 2);
            tint(C_OUTLINE, ga);
            batch.draw(px, x + 1, bob + a.eyeY + 1, 1, 1);
        } else if (a.eyeStyle == 1) {
            tint(C_OUTLINE, ga);
            batch.draw(px, x, bob + a.eyeY, a.eyeW, a.eyeH);
            batch.setColor(1f, 1f, 1f, ga);
            batch.draw(px, x + 1, bob + a.eyeY + a.eyeH - 1, 1, 1);
        } else {
            tint(C_OUTLINE, ga);
            batch.draw(px, x - 1, bob + a.eyeY - 1, a.eyeW + 2, a.eyeH + 2);
            tint(a.irisColor, ga);
            batch.draw(px, x, bob + a.eyeY, a.eyeW, a.eyeH);
            batch.setColor(1f, 1f, 1f, ga);
            batch.draw(px, x + 1, bob + a.eyeY + a.eyeH - 2, 1, 2);
        }
        batch.setColor(Color.WHITE);
    }

    private void tint(Color c, float alpha) {
        batch.setColor(c.r, c.g, c.b, alpha);
    }

    private void moveWindow(float dt) {
        float tx = usable.x + peer.xFrac * Math.max(1, usable.width - CatApp.winW());
        float ty = usable.y + peer.yFrac * Math.max(1, usable.height - CatApp.winH());
        if (winXf < 0 || Math.abs(tx - winXf) > usable.width / 2f) {
            // first placement, or the peer wrapped around a screen edge —
            // snap instead of sliding across the whole desktop
            winXf = tx;
            winYf = ty;
        } else {
            // light easing over the 60 Hz state stream smooths packet jitter
            float k = Math.min(1f, dt * 30f);
            winXf += (tx - winXf) * k;
            winYf += (ty - winYf) * k;
        }
        window.setPosition(Math.round(winXf), Math.round(winYf));

        // while overlapping the local pet, go click-through so it stays
        // draggable underneath; clickable again once apart
        int w = CatApp.winW(), h = CatApp.winH();
        int x = Math.round(winXf), y = Math.round(winYf);
        boolean overlap = x < CatApp.mainWinX + w && x + w > CatApp.mainWinX
                && y < CatApp.mainWinY + h && y + h > CatApp.mainWinY;
        if (overlap != clickThrough) {
            clickThrough = overlap;
            WindowTricks.setClickThrough(window, overlap);
        }
    }

    private void updateBlink(float dt) {
        if (blinkLeft > 0f) {
            blinkLeft -= dt;
            return;
        }
        blinkIn -= dt;
        if (blinkIn <= 0f) {
            blinkLeft = 0.12f;
            blinkIn = MathUtils.random(2.5f, 6f);
        }
    }

    private void updateTail(float dt) {
        tailTime += dt;
        float interval = peer.anim == LanMsg.ANIM_SLEEP ? 0.8f
                : (peer.anim == LanMsg.ANIM_WALK ? 0.15f : 0.3f);
        if (tailTime >= interval) {
            tailTime = 0f;
            tailFrame = (tailFrame + 1) % TAIL_CYCLE.length;
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        batch.dispose();   // textures and font are shared — main window owns them
    }
}
