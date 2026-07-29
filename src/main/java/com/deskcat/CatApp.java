package com.deskcat;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import org.lwjgl.glfw.GLFW;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;

/**
 * A pixel pet that sits on the desktop in a transparent, always-on-top window
 * with no taskbar button; a system-tray icon hosts the Exit menu. Drag to
 * carry it (it stretches like mochi), pet its head for hearts, click it for a
 * thunderbolt (pikachu skin), leave it alone and it falls asleep.
 */
public class CatApp extends ApplicationAdapter {

    public static final int SCALE = 5;
    public static final int UNITS_W = 44;
    public static final int UNITS_H = 42;
    public static final int WIN_W_PX = UNITS_W * SCALE;
    public static final int WIN_H_PX = UNITS_H * SCALE;

    // Cat canvas (body + tail) rendered to an FBO so squash/stretch warps
    // the whole cat uniformly, eyes included.
    private static final int FBO_W = 40;
    private static final int FBO_H = 30;
    private static final int CAT_X = 2;      // FBO position in window units
    private static final int BODY_X = 2;     // body position inside the FBO

    // eye styles: 0 = iris on white patch (cat), 1 = solid bead (pikachu),
    // 2 = outlined iris block (squirtle)

    /** Selected before create(): "cat", "pikachu", or "squirtle". */
    public static String skin = "squirtle";

    // per-skin geometry and style, set in create()
    private int tailX;
    private int eyeLX, eyeRX, eyeW, eyeH, eyeY, eyeStyle;
    private float eyeCenterX, eyeCenterY;
    private Color furColor, irisColor;

    private static final Color C_OUTLINE = Color.valueOf("26202A");
    private static final Color C_ORANGE = Color.valueOf("F29A4B");
    private static final Color C_YELLOW = Color.valueOf("F9D848");
    private static final Color C_BLUE = Color.valueOf("8CCFE8");
    private static final Color C_IRIS_GREEN = Color.valueOf("7CC46B");
    private static final Color C_IRIS_BROWN = Color.valueOf("5B3A26");

    private SpriteBatch batch;
    private OrthographicCamera cam, fboCam;
    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private Texture bodyTex, heartTex, zzzTex, alertTex, sparkTex, pawTex, px;
    private Texture waterDropTex;
    private Texture[] tailTex;

    private Lwjgl3Window window;

    private float time;

    // squash & stretch spring
    private float scaleY = 1f, scaleVel = 0f;

    // dragging
    private boolean dragging;
    private int grabDX, grabDY;

    // gaze
    private float gazeX, gazeY;

    // blink
    private float blinkIn = 3f, blinkLeft = 0f;

    // tail
    private int tailFrame;
    private float tailTime;
    private static final int[] TAIL_CYCLE = {0, 1, 2, 1};

    // global cursor tracking
    private final Point cursor = new Point();
    private final Point lastCursor = new Point();
    private boolean cursorValid;
    private float cursorSpeed;
    private float idleTime;

    // moods
    private boolean sleeping;
    private float zzzIn;
    private float petFresh, petCharge, heartIn;
    private boolean petActive;
    private float alertLeft;

    // click attack: 0 = startle only (cat), 1 = thunderbolt (pikachu),
    // 2 = water gun (squirtle)
    private int attackType;
    private float boltLeft;
    private final ArrayList<float[]> bolts = new ArrayList<float[]>();
    private float waterLeft, dropIn;

    // a press becomes a drag once the cursor travels; otherwise it's a click
    private boolean pressed;
    private float pressT;
    private int pressGX, pressGY;

    private boolean trayOk;
    private TrayIcon trayIcon;

    // keyboard kneading: the global hook only bumps a counter — which keys
    // were pressed is never inspected or stored
    private final AtomicInteger keyTicks = new AtomicInteger();
    private int seenKeyTicks;
    private float typingLeft;
    private boolean kneadNow;

    // wandering along the bottom screen edge:
    // 0 = idle, 1 = falling, 2 = patrolling endlessly,
    // 3 = hurrying home along the floor, 4 = hopping up to the home spot
    private int wanderState;
    private float wanderIn = 25f;
    private float fallVy, winXf;
    private int floorY, walkTargetX;
    private int homeX, homeY, patrolMinX, patrolMaxX;
    private float returnT, leapFromY;
    private boolean facingLeft;

    private final ArrayList<Particle> particles = new ArrayList<Particle>();

