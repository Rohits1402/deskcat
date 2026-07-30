package com.deskcat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/**
 * Windows-style volume flyout: clicking Sound in the tray pops a bare slider
 * next to the cursor. Dragging it sets {@link SoundFx#volume} live; 0 is
 * silence. It closes when it loses focus or on Escape.
 *
 * Session-only, like the reminder settings — nothing is written to disk.
 */
public final class VolumePopup {

    private static final Color BG = new Color(0x26, 0x20, 0x2A);
    private static final Color FG = Color.WHITE;

    private static Window open;

    private VolumePopup() {
    }

    /** Safe to call from any thread; everything hops to the EDT. */
    public static void show() {
        SwingUtilities.invokeLater(() -> {
            if (open != null) {
                open.dispose();
                open = null;
            }
            final JDialog w = new JDialog((Frame) null);
            w.setUndecorated(true);
            w.setAlwaysOnTop(true);

            JPanel root = new JPanel(new BorderLayout(10, 0));
            root.setBackground(BG);
            root.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x46, 0x3F, 0x4C)),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));

            final JLabel pct = new JLabel();
            pct.setForeground(FG);
            pct.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            pct.setPreferredSize(new Dimension(38, 20));

            final JSlider slider = new JSlider(0, 100,
                    Math.round(SoundFx.volume * 100f));
            slider.setBackground(BG);
            slider.setForeground(FG);
            slider.setPreferredSize(new Dimension(170, 22));
            slider.addChangeListener(e -> {
                SoundFx.volume = slider.getValue() / 100f;
                pct.setText(slider.getValue() + "%");
            });
            pct.setText(slider.getValue() + "%");

            slider.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        w.dispose();
                    }
                }
            });

            root.add(slider, BorderLayout.CENTER);
            root.add(pct, BorderLayout.EAST);
            w.add(root);
            w.pack();
            w.setLocation(spotNearCursor(w.getWidth(), w.getHeight()));

            w.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    w.dispose();
                    open = null;
                }
            });

            w.setVisible(true);
            w.toFront();
            w.requestFocus();
            slider.requestFocusInWindow();
            open = w;
        });
    }

    /** Just above-left of the pointer, kept inside the work area. */
    private static Point spotNearCursor(int w, int h) {
        Rectangle area;
        try {
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            Rectangle b = gc.getBounds();
            Insets in = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc);
            area = new Rectangle(b.x + in.left, b.y + in.top,
                    b.width - in.left - in.right,
                    b.height - in.top - in.bottom);
        } catch (Throwable t) {
            area = new Rectangle(0, 0, 1280, 720);
        }
        int x = area.x + area.width - w - 8;
        int y = area.y + area.height - h - 8;
        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi != null) {
            Point p = pi.getLocation();
            x = p.x - w / 2;
            y = p.y - h - 12;
        }
        x = Math.max(area.x, Math.min(x, area.x + area.width - w));
        y = Math.max(area.y, Math.min(y, area.y + area.height - h));
        return new Point(x, y);
    }
}
