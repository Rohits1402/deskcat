package com.deskcat;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {

    /** Usage: DesktopLauncher [skin] [display name] */
    public static void main(String[] args) {
        if (args.length > 0) {
            CatApp.skin = args[0];
        }
        if (args.length > 1) {
            CatApp.userName = args[1];
        }
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("DeskCat");
        cfg.setWindowedMode(CatApp.winW(), CatApp.winH());
        cfg.setDecorated(false);
        cfg.setResizable(false);
        cfg.setTransparentFramebuffer(true);
        cfg.setForegroundFPS(60);
        cfg.setIdleFPS(30);
        Graphics.DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
        cfg.setWindowPosition(dm.width - CatApp.winW() - 60,
                dm.height - CatApp.winH() - 120);
        new Lwjgl3Application(new CatApp(), cfg);
        // the AWT tray thread is non-daemon and would keep the JVM alive
        System.exit(0);
    }
}