    private static class Particle {
        float x, y, vx, vy, grav, life, maxLife, scale;
        Texture tex;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        cam = new OrthographicCamera();
        cam.setToOrtho(false, UNITS_W, UNITS_H);
        fboCam = new OrthographicCamera();
        fboCam.setToOrtho(false, FBO_W, FBO_H);

        if ("squirtle".equalsIgnoreCase(skin)) {
            eyeStyle = 2;
            attackType = 2;
            bodyTex = PixelArt.squirtleBody();
            tailTex = PixelArt.squirtleTail();
            tailX = 26;
            eyeLX = 11;
            eyeRX = 20;
            eyeW = 3;
            eyeH = 4;
            eyeY = 16;
            eyeCenterX = 16.5f;
            eyeCenterY = 17.5f;
            furColor = C_BLUE;
            irisColor = C_IRIS_BROWN;
            pawTex = PixelArt.fromMap(PixelArt.PAW_SQUIRT);
        } else if ("pikachu".equalsIgnoreCase(skin)) {
            eyeStyle = 1;
            attackType = 1;
            bodyTex = PixelArt.fromMap(PixelArt.PIKA_BODY);
            tailTex = new Texture[] {
                    PixelArt.fromMap(PixelArt.PIKA_TAIL_A),
                    PixelArt.fromMap(PixelArt.PIKA_TAIL_B),
                    PixelArt.fromMap(PixelArt.PIKA_TAIL_C),
            };
            tailX = 27;
            eyeLX = 9;
            eyeRX = 20;
            eyeW = 3;
            eyeH = 3;
            eyeY = 15;
            eyeCenterX = 15.5f;
            eyeCenterY = 16f;
            furColor = C_YELLOW;
            irisColor = C_IRIS_GREEN;
            pawTex = PixelArt.fromMap(PixelArt.PAW_PIKA);
        } else {
            eyeStyle = 0;
            attackType = 0;
            bodyTex = PixelArt.fromMap(PixelArt.BODY);
            tailTex = new Texture[] {
                    PixelArt.fromMap(PixelArt.TAIL_A),
                    PixelArt.fromMap(PixelArt.TAIL_B),
                    PixelArt.fromMap(PixelArt.TAIL_C),
            };
            tailX = 26;
            eyeLX = 7;
            eyeRX = 18;
            eyeW = 4;
            eyeH = 3;
            eyeY = 15;
            eyeCenterX = 14f;
            eyeCenterY = 16f;
            furColor = C_ORANGE;
            irisColor = C_IRIS_GREEN;
            pawTex = PixelArt.fromMap(PixelArt.PAW_CAT);
        }
        waterDropTex = PixelArt.fromMap(PixelArt.WATER_DROP);
        heartTex = PixelArt.fromMap(PixelArt.HEART);
        zzzTex = PixelArt.fromMap(PixelArt.ZZZ);
        alertTex = PixelArt.fromMap(PixelArt.ALERT);
        sparkTex = PixelArt.fromMap(PixelArt.SPARK);
        px = PixelArt.pixel();

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_W, FBO_H, false);
        fbo.getColorBufferTexture().setFilter(
                Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);

