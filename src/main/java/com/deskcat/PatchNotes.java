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
            + "v1.8.0\n"
            + "------\n"
            + "* New click tricks: the cat bats a ball of yarn, the black cat\n"
            + "  pushes a mug off a shelf while staring at you, doge gets\n"
            + "  the zoomies, Pikachu's Thunderbolt leaves it frazzled,\n"
            + "  Squirtle Rapid Spins and pops out dizzy, Bulbasaur fires\n"
            + "  Razor Leaf, the goldfish leaps and flips\n"
            + "* Idle fidgets: now and then your pet licks a paw, washes,\n"
            + "  scratches an ear, rubs its cheeks, polishes its shell,\n"
            + "  droops and perks up, or blows a bubble\n"
            + "* Click spam: a trick now plays to the end; click 3+ times\n"
            + "  during it and your pet gets fed up afterwards - the cats\n"
            + "  glare and turn their backs, doge sulks, Pikachu folds its\n"
            + "  arms, Squirtle hides in its shell, Bulbasaur crosses its\n"
            + "  vines, the goldfish turns away\n"
            + "* Updating is one click: Check for updates now installs the\n"
            + "  new version right away when there is one\n"
            + "* Your pet now stays on top of everything, including other\n"
            + "  always-on-top windows and the taskbar\n"
            + "\n"
            + "v1.7.0\n"
            + "------\n"
            + "* Stretch and water reminders now play an animation of their\n"
            + "  own for every pet, with a speech bubble instead of the\n"
            + "  Windows notification: cats bow and yawn, the black cat\n"
            + "  arches, doge shakes and slurps, Pikachu reaches up and\n"
            + "  gulps a bottle, Squirtle stars out of its shell and drinks\n"
            + "  a fountain, Bulbasaur's vines stretch and a rain cloud\n"
            + "  waters its bulb, the goldfish bends and catches a drop\n"
            + "* Picking your pet up is new too: instead of stretching like\n"
            + "  mochi it dangles from your cursor and swings as you move -\n"
            + "  the cat goes limp, the black cat sulks, doge paddles,\n"
            + "  Pikachu kicks its feet, Squirtle swims in the air, Bulbasaur\n"
            + "  hangs by its vines and the goldfish flops\n"
            + "\n"
            + "v1.6.1\n"
            + "------\n"
            + "* Fixed: updating the DeskCat.exe version from the tray showed\n"
            + "  a \"cannot find javaw.exe\" error instead of restarting\n"
            + "\n"
            + "v1.6.0\n"
            + "------\n"
            + "* Your pet steps aside while a fullscreen game, video or\n"
            + "  slideshow is in front, and comes back when it's gone\n"
            + "* Clicks on the empty space around your pet now go through\n"
            + "  to whatever is underneath\n"
            + "* Tray Multiplayer, now off when DeskCat starts: switch it on\n"
            + "  to see friends' pets on your network, chat and shoot; off\n"
            + "  leaves their screens and hides theirs\n"
            + "* Tray Quiet mode: no sounds, wandering, perching or reminders\n"
            + "\n"
            + "v1.5.0\n"
            + "------\n"
            + "* New skin: Bulbasaur! Click it for Vine Whip - a vine cracks\n"
            + "  up out of each side of its bulb\n"
            + "* Every pet now looks like itself in every state: walking,\n"
            + "  sleeping and belly-up use the sitting pet's head, eyes and\n"
            + "  markings instead of a slimmer stand-in\n"
            + "* Real walk cycles: 8 frames, jointed legs stepping in a\n"
            + "  natural order, the body bobbing, feet that don't slide\n"
            + "* Pikachu now walks upright; cats and dogs roll belly-up\n"
            + "\n"
            + "v1.4.2\n"
            + "------\n"
            + "* Fixed: if DeskCat was force-closed (e.g. from Task Manager),\n"
            + "  its small music-level helper kept running in the background\n"
            + "\n"
            + "v1.4.1\n"
            + "------\n"
            + "* Squirtle redrawn to its real proportions: a smaller round\n"
            + "  head, big outlined eyes whose pupils follow your cursor, an\n"
            + "  open smile, a shaded shell and a little curled tail\n"
            + "* Squirtle now walks upright on two legs, like in the games\n"
            + "\n"
            + "v1.4.0\n"
            + "------\n"
            + "* Cat and black cat redrawn by hand: cleaner shapes, bigger\n"
            + "  eyes, a crisp outline (light on the black cat so it shows\n"
            + "  on dark wallpaper) and new walk, sleep and belly-up poses\n"
            + "* The black cat now walks, curls up to sleep and rolls over too\n"
            + "* Doge, goldfish, Pikachu and Squirtle redrawn the same way\n"
            + "* Doge now walks, curls up to sleep and rolls belly-up too\n"
            + "* Pikachu redrawn to its real proportions: rounder head, long\n"
            + "  upright ears, bigger eyes, shading and a proper bolt tail\n"
            + "\n"
            + "v1.3.0\n"
            + "------\n"
            + "* Three new skins: black cat, doge and goldfish\n"
            + "* Black cat arches, hisses and bristles, then swipes\n"
            + "* Doge digs frantically, flinging dirt everywhere\n"
            + "* Goldfish puffs up like a pufferfish and blows bubbles,\n"
            + "  and swims freely around your screen instead of walking\n"
            + "* Perches on the window you're using and rides along with it\n"
            + "* Cat, Pikachu and Squirtle: real walk cycles, sleep poses,\n"
            + "  and a belly-up roll when you keep petting them\n"
            + "* Cats groom and pounce at a slow cursor; Pikachu's cheeks\n"
            + "  spark; Squirtle hides in its shell when startled or carried\n"
            + "* Type fast for a while and your pet overheats\n"
            + "* Pikachu redrawn: rounder head, tall ears, slab bolt tail\n"
            + "\n"
            + "v1.2.1\n"
            + "------\n"
            + "* The cat swipes a paw at your cursor when clicked, claws out\n"
            + "* Each skin has its own voice: a real meow for the cat, a\n"
            + "  pi-ka chirp for Pikachu, a watery squeak for Squirtle\n"
            + "* Tray Sound opens a volume slider; 0% is silence\n"
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
