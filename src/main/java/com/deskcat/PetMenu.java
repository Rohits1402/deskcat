package com.deskcat;

import java.awt.Component;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Small right-click context menu shown at a screen position (the pets have no
 * window chrome, so an invisible 1x1 anchor window hosts the popup).
 * Actions run on the EDT — wrap GL work in Gdx.app.postRunnable.
 */
public final class PetMenu {

    public static final class Item {
        final String label;
        final Runnable action;

        public Item(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    private PetMenu() {
    }

    /** Safe to call from the GL thread. */
    public static void show(int screenX, int screenY, Item... items) {
        SwingUtilities.invokeLater(() -> {
            JWindow anchor = new JWindow();
            anchor.setFocusableWindowState(true);
            anchor.setAlwaysOnTop(true);
            anchor.setSize(1, 1);
            anchor.setLocation(screenX, screenY);
            anchor.setVisible(true);

            JPopupMenu menu = new JPopupMenu();
            for (Item it : items) {
                JMenuItem mi = new JMenuItem(it.label);
                mi.addActionListener(e -> it.action.run());
                menu.add(mi);
            }
            menu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    anchor.dispose();
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
            menu.show((Component) anchor.getContentPane(), 0, 0);
        });
    }
}