        window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
        GLFW.glfwSetWindowAttrib(window.getWindowHandle(),
                GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
        hideFromTaskbar();
        setupTray();
        setupKeyboardHook();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int sx, int sy, int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    if (!trayOk) {
                        Gdx.app.exit();   // fallback exit when no tray exists
                    }
                    return true;
                }
                float ux = sx / (float) SCALE;
                float uy = UNITS_H - sy / (float) SCALE;
                if (ux >= CAT_X + 1 && ux <= CAT_X + FBO_W - 4 && uy <= 28) {
                    Point p = globalCursor();
                    if (p != null) {
                        pressed = true;
                        pressT = 0f;
                        pressGX = p.x;
                        pressGY = p.y;
                        grabDX = p.x - window.getPositionX();
                        grabDY = p.y - window.getPositionY();
                        wanderState = 0;
                        wanderIn = MathUtils.random(18f, 40f);
                        wake();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchUp(int sx, int sy, int pointer, int button) {
                if (pressed && !dragging) {
                    attack();
                }
                pressed = false;
                if (dragging) {
                    dragging = false;
                    scaleVel += 3f;   // jelly wobble on release
                }
                return true;
            }

            @Override
            public boolean mouseMoved(int sx, int sy) {
                float ux = sx / (float) SCALE;
                float uy = UNITS_H - sy / (float) SCALE;
                if (!dragging && ux >= 4 && ux <= 27 && uy >= 11 && uy <= 25) {
                    petFresh = 0.25f;   // cursor is stroking the head
                }
                return false;
            }
        });
    }

    private static Point globalCursor() {
        PointerInfo pi = MouseInfo.getPointerInfo();
        return pi == null ? null : pi.getLocation();
    }

    private void wake() {
        idleTime = 0f;
        sleeping = false;
    }

    private static final int GWL_EXSTYLE = -20;
    private static final long WS_EX_TOOLWINDOW = 0x00000080L;
    private static final long WS_EX_APPWINDOW = 0x00040000L;

    /** Swap the taskbar button for tool-window style (Windows only). */
    private void hideFromTaskbar() {
        try {
            long glfwWin = window.getWindowHandle();
            long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(glfwWin);
            GLFW.glfwHideWindow(glfwWin);
            long ex = org.lwjgl.system.windows.User32.GetWindowLongPtr(hwnd, GWL_EXSTYLE);
            ex = (ex | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW;
            org.lwjgl.system.windows.User32.SetWindowLongPtr(hwnd, GWL_EXSTYLE, ex);
            GLFW.glfwShowWindow(glfwWin);
        } catch (Throwable t) {
            // non-Windows or API unavailable: the window stays in the taskbar
        }
    }

    private void setupKeyboardHook() {
        try {
            java.util.logging.Logger l = java.util.logging.Logger
                    .getLogger(GlobalScreen.class.getPackage().getName());
            l.setLevel(java.util.logging.Level.OFF);
            l.setUseParentHandlers(false);
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                public void nativeKeyPressed(NativeKeyEvent e) {
                    keyTicks.incrementAndGet();
                }

                public void nativeKeyReleased(NativeKeyEvent e) {
                }

                public void nativeKeyTyped(NativeKeyEvent e) {
                }
            });
        } catch (Throwable t) {
            // hook unavailable: kneading simply never triggers
        }
    }

    private void setupTray() {
        try {
            if (!SystemTray.isSupported()) {
                return;
            }
            PopupMenu menu = new PopupMenu();
            MenuItem exit = new MenuItem("Exit DeskCat");
            exit.addActionListener(e -> Gdx.app.postRunnable(() -> Gdx.app.exit()));
            trayIcon = new TrayIcon(trayImage(), "DeskCat", menu);
            SystemTray.getSystemTray().add(trayIcon);
            trayOk = true;
        } catch (Throwable t) {
            trayOk = false;   // right-click on the pet quits instead
        }
    }

    private static BufferedImage trayImage() {
        String[] bolt = {
                "....YYY.",
                "...YYY..",
                "..YYY...",
                ".YYYYY..",
                "...YY...",
                "..YY....",
                ".YY.....",
                "YY......",
        };
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (bolt[y].charAt(x) == 'Y') {
                    for (int dy = 0; dy < 2; dy++) {
                        for (int dx = 0; dx < 2; dx++) {
                            img.setRGB(x * 2 + dx, y * 2 + dy, 0xFFF9D848);
                        }
                    }
                }
            }
        }
        return img;
    }

    private void attack() {
        if (attackType == 0) {
            alertLeft = 0.8f;    // the tabby just gets startled by pokes
            return;
        }
        wake();
        scaleVel += 5f;          // excited hop
        if (attackType == 1) {
            boltLeft = 0.7f;
            bolts.clear();
            for (int i = 0; i < 3; i++) {
                bolts.add(makeBolt(
                        CAT_X + eyeCenterX + MathUtils.random(-3f, 3f)));
            }
            sparkBurst(CAT_X + 15, 13f, 10, sparkTex, 14f, 6f, 13f);
        } else {
            waterLeft = 0.85f;   // eyes shut, spray until the timer runs out
            dropIn = 0f;
        }
    }

    private void sparkBurst(float cx, float cy, int count, Texture tex,
            float speed, float rMin, float rMax) {
        for (int i = 0; i < count; i++) {
            Particle s = new Particle();
            s.tex = tex;
            float ang = MathUtils.random(MathUtils.PI2);
            float r = MathUtils.random(rMin, rMax);
            s.x = cx + MathUtils.cos(ang) * r;
            s.y = cy + MathUtils.sin(ang) * r * 0.8f;
            s.vx = MathUtils.cos(ang) * speed;
            s.vy = MathUtils.sin(ang) * speed;
            s.maxLife = MathUtils.random(0.25f, 0.5f);
            s.scale = MathUtils.randomBoolean() ? 1f : 1.5f;
            particles.add(s);
        }
    }

    /** Jagged strike from the top of the window down onto the head. */
    private float[] makeBolt(float targetX) {
        int steps = 16;
        float[] pts = new float[steps * 2];
        float x = targetX + MathUtils.random(-12f, 12f);
        float y = UNITS_H - 1;
        float drop = (UNITS_H - 1 - 24f) / steps;
        for (int i = 0; i < steps; i++) {
            pts[i * 2] = x;
            pts[i * 2 + 1] = y;
            x += (targetX - x) * 0.25f + MathUtils.random(-2f, 2f);
            y -= drop;
        }
        return pts;
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1 / 20f);
        time += dt;

        pollCursor(dt);
        updateMood(dt);
        updateWander(dt);
        updateSpring(dt);
        updateBlink(dt);
        updateTail(dt);
        updateParticles(dt);

        renderCatToFbo();
        renderWindow();
    }

    private void pollCursor(float dt) {
        Point p = globalCursor();
        if (p == null) {
            cursorSpeed = 0;
            return;
        }
        if (cursorValid) {
            float d = (float) p.distance(lastCursor);
            cursorSpeed = MathUtils.lerp(cursorSpeed, d / dt, 0.5f);
        }
        lastCursor.setLocation(cursor);
        cursor.setLocation(p);
        cursorValid = true;

        if (cursorSpeed > 40f || dragging) {
            wake();
        } else {
            idleTime += dt;
        }

        // gaze from eye center (screen coords, y down) toward the cursor
        float eyeScrX = window.getPositionX() + (CAT_X + eyeCenterX) * SCALE;
        float eyeScrY = window.getPositionY() + (UNITS_H - eyeCenterY) * SCALE;
        gazeX = MathUtils.clamp((cursor.x - eyeScrX) / 240f, -1f, 1f);
        gazeY = MathUtils.clamp((eyeScrY - cursor.y) / 240f, -1f, 1f);

        // startled by a cursor rushing past nearby
        float dx = cursor.x - eyeScrX, dy = cursor.y - eyeScrY;
        if (!dragging && !sleeping && cursorSpeed > 2200f
                && dx * dx + dy * dy < 500f * 500f && alertLeft <= 0f) {
            alertLeft = 0.8f;
        }

        if (pressed && !dragging) {
            pressT += dt;
            boolean moved = Math.abs(cursor.x - pressGX)
                    + Math.abs(cursor.y - pressGY) > 4;
            if (moved || pressT > 0.35f) {
                dragging = true;
            }
        }

        if (dragging) {
            window.setPosition(cursor.x - grabDX, cursor.y - grabDY);
        }
    }

    private void updateMood(float dt) {
        alertLeft -= dt;
        boltLeft -= dt;
        waterLeft -= dt;
        typingLeft -= dt;

        if (waterLeft > 0.1f) {
            dropIn -= dt;
            while (dropIn <= 0f) {
                dropIn += 0.04f;
                Particle d = new Particle();
                d.tex = waterDropTex;
                d.x = CAT_X + eyeCenterX + MathUtils.random(-1f, 1f);
                d.y = 15f;
                d.vx = MathUtils.random(-7f, 7f);
                d.vy = MathUtils.random(36f, 50f);
                d.grav = -70f;    // droplets arc up and fall back down
                d.maxLife = 1.1f;
                d.scale = MathUtils.randomBoolean() ? 1f : 1.4f;
                particles.add(d);
            }
        }

        int kc = keyTicks.get();
        if (kc != seenKeyTicks) {
            seenKeyTicks = kc;
            typingLeft = 0.55f;
            if (wanderState == 1 || wanderState == 2) {
                beginReturn();   // scurry home, knead once it gets there
            }
            wake();
        }
        kneadNow = typingLeft > 0f && !dragging && wanderState == 0
                && boltLeft <= 0f && waterLeft <= 0f && !sleeping;

        petFresh -= dt;
        if (petFresh > 0f) {
            petCharge = Math.min(petCharge + dt, 2f);
            wake();
        } else {
            petCharge = Math.max(0f, petCharge - dt * 2f);
        }
        petActive = petCharge > 0.35f;

        heartIn -= dt;
        if (petActive && heartIn <= 0f) {
            Particle h = new Particle();
            h.tex = heartTex;
            h.x = CAT_X + 9 + MathUtils.random(0f, 12f);
            h.y = 27f;
            h.vx = MathUtils.random(-1.5f, 1.5f);
            h.vy = MathUtils.random(6f, 8f);
            h.maxLife = 1.5f;
            h.scale = MathUtils.randomBoolean() ? 1f : 0.7f;
            particles.add(h);
            heartIn = 0.35f;
        }

        if (!sleeping && idleTime > 60f && !dragging && !petActive
                && wanderState == 0 && typingLeft <= 0f) {
            sleeping = true;
        }
        if (sleeping) {
            zzzIn -= dt;
            if (zzzIn <= 0f) {
                Particle z = new Particle();
                z.tex = zzzTex;
                z.x = CAT_X + 26;
                z.y = 26f;
                z.vx = 1.4f;
                z.vy = 3.5f;
                z.maxLife = 2.2f;
                z.scale = MathUtils.randomBoolean() ? 1f : 1.5f;
                particles.add(z);
                zzzIn = 1.6f;
            }
        }
    }

    private void updateWander(float dt) {
        // any user input sends it hurrying back to where it started
        if ((wanderState == 1 || wanderState == 2)
                && (cursorSpeed > 40f || typingLeft > 0f)) {
            beginReturn();
        }

        if (wanderState == 0) {
            boolean eligible = !sleeping && !dragging && !pressed && !petActive
                    && boltLeft <= 0f && waterLeft <= 0f && typingLeft <= 0f
                    && idleTime > 3f;
            if (eligible) {
                wanderIn -= dt;
                if (wanderIn <= 0f) {
                    startWander();
                }
            }
        } else if (wanderState == 1) {
            fallVy += 2600f * dt;
            float ny = window.getPositionY() + fallVy * dt;
            if (ny >= floorY) {
                ny = floorY;
                wanderState = 2;
                scaleVel -= 5f;   // landing squash
                winXf = window.getPositionX();
            }
            window.setPosition(window.getPositionX(), Math.round(ny));
        } else if (wanderState == 2) {
            float dir = walkTargetX < winXf ? -1f : 1f;
            facingLeft = dir < 0f;
            winXf += dir * 85f * dt;
            window.setPosition(Math.round(winXf), floorY);
            if (Math.abs(winXf - walkTargetX) < 4f) {
                // turn around at the screen edge and keep patrolling
                walkTargetX = walkTargetX <= patrolMinX + 4
                        ? patrolMaxX : patrolMinX;
            }
        } else if (wanderState == 3) {
            float dir = homeX < winXf ? -1f : 1f;
            facingLeft = dir < 0f;
            winXf += dir * 140f * dt;
            boolean arrived = dir < 0f ? winXf <= homeX : winXf >= homeX;
            if (arrived) {
                winXf = homeX;
                wanderState = 4;
                returnT = 0f;
                leapFromY = window.getPositionY();
                facingLeft = false;
            }
            window.setPosition(Math.round(winXf), window.getPositionY());
        } else if (wanderState == 4) {
            returnT += dt;
            float t = Math.min(1f, returnT / 0.35f);
            float ease = 1f - (1f - t) * (1f - t);
            window.setPosition(homeX,
                    Math.round(MathUtils.lerp(leapFromY, homeY, ease)));
            if (t >= 1f) {
                wanderState = 0;
                wanderIn = MathUtils.random(18f, 40f);
                scaleVel += 2f;   // little settle wobble back on its perch
            }
        }
    }

    /** Remember home, then head for the bottom edge of the primary screen. */
    private void startWander() {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            Rectangle b = gc.getBounds();
            Insets ins = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            floorY = b.y + b.height - ins.bottom - WIN_H_PX;
            homeX = window.getPositionX();
            homeY = window.getPositionY();
            patrolMinX = b.x + 10;
            patrolMaxX = b.x + b.width - WIN_W_PX - 10;
            walkTargetX = MathUtils.randomBoolean() ? patrolMinX : patrolMaxX;
            winXf = homeX;
            facingLeft = walkTargetX < winXf;
            fallVy = 0f;
            if (homeY < floorY - 4) {
                wanderState = 1;
            } else {
                wanderState = 2;
                window.setPosition(Math.round(winXf), floorY);
            }
        } catch (Throwable t) {
            wanderIn = MathUtils.random(18f, 40f);
        }
    }

    private void beginReturn() {
        wanderState = 3;
        winXf = window.getPositionX();
    }

    private void updateSpring(float dt) {
        float target = dragging ? 1.2f
                : (wanderState == 1 ? 1.12f          // stretch while falling
                : (wanderState == 4 ? 1.08f : 1f));  // and while hopping home
        scaleVel += (target - scaleY) * 160f * dt;
        scaleVel -= scaleVel * 12f * dt;
        scaleY += scaleVel * dt;
        scaleY = MathUtils.clamp(scaleY, 0.7f, 1.5f);
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
        float interval = sleeping ? 0.8f
                : (alertLeft > 0f ? 0.12f
                : (wanderState == 2 || wanderState == 3 ? 0.15f : 0.3f));
        tailTime += dt;
        if (tailTime >= interval) {
            tailTime = 0f;
            tailFrame = (tailFrame + 1) % TAIL_CYCLE.length;
        }
    }

    private void updateParticles(float dt) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext();) {
            Particle p = it.next();
            p.life += dt;
            if (p.life >= p.maxLife) {
                it.remove();
                continue;
            }
            p.vy += p.grav * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }

    private void renderCatToFbo() {
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(fboCam.combined);
        batch.begin();

        float shake = petActive ? MathUtils.sin(time * 45f) * 0.25f : 0f;

        Texture tail = tailTex[TAIL_CYCLE[tailFrame]];
        batch.draw(tail, tailX + shake, 0, tail.getWidth(), tail.getHeight());
        batch.draw(bodyTex, BODY_X + shake, 0,
                bodyTex.getWidth(), bodyTex.getHeight());

        // determined attack face while the bolt flies
        if (attackType == 1 && boltLeft > 0f && MathUtils.sin(time * 40f) > 0f) {
            batch.setColor(1f, 1f, 0.75f, 1f);
            batch.draw(px, BODY_X + 3 + shake, 11, 3, 3);     // cheeks spark
            batch.draw(px, BODY_X + 22 + shake, 11, 3, 3);
            batch.setColor(Color.WHITE);
        }

        boolean closed = sleeping || blinkLeft > 0f || boltLeft > 0.15f
                || waterLeft > 0.2f;
        if (closed) {
            drawClosedEye(eyeLX + shake);
            drawClosedEye(eyeRX + shake);
        } else {
            int pgx = gazeX > 0.3f ? 1 : (gazeX < -0.3f ? -1 : 0);
            if ((wanderState == 2 || wanderState == 3) && facingLeft) {
                pgx = -pgx;   // the whole FBO is mirrored while walking left
            }
            int irisY = kneadNow || gazeY < -0.25f ? eyeY : eyeY + 1;
            drawOpenEye(eyeLX + shake, pgx, irisY);
            drawOpenEye(eyeRX + shake, pgx, irisY);
        }

        if (kneadNow) {
            boolean leftUp = MathUtils.sin(time * 14f) > 0f;
            batch.draw(pawTex, 9 + shake, leftUp ? 1 : 0, 4, 4);
            batch.draw(pawTex, 18 + shake, leftUp ? 0 : 1, 4, 4);
        }

        batch.end();
        fbo.end();
    }

    private void drawOpenEye(float eyeX, int pgx, int irisY) {
        float gazeDown = kneadNow ? -1f : gazeY;   // watch the paws knead
        if (eyeStyle == 0) {
            float irisX = eyeX + 1 + pgx;
            batch.setColor(irisColor);
            batch.draw(px, irisX, irisY, 2, 2);
            batch.setColor(C_OUTLINE);
            if (alertLeft > 0f) {
                batch.draw(px, irisX, irisY, 2, 2);   // wide startled pupils
            } else {
                float pupilX = irisX + (pgx >= 0 ? 1 : 0);
                float pupilY = irisY + (gazeDown >= 0f ? 1 : 0);
                batch.draw(px, pupilX, pupilY, 1, 1);
            }
        } else if (eyeStyle == 1) {
            // solid bead eye; the white glint wanders with the gaze and
            // vanishes while startled
            batch.setColor(C_OUTLINE);
            batch.draw(px, eyeX, eyeY, eyeW, eyeH);
            if (alertLeft <= 0f) {
                batch.setColor(Color.WHITE);
                float glintX = eyeX + 1 + pgx;
                float glintY = gazeDown >= 0f ? eyeY + eyeH - 1 : eyeY + eyeH - 2;
                batch.draw(px, glintX, glintY, 1, 1);
            }
        } else {
            // big outlined iris block with a tall glint
            batch.setColor(C_OUTLINE);
            batch.draw(px, eyeX - 1, eyeY - 1, eyeW + 2, eyeH + 2);
            batch.setColor(alertLeft > 0f ? C_OUTLINE : irisColor);
            batch.draw(px, eyeX, eyeY, eyeW, eyeH);
            if (alertLeft <= 0f) {
                batch.setColor(Color.WHITE);
                float glintX = eyeX + Math.max(0, Math.min(eyeW - 1, 1 + pgx));
                float glintY = gazeDown >= 0f ? eyeY + eyeH - 2 : eyeY + eyeH - 3;
                batch.draw(px, glintX, glintY, 1, 2);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawClosedEye(float eyeX) {
        if (eyeStyle == 2) {
            batch.setColor(furColor);
            batch.draw(px, eyeX - 1, eyeY - 1, eyeW + 2, eyeH + 2);
            batch.setColor(C_OUTLINE);
            batch.draw(px, eyeX - 1, eyeY + eyeH / 2, eyeW + 2, 1);
        } else {
            batch.setColor(furColor);
            batch.draw(px, eyeX, eyeY, eyeW, eyeH);
            batch.setColor(C_OUTLINE);
            batch.draw(px, eyeX, eyeY + 1, eyeW, 1);
        }
        batch.setColor(Color.WHITE);
    }

    private void renderWindow() {
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        float scaleX = 1f - (scaleY - 1f) * 0.55f;
        float bob = 0f, waddle = 0f;
        if (wanderState == 2 || wanderState == 3) {
            float rate = wanderState == 3 ? 12f : 9f;   // hurried gait going home
            bob = Math.abs(MathUtils.sin(time * rate)) * 0.8f;
            waddle = MathUtils.sin(time * rate) * 3f;
            if (facingLeft) {
                scaleX = -scaleX;
            }
        }
        batch.draw(fboRegion, CAT_X, bob, FBO_W / 2f, 0,
                FBO_W, FBO_H, scaleX, scaleY, waddle);

        if (boltLeft > 0f && MathUtils.sin(time * 55f) > -0.4f) {
            float a = MathUtils.clamp(boltLeft / 0.3f, 0f, 1f);
            for (float[] b : bolts) {
                batch.setColor(1f, 0.92f, 0.35f, a);
                for (int i = 0; i < b.length; i += 2) {
                    batch.draw(px, b[i] - 1f, b[i + 1] - 1f, 2f, 2f);
                }
                batch.setColor(1f, 1f, 0.9f, a);
                for (int i = 0; i < b.length; i += 2) {
                    batch.draw(px, b[i] - 0.5f, b[i + 1] - 0.5f, 1f, 1f);
                }
            }
            batch.setColor(Color.WHITE);
        }

        for (Particle p : particles) {
            float fade = MathUtils.clamp(
                    (p.maxLife - p.life) / 0.5f, 0f, 1f);
            batch.setColor(1f, 1f, 1f, fade);
            batch.draw(p.tex, p.x, p.y,
                    p.tex.getWidth() * p.scale, p.tex.getHeight() * p.scale);
        }
        batch.setColor(Color.WHITE);

        if (alertLeft > 0f) {
            float alertBob = Math.abs(MathUtils.sin(time * 14f)) * 1.5f;
            batch.draw(alertTex, CAT_X + 13, 28 + alertBob,
                    alertTex.getWidth(), alertTex.getHeight());
        }

        batch.end();
    }

    @Override
    public void dispose() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (Throwable ignored) {
        }
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Throwable ignored) {
            }
        }
        batch.dispose();
        fbo.dispose();
        bodyTex.dispose();
        for (Texture t : tailTex) {
            t.dispose();
        }
        heartTex.dispose();
        zzzTex.dispose();
        alertTex.dispose();
        sparkTex.dispose();
        pawTex.dispose();
        waterDropTex.dispose();
        px.dispose();
    }
}
