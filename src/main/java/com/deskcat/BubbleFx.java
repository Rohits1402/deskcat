package com.deskcat;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.awt.Rectangle;
import java.util.List;

/**
 * Oversized speech bubble in its own click-through overlay window — used when
 * the text (e.g. "/size 1000") can't fit inside the pet window. Sized up to
 * the screen, follows its pet, and closes itself when the bubble expires or
 * is replaced/dismissed.
 */
public class BubbleFx implements ApplicationListener {

    /** Where the bubble hangs: {pet center x, pet window top y}, per frame. */
    public interface Anchor {
        float[] pos();
    }

    private final BitmapFont font;
    private final Bubble bubble;
    private final long untilKey;
    private final Anchor anchor;
    private final Rectangle usable;
    private final float effScale;
    private final int winW, winH;

    private SpriteBatch batch;
    private OrthographicCamera cam;
    private Texture px;
    private Lwjgl3Window window;
    private float time;

    private BubbleFx(BitmapFont font, Bubble bubble, Anchor anchor,
            Rectangle usable, float effScale, int winW, int winH) {
        this.font = font;
        this.bubble = bubble;
        this.untilKey = bubble.untilMs();
        this.anchor = anchor;
        this.usable = usable;
        this.effScale = effScale;
        this.winW = winW;
        this.winH = winH;
    }

    /**
     * Must be called on the GL thread (it measures with the shared font).
     * Shrinks the requested scale only if the bubble would exceed the screen.
     */
    public static void spawn(Lwjgl3Application app, BitmapFont uiFont,
            Bubble bubble, Anchor anchor) {
        Rectangle usable = RemotePetsManager.usableScreenBounds();
        // crisp text: render with the high-res font when available, scaled
        // from its native size to the requested pixel size
        BitmapFont font = Fonts.big(uiFont);
        float targetPx = bubble.scale() * 16f;
        float scale = Fonts.hasBig() ? targetPx / Fonts.BIG_PX : bubble.scale();
        GlyphLayout layout = new GlyphLayout();
        Bubbles.Measurer m = s -> {
            layout.setText(font, s);
            return layout.width;
        };
        float maxTextW = usable.width - 60;
        float maxTextH = usable.height - 80;
        float minScale = Fonts.hasBig() ? 0.1f : 1f;
        float bw, bh;
        while (true) {
            font.getData().setScale(scale);
            List<String> lines = Bubbles.wrap(m, bubble.text(), maxTextW, 6);
            bw = 0;
            for (String line : lines) {
                bw = Math.max(bw, m.width(line));
            }
            bh = lines.size() * font.getLineHeight() + 10;
            if ((bw <= maxTextW && bh <= maxTextH) || scale <= minScale) {
                break;
            }
            scale *= 0.9f;
        }
        font.getData().setScale(1f);

        // generous margins: the draw-time wrap must never come out tighter
        // than this measurement, or words hard-split into stacked letters
        int w = (int) Math.ceil(bw) + 64;
        int h = (int) Math.ceil(bh) + 24;
        Lwjgl3WindowConfiguration cfg = new Lwjgl3WindowConfiguration();
        cfg.setTitle("DeskCat-bubble");
        cfg.setWindowedMode(w, h);
        cfg.setDecorated(false);
        cfg.setResizable(false);
        float[] p = anchor.pos();
        cfg.setWindowPosition(
                Math.round(p[0]) - w / 2,
                Math.max(usable.y, Math.round(p[1]) - h));
        app.newWindow(new BubbleFx(font, bubble, anchor, usable, scale, w, h),
                cfg);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        cam = new OrthographicCamera();
        cam.setToOrtho(false, winW, winH);
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.drawPixel(0, 0, Color.rgba8888(Color.WHITE));
        px = new Texture(pm);
        pm.dispose();
        window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
        WindowTricks.floating(window);
        WindowTricks.applyExStyles(window, true);   // click-through
    }

    @Override
    public void render() {
        long now = System.currentTimeMillis();
        if (!bubble.isActive(now) || bubble.untilMs() != untilKey) {
            window.closeWindow();   // expired, dismissed, or replaced
            return;
        }
        time += Math.min(Gdx.graphics.getDeltaTime(), 1 / 20f);

        float[] p = anchor.pos();
        int x = Math.round(p[0]) - winW / 2;
        int y = Math.round(p[1]) - winH;
        x = Math.max(usable.x, Math.min(x, usable.x + usable.width - winW));
        y = Math.max(usable.y, y);
        window.setPosition(x, y);

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        Bubbles.draw(batch, font, px, bubble.text(), bubble.alpha(now),
                winW, winH - 4f, effScale, bubble.effect(), bubble.colorHex(),
                time);
        batch.end();
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
        batch.dispose();
        px.dispose();
    }
}
