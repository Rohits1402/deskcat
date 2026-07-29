package com.deskcat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Tiny always-on-top text field summoned above the pet (middle-click).
 * Enter sends, Escape or losing focus dismisses. "@name message" sends a DM
 * to that peer; anything else broadcasts.
 *
 * An undecorated utility JFrame is used rather than a JWindow — ownerless
 * JWindows can never take keyboard focus on Windows.
 */
public final class ChatInput {

    private static Window open;

    private ChatInput() {
    }

    /** Safe to call from the GL thread; everything hops to the EDT. */
    public static void show(int screenX, int screenY, Consumer<String> onSend) {
        SwingUtilities.invokeLater(() -> {
            if (open != null) {
                open.dispose();
                open = null;
            }
            JFrame w = new JFrame();
            w.setUndecorated(true);
            w.setType(Window.Type.UTILITY);   // no taskbar button
            w.setAlwaysOnTop(true);
            w.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JTextField field = new JTextField(30);
            field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
            field.setBackground(new Color(0x26, 0x20, 0x2A));
            field.setForeground(Color.WHITE);
            field.setCaretColor(Color.WHITE);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xF9, 0xD8, 0x48), 2),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));

            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        String text = field.getText().trim();
                        w.dispose();
                        open = null;
                        if (!text.isEmpty()) {
                            onSend.accept(text);
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        w.dispose();
                        open = null;
                    }
                }
            });
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    w.dispose();
                    if (open == w) {
                        open = null;
                    }
                }
            });

            // grow with the text, clamped to the screen's right edge
            field.getDocument().addDocumentListener(new DocumentListener() {
                private void grow() {
                    int cols = Math.max(30, Math.min(70,
                            field.getText().length() + 2));
                    if (cols != field.getColumns()) {
                        field.setColumns(cols);
                        w.pack();
                        int screenW = Toolkit.getDefaultToolkit()
                                .getScreenSize().width;
                        w.setLocation(Math.max(0, Math.min(w.getX(),
                                screenW - w.getWidth())), w.getY());
                    }
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    grow();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    grow();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });

            w.add(field);
            w.pack();
            w.setLocation(screenX, Math.max(0, screenY - w.getHeight() - 6));
            w.setVisible(true);
            w.toFront();
            field.requestFocusInWindow();
            open = w;
        });
    }
}
