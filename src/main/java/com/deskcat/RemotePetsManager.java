package com.deskcat;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import com.deskcat.net.Peer;

/**
 * Opens and closes one {@link RemotePetWindow} per live peer. Call sync() on
 * the render thread after the registry has been updated/pruned.
 */
public class RemotePetsManager {

    private final Lwjgl3Application app;
    private final BitmapFont font;
    private final Texture px;
    private final BiConsumer<Peer, String> dmSender;
    private final Consumer<Peer> shooter;
    private final Rectangle usable = usableScreenBounds();
    private final Map<String, Lwjgl3Window> windows = new HashMap<String, Lwjgl3Window>();

    public RemotePetsManager(Lwjgl3Application app, BitmapFont font, Texture px,
            BiConsumer<Peer, String> dmSender, Consumer<Peer> shooter) {
        this.app = app;
        this.font = font;
        this.px = px;
        this.dmSender = dmSender;
        this.shooter = shooter;
    }

    /** The primary screen minus the taskbar; also used for position mapping. */
    public static Rectangle usableScreenBounds() {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            Rectangle b = gc.getBounds();
            Insets ins = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            return new Rectangle(b.x + ins.left, b.y + ins.top,
                    b.width - ins.left - ins.right,
                    b.height - ins.top - ins.bottom);
        } catch (Throwable t) {
            return new Rectangle(0, 0, 1920, 1080);
        }
    }

    public Rectangle usable() {
        return usable;
    }

    public void sync(Collection<Peer> peers) {
        Set<String> live = new HashSet<String>();
        for (Peer p : peers) {
            if (p.sameHost) {
                continue;   // another instance on this PC — its pet is already visible
            }
            live.add(p.id);
            if (!windows.containsKey(p.id)) {
                windows.put(p.id, open(p));
            }
        }
        for (Iterator<Map.Entry<String, Lwjgl3Window>> it =
                windows.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Lwjgl3Window> e = it.next();
            if (!live.contains(e.getKey())) {
                e.getValue().closeWindow();
                it.remove();
            }
        }
    }

    private Lwjgl3Window open(Peer peer) {
        Lwjgl3WindowConfiguration cfg = new Lwjgl3WindowConfiguration();
        cfg.setTitle("DeskCat-" + peer.name);
        cfg.setWindowedMode(CatApp.winW(), CatApp.winH());
        cfg.setDecorated(false);
        cfg.setResizable(false);
        // transparent framebuffer is inherited from the app configuration
        cfg.setWindowPosition(
                usable.x + Math.round(peer.xFrac * Math.max(1, usable.width - CatApp.winW())),
                usable.y + Math.round(peer.yFrac * Math.max(1, usable.height - CatApp.winH())));
        return app.newWindow(new RemotePetWindow(peer, font, px, usable,
                text -> dmSender.accept(peer, text),
                () -> shooter.accept(peer)), cfg);
    }

    public void closeAll() {
        for (Lwjgl3Window w : windows.values()) {
            w.closeWindow();
        }
        windows.clear();
    }
}
