package com.deskcat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * "Patch notes" window opened from the tray. Update {@link #NOTES} when
 * cutting a release (newest entry on top).
 */
public final class PatchNotes {

    static final String NOTES = ""
            + "DeskCat " + CatApp.VERSION + " — patch notes\n"
            + "\n"
            + "v1.2.0\n"
            + "------\n"
            + "* Chat commands: /big /huge /small /size N, /shake, /rainbow,\n"
            + "  /color <name> — chainable, everyone sees the same bubble\n"
            + "* Huge /size text opens a crisp screen-sized overlay bubble\n"
            + "* /shoot (chat or right-click menus): a pellet flies between\n"
            + "  pets; the target squishes flat or falls over, then recovers\n"
            + "* Speech bubbles word-wrap; the chat box grows while you type\n"
            + "* Patrol wraps around the screen edges instead of turning\n"
            + "* Friends' pets fade in; tray Peer fade sets their opacity\n"
            + "* One tray icon per machine; Patch notes window in the tray\n"
            + "\n"
            + "v1.1.0\n"
            + "------\n"
            + "* LAN presence: see teammates' pets with name labels\n"
            + "* Chat with speech bubbles, broadcasts and @name DMs\n"
            + "* Right-click menus: say, dismiss, hide behind taskbar\n"
            + "* Size control (Small/Normal/Large) and taskbar gap\n"
            + "* Stretch/water reminders, procedural sound, music groove\n"
            + "* Start with Windows, auto-updater, single runnable jar\n"
            + "\n"
            + "v1.0.0\n"
            + "------\n"
            + "* Squirtle, Pikachu and cat skins with click attacks\n"
            + "* Eye tracking, mochi drag, petting, kneading, patrol, sleep\n";

    private static Window open;

    private PatchNotes() {
    }

    /** Safe to call from any thread; everything hops to the EDT. */
    public static void show() {
        SwingUtilities.invokeLater(() -> {
            if (open != null) {
                open.dispose();
            }
            JFrame w = new JFrame("DeskCat — patch notes");
            w.setType(Window.Type.UTILITY);
            w.setAlwaysOnTop(true);
            w.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JTextArea area = new JTextArea(NOTES);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            area.setBackground(new Color(0x26, 0x20, 0x2A));
            area.setForeground(Color.WHITE);
            area.setCaretColor(Color.WHITE);
            area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            area.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        w.dispose();
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(area);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setPreferredSize(new Dimension(460, 420));
            w.add(scroll);
            w.pack();
            w.setLocationRelativeTo(null);
            w.setVisible(true);
            open = w;
        });
    }
}
