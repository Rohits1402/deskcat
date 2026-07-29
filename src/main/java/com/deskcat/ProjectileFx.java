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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * A shot pellet: a tiny click-through always-on-top window that flies in a
 * straight line across the desktop and closes on arrival.
 */
public class ProjectileFx implements ApplicationListener {

    static final int SIZE = 18;
    private static final float SPEED = 1400f;   // px/s

    private static final Color C_CORE = Color.valueOf("F9D848");
    private static final Color C_OUTLINE = Color.valueOf("26202A");

    private final float fromX, fromY, toX, toY, duration;
    private float t;
    private SpriteBatch batch;
    private OrthographicCamera cam;
    private Texture px;
    private Lwjgl3Window window;

    private ProjectileFx(float fromX, float fromY, float toX, float toY) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.duration = duration(fromX, fromY, toX, toY);
    }

    /** Flight time for a shot between two screen points. */
    public static float duration(float fromX, float fromY, float toX, float toY) {
        float dist = (float) Math.hypot(toX - fromX, toY - fromY);
        return Math.max(0.25f, Math.min(0.9f, dist / SPEED));
    }

    /** Fire from one screen point to another (coordinates are centers). */
    public static void fire(Lwjgl3Application app, float fromX, float fromY,
            float toX, float toY) {
        Lwjgl3WindowConfiguration cfg = new Lwjgl3WindowConfiguration();
        cfg.setTitle("DeskCat-shot");
        cfg.setWindowedMode(SIZE, SIZE);
        cfg.setDecorated(false);
        cfg.setResizable(false);
        cfg.setWindowPosition(Math.round(fromX) - SIZE / 2,
                Math.round(fromY) - SIZE / 2);
        app.newWindow(new ProjectileFx(fromX, fromY, toX, toY), cfg);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        cam = new OrthographicCamera();
        cam.setToOrtho(false, SIZE, SIZE);
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
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1 / 20f);
        t += dt;
        float k = Math.min(1f, t / duration);
        float x = fromX + (toX - fromX) * k;
        float y = fromY + (toY - fromY) * k;
        window.setPosition(Math.round(x) - SIZE / 2, Math.round(y) - SIZE / 2);

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        batch.setColor(C_OUTLINE);
        batch.draw(px, 4, 4, 10, 10);
        batch.setColor(C_CORE);
        batch.draw(px, 5, 5, 8, 8);
        batch.setColor(Color.WHITE);
        batch.draw(px, 7, 9, 2, 2);   // glint
        batch.end();

        if (t >= duration) {
            window.closeWindow();
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
        batch.dispose();
        px.dispose();
    }
}
