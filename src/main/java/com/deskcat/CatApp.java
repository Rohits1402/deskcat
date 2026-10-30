package com.deskcat;

import java.awt.CheckboxMenuItem;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Menu;
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.deskcat.net.LanClient;
import com.deskcat.net.LanMsg;
import com.deskcat.net.LanProtocol;
import com.deskcat.net.Peer;
import com.deskcat.net.PeerRegistry;

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
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.BufferUtils;

/**
 * A pixel pet that sits on the desktop in a transparent, always-on-top window
 * with no taskbar button; a system-tray icon hosts the Exit menu. Drag to
 * carry it (it dangles and swings from the cursor), pet its head for hearts, click it for a
 * thunderbolt (pikachu skin), leave it alone and it falls asleep.
 */
public class CatApp extends ApplicationAdapter {

    /** Keep in sync with the fatJar version in build.gradle. */
    public static final String VERSION = "1.8.0";

    /** Pixels per world unit; adjustable from the tray (Small/Normal/Large). */
    public static volatile int scale = 3;
    /**
     * Extra gap kept above the work-area bottom while patrolling.
     * -1 = auto: screen insets cover a normal taskbar, and an auto-hide
     * taskbar (insets 0) falls back to the measured tray height.
     */
    public static volatile int bottomGap = -1;
    public static final int UNITS_W = 44;
    public static final int UNITS_H = 42;

    public static int winW() {
        return UNITS_W * scale;
    }

    public static int winH() {
        return UNITS_H * scale;
    }

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

    /** Display name shown to LAN peers; set by the launcher. */
    public static String userName = System.getProperty("user.name", "DeskCat");

    /** Live main-window position, read by remote windows for overlap checks. */
    public static volatile int mainWinX, mainWinY;

    /** Opacity of remote peers' pets (tray → Peer fade). */
    public static volatile float peerAlpha = 0.9f;

    // per-skin geometry and style, set in create()
    private int tailX;
    private int eyeLX, eyeRX, eyeW, eyeH, eyeY, eyeStyle;
    private float eyeCenterX, eyeCenterY;
    private Color furColor, irisColor, lineColor;

    private static final Color C_OUTLINE = Color.valueOf("26202A");
    private static final Color C_PUPIL = Color.valueOf("120F16");
    private static final Color C_ORANGE = Color.valueOf("F29A4B");
    private static final Color C_YELLOW = Color.valueOf("F9D848");
    private static final Color C_BLUE = Color.valueOf("8CCFE8");
    private static final Color C_IRIS_GREEN = Color.valueOf("7CC46B");
    private static final Color C_IRIS_BROWN = Color.valueOf("5B3A26");

    private SpriteBatch batch;
    private OrthographicCamera cam, fboCam;
    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private Texture bodyTex, heartTex, zzzTex, alertTex, sparkTex, leafTex, pawTex, px;
    private Texture waterDropTex, noteTex;
    private Texture[] tailTex;

    private Lwjgl3Window window;

    private float time;

    // squash & stretch spring
    private final Spring squash = new Spring(160f, 12f, 0.7f, 1.5f);
    private float scaleY = 1f;

    // dragging
    private boolean dragging;
    private int grabDX, grabDY;
    // carried: the skin's dangle loop (CarryArt) hanging from the cursor,
    // swinging like a pendulum (radians) as the window is moved
    private Texture[] carryTex;
    private final TextureRegion carryRegion = new TextureRegion();
    private float carryT, swing, swingVel, carryVx;
    private int carryLastX;
    /** Where the dangle art is held, in window units from the top-left. */
    private static final int CARRY_GRAB_X = 22, CARRY_GRAB_Y = 2;
    /** Extra window room while carried (units): each side, below and above. */
    private static final int CARRY_PAD_X = 26, CARRY_PAD_B = 12, CARRY_PAD_T = 8;
    private boolean carryBig;

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

    // click attack: 0 = paw swipe (cat), 1 = thunderbolt (pikachu),
    // 2 = water gun (squirtle), 3 = hiss + swipe (black cat),
    // 4 = dig (doge), 5 = puff up (goldfish)
    private int attackType;
    private float boltLeft;
    /** Cat swipe: two paw slaps toward the cursor side. */
    private static final float SCRATCH_DUR = 0.6f;
    private float scratchLeft;
    private int scratchDir = 1;
    // Bulbasaur's Vine Whip: a vine lashes out of each side of the bulb
    private static final float VINE_DUR = 0.7f;
    private float vineLeft;
    private boolean vineCracked;
    private final ArrayList<float[]> bolts = new ArrayList<float[]>();
    private float waterLeft, dropIn;
    // black cat bristles before it swipes; doge digs; goldfish puffs up
    private static final float HISS_ARCH = 0.35f;
    private static final float DIG_DUR = 1.6f;
    private static final float PUFF_DUR = 1.4f;
    private float hissLeft, digLeft, digIn, puffLeft;
    private Texture dirtTex, bubbleTex;
    // goldfish: free-swims toward random points instead of walking the floor
    private boolean swims, oneEye;
    private float swimTX, swimTY, swimXf, swimYf;
    private int swimMinX, swimMinY, swimMaxX, swimMaxY;

    // whole-frame poses (walk / sleep / belly-up / shell); null if none
    private Texture[] walkTex, sleepTex, rollTex;
    private Texture shellTex, steamTex;
    private int shellEyeX, shellEyeY;
    private float cheekLX, cheekRX, cheekY;
    private static final Color C_TONGUE = Color.valueOf("F2A3B3");
    // idle quirks: cats groom, Pikachu's cheeks spark, Squirtle hides
    private float groomLeft, groomIn = 25f, sparkLeft, sparkIn = 9f, shellLeft;
    // fast sustained typing builds heat until it overheats
    private float heat, steamIn;
    private boolean overheated;
    // long petting rolls it belly-up
    private float petHold, rollLeft;
    // cats stalk a slow cursor beside them, crouch, then pounce at it
    private float stalkT, crouchLeft, pounceCool = 6f;
    private int pounceDir = 1;
    private boolean pounceSwipe;
    // parabolic window hop; wanderState becomes hopNext on landing
    private float hopFX, hopFY, hopTX, hopTY, hopT, hopDur, hopH;
    private int hopNext;
    // perched on another window's title bar, riding along as it moves
    private long perchHwnd;
    private int perchOffX;
    private float perchLeft;
    private final int[] perchRect = new int[4];

    // a press becomes a drag once the cursor travels; otherwise it's a click
    private boolean pressed;
    private float pressT;
    private int pressGX, pressGY;

    private boolean trayOk;
    private TrayIcon trayIcon;
    private MenuItem updateItem;
    private volatile Updater.Release pendingUpdate;

    // stretch / water reminders (session-only; nothing is persisted)
    private static final float STRETCH_EVERY = 30 * 60f;
    private static final float WATER_EVERY = 45 * 60f;
    private volatile boolean stretchOn, waterOn;
    private float stretchIn, waterIn;
    // while a reminder animation plays, stretchLeft / waterRemindLeft hold
    // its remaining time; it waits (remindPending 1 stretch, 2 water) until
    // the pet is sitting somewhere
    private float stretchLeft, waterRemindLeft;
    private int remindPending;
    private Texture[] remindTex;
    private ReminderArt.Anim remindAnim;
    private float remindT;

    // click tricks and idle fidgets (TrickArt); travelling ones move the
    // window by their per-frame offsets from where they started
    private static final int TRICK_FIDGET = 0, TRICK_CLICK = 1, TRICK_ANNOYED = 2;
    /** Clicks during one trick that make the pet annoyed afterwards. */
    private static final int SPAM_CLICKS = 3;
    private final ReminderArt.Anim[] trickAnims = new ReminderArt.Anim[3];
    private final Texture[][] trickTexs = new Texture[3][];
    private ReminderArt.Anim trickAnim;
    private Texture[] trickTex;
    private int trickKind, clickSpam;
    private float trickT;
    private int trickX, trickY;
    private float fidgetIn = MathUtils.random(25f, 60f);

    // grooving to whatever the system is playing
    private float musicAmp, musicAbove, musicBelow, noteIn;
    private boolean musicOn, danceNow;

    // keyboard kneading: the global hook only bumps a counter — which keys
    // were pressed is never inspected or stored
    private final AtomicInteger keyTicks = new AtomicInteger();
    private int seenKeyTicks;
    private float typingLeft;
    private boolean kneadNow;

    // wandering along the bottom screen edge:
    // 0 = idle, 1 = falling, 2 = patrolling endlessly,
    // 3 = hurrying home along the floor, 4 = hopping up to the home spot,
    // 5 = free-swimming (goldfish), 6 = parabolic hop (pounce, perch),
    // 7 = perched on another app's title bar
    private int wanderState;
    private float wanderIn = 25f;
    private float fallVy, winXf;
    private int floorY, patrolDir;
    private int homeX, homeY, patrolMinX, patrolMaxX;
    private float returnT, leapFromY;
    private boolean facingLeft;

    private final ArrayList<Particle> particles = new ArrayList<Particle>();

    // LAN presence + chat
    private final String selfId = UUID.randomUUID().toString();
    private LanClient lan;
    private PeerRegistry peerReg;
    private RemotePetsManager remoteMgr;
    private BitmapFont font;
    private OrthographicCamera pxCam;
    private float stateTick;
    private final Bubble ownBubble = new Bubble();
    private long ownBubbleSpawned;
    /** Countdown until an incoming pellet lands and we flinch. */
    private float pendingShotIn = -1f;
    private int pendingShotDir = 1;

    // knocked out by a shot: squish flat or fall over 90°, then get back up
    private static final float KO_TOTAL = 1.6f;
    private float koT;
    private int koMode, koDir = 1;

    // tucked behind the taskbar via the right-click menu
    private boolean hidden;
    private int prevHideX, prevHideY;

    // a fullscreen game / video / slideshow in front on our monitor: the pet
    // (and friends' pets) step aside and go quiet until it's gone
    private boolean fullscreen;
    private float fullscreenCheckIn;
    // re-asserted every half second so other always-on-top windows (and
    // the taskbar) never stay above the pet
    private float raiseIn;
    private long ownHwnd;

    // tray toggles (session-only): quiet mode silences the pet and keeps it
    // at home; multiplayer off closes the LAN socket entirely
    private volatile boolean quiet;
    private volatile boolean multiplayer;   // off until switched on from the tray

    // clicks on the window's empty pixels fall through to whatever is below
    private static final int HIT_PX = 5;
    private final ByteBuffer hitPixels = BufferUtils.newByteBuffer(HIT_PX * HIT_PX * 4);
    private boolean passThrough;

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

