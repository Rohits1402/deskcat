package com.deskcat;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {

    public static void main(String[] args) {
        if (args.length > 0) {
            CatApp.skin = args[0];
        }
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("DeskCat");
        cfg.setWindowedMode(CatApp.WIN_W_PX, CatApp.WIN_H_PX);
        cfg.setDecorated(false);
        cfg.setResizable(false);
        cfg.setTransparentFramebuffer(true);
        cfg.setForegroundFPS(60);
        cfg.setIdleFPS(30);
        Graphics.DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
        cfg.setWindowPosition(dm.width - CatApp.WIN_W_PX - 60,
                dm.height - CatApp.WIN_H_PX - 120);
        new Lwjgl3Application(new CatApp(), cfg);
        // the AWT tray thread is non-daemon and would keep the JVM alive
        System.exit(0);
    }
}