        applySkin(skin);
        waterDropTex = PixelArt.fromMap(PixelArt.WATER_DROP);
        noteTex = PixelArt.fromMap(PixelArt.NOTE);
        dirtTex = PixelArt.fromMap(PixelArt.DIRT);
        bubbleTex = PixelArt.fromMap(PixelArt.BUBBLE);
        steamTex = PixelArt.tex(PoseArt.steam());
        heartTex = PixelArt.fromMap(PixelArt.HEART);
        zzzTex = PixelArt.fromMap(PixelArt.ZZZ);
        alertTex = PixelArt.fromMap(PixelArt.ALERT);
        sparkTex = PixelArt.fromMap(PixelArt.SPARK);
        leafTex = PixelArt.fromMap(PixelArt.LEAF);
        px = PixelArt.pixel();

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_W, FBO_H, false);
        fbo.getColorBufferTexture().setFilter(
                Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);

        window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
        WindowTricks.floating(window);
        WindowTricks.applyExStyles(window, false);
        try {
            ownHwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(
                    window.getWindowHandle());
        } catch (Throwable t) {
            ownHwnd = 0;   // non-Windows: never treated as under a fullscreen app
        }
        setupTray();
        setupKeyboardHook();
        SystemAudio.start();

        font = new BitmapFont();
        pxCam = new OrthographicCamera();
        pxCam.setToOrtho(false, winW(), winH());

        peerReg = new PeerRegistry(selfId);
        remoteMgr = new RemotePetsManager((Lwjgl3Application) Gdx.app, font, px,
                (peer, text) -> doSendChat(text, peer.id),
                peer -> shoot(peer.id));
        // multiplayer starts off: no LAN socket until the tray turns it on

        Thread boot = new Thread(() -> {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException ignored) {
            }
            checkForUpdates(true);
        }, "deskcat-update-boot");
        boot.setDaemon(true);
        boot.start();

        // test hook: DESKCAT_TEST_CHAT="/size 1000 hi" auto-sends a chat
        // message shortly after launch (drives bubble rendering in CI/dev)
        String testChat = System.getenv("DESKCAT_TEST_CHAT");
        if (testChat != null && !testChat.isEmpty()) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException ignored) {
                }
                Gdx.app.postRunnable(() -> sendChat(testChat));
            }, "deskcat-test-chat");
            t.setDaemon(true);
            t.start();
        }
        // test hook: DESKCAT_TEST_TRICK=click|fidget|spam plays that trick
        // shortly after launch (spam: a click, then three more during it)
        String testTrick = System.getenv("DESKCAT_TEST_TRICK");
        if ("click".equals(testTrick) || "fidget".equals(testTrick)
                || "spam".equals(testTrick)) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
                Gdx.app.postRunnable(() -> {
                    if ("click".equals(testTrick)) {
                        attack();
                    } else if ("spam".equals(testTrick)) {
                        for (int i = 0; i < 4; i++) {
                            attack();
                        }
                    } else {
                        startTrick(TRICK_FIDGET);
                    }
                });
            }, "deskcat-test-trick");
            t.setDaemon(true);
            t.start();
        }
        // test hook: DESKCAT_TEST_REMIND=stretch|water fires that reminder
        // shortly after launch instead of after 30 / 45 minutes
        String testRemind = System.getenv("DESKCAT_TEST_REMIND");
        if ("stretch".equals(testRemind) || "water".equals(testRemind)) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
                Gdx.app.postRunnable(() -> remindPending = "stretch".equals(testRemind) ? 1 : 2);
            }, "deskcat-test-remind");
            t.setDaemon(true);
            t.start();
        }

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int sx, int sy, int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    showPetMenu();
                    return true;
                }
                if (button == Input.Buttons.MIDDLE) {
                    if (lan == null) {
                        return true;   // multiplayer off: no chat
                    }
                    ChatInput.show(window.getPositionX(), window.getPositionY(),
                            text -> Gdx.app.postRunnable(() -> sendChat(text)));
                    return true;
                }
                if (hidden) {
                    toggleHidden();   // click the peeking head to bring it out
                    return true;
                }
                float ux = sx / (float) scale;
                float uy = UNITS_H - sy / (float) scale;
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
                        perchHwnd = 0;      // picking it up ends a perch
                        crouchLeft = 0f;
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
                    shrinkAfterCarry();
                    squash.kick(3f);   // jelly wobble on release
                }
                return true;
            }

            @Override
            public boolean mouseMoved(int sx, int sy) {
                float ux = sx / (float) scale;
                float uy = UNITS_H - sy / (float) scale;
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

    private void showPetMenu() {
        Point p = globalCursor();
        int mx = p == null ? window.getPositionX() : p.x;
        int my = p == null ? window.getPositionY() : p.y;
        ArrayList<PetMenu.Item> items = new ArrayList<PetMenu.Item>();
        if (lan != null) {
            items.add(new PetMenu.Item("Say…", () ->
                    ChatInput.show(window.getPositionX(), window.getPositionY(),
                            text -> Gdx.app.postRunnable(() -> sendChat(text)))));
        }
        if (ownBubble.isActive(System.currentTimeMillis())) {
            items.add(new PetMenu.Item("Dismiss bubble", () ->
                    Gdx.app.postRunnable(ownBubble::clear)));
        }
        items.add(new PetMenu.Item(hidden ? "Come out" : "Hide behind taskbar",
                () -> Gdx.app.postRunnable(this::toggleHidden)));
        for (Peer peer : peerReg.peers()) {
            final String pid = peer.id;
            String pname = peer.name == null || peer.name.isEmpty() ? "?" : peer.name;
            items.add(new PetMenu.Item("Shoot " + pname,
                    () -> Gdx.app.postRunnable(() -> shoot(pid))));
        }
        if (!trayOk) {
            items.add(new PetMenu.Item("Quit DeskCat", () ->
                    Gdx.app.postRunnable(() -> Gdx.app.exit())));
        }
        PetMenu.show(mx, my, items.toArray(new PetMenu.Item[0]));
    }

    /**
     * Tuck the pet down into the taskbar area (dropping always-on-top so the
     * taskbar covers it, ears peeking out); toggling again restores it.
     */
    private void toggleHidden() {
        if (!hidden) {
            hidden = true;
            prevHideX = window.getPositionX();
            prevHideY = window.getPositionY();
            wanderState = 0;
            sleeping = false;
            try {
                GraphicsConfiguration gc = GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration();
                Rectangle b = gc.getBounds();
                Insets ins = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                int taskbarTop = b.y + b.height - ins.bottom;
                GLFW.glfwSetWindowAttrib(window.getWindowHandle(),
                        GLFW.GLFW_FLOATING, GLFW.GLFW_FALSE);
                // head top sits ~14 units below the window top; leave ~8 units
                // of head poking out above the taskbar
                window.setPosition(window.getPositionX(), taskbarTop - 22 * scale);
            } catch (Throwable t) {
                hidden = false;
            }
        } else {
            hidden = false;
            WindowTricks.floating(window);
            window.setPosition(prevHideX, prevHideY);
            squash.kick(2f);
        }
    }

    /**
     * Tray "Multiplayer": off closes the LAN socket (saying goodbye, so our
     * pet leaves friends' screens), drops their pets here and disables chat
     * and shooting; on opens a fresh socket.
     */
    private void setMultiplayer(boolean on) {
        if (on == (lan != null)) {
            return;
        }
        if (on) {
            lan = new LanClient(selfId);
            lan.start();   // failure is silent; the pet just stays solo
        } else {
            lan.close();
            lan = null;
            peerReg = new PeerRegistry(selfId);
            remoteMgr.closeAll();
            pendingShotIn = -1f;
        }
    }

    /** Tray "Quiet mode": no sounds, wandering, perching or reminders. */
    private void setQuiet(boolean on) {
        quiet = on;
        if (!on) {
            return;
        }
        if (wanderState == 7 || (wanderState == 6 && hopNext == 7)) {
            perchHwnd = 0;
            hopTo(homeX, homeY, 0);
        } else if (roaming()) {
            beginReturn();
        }
    }

    /**
     * Twice a second, put the pet back on top of every other always-on-top
     * window, like other desktop pets do - except while tucked behind the
     * taskbar, hidden for a fullscreen app, or while one of our own windows
     * (chat box, menus) is in front, which it would otherwise cover.
     */
    private void keepOnTop(float dt) {
        raiseIn -= dt;
        if (raiseIn > 0f) {
            return;
        }
        raiseIn = 0.5f;
        if (!hidden && !fullscreen && ownHwnd != 0 && !Win32Windows.foregroundIsOurs()) {
            WindowTricks.raiseTopmost(ownHwnd);
        }
    }

    /**
     * Once a second: is a fullscreen app in front on our monitor? Then hide
     * the pet and friends' pets until it goes away.
     */
    private void updateFullscreen(float dt) {
        fullscreenCheckIn -= dt;
        if (fullscreenCheckIn > 0f) {
            return;
        }
        fullscreenCheckIn = 1f;
        boolean fs = !dragging && !pressed && Win32Windows.fullscreenOver(ownHwnd);
        if (fs == fullscreen) {
            return;
        }
        fullscreen = fs;
        if (fs) {
            WindowTricks.setShown(window, false);
            remoteMgr.closeAll();
        } else {
            WindowTricks.setShown(window, true);
            if (!hidden) {
                WindowTricks.floating(window);
            }
        }
    }

    /**
     * Let clicks through wherever the cursor is over an empty pixel of the
     * window: reads a few pixels of what was just drawn under the cursor.
     * Stays solid while the pet is held.
     */
    private void updateClickThrough() {
        int bw = Gdx.graphics.getBackBufferWidth();
        int bh = Gdx.graphics.getBackBufferHeight();
        int cx = Math.round((cursor.x - mainWinX) * bw / (float) winW());
        int cy = Math.round((cursor.y - mainWinY) * bh / (float) winH());
        if (!cursorValid || cx < 0 || cy < 0 || cx >= bw || cy >= bh) {
            return;   // not over our window: nothing to decide
        }
        boolean solid = pressed || dragging;
        if (!solid) {
            int x0 = MathUtils.clamp(cx - HIT_PX / 2, 0, Math.max(0, bw - HIT_PX));
            int y0 = MathUtils.clamp(bh - 1 - cy - HIT_PX / 2, 0, Math.max(0, bh - HIT_PX));
            hitPixels.clear();
            Gdx.gl.glReadPixels(x0, y0, HIT_PX, HIT_PX,
                    GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, hitPixels);
            for (int i = 3; i < HIT_PX * HIT_PX * 4; i += 4) {
                if ((hitPixels.get(i) & 0xFF) > 16) {
                    solid = true;
                    break;
                }
            }
        }
        if (passThrough == solid) {
            passThrough = !solid;
            WindowTricks.setClickThrough(window, passThrough);
        }
    }

    /** Drain received LAN messages, sync peer windows, broadcast our state. */
    private void updateNet(float dt) {
        if (lan == null) {
            return;   // multiplayer switched off
        }
        long now = System.currentTimeMillis();
        for (com.deskcat.net.LanMsg m = lan.poll(); m != null; m = lan.poll()) {
            peerReg.onMessage(m, now);
            if (m.type == LanMsg.ACTION && "shoot".equals(m.action)
                    && (m.target == null || m.target.isEmpty()
                            || m.target.equals(selfId)) && !fullscreen) {
                incomingShot(m.id);
            }
        }
        peerReg.prune(now);
        if (!fullscreen) {
            remoteMgr.sync(peerReg.peers());
        }

        if (pendingShotIn > 0f) {
            pendingShotIn -= dt;
            if (pendingShotIn <= 0f) {
                reactToShot();
            }
        }

        stateTick -= dt;
        if (stateTick <= 0f) {
            stateTick = 1 / 60f;   // 60 Hz pose sync — trivial bandwidth on a LAN
            Rectangle ub = remoteMgr.usable();
            float xf = (window.getPositionX() - ub.x)
                    / (float) Math.max(1, ub.width - winW());
            float yf = (window.getPositionY() - ub.y)
                    / (float) Math.max(1, ub.height - winH());
            int anim = koT > 0f ? LanMsg.ANIM_KO
                    : dragging ? LanMsg.ANIM_DRAG
                    : sleeping ? LanMsg.ANIM_SLEEP
                    : (moving()) ? LanMsg.ANIM_WALK
                    : LanMsg.ANIM_IDLE;
            lan.send(LanProtocol.encodeState(selfId, userName, skin,
                    clamp01(xf), clamp01(yf),
                    facingLeft && (moving()), anim));
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /**
     * Called on the render thread with what the user typed. "@name message"
     * DMs the peer with that display name; "/shoot" shoots; other /commands
     * style the bubble (see ChatCommands).
     */
    private void sendChat(String raw) {
        String text = raw;
        String target = null;
        if (raw.startsWith("@")) {
            int sp = raw.indexOf(' ');
            if (sp > 1) {
                Peer p = peerReg.byName(raw.substring(1, sp));
                if (p != null) {
                    target = p.id;
                    text = raw.substring(sp + 1).trim();
                }
            }
        }
        doSendChat(text, target);
    }

    /** targetId null → broadcast; otherwise a DM to that peer. */
    private void doSendChat(String text, String targetId) {
        if (lan == null) {
            return;
        }
        if (text.trim().toLowerCase().startsWith("/shoot")) {
            shoot(targetId);
            return;
        }
        ChatCommands.Parsed p = ChatCommands.parse(text);
        if (p.text.isEmpty()) {
            return;
        }
        lan.send(LanProtocol.encodeChat(selfId, userName, p.text, targetId,
                p.scale, p.effect, p.colorHex));
        ownBubble.show(p.text, System.currentTimeMillis(),
                p.scale, p.effect, p.colorHex);
        wake();
    }

    /** Fire at one peer (or everyone when targetId is null). */
    private void shoot(String targetId) {
        if (lan == null) {
            return;
        }
        lan.send(LanProtocol.encodeAction(selfId, userName, "shoot", targetId));
        signatureAttack();   // our pet plays its signature attack as the muzzle flash
        float fromX = window.getPositionX() + winW() / 2f;
        float fromY = window.getPositionY() + winH() / 2f;
        for (Peer p : peerReg.peers()) {
            if (targetId == null || targetId.equals(p.id)) {
                ProjectileFx.fire((Lwjgl3Application) Gdx.app, fromX, fromY,
                        peerCenterX(p), peerCenterY(p));
            }
        }
        wake();
    }

    private float peerCenterX(Peer p) {
        Rectangle ub = remoteMgr.usable();
        return ub.x + p.xFrac * Math.max(1, ub.width - winW()) + winW() / 2f;
    }

    private float peerCenterY(Peer p) {
        Rectangle ub = remoteMgr.usable();
        return ub.y + p.yFrac * Math.max(1, ub.height - winH()) + winH() / 2f;
    }

    /** Someone shot us: incoming pellet, then flinch on impact. */
    private void incomingShot(String shooterId) {
        Peer shooter = peerReg.byId(shooterId);
        float toX = window.getPositionX() + winW() / 2f;
        float toY = window.getPositionY() + winH() / 2f;
        if (shooter != null) {
            float fromX = peerCenterX(shooter), fromY = peerCenterY(shooter);
            ProjectileFx.fire((Lwjgl3Application) Gdx.app, fromX, fromY, toX, toY);
            pendingShotIn = ProjectileFx.duration(fromX, fromY, toX, toY);
            pendingShotDir = fromX < toX ? -1 : 1;   // fall away from the shot
        } else {
            pendingShotDir = MathUtils.randomBoolean() ? -1 : 1;
            reactToShot();   // unknown shooter: no pellet, just the hit
        }
    }

    private void reactToShot() {
        wake();
        alertLeft = 1.2f;
        koT = KO_TOTAL;
        koMode = MathUtils.randomBoolean() ? 0 : 1;   // fall over / squish flat
        koDir = pendingShotDir;
        squash.kick(-6f);
        sparkBurst(CAT_X + eyeCenterX, 18f, 12, sparkTex, 16f, 4f, 12f);
    }

    /**
     * While carried the pet swings and stretches well past its normal
     * window, so the window grows by CARRY_PAD_X on each side, CARRY_PAD_B
     * below and CARRY_PAD_T above. The cameras widen to match with the
     * world origin unchanged, so everything draws in its usual units and
     * stays in the same place on screen.
     */
    private void growForCarry() {
        if (carryBig) {
            return;
        }
        carryBig = true;
        int padX = CARRY_PAD_X * scale, padB = CARRY_PAD_B * scale, padT = CARRY_PAD_T * scale;
        int x = window.getPositionX() - padX, y = window.getPositionY() - padT;
        Gdx.graphics.setWindowedMode(winW() + 2 * padX, winH() + padB + padT);
        window.setPosition(x, y);
        cam.setToOrtho(false, UNITS_W + 2 * CARRY_PAD_X, UNITS_H + CARRY_PAD_B + CARRY_PAD_T);
        cam.position.set(UNITS_W / 2f, (UNITS_H + CARRY_PAD_T - CARRY_PAD_B) / 2f, 0f);
        cam.update();
        pxCam.setToOrtho(false, winW() + 2 * padX, winH() + padB + padT);
        pxCam.position.set(winW() / 2f, (winH() + padT - padB) / 2f, 0f);
        pxCam.update();
    }

    /** Back to the normal window, keeping the pet where it is on screen. */
    private void shrinkAfterCarry() {
        if (!carryBig) {
            return;
        }
        carryBig = false;
        int x = window.getPositionX() + CARRY_PAD_X * scale;
        int y = window.getPositionY() + CARRY_PAD_T * scale;
        Gdx.graphics.setWindowedMode(winW(), winH());
        window.setPosition(x, y);
        cam.setToOrtho(false, UNITS_W, UNITS_H);
        pxCam.setToOrtho(false, winW(), winH());
    }

    /** Resize the pet (and the chat/bubble camera); remote windows follow. */
    private void applyScale(int s) {
        shrinkAfterCarry();
        int homeXu = window.getPositionX(), homeYu = window.getPositionY();
        scale = s;
        Gdx.graphics.setWindowedMode(winW(), winH());
        window.setPosition(homeXu, homeYu);
        pxCam.setToOrtho(false, winW(), winH());
        wanderState = 0;   // stale floor/patrol geometry after resize
    }

    /**
     * Load (or swap, at runtime) all skin-specific art and geometry. Textures
     * come from the shared {@link SkinAssets} cache — remote peers' pets may
     * render the same skin, so nothing is disposed here; the cache is torn
     * down once in dispose().
     */
    private void applySkin(String name) {
        skin = name;
        if (remindAnim != null) {
            endReminderAnim();   // that skin's frames are no use to this one
        }
        disposeCarry();
        if (trickAnim != null) {
            endTrick();
        }
        clickSpam = 0;
        loadTricks();
        ReminderArt.Anim carry = CarryArt.carry(name);
        carryTex = new Texture[carry.frames.length];
        for (int i = 0; i < carryTex.length; i++) {
            carryTex[i] = PixelArt.tex(carry.frames[i]);
        }
        SkinAssets a = SkinAssets.get(name);
        eyeStyle = a.eyeStyle;
        attackType = a.attackType;
        bodyTex = a.bodyTex;
        tailTex = a.tailTex;
        pawTex = a.pawTex;
        tailX = a.tailX;
        eyeLX = a.eyeLX;
        eyeRX = a.eyeRX;
        eyeW = a.eyeW;
        eyeH = a.eyeH;
        eyeY = a.eyeY;
        eyeCenterX = a.eyeCenterX;
        eyeCenterY = a.eyeCenterY;
        furColor = a.furColor;
        irisColor = a.irisColor;
        lineColor = a.lineColor != null ? a.lineColor : C_OUTLINE;
        swims = a.swims;
        oneEye = a.oneEye;
        walkTex = a.walkTex;
        sleepTex = a.sleepTex;
        rollTex = a.rollTex;
        shellTex = a.shellTex;
        shellEyeX = a.shellEyeX;
        shellEyeY = a.shellEyeY;
        cheekLX = a.cheekLX;
        cheekRX = a.cheekRX;
        cheekY = a.cheekY;
        // drop any in-flight attack so it doesn't straddle two skins
        boltLeft = 0f;
        waterLeft = 0f;
        scratchLeft = 0f;
        vineLeft = 0f;
        hissLeft = 0f;
        digLeft = 0f;
        puffLeft = 0f;
        alertLeft = 0f;
        groomLeft = 0f;
        sparkLeft = 0f;
        shellLeft = 0f;
        rollLeft = 0f;
        crouchLeft = 0f;
        // a walker can't keep patrolling as a fish (or vice versa): head home
        if (wanderState == 6 || wanderState == 7) {
            perchHwnd = 0;
            pounceSwipe = false;
            hopTo(homeX, homeY, 0);
        } else if (wanderState != 0 && wanderState != 4) {
            beginReturn();
        }
        if (trayIcon != null) {
            trayIcon.setToolTip("DeskCat - " + skin);
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

    private static final String[] SKINS =
            {"squirtle", "pikachu", "cat", "blackcat", "doge", "goldfish", "bulbasaur"};
    private static final String[] SKIN_LABELS =
            {"Squirtle", "Pikachu", "Cat", "Black cat", "Doge", "Goldfish", "Bulbasaur"};

    // one tray icon per machine: the first instance binds this loopback port
    // and owns the tray; later instances quit via the pet's right-click menu
    private java.net.ServerSocket trayLock;

    private void setupTray() {
        try {
            if (!SystemTray.isSupported()) {
                return;
            }
            try {
                trayLock = new java.net.ServerSocket(42108, 1,
                        java.net.InetAddress.getByName("127.0.0.1"));
            } catch (Throwable taken) {
                trayOk = false;   // another instance already shows the tray
                return;
            }
            PopupMenu menu = new PopupMenu();

            Menu skinMenu = new Menu("Skin");
            final CheckboxMenuItem[] items = new CheckboxMenuItem[SKINS.length];
            for (int i = 0; i < SKINS.length; i++) {
                final int idx = i;
                String label = SKIN_LABELS[i];
                items[i] = new CheckboxMenuItem(label,
                        SKINS[i].equalsIgnoreCase(skin));
                items[i].addItemListener(e -> {
                    for (int j = 0; j < items.length; j++) {
                        items[j].setState(j == idx);
                    }
                    Gdx.app.postRunnable(() -> applySkin(SKINS[idx]));
                });
                skinMenu.add(items[i]);
            }
            menu.add(skinMenu);

            // pet size: pixels per world unit (applies to remote pets too)
            Menu sizeMenu = new Menu("Size");
            String[] sizeNames = {"Small", "Normal", "Large"};
            int[] sizeVals = {3, 5, 7};
            final CheckboxMenuItem[] sizeItems = new CheckboxMenuItem[sizeVals.length];
            for (int i = 0; i < sizeVals.length; i++) {
                final int idx = i;
                sizeItems[i] = new CheckboxMenuItem(sizeNames[i], sizeVals[i] == scale);
                sizeItems[i].addItemListener(e -> {
                    for (int j = 0; j < sizeItems.length; j++) {
                        sizeItems[j].setState(j == idx);
                    }
                    Gdx.app.postRunnable(() -> applyScale(sizeVals[idx]));
                });
                sizeMenu.add(sizeItems[i]);
            }
            menu.add(sizeMenu);

            // extra clearance above the taskbar while patrolling
            Menu gapMenu = new Menu("Taskbar gap");
            String[] gapNames = {"Auto", "0 px", "20 px", "40 px", "60 px"};
            int[] gapVals = {-1, 0, 20, 40, 60};
            final CheckboxMenuItem[] gapItems = new CheckboxMenuItem[gapVals.length];
            for (int i = 0; i < gapVals.length; i++) {
                final int idx = i;
                gapItems[i] = new CheckboxMenuItem(gapNames[i],
                        gapVals[i] == bottomGap);
                gapItems[i].addItemListener(e -> {
                    for (int j = 0; j < gapItems.length; j++) {
                        gapItems[j].setState(j == idx);
                    }
                    bottomGap = gapVals[idx];
                });
                gapMenu.add(gapItems[i]);
            }
            menu.add(gapMenu);

            // how transparent other people's pets render
            Menu fadeMenu = new Menu("Peer fade");
            String[] fadeNames = {"Off", "Light", "Heavy"};
            float[] fadeVals = {1f, 0.9f, 0.55f};
            final CheckboxMenuItem[] fadeItems = new CheckboxMenuItem[fadeVals.length];
            for (int i = 0; i < fadeVals.length; i++) {
                final int idx = i;
                fadeItems[i] = new CheckboxMenuItem(fadeNames[i],
                        Math.abs(fadeVals[i] - peerAlpha) < 0.01f);
                fadeItems[i].addItemListener(e -> {
                    for (int j = 0; j < fadeItems.length; j++) {
                        fadeItems[j].setState(j == idx);
                    }
                    peerAlpha = fadeVals[idx];
                });
                fadeMenu.add(fadeItems[i]);
            }
            menu.add(fadeMenu);

            Menu remindMenu = new Menu("Reminders");
            final CheckboxMenuItem stretchItem =
                    new CheckboxMenuItem("Stretch every 30 min", false);
            stretchItem.addItemListener(e -> {
                stretchOn = stretchItem.getState();
                stretchIn = STRETCH_EVERY;
            });
            remindMenu.add(stretchItem);
            final CheckboxMenuItem waterItem =
                    new CheckboxMenuItem("Drink water every 45 min", false);
            waterItem.addItemListener(e -> {
                waterOn = waterItem.getState();
                waterIn = WATER_EVERY;
            });
            remindMenu.add(waterItem);
            menu.add(remindMenu);

            MenuItem soundItem = new MenuItem("Sound");
            soundItem.addActionListener(e -> VolumePopup.show());
            menu.add(soundItem);

            final CheckboxMenuItem quietItem = new CheckboxMenuItem("Quiet mode", quiet);
            quietItem.addItemListener(e -> {
                boolean on = quietItem.getState();
                Gdx.app.postRunnable(() -> setQuiet(on));
            });
            menu.add(quietItem);

            final CheckboxMenuItem netItem =
                    new CheckboxMenuItem("Multiplayer", multiplayer);
            netItem.addItemListener(e -> {
                multiplayer = netItem.getState();
                boolean on = multiplayer;
                Gdx.app.postRunnable(() -> setMultiplayer(on));
            });
            menu.add(netItem);

            final CheckboxMenuItem startupItem = new CheckboxMenuItem(
                    "Start with Windows", isStartupEnabled());
            startupItem.addItemListener(e -> {
                if (!setStartup(startupItem.getState())) {
                    startupItem.setState(false);
                }
            });
            menu.add(startupItem);

            updateItem = new MenuItem("Check for updates");
            updateItem.addActionListener(e -> installOrCheck());
            menu.add(updateItem);

            MenuItem notesItem = new MenuItem("Patch notes");
            notesItem.addActionListener(e -> PatchNotes.show());
            menu.add(notesItem);
            menu.addSeparator();

            MenuItem exit = new MenuItem("Quit DeskCat");
            exit.addActionListener(e -> Gdx.app.postRunnable(() -> Gdx.app.exit()));
            menu.add(exit);

            trayIcon = new TrayIcon(trayImage(), "DeskCat - " + skin, menu);
            SystemTray.getSystemTray().add(trayIcon);
            trayOk = true;
        } catch (Throwable t) {
            trayOk = false;   // right-click on the pet quits instead
        }
    }

    private static final String RUN_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    /** Launch command for the Run key: the packaged exe, or javaw + jar. */
    private static String startupCommand() {
        try {
            java.io.File jar = Updater.runningJar();
            if (jar == null) {
                return null;   // dev mode: nothing sensible to register
            }
            java.io.File exe = Updater.appImageExe(jar);
            if (exe != null) {
                return "\"" + exe.getAbsolutePath() + "\"";
            }
            String javaw = new java.io.File(new java.io.File(
                    System.getProperty("java.home"), "bin"),
                    "javaw.exe").getAbsolutePath();
            return "\"" + javaw + "\" -jar \"" + jar.getAbsolutePath() + "\"";
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean isStartupEnabled() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] {
                    "reg", "query", RUN_KEY, "/v", "DeskCat"});
            return p.waitFor() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean setStartup(boolean on) {
        try {
            if (on) {
                String cmd = startupCommand();
                if (cmd == null) {
                    notifyTray("DeskCat", "Startup needs the packaged jar or "
                            + "exe; a gradle dev run can't be registered.");
                    return false;
                }
                Process p = Runtime.getRuntime().exec(new String[] {
                        "reg", "add", RUN_KEY, "/v", "DeskCat", "/t", "REG_SZ",
                        "/d", cmd, "/f"});
                return p.waitFor() == 0;
            }
            Process p = Runtime.getRuntime().exec(new String[] {
                    "reg", "delete", RUN_KEY, "/v", "DeskCat", "/f"});
            p.waitFor();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Quiet checks (at launch) only speak up when an update exists; the tray
     * item's check (install = true) goes straight on to install it.
     */
    private void checkForUpdates(final boolean quiet) {
        checkForUpdates(quiet, false);
    }

    private void checkForUpdates(final boolean quiet, final boolean install) {
        Thread t = new Thread(() -> {
            Updater.Release r = Updater.fetchLatest();
            if (r == null) {
                if (!quiet) {
                    notifyTray("DeskCat",
                            "Could not reach GitHub to check for updates.");
                }
                return;
            }
            if (Updater.isNewer(r.version, VERSION) && install) {
                pendingUpdate = r;
                installUpdate(r);                  // one click: check and install
            } else if (Updater.isNewer(r.version, VERSION)) {
                pendingUpdate = r;
                final String v = r.version;
                java.awt.EventQueue.invokeLater(() -> {
                    if (updateItem != null) {
                        updateItem.setLabel("Install update v" + v);
                    }
                });
                notifyTray("DeskCat update available",
                        "v" + v + " is out (you have v" + VERSION
                                + "). Right-click the tray icon to install.");
            } else if (!quiet) {
                notifyTray("DeskCat",
                        "You're on the latest version (v" + VERSION + ").");
            }
        }, "deskcat-update-check");
        t.setDaemon(true);
        t.start();
    }

    /** The tray item: install the update already found, else check and install. */
    private void installOrCheck() {
        final Updater.Release r = pendingUpdate;
        if (r == null) {
            checkForUpdates(false, true);
            return;
        }
        installUpdate(r);
    }

    private volatile boolean installing;

    /** Download the release, swap it in and restart; safe from any thread. */
    private void installUpdate(final Updater.Release r) {
        if (installing) {
            return;   // already downloading
        }
        installing = true;
        notifyTray("DeskCat", "Downloading v" + r.version + "...");
        Thread t = new Thread(() -> {
            java.io.File jar = Updater.runningJar();
            java.io.File dl = jar != null ? Updater.download(r) : null;
            if (jar != null && dl != null && Updater.stageSwap(dl, jar)) {
                Gdx.app.postRunnable(() -> Gdx.app.exit());
            } else {
                installing = false;
                Updater.openReleasePage(r);   // dev mode or download failed
            }
        }, "deskcat-update-install");
        t.setDaemon(true);
        t.start();
    }

    /**
     * A due reminder waits until the pet is sitting, then plays the skin's
     * own stretch or drink (ReminderArt) with a speech bubble; picking the
     * pet up cuts it short.
     */
    private void updateReminderAnim(float dt) {
        if (remindAnim == null && trickAnim == null && remindPending != 0 && seated() && !dragging
                && !pressed && !hidden && !fullscreen) {
            boolean stretch = remindPending == 1;
            remindPending = 0;
            remindAnim = stretch ? ReminderArt.stretch(skin) : ReminderArt.water(skin);
            remindTex = new Texture[remindAnim.frames.length];
            for (int i = 0; i < remindTex.length; i++) {
                remindTex[i] = PixelArt.tex(remindAnim.frames[i]);
            }
            remindT = 0f;
            wake();
            rollLeft = 0f;
            shellLeft = 0f;
            crouchLeft = 0f;
            groomLeft = 0f;
            ownBubble.show(stretch ? "Stretch time!" : "Water break!",
                    System.currentTimeMillis());
            if (stretch) {
                SoundFx.chirp();
            } else {
                SoundFx.splash();
            }
            if (stretch) {
                stretchLeft = remindAnim.duration();
            } else {
                waterRemindLeft = remindAnim.duration();
            }
        }
        if (remindAnim == null) {
            return;
        }
        remindT += dt;
        if (dragging || pressed || remindT >= remindAnim.duration()) {
            endReminderAnim();
            return;
        }
        float left = remindAnim.duration() - remindT;
        if (stretchLeft > 0f) {
            stretchLeft = left;
        } else {
            waterRemindLeft = left;
        }
    }

    private void disposeCarry() {
        if (carryTex != null) {
            for (Texture t : carryTex) {
                t.dispose();
            }
            carryTex = null;
        }
    }

    private void endReminderAnim() {
        if (remindTex != null) {
            for (Texture t : remindTex) {
                t.dispose();
            }
        }
        remindTex = null;
        remindAnim = null;
        stretchLeft = 0f;
        waterRemindLeft = 0f;
    }

    /** Build this skin's trick, fidget and annoyed frames once, up front. */
    private void loadTricks() {
        disposeTricks();
        ReminderArt.Anim[] a = {TrickArt.fidget(skin), TrickArt.click(skin), TrickArt.annoyed(skin)};
        for (int k = 0; k < a.length; k++) {
            trickAnims[k] = a[k];
            trickTexs[k] = new Texture[a[k].frames.length];
            for (int i = 0; i < a[k].frames.length; i++) {
                trickTexs[k][i] = PixelArt.tex(a[k].frames[i]);
            }
        }
    }

    private void disposeTricks() {
        for (int k = 0; k < trickTexs.length; k++) {
            if (trickTexs[k] != null) {
                for (Texture t : trickTexs[k]) {
                    t.dispose();
                }
                trickTexs[k] = null;
            }
            trickAnims[k] = null;
        }
    }

    private void startTrick(int kind) {
        endTrick();
        trickKind = kind;
        trickAnim = trickAnims[kind];
        trickTex = trickTexs[kind];
        trickT = 0f;
        trickX = window.getPositionX();
        trickY = window.getPositionY();
    }

    private void endTrick() {
        if (trickAnim != null && trickAnim.dx != null && !dragging) {
            window.setPosition(trickX, trickY);   // back where it started
        }
        trickTex = null;
        trickAnim = null;
    }

    /**
     * Plays the current trick (moving the window for travelling ones) and,
     * every so often while the pet sits calmly, starts an idle fidget.
     */
    private void updateTrick(float dt) {
        if (trickAnim == null) {
            boolean calm = seated() && !sleeping && !dragging && !pressed && !hidden
                    && !fullscreen && !petActive && remindAnim == null
                    && remindPending == 0 && !danceNow && !kneadNow
                    && typingLeft <= 0f && rollLeft <= 0f && shellLeft <= 0f
                    && crouchLeft <= 0f && groomLeft <= 0f && idleTime > 2f;
            if (calm) {
                fidgetIn -= dt;
                if (fidgetIn <= 0f) {
                    fidgetIn = MathUtils.random(25f, 60f);
                    startTrick(TRICK_FIDGET);
                }
            }
            return;
        }
        trickT += dt;
        // a fidget gives way to a press; click tricks and the annoyed
        // reaction always play out unless the pet is picked up
        if (dragging || (trickKind == TRICK_FIDGET && pressed && trickT > 0.2f)) {
            endTrick();
            return;
        }
        if (trickT >= trickAnim.duration()) {
            boolean spammed = trickKind == TRICK_CLICK && clickSpam >= SPAM_CLICKS;
            endTrick();
            clickSpam = 0;
            if (spammed) {                         // clicked over and over: fed up
                if (attackType == 0 || attackType == 3) {
                    SoundFx.hiss();
                } else {
                    playVoice();
                }
                startTrick(TRICK_ANNOYED);
            }
            return;
        }
        if (trickAnim.dx != null) {
            int i = trickAnim.index(trickT);
            Rectangle area = screenArea(trickX + winW() / 2, trickY + winH() / 2);
            int x = MathUtils.clamp(trickX + trickAnim.dx[i] * scale,
                    area.x, area.x + area.width - winW());
            int y = MathUtils.clamp(trickY + trickAnim.dy[i] * scale,
                    area.y, area.y + area.height - winH());
            if (x != window.getPositionX() || y != window.getPositionY()) {
                window.setPosition(x, y);
            }
        }
    }

    /** The trick frame showing now, or null. */
    private Texture trickFrame() {
        return trickAnim == null ? null : trickTex[trickAnim.index(trickT)];
    }

    /** The reminder frame showing now, or null. */
    private Texture reminderFrame() {
        if (remindAnim == null) {
            return null;
        }
        float t = remindT;
        for (int i = 0; i < remindTex.length; i++) {
            t -= remindAnim.secs[i];
            if (t < 0f) {
                return remindTex[i];
            }
        }
        return remindTex[remindTex.length - 1];
    }

    private void notifyTray(String title, String msg) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, msg, TrayIcon.MessageType.INFO);
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

    /**
     * A click: the skin's trick (TrickArt), with its voice. Clicks while a
     * trick or the annoyed reaction is playing don't restart it; they're
     * counted, and enough of them make the pet annoyed once it finishes.
     */
    private void attack() {
        wake();
        if (trickAnim != null && trickKind != TRICK_FIDGET) {
            if (trickKind == TRICK_CLICK) {
                clickSpam++;
            }
            return;
        }
        shellLeft = 0f;
        rollLeft = 0f;
        crouchLeft = 0f;
        groomLeft = 0f;
        if (remindAnim != null) {
            endReminderAnim();
        }
        playVoice();
        clickSpam = 0;
        startTrick(TRICK_CLICK);
    }

    private void playVoice() {
        switch (attackType) {
            case 0: SoundFx.meow(); break;
            case 1: SoundFx.pikaCry(); break;
            case 2: SoundFx.squirtleCry(); break;
            case 3: SoundFx.hiss(); break;
            case 4: SoundFx.bork(); break;
            case 5: SoundFx.blub(); break;
            default: SoundFx.vineWhip(); break;
        }
    }

    /**
     * The skin's original quick attack (paw swipe, thunderbolt, water gun,
     * hiss, dig, puff, Vine Whip), kept as the muzzle flash when shooting a
     * friend's pet over LAN.
     */
    private void signatureAttack() {
        shellLeft = 0f;
        rollLeft = 0f;
        crouchLeft = 0f;
        groomLeft = 0f;
        if (attackType == 0) {
            // the tabby slaps a paw out toward whichever side the cursor is on
            wake();
            scratchLeft = SCRATCH_DUR;
            scratchDir = gazeX < 0f ? -1 : 1;
            squash.kick(2.5f);
            SoundFx.meow();
            return;
        }
        if (attackType == 3) {
            // black cat: back arches and fur bristles, then the claws come out
            wake();
            hissLeft = HISS_ARCH + SCRATCH_DUR;
            scratchDir = gazeX < 0f ? -1 : 1;
            squash.kick(4f);
            SoundFx.hiss();
            return;
        }
        if (attackType == 4) {
            // doge: digs frantically, flinging dirt up and out
            wake();
            digLeft = DIG_DUR;
            digIn = 0f;
            squash.kick(2f);
            SoundFx.bork();
            return;
        }
        if (attackType == 5) {
            // goldfish: inflates like a pufferfish, holds, then deflates
            wake();
            puffLeft = PUFF_DUR;
            SoundFx.blub();
            return;
        }
        if (attackType == 6) {
            // Bulbasaur: Vine Whip - a vine cracks out of each side of the bulb
            wake();
            vineLeft = VINE_DUR;
            vineCracked = false;
            squash.kick(3f);
            SoundFx.vineWhip();
            return;
        }
        wake();
        squash.kick(5f);          // excited hop
        if (attackType == 1) {
            SoundFx.pikaCry();
        } else {
            SoundFx.squirtleCry();
        }
        if (attackType == 1) {
            boltLeft = 0.7f;
            bolts.clear();
            for (int i = 0; i < 3; i++) {
                bolts.add(makeBolt(
                        CAT_X + eyeCenterX + MathUtils.random(-3f, 3f)));
            }
            sparkBurst(CAT_X + eyeCenterX, cheekY, 10, sparkTex, 14f, 6f, 13f);
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

        mainWinX = window.getPositionX();
        mainWinY = window.getPositionY();

        pollCursor(dt);
        updateFullscreen(dt);
        keepOnTop(dt);
        SoundFx.muted = quiet || fullscreen;
        updateMood(dt);
        updateWander(dt);
        updateSpring(dt);
        updateBlink(dt);
        updateTail(dt);
        updateParticles(dt);
        updateNet(dt);

        if (!fullscreen) {   // hidden under a fullscreen app: skip the drawing
            renderCatToFbo();
            renderWindow();
            updateClickThrough();
        }
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
        float eyeScrX = window.getPositionX() + (CAT_X + eyeCenterX) * scale;
        float eyeScrY = window.getPositionY() + (UNITS_H - eyeCenterY) * scale;
        gazeX = MathUtils.clamp((cursor.x - eyeScrX) / 240f, -1f, 1f);
        gazeY = MathUtils.clamp((eyeScrY - cursor.y) / 240f, -1f, 1f);

        // startled by a cursor rushing past nearby
        float dx = cursor.x - eyeScrX, dy = cursor.y - eyeScrY;
        if (!dragging && !sleeping && cursorSpeed > 2200f
                && dx * dx + dy * dy < 500f * 500f && alertLeft <= 0f) {
            alertLeft = 0.8f;
            if (shellTex != null) {
                shellLeft = Math.max(shellLeft, 2.2f);   // ducks into its shell
            }
            crouchLeft = 0f;   // a startled cat abandons the stalk
        }

        if (pressed && !dragging) {
            pressT += dt;
            boolean moved = Math.abs(cursor.x - pressGX)
                    + Math.abs(cursor.y - pressGY) > 4;
            if (moved || pressT > 0.35f) {
                dragging = true;
                // picked up by the scruff: re-hang so the dangle art's
                // grab point is under the cursor, then let it swing
                growForCarry();
                grabDX = (CARRY_GRAB_X + CARRY_PAD_X) * scale;
                grabDY = (CARRY_GRAB_Y + CARRY_PAD_T) * scale;
                carryT = 0f;
                swing = 0f;
                swingVel = 0f;
                carryVx = 0f;
                carryLastX = cursor.x;
            }
        }

        if (dragging) {
            window.setPosition(cursor.x - grabDX, cursor.y - grabDY);
            carryT += dt;
            // pendulum: the pet lags behind the hand's acceleration
            float vx = (cursor.x - carryLastX) / dt;
            carryLastX = cursor.x;
            float prevVx = carryVx;
            carryVx = MathUtils.lerp(carryVx, vx, 0.35f);
            float acc = (carryVx - prevVx) / dt;
            swingVel += (-60f * swing - 5f * swingVel - acc * 0.0012f) * dt;
            swing = MathUtils.clamp(swing + swingVel * dt, -0.6f, 0.6f);
        }
    }

    private void updateMood(float dt) {
        alertLeft -= dt;
        boltLeft -= dt;
        scratchLeft -= dt;
        vineLeft -= dt;
        float hissBefore = hissLeft;
        hissLeft -= dt;
        if (hissBefore > SCRATCH_DUR && hissLeft <= SCRATCH_DUR && hissLeft > 0f) {
            scratchLeft = SCRATCH_DUR;   // claws out once the arch has landed
        }
        digLeft -= dt;
        if (digLeft > 0f) {
            digIn -= dt;
            while (digIn <= 0f) {
                digIn += 0.05f;
                Particle d = new Particle();
                d.tex = dirtTex;
                d.x = CAT_X + eyeCenterX + MathUtils.random(-3f, 3f);
                d.y = 2f;
                d.vx = MathUtils.random(-26f, 26f);
                d.vy = MathUtils.random(14f, 26f);
                d.grav = -70f;
                d.maxLife = 0.9f;
                d.scale = MathUtils.randomBoolean() ? 1f : 1.4f;
                particles.add(d);
            }
        }
        puffLeft -= dt;
        if (puffLeft > 0f && MathUtils.random() < dt * 9f) {
            Particle b = new Particle();
            b.tex = bubbleTex;
            b.x = CAT_X + eyeCenterX + 2f + MathUtils.random(-1f, 1f);
            b.y = eyeCenterY - 2f;
            b.vx = MathUtils.random(-2f, 2f);
            b.vy = MathUtils.random(6f, 11f);
            b.maxLife = 1.6f;
            b.scale = MathUtils.randomBoolean() ? 0.8f : 1.2f;
            particles.add(b);
        }
        waterLeft -= dt;
        typingLeft -= dt;
        koT -= dt;

        boolean remindersPaused = quiet || fullscreen;
        if (stretchOn && !dragging && !remindersPaused) {
            stretchIn -= dt;
            if (stretchIn <= 0f) {
                stretchIn = STRETCH_EVERY;
                remindPending = 1;
                wake();
                if (roaming()) {
                    beginReturn();
                }
            }
        }
        if (waterOn && !dragging && !remindersPaused) {
            waterIn -= dt;
            if (waterIn <= 0f) {
                waterIn = WATER_EVERY;
                remindPending = 2;
                wake();
                if (roaming()) {
                    beginReturn();
                }
            }
        }
        updateReminderAnim(dt);
        updateTrick(dt);
        float sysPeak = SystemAudio.peak();
        musicAmp = MathUtils.lerp(musicAmp, Math.min(1f, sysPeak * 6f), 0.25f);
        if (!musicOn) {
            musicAbove = sysPeak > 0.02f ? musicAbove + dt : 0f;
            if (musicAbove > 0.6f) {
                musicOn = true;
                wake();
            }
        } else {
            musicBelow = sysPeak < 0.005f ? musicBelow + dt : 0f;
            if (musicBelow > 2.5f) {
                musicOn = false;
            }
        }
        danceNow = musicOn && !sleeping && !dragging && seated()
                && boltLeft <= 0f && waterLeft <= 0f && stretchLeft <= 0f
                && scratchLeft <= 0f && hissLeft <= 0f && digLeft <= 0f
                && puffLeft <= 0f && !kneadNow;
        if (danceNow) {
            noteIn -= dt;
            if (noteIn <= 0f) {
                noteIn = MathUtils.random(0.5f, 0.9f);
                Particle m = new Particle();
                m.tex = noteTex;
                m.x = CAT_X + eyeCenterX + MathUtils.random(-8f, 8f);
                m.y = 27f;
                m.vx = MathUtils.random(-2f, 2f);
                m.vy = MathUtils.random(5f, 8f);
                m.maxLife = 1.7f;
                m.scale = MathUtils.randomBoolean() ? 1f : 1.5f;
                particles.add(m);
            }
        }

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
        int keysNow = kc - seenKeyTicks;
        if (kc != seenKeyTicks) {
            seenKeyTicks = kc;
            typingLeft = 0.55f;
            if (roaming()) {
                beginReturn();   // scurry home, knead once it gets there
            }
            wake();
        }
        kneadNow = typingLeft > 0f && !dragging && seated()
                && boltLeft <= 0f && waterLeft <= 0f && scratchLeft <= 0f
                && hissLeft <= 0f && digLeft <= 0f && puffLeft <= 0f
                && !sleeping;

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
            h.y = rollLeft > 0f && rollTex != null ? rollTex[0].getHeight() + 1f : 27f;
            h.vx = MathUtils.random(-1.5f, 1.5f);
            h.vy = MathUtils.random(6f, 8f);
            h.maxLife = 1.5f;
            h.scale = MathUtils.randomBoolean() ? 1f : 0.7f;
            particles.add(h);
            heartIn = rollLeft > 0f ? 0.18f : 0.35f;   // belly rubs earn more hearts
        }

        if (!sleeping && idleTime > 60f && !dragging && !petActive
                && seated() && typingLeft <= 0f && !musicOn) {
            sleeping = true;
        }
        if (sleeping) {
            zzzIn -= dt;
            if (zzzIn <= 0f) {
                Particle z = new Particle();
                z.tex = zzzTex;
                z.x = CAT_X + 26;
                z.y = sleepTex != null ? sleepTex[0].getHeight() - 2f : 26f;
                z.vx = 1.4f;
                z.vy = 3.5f;
                z.maxLife = 2.2f;
                z.scale = MathUtils.randomBoolean() ? 1f : 1.5f;
                particles.add(z);
                zzzIn = 1.6f;
            }
        }
        updateQuirks(dt, keysNow);
    }

    /**
     * Personality between interactions: fast typing overheats it, Squirtle
     * ducks into its shell, long petting rolls it belly-up, cats groom and
     * stalk-then-pounce a slow cursor, and Pikachu's cheeks spark.
     */
    private void updateQuirks(float dt, int keysNow) {
        // overheat: ~8 keys/s for a few seconds; ordinary typing cools off
        heat = MathUtils.clamp(heat + keysNow * 0.15f - dt * 0.75f, 0f, 2.5f);
        if (!overheated && heat > 1.5f) {
            overheated = true;
        } else if (overheated && heat < 0.8f) {
            overheated = false;
        }
        if (overheated && !sleeping) {
            steamIn -= dt;
            if (steamIn <= 0f) {
                if (attackType == 1) {
                    steamIn = 0.14f;
                    cheekSparks(1);               // Pikachu crackles with static
                } else {
                    steamIn = 0.22f;
                    Particle s = new Particle();
                    s.tex = steamTex;
                    s.x = CAT_X + eyeCenterX + MathUtils.random(-5f, 4f);
                    s.y = eyeCenterY + 6f;
                    s.vx = MathUtils.random(-1.5f, 1.5f);
                    s.vy = MathUtils.random(5f, 8f);
                    s.maxLife = 1.1f;
                    s.scale = MathUtils.randomBoolean() ? 1f : 1.3f;
                    particles.add(s);
                }
            }
        }

        float shellBefore = shellLeft;
        shellLeft -= dt;
        if (shellBefore > 0f && shellLeft <= 0f) {
            squash.kick(3f);
        }

        // belly-up after ~3 s of continuous petting
        if (petActive && rollTex != null && seated() && !sleeping && !dragging) {
            petHold += dt;
            if (petHold > 3f) {
                if (rollLeft <= 0f) {
                    squash.kick(3f);
                }
                rollLeft = 1.2f;                  // stays over while rubs continue
            }
        } else {
            petHold = 0f;
        }
        float rollBefore = rollLeft;
        rollLeft -= dt;
        if (rollBefore > 0f && rollLeft <= 0f) {
            squash.kick(3f);                      // flops back upright
        }

        boolean busy = boltLeft > 0f || waterLeft > 0f || scratchLeft > 0f || vineLeft > 0f
                || hissLeft > 0f || digLeft > 0f || puffLeft > 0f
                || stretchLeft > 0f || waterRemindLeft > 0f || koT > 0f || trickAnim != null;
        boolean calm = seated() && !sleeping && !dragging && !pressed && !hidden
                && !petActive && typingLeft <= 0f && !danceNow && !busy
                && rollLeft <= 0f && crouchLeft <= 0f && shellLeft <= 0f;

        groomLeft -= dt;
        sparkLeft -= dt;
        if (!calm) {
            groomLeft = 0f;                       // interrupted mid-wash
        } else if (isCat()) {
            groomIn -= dt;
            if (groomIn <= 0f) {
                groomIn = MathUtils.random(25f, 55f);
                groomLeft = 2.6f;
            }
        } else if (attackType == 1) {
            sparkIn -= dt;
            if (sparkIn <= 0f) {
                sparkIn = MathUtils.random(6f, 15f);
                sparkLeft = 0.5f;
                cheekSparks(4);
            }
        }

        // cats: stalk a slow cursor beside them, crouch, wiggle, pounce
        pounceCool -= dt;
        if (crouchLeft > 0f) {
            if (cursorSpeed > 900f || dragging || pressed) {
                crouchLeft = 0f;                  // spooked: the stalk is off
            } else {
                crouchLeft -= dt;
                if (crouchLeft <= 0f) {
                    pounce();
                }
            }
        } else if (isCat() && wanderState == 0 && calm && pounceCool <= 0f) {
            float pcx = window.getPositionX() + winW() / 2f;
            float pfy = window.getPositionY() + winH();
            float ddx = cursor.x - pcx, ddy = cursor.y - pfy;
            boolean beside = Math.abs(ddx) > winW() * 0.6f && Math.abs(ddx) < 280f
                    && ddy > -winH() * 1.2f && ddy < 60f;
            boolean stalking = cursorSpeed > 10f && cursorSpeed < 170f;
            stalkT = beside && stalking ? stalkT + dt : Math.max(0f, stalkT - dt * 2f);
            if (stalkT > 0.6f) {
                stalkT = 0f;
                crouchLeft = 0.9f;
                pounceDir = ddx < 0f ? -1 : 1;
            }
        }
    }

    /** Leap toward the cursor, then swat on landing (see the hop state). */
    private void pounce() {
        float pcx = window.getPositionX() + winW() / 2f;
        int dx = Math.round(MathUtils.clamp(cursor.x - pcx, -170f, 170f));
        Rectangle area = screenArea(Math.round(pcx), window.getPositionY());
        int tx = MathUtils.clamp(window.getPositionX() + dx, area.x,
                area.x + area.width - winW());
        pounceSwipe = true;
        pounceCool = MathUtils.random(10f, 18f);
        hopTo(tx, window.getPositionY(), 0);
    }

    private void cheekSparks(int n) {
        for (int i = 0; i < n; i++) {
            Particle s = new Particle();
            s.tex = sparkTex;
            boolean left = i % 2 == 0;
            s.x = CAT_X + (left ? cheekLX - 2f : cheekRX + 1f);
            s.y = cheekY + MathUtils.random(-1f, 1.5f);
            s.vx = (left ? -1f : 1f) * MathUtils.random(6f, 12f);
            s.vy = MathUtils.random(-2f, 6f);
            s.maxLife = MathUtils.random(0.18f, 0.32f);
            s.scale = 0.7f;
            particles.add(s);
        }
    }

    private boolean isCat() {
        return attackType == 0 || attackType == 3;
    }

    /** Sitting still somewhere: at home, or perched on a window. */
    private boolean seated() {
        return wanderState == 0 || wanderState == 7;
    }

    /** Parabolic hop of the window to (tx, ty); wanderState = next on landing. */
    private void hopTo(int tx, int ty, int next) {
        hopFX = window.getPositionX();
        hopFY = window.getPositionY();
        hopTX = tx;
        hopTY = ty;
        float dist = (float) Math.hypot(tx - hopFX, ty - hopFY);
        hopDur = MathUtils.clamp(dist / 900f, 0.28f, 0.75f);
        hopH = MathUtils.clamp(dist * 0.25f, 30f, 140f);
        hopT = 0f;
        hopNext = next;
        wanderState = 6;
        squash.kick(4f);
    }

    /** Work area (screen minus taskbar) of the monitor containing a point. */
    private static Rectangle screenArea(int x, int y) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.GraphicsDevice dev = ge.getDefaultScreenDevice();
            for (java.awt.GraphicsDevice d : ge.getScreenDevices()) {
                if (d.getDefaultConfiguration().getBounds().contains(x, y)) {
                    dev = d;
                    break;
                }
            }
            GraphicsConfiguration gc = dev.getDefaultConfiguration();
            Rectangle b = gc.getBounds();
            Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            return new Rectangle(b.x + in.left, b.y + in.top,
                    b.width - in.left - in.right, b.height - in.top - in.bottom);
        } catch (Throwable t) {
            return new Rectangle(0, 0, 1920, 1080);
        }
    }

    /** Hop up onto the title bar of the window being used, if there's room. */
    private boolean startPerch() {
        long hwnd = Win32Windows.foreground();
        if (!Win32Windows.perchable(hwnd) || !Win32Windows.rect(hwnd, perchRect)) {
            return false;
        }
        int left = perchRect[0], top = perchRect[1], right = perchRect[2];
        int w = winW(), h = winH();
        int y = top - h + scale;
        if (right - left < w + 60
                || y < screenArea(left + (right - left) / 2, top).y) {
            return false;   // too narrow, or no room above it
        }
        int lo = left + 24, hi = Math.max(lo, right - w - 24);
        int x = MathUtils.clamp(window.getPositionX(), lo, hi);
        homeX = window.getPositionX();
        homeY = window.getPositionY();
        perchHwnd = hwnd;
        perchOffX = x - left;
        perchLeft = MathUtils.random(60f, 150f);
        hopTo(x, y, 7);
        return true;
    }

    /** Ride along with the window; leave when it goes away or we get bored. */
    private void updatePerch(float dt) {
        perchLeft -= dt;
        if (!Win32Windows.stillPerchable(perchHwnd)
                || !Win32Windows.rect(perchHwnd, perchRect)) {
            dropFromPerch();   // closed, minimised or maximised: tumble down
            return;
        }
        int left = perchRect[0], top = perchRect[1], right = perchRect[2];
        int w = winW(), h = winH();
        int x = left + Math.min(perchOffX, Math.max(0, right - left - w));
        int y = top - h + scale;
        if (y < screenArea(x + w / 2, top).y) {
            dropFromPerch();   // its window was pushed up against the screen top
            return;
        }
        if (x != window.getPositionX() || y != window.getPositionY()) {
            window.setPosition(x, y);
        }
        if (perchLeft <= 0f && !sleeping) {
            perchHwnd = 0;
            hopTo(homeX, homeY, 0);   // bored of this window: hop back home
        }
    }

    /** Fall to the floor and patrol; input still brings it home. */
    private void dropFromPerch() {
        perchHwnd = 0;
        int hx = homeX, hy = homeY;
        wake();
        startWander();
        homeX = hx;   // keep the pre-perch home, not the empty perch
        homeY = hy;
    }

    /** A whole-frame pose replaces body, tail and eyes; null = sit normally. */
    private Texture poseFrame() {
        if (shellTex != null && shellLeft > 0f) {
            return shellTex;
        }
        if (sleeping && sleepTex != null) {
            return sleepTex[((int) (time / 1.1f)) % sleepTex.length];
        }
        if (rollLeft > 0f && rollTex != null) {
            return rollTex[((int) (time * 5f)) % rollTex.length];
        }
        if (walkTex != null && !swims && (wanderState == 2 || wanderState == 3)) {
            // feet planted in a stride move back ~1.7 units a frame, so match
            // the frame rate to how fast the window travels (85 or 140 px/s)
            float speed = (wanderState == 3 ? 140f : 85f) / Math.max(1, scale);
            float fps = MathUtils.clamp(speed / 1.7f, 6f, 24f);
            return walkTex[((int) (time * fps)) % walkTex.length];
        }
        return null;
    }

    /** Walk frames turn about the FBO's mirror axis; other poses sit centred. */
    private float poseCenterX() {
        return moving() ? FBO_W / 2f : BODY_X + bodyTex.getWidth() / 2f;
    }

    private void drawShellEyes(float poseX) {
        for (int i = 0; i < 2; i++) {
            float x = poseX + shellEyeX + i * 3;
            batch.setColor(Color.WHITE);
            batch.draw(px, x, shellEyeY, 1, 1);
            batch.setColor(irisColor);
            batch.draw(px, x + 1, shellEyeY, 1, 1);
        }
        batch.setColor(Color.WHITE);
    }

    /** Cat grooming: licks a raised paw, then wipes it over its face. */
    private void drawGroom(float shake) {
        float k = MathUtils.sin(time * 9f);
        boolean wiping = groomLeft < 1.2f;
        float pawX = eyeCenterX - 2f + (wiping ? k * 2.5f : 0f) + shake;
        float pawY = eyeY - 6f + Math.abs(k) * 1.5f;
        batch.draw(pawTex, pawX, pawY, 4, 4);
        if (!wiping && k > 0.3f) {               // tongue flick
            batch.setColor(C_TONGUE);
            batch.draw(px, eyeCenterX - 0.5f + shake, pawY + 4f, 1, 1);
            batch.setColor(Color.WHITE);
        }
    }

    private void updateWander(float dt) {
        // any user input sends it hurrying back to where it started
        if ((roaming())
                && (cursorSpeed > 40f || typingLeft > 0f)) {
            beginReturn();
        }

        if (wanderState == 0) {
            boolean eligible = !sleeping && !dragging && !pressed && !petActive
                    && !hidden && !quiet && !fullscreen && trickAnim == null
                    && boltLeft <= 0f && waterLeft <= 0f
                    && scratchLeft <= 0f && typingLeft <= 0f
                    && hissLeft <= 0f && digLeft <= 0f && puffLeft <= 0f
                    && stretchLeft <= 0f && waterRemindLeft <= 0f
                    && idleTime > 3f;
            eligible = eligible && crouchLeft <= 0f && rollLeft <= 0f
                    && shellLeft <= 0f && groomLeft <= 0f;
            if (eligible) {
                wanderIn -= dt;
                if (wanderIn <= 0f) {
                    // sometimes hop up onto the window in use instead
                    if (swims || !MathUtils.randomBoolean(0.55f) || !startPerch()) {
                        startWander();
                    }
                }
            }
        } else if (wanderState == 1) {
            fallVy += 2600f * dt;
            float ny = window.getPositionY() + fallVy * dt;
            if (ny >= floorY) {
                ny = floorY;
                wanderState = 2;
                squash.kick(-5f);   // landing squash
                winXf = window.getPositionX();
            }
            window.setPosition(window.getPositionX(), Math.round(ny));
        } else if (wanderState == 2) {
            facingLeft = patrolDir < 0;
            winXf += patrolDir * 85f * dt;
            // wraparound: walk fully off one edge, reappear at the other
            if (patrolDir > 0 && winXf > patrolMaxX) {
                winXf = patrolMinX - winW();
            } else if (patrolDir < 0 && winXf + winW() < patrolMinX) {
                winXf = patrolMaxX;
            }
            window.setPosition(Math.round(winXf), floorY);
        } else if (wanderState == 3 && swims) {
            // swim straight home on both axes; no hop needed
            float dx = homeX - swimXf, dy = homeY - swimYf;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 4f) {
                window.setPosition(homeX, homeY);
                wanderState = 0;
                wanderIn = MathUtils.random(18f, 40f);
                facingLeft = false;
                squash.kick(2f);
            } else {
                float step = Math.min(dist, 160f * dt);
                swimXf += dx / dist * step;
                swimYf += dy / dist * step;
                facingLeft = dx < 0f;
                window.setPosition(Math.round(swimXf), Math.round(swimYf));
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
                squash.kick(2f);   // little settle wobble back on its perch
            }
        } else if (wanderState == 5) {
            // lazy S-curves: steer at the target with a sideways wobble
            float dx = swimTX - swimXf, dy = swimTY - swimYf;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 12f) {
                pickSwimTarget();
            } else {
                float nx = dx / dist, ny = dy / dist;
                float wob = MathUtils.sin(time * 2.2f) * 0.45f;
                swimXf += (nx - ny * wob) * 70f * dt;
                swimYf += (ny + nx * wob) * 70f * dt;
                if (Math.abs(dx) > 2f) {
                    facingLeft = dx < 0f;
                }
            }
            window.setPosition(Math.round(swimXf), Math.round(swimYf));
        } else if (wanderState == 6) {
            hopT += dt;
            float t = Math.min(1f, hopT / hopDur);
            float hx = MathUtils.lerp(hopFX, hopTX, t);
            float hy = MathUtils.lerp(hopFY, hopTY, t) - 4f * hopH * t * (1f - t);
            window.setPosition(Math.round(hx), Math.round(hy));
            if (t >= 1f) {
                wanderState = hopNext;
                facingLeft = false;
                squash.kick(-4f);   // landing squash
                if (hopNext == 0) {
                    wanderIn = MathUtils.random(18f, 40f);
                }
                if (pounceSwipe) {   // a pounce ends in a swat
                    pounceSwipe = false;
                    scratchLeft = SCRATCH_DUR;
                    scratchDir = pounceDir;
                }
            }
        } else if (wanderState == 7) {
            updatePerch(dt);
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
            if (swims) {
                // roam the whole work area instead of dropping to the floor
                homeX = window.getPositionX();
                homeY = window.getPositionY();
                swimMinX = b.x + ins.left;
                swimMinY = b.y + ins.top;
                swimMaxX = b.x + b.width - ins.right - winW();
                swimMaxY = b.y + b.height - ins.bottom - winH();
                swimXf = homeX;
                swimYf = homeY;
                pickSwimTarget();
                wanderState = 5;
                return;
            }
            // insets auto-detect the taskbar; bottomGap only adds extra room
            floorY = b.y + b.height - ins.bottom - winH() - Math.max(0, bottomGap);
            homeX = window.getPositionX();
            homeY = window.getPositionY();
            // wrap bounds: full screen edges, so the pet leaves completely
            // before reappearing on the far side
            patrolMinX = b.x;
            patrolMaxX = b.x + b.width;
            patrolDir = MathUtils.randomBoolean() ? -1 : 1;
            winXf = homeX;
            facingLeft = patrolDir < 0;
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
        swimXf = window.getPositionX();
        swimYf = window.getPositionY();
    }

    private void updateSpring(float dt) {
        float target = dragging ? 1.2f
                : (wanderState == 1 ? 1.12f          // stretch while falling
                : (wanderState == 4 || wanderState == 6 ? 1.08f   // and mid-hop
                : (crouchLeft > 0f ? 0.8f           // crouched to pounce
                : 1f)));
        scaleY = squash.update(target, dt);
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
                : (alertLeft > 0f || crouchLeft > 0f ? 0.07f
                : (moving() ? 0.15f
                : (danceNow ? 0.18f : 0.3f)));
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
        if (scratchLeft > 0f) {
            shake += scratchExt() * 1.5f * scratchDir;   // lean into the swipe
        }
        if (digLeft > 0f) {
            shake += MathUtils.sin(time * 38f) * 0.45f;    // frantic digging
        }
        if (crouchLeft > 0f && crouchLeft < 0.45f) {
            shake += MathUtils.sin(time * 40f) * 0.5f;     // the pre-pounce wiggle
        }

        Texture pose = poseFrame();
        if (pose != null) {
            // keep wide poses (sleeping, belly-up) inside the frame
            float half = pose.getWidth() / 2f;
            float poseX = MathUtils.clamp(poseCenterX(), half, FBO_W - half) - half + shake;
            batch.draw(pose, poseX, 0, pose.getWidth(), pose.getHeight());
            if (pose == shellTex && shellLeft <= 0.7f) {
                drawShellEyes(poseX);   // peeking out before popping back
            }
            batch.end();
            fbo.end();
            return;
        }

        Texture tail = tailTex[TAIL_CYCLE[tailFrame]];
        batch.draw(tail, tailX + shake, 0, tail.getWidth(), tail.getHeight());
        batch.draw(bodyTex, BODY_X + shake, 0,
                bodyTex.getWidth(), bodyTex.getHeight());
        if (hissLeft > 0f) {
            drawHissFur(shake);
        }

        // determined attack face while the bolt flies
        if (attackType == 1 && (boltLeft > 0f || sparkLeft > 0f || overheated)
                && MathUtils.sin(time * 40f) > 0f) {
            batch.setColor(1f, 1f, 0.75f, 1f);
            batch.draw(px, cheekLX - 1.5f + shake, cheekY - 1.5f, 3, 3);   // cheeks spark
            batch.draw(px, cheekRX - 1.5f + shake, cheekY - 1.5f, 3, 3);
            batch.setColor(Color.WHITE);
        }

        boolean closed = sleeping || blinkLeft > 0f || boltLeft > 0.15f
                || groomLeft > 0f
                || waterLeft > 0.2f;
        if (closed) {
            drawClosedEye(eyeLX + shake);
            if (!oneEye) {
                drawClosedEye(eyeRX + shake);
            }
        } else {
            int pgx = gazeX > 0.3f ? 1 : (gazeX < -0.3f ? -1 : 0);
            if (scratchLeft > 0f) {
                pgx = scratchDir;   // watch the paw it's swinging
            }
            if ((moving()) && facingLeft) {
                pgx = -pgx;   // the whole FBO is mirrored while walking left
            }
            int irisY = kneadNow || digLeft > 0f || gazeY < -0.25f ? eyeY : eyeY + 1;
            drawOpenEye(eyeLX + shake, pgx, irisY);
            if (!oneEye) {
                drawOpenEye(eyeRX + shake, pgx, irisY);
            }
        }

        if (kneadNow && !swims) {
            // overheated typing makes it knead in a frenzy
            boolean leftUp = MathUtils.sin(time * (overheated ? 26f : 14f)) > 0f;
            batch.draw(pawTex, eyeCenterX - 5f + shake, leftUp ? 1 : 0, 4, 4);
            batch.draw(pawTex, eyeCenterX + 4f + shake, leftUp ? 0 : 1, 4, 4);
        }
        if (groomLeft > 0f) {
            drawGroom(shake);
        }

        if (scratchLeft > 0f) {
            drawScratch(shake);
        }
        if (digLeft > 0f) {
            boolean leftDown = MathUtils.sin(time * 30f) > 0f;
            batch.draw(pawTex, eyeCenterX - 7f + shake, leftDown ? 0 : 2, 4, 4);
            batch.draw(pawTex, eyeCenterX + 3f + shake, leftDown ? 2 : 0, 4, 4);
        }

        batch.end();
        fbo.end();
    }

    /** Bristled fur: dark spikes off both flanks and a ridge on the skull. */
    private void drawHissFur(float shake) {
        float grown = Math.min(1f, (HISS_ARCH + SCRATCH_DUR - hissLeft) / 0.2f);
        batch.setColor(lineColor);
        for (int i = 0; i < 5; i++) {
            float y = 4f + i * 2.2f;
            float len = 1f + grown * 2f + (i % 2);
            float side = i < 2 ? 12f : 10f;   // haunches flare wider than the chest
            batch.draw(px, eyeCenterX - side - len + shake, y, len, 1f);
            batch.draw(px, eyeCenterX + side - 1f + shake, y, len, 1f);
        }
        for (int i = -2; i <= 2; i++) {
            float h = 1f + grown * (2f - Math.abs(i) * 0.5f);
            batch.draw(px, eyeCenterX + i * 2f - 0.5f + shake, eyeY + 8f, 1f, h);
        }
        batch.setColor(Color.WHITE);
    }

    /** Pufferfish curve: swell fast, hold, then deflate with a wobble. */
    private float puffScale() {
        if (puffLeft <= 0f) {
            return 1f;
        }
        float e = PUFF_DUR - puffLeft;
        float amt = e < 0.22f ? e / 0.22f : (puffLeft > 0.4f ? 1f : puffLeft / 0.4f);
        float ease = amt * amt * (3f - 2f * amt);
        return 1f + 0.3f * ease + 0.02f * MathUtils.sin(time * 30f) * ease;
    }

    /** Out wandering: falling, patrolling the floor, or free-swimming. */
    private boolean roaming() {
        return wanderState == 5 || wanderState == 2 || wanderState == 1;
    }

    /** Travelling sideways, so the sprite should face its direction. */
    private boolean moving() {
        return wanderState == 5 || wanderState == 3 || wanderState == 2;
    }

    private void pickSwimTarget() {
        swimTX = MathUtils.random(swimMinX, Math.max(swimMinX, swimMaxX));
        swimTY = MathUtils.random(swimMinY, Math.max(swimMinY, swimMaxY));
    }

    /** 0 at rest, 1 at full reach; two swipes over SCRATCH_DUR. */
    private float scratchExt() {
        float phase = (SCRATCH_DUR - scratchLeft) / (SCRATCH_DUR / 2f);
        return MathUtils.sin((phase - (int) phase) * MathUtils.PI);
    }

    /** One paw thrust out to the cursor side, claw marks at full reach. */
    private void drawScratch(float shake) {
        float ext = scratchExt();
        float pawX = eyeCenterX - 2f + scratchDir * (6f + ext * 7f) + shake;
        float pawY = 4f + ext * 1.5f;
        batch.draw(pawTex, pawX, pawY, 4, 4);
        if (ext > 0.55f) {
            batch.setColor(1f, 1f, 1f, (ext - 0.55f) / 0.45f);
            float clawX = pawX + (scratchDir > 0 ? 4.5f : -1.5f);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    batch.draw(px, clawX + (i * 2f + j * 0.7f) * scratchDir,
                            pawY + 3.5f - j - i * 0.4f, 1f, 1f);
                }
            }
            batch.setColor(Color.WHITE);
        }
    }

    /**
     * Vine Whip: a vine grows straight up out of each side of the bulb into
     * the open space above the pet, leaning outward and rippling, cracks at
     * the top (a burst of leaves off each tip) and sinks back. Drawn in
     * window space after the pet, following its bob, squash and facing.
     */
    private void drawVines(float bob, float sx, float sy) {
        float prog = 1f - vineLeft / VINE_DUR;
        float ext = MathUtils.sin(MathUtils.clamp(prog, 0f, 1f) * MathUtils.PI);
        int len = Math.round(ext * 14f);
        Color vine = Color.valueOf("5DB54A"), dark = Color.valueOf("1F3B33");
        float[][] tips = new float[2][2];
        for (int pass = 0; pass < 2; pass++) {
            batch.setColor(pass == 0 ? dark : vine);
            for (int side = 0; side < 2; side++) {
                int dir = side == 0 ? -1 : 1;
                float baseX = eyeCenterX - 0.5f + dir * 7.5f;    // the bulb's flanks
                for (int i = 0; i <= len; i++) {
                    float t = i / 14f;
                    float fx = baseX + dir * (0.35f * i + MathUtils.sin(i * 0.8f - time * 22f) * t);
                    float fy = 23f + i;
                    float x = CAT_X + FBO_W / 2f + (fx - FBO_W / 2f) * sx;
                    float y = bob + fy * sy;
                    if (pass == 0) {
                        batch.draw(px, x - 1f, y - 1f, 3, 3);   // outline
                    } else {
                        batch.draw(px, x - 0.5f, y - 0.5f, 2, 2);
                    }
                    if (i == len) {
                        tips[side][0] = x;
                        tips[side][1] = y;
                    }
                }
            }
        }
        batch.setColor(Color.WHITE);
        if (!vineCracked && prog >= 0.5f) {
            vineCracked = true;                                 // crack! leaves fly off both tips
            for (float[] tip : tips) {
                sparkBurst(tip[0], tip[1], 4, leafTex, 12f, 0f, 2f);
            }
        }
    }

    private void drawOpenEye(float eyeX, int pgx, int irisY) {
        float gazeDown = kneadNow || digLeft > 0f ? -1f : gazeY;   // watch the paws
        if (eyeStyle == 1) {
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
        } else if (eyeStyle == 5) {
            // the iris and its ring are baked into the body; only the pupil
            // and glint move. Startled: tall, blown pupils
            batch.setColor(C_PUPIL);
            if (alertLeft > 0f || hissLeft > 0f || crouchLeft > 0f) {
                batch.draw(px, eyeX + 1, eyeY, 2, eyeH);
            } else {
                float pupilX = eyeX + 1 + pgx;
                batch.draw(px, pupilX, irisY, 2, 2);
                batch.setColor(Color.WHITE);
                batch.draw(px, pupilX, irisY + 1, 1, 1);
            }
        } else if (eyeStyle == 6) {
            // white baked socket; a 3x3 red iris with an upright pupil and a
            // glint moves inside it, resting toward the nose so it looks at
            // you. Startled: the pupil swallows the iris
            int inner = eyeX < eyeCenterX ? 1 : 0;
            float ix = eyeX + MathUtils.clamp(inner + pgx, 0, 1);
            if (alertLeft > 0f || crouchLeft > 0f) {
                batch.setColor(C_PUPIL);
                batch.draw(px, ix, irisY, 3, 3);
            } else {
                batch.setColor(irisColor);
                batch.draw(px, ix, irisY, 3, 3);
                batch.setColor(C_PUPIL);
                batch.draw(px, ix + 1, irisY, 1, 2);
                batch.setColor(Color.WHITE);
                batch.draw(px, ix, irisY + 2, 1, 1);
            }
        } else if (eyeStyle == 4) {
            // round bead (corners cut) with a two-pixel glint following the gaze
            batch.setColor(C_OUTLINE);
            batch.draw(px, eyeX + 1, eyeY, eyeW - 2, eyeH);
            batch.draw(px, eyeX, eyeY + 1, eyeW, eyeH - 2);
            if (alertLeft <= 0f) {
                batch.setColor(Color.WHITE);
                float glintX = eyeX + (pgx < 0 ? 0 : 1);
                float glintTop = gazeDown >= 0f ? eyeY + eyeH - 2 : eyeY + eyeH - 3;
                batch.draw(px, glintX, glintTop - 1, 1, 2);
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
        if (eyeStyle == 5 || eyeStyle == 6) {
            // fur over the socket (ring included), then a content curve
            batch.setColor(furColor);
            batch.draw(px, eyeX, eyeY - 1, eyeW, eyeH + 2);
            batch.draw(px, eyeX - 1, eyeY, eyeW + 2, eyeH);
            batch.setColor(lineColor);
            batch.draw(px, eyeX, eyeY + 1, eyeW, 1);
            batch.draw(px, eyeX - 1, eyeY + 2, 1, 1);
            batch.draw(px, eyeX + eyeW, eyeY + 2, 1, 1);
        } else if (eyeStyle == 2) {
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
        if (moving()) {
            if (swims) {
                // a slow undulating glide rather than a waddle
                bob = MathUtils.sin(time * 4f) * 0.6f + 0.6f;
                waddle = MathUtils.sin(time * 4f) * 2f;
            } else {
                float rate = wanderState == 3 ? 12f : 9f;   // hurried gait going home
                // real walk frames move their legs, so drop the sticker waddle
                // walk frames bake their own bob; others get the sticker bounce
                bob = walkTex != null ? 0f : Math.abs(MathUtils.sin(time * rate)) * 0.8f;
                waddle = walkTex != null ? 0f : MathUtils.sin(time * rate) * 3f;
            }
            if (facingLeft) {
                scaleX = -scaleX;
            }
        } else if (danceNow) {
            // groove scaled by how loud the music actually is
            float amp = 0.3f + 0.7f * musicAmp;
            bob = Math.abs(MathUtils.sin(time * 7f)) * 1.2f * amp;
            waddle = MathUtils.sin(time * 7f) * 3.5f * amp;
        } else if (swims) {
            bob = MathUtils.sin(time * 2f) * 0.5f + 0.5f;   // treading water
        }

        // knocked out by a shot: fall over 90° or squish flat, then recover
        float drawScaleY = scaleY;
        if (koT > 0f) {
            float e = KO_TOTAL - koT;
            if (koMode == 0) {
                float rot;
                if (e < 0.25f) {
                    rot = 90f * (e / 0.25f);              // topple
                } else if (e < 1.1f) {
                    rot = 90f;                            // lie there
                } else {
                    rot = 90f * (1f - (e - 1.1f) / 0.5f); // get back up
                }
                waddle += rot * koDir;
            } else {
                float squish;
                if (e < 0.15f) {
                    squish = 1f - 0.75f * (e / 0.15f);    // flatten
                } else if (e < 1.0f) {
                    squish = 0.25f;                       // pancake
                } else {
                    squish = 0.25f + 0.75f * Math.min(1f, (e - 1.0f) / 0.6f);
                }
                drawScaleY *= squish;
                scaleX *= 1f + (1f - squish) * 0.6f;      // spread sideways
            }
        }
        float puff = puffScale();
        if (overheated) {
            float f = 0.76f + 0.08f * MathUtils.sin(time * 9f);
            batch.setColor(1f, f, f, 1f);   // flushed red
        }
        Texture remindFrame = reminderFrame();
        if (dragging && carryTex != null) {
            // carried: the dangle loop hangs from the cursor and swings about
            // it, with a springy stretch for a moment after pick-up
            carryRegion.setRegion(carryTex[(int) (carryT / CarryArt.FRAME_SECS) % carryTex.length]);
            float stretch = 1f + 0.18f * (float) Math.exp(-carryT * 7f)
                    * MathUtils.cos(carryT * 30f);
            float gx = CARRY_GRAB_X, gy = UNITS_H - CARRY_GRAB_Y;
            batch.setColor(Color.WHITE);
            batch.draw(carryRegion, 0, 0, gx, gy, UNITS_W, UNITS_H,
                    1f, stretch, swing * MathUtils.radiansToDegrees);
        } else if (trickFrame() != null) {
            batch.setColor(Color.WHITE);
            batch.draw(trickFrame(), 0, 0, UNITS_W, UNITS_H);
        } else if (remindFrame != null) {
            // reminder animations are whole window-sized frames, props included
            batch.setColor(Color.WHITE);
            batch.draw(remindFrame, 0, 0, UNITS_W, UNITS_H);
        } else {
            batch.draw(fboRegion, CAT_X, bob, FBO_W / 2f, 0,
                    FBO_W, FBO_H, scaleX * puff, drawScaleY * puff, waddle);
        }
        batch.setColor(Color.WHITE);
        if (vineLeft > 0f) {
            drawVines(bob, scaleX * puff, drawScaleY * puff);
        }

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

        long nowMs = System.currentTimeMillis();
        if (ownBubble.isActive(nowMs)) {
            if (Bubbles.fits(font, ownBubble.text(), ownBubble.scale(),
                    winW(), winH() - 4)) {
                batch.setProjectionMatrix(pxCam.combined);
                batch.begin();
                Bubbles.draw(batch, font, px, ownBubble.text(),
                        ownBubble.alpha(nowMs), winW(), winH() - 4,
                        ownBubble.scale(), ownBubble.effect(),
                        ownBubble.colorHex(), time);
                batch.end();
            } else if (ownBubbleSpawned != ownBubble.untilMs()) {
                // too big for the pet window: overlay window, sized to screen
                ownBubbleSpawned = ownBubble.untilMs();
                BubbleFx.spawn((Lwjgl3Application) Gdx.app, font, ownBubble,
                        () -> new float[] {mainWinX + winW() / 2f, mainWinY});
            }
        }
    }

    @Override
    public void dispose() {
        endReminderAnim();
        endTrick();
        disposeTricks();
        disposeCarry();
        if (lan != null) {
            lan.close();
        }
        if (trayLock != null) {
            try {
                trayLock.close();
            } catch (Throwable ignored) {
            }
        }
        SystemAudio.stop();
        SoundFx.dispose();
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
        font.dispose();
        Fonts.disposeAll();
        SkinAssets.disposeAll();
        heartTex.dispose();
        zzzTex.dispose();
        alertTex.dispose();
        sparkTex.dispose();
        leafTex.dispose();
        waterDropTex.dispose();
        noteTex.dispose();
        dirtTex.dispose();
        bubbleTex.dispose();
        steamTex.dispose();
        px.dispose();
    }
}
