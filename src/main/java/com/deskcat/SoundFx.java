package com.deskcat;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

/**
 * Every effect is synthesized as PCM at first use — no audio assets, keeping
 * with the everything-is-code principle.
 *
 * The one exception is the cat, which plays a real CC0 (public-domain) meow
 * recording bundled at {@code sfx/meow.ogg}; if that resource is missing the
 * synthesized meow below is used instead. That fallback follows the published
 * acoustics of real human-directed meows: ~0.55 s long, f0 rising to roughly
 * 700 Hz then falling, with the vowel formants sliding from a closed-mouth
 * nasal through "ee" and "ah" to "ow" as the mouth opens and closes again.
 *
 * The Pikachu and Squirtle voices are original synthesis: those characters'
 * actual cries are copyrighted recordings and are never bundled.
 */
final class SoundFx {

    private static final int RATE = 22050;

    /** 0..1 master level, set from the tray's volume flyout; 0 is silence. */
    static volatile float volume = 0.8f;

    private static short[] meowPcm, pikaPcm, squirtlePcm, splashPcm, chirpPcm;

    private static Sound meowClip;
    private static boolean meowClipTried;

    private SoundFx() {
    }

    /** Cat voice: the bundled CC0 meow recording, else synthesis. */
    static void meow() {
        if (volume <= 0f) {
            return;
        }
        if (!meowClipTried) {
            meowClipTried = true;
            try {
                FileHandle f = Gdx.files.internal("sfx/meow.ogg");
                if (f.exists()) {
                    meowClip = Gdx.audio.newSound(f);
                }
            } catch (Throwable ignored) {
            }
        }
        if (meowClip != null) {
            try {
                meowClip.play(volume);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (meowPcm == null) {
            meowPcm = buildMeow();
        }
        play(meowPcm);
    }

    static void dispose() {
        if (meowClip != null) {
            try {
                meowClip.dispose();
            } catch (Throwable ignored) {
            }
            meowClip = null;
        }
    }

    /** Pikachu voice: a bright two-syllable electric chirp with a crackle. */
    static void pikaCry() {
        if (pikaPcm == null) {
            pikaPcm = buildPikaCry();
        }
        play(pikaPcm);
    }

    /** Squirtle voice: a warbling watery squeak that ends in a bubble. */
    static void squirtleCry() {
        if (squirtlePcm == null) {
            squirtlePcm = buildSquirtleCry();
        }
        play(squirtlePcm);
    }

    /** Noisy burst with rising bubbles; used for the water reminder. */
    static void splash() {
        if (splashPcm == null) {
            splashPcm = buildSplash();
        }
        play(splashPcm);
    }

    /** Short soft sweep for startles and the stretch reminder. */
    static void chirp() {
        if (chirpPcm == null) {
            chirpPcm = buildChirp();
        }
        play(chirpPcm);
    }

    private static void play(final short[] pcm) {
        final float gain = volume;
        if (gain <= 0f) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioDevice d = Gdx.audio.newAudioDevice(RATE, true);
                    d.setVolume(gain);
                    d.writeSamples(pcm, 0, pcm.length);
                    d.dispose();
                } catch (Throwable ignored) {
                }
            }
        }, "deskcat-sfx");
        t.setDaemon(true);
        t.start();
    }

    /** Two-pole resonator; st holds the last two outputs. */
    private static double reson(double x, double f, double r, double[] st) {
        double b1 = 2 * r * Math.cos(2 * Math.PI * f / RATE);
        double b2 = -r * r;
        double y = (1 - r) * x + b1 * st[0] + b2 * st[1];
        st[1] = st[0];
        st[0] = y;
        return y;
    }

    private static double clip(double v) {
        return v > 1 ? 1 : (v < -1 ? -1 : v);
    }

    private static short[] buildMeow() {
        double dur = 0.55;
        int n = (int) (RATE * dur);
        short[] s = new short[n];
        double phase = 0;
        double[] r1 = new double[2];
        double[] r2 = new double[2];
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double u = t / dur;
            // rise to the peak in the first third, then fall away
            double f0 = 470
                    + 240 * Math.sin(Math.min(1, u / 0.3) * Math.PI / 2)
                    - 300 * Math.max(0, (u - 0.35) / 0.65);
            f0 *= 1 + 0.03 * Math.sin(2 * Math.PI * 24 * t);
            phase += 2 * Math.PI * f0 / RATE;
            // glottal-ish source: harmonic stack with 1/k rolloff
            double src = 0;
            for (int k = 1; k <= 8; k++) {
                src += Math.sin(phase * k) / k;
            }
            src *= 0.35;
            // vowel morph: nasal -> "ee" -> "ah" -> "ow"
            double f1;
            double f2;
            if (u < 0.5) {
                double a = u / 0.5;
                f1 = 400 + a * 500;
                f2 = 2100 - a * 700;
            } else {
                double a = (u - 0.5) / 0.5;
                f1 = 900 - a * 520;
                f2 = 1400 - a * 550;
            }
            double v = reson(src, f1, 0.97, r1) * 0.72
                    + reson(src, f2, 0.95, r2) * 0.28;
            double env = Math.min(1, t / 0.035)
                    * (u < 0.6 ? 1 : 1 - (u - 0.6) / 0.4);
            if (u < 0.08) {
                env *= 0.55;   // mouth still closed on the "m"
            }
            s[i] = (short) (clip(v * env * 3.2) * Short.MAX_VALUE * 0.7);
        }
        return s;
    }

    private static short[] buildPikaCry() {
        double dur = 0.42;
        int n = (int) (RATE * dur);
        short[] s = new short[n];
        Random rnd = new Random(3);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double f = 0;
            double env = 0;
            if (t < 0.10) {                       // "pi" - short and rising
                double a = t / 0.10;
                f = 950 + 600 * a;
                env = Math.min(1, t / 0.008) * (1 - 0.35 * a);
            } else if (t >= 0.15 && t < 0.34) {   // "ka" - longer, falling
                double a = (t - 0.15) / 0.19;
                f = 1400 - 640 * a;
                env = Math.min(1, (t - 0.15) / 0.008) * (1 - a);
            }
            double v = 0;
            if (env > 0) {
                ph += 2 * Math.PI * f / RATE;
                double tone = Math.sin(ph) + 0.35 * Math.sin(2 * ph)
                        + 0.18 * Math.sin(3 * ph);
                v = tone * env * 0.30;
                if (t > 0.24) {
                    v += (rnd.nextDouble() * 2 - 1) * env * 0.14;   // crackle
                }
            }
            s[i] = (short) (clip(v) * Short.MAX_VALUE);
        }
        return s;
    }

    private static short[] buildSquirtleCry() {
        double dur = 0.45;
        int n = (int) (RATE * dur);
        short[] s = new short[n];
        Random rnd = new Random(5);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double u = t / dur;
            double f = 620 + 380 * Math.sin(Math.min(1, u / 0.5) * Math.PI);
            f *= 1 + 0.05 * Math.sin(2 * Math.PI * 30 * t);   // watery warble
            ph += 2 * Math.PI * f / RATE;
            double env = Math.min(1, t / 0.02)
                    * (u < 0.55 ? 1 : 1 - (u - 0.55) / 0.45);
            double v = (Math.sin(ph) + 0.25 * Math.sin(2 * ph)) * 0.30 * env;
            if (u > 0.6) {                                     // parting bubble
                double bt = (u - 0.6) / 0.4;
                v += Math.sin(2 * Math.PI * (300 + 500 * bt) * t)
                        * 0.10 * (1 - bt);
            }
            v += (rnd.nextDouble() * 2 - 1) * 0.02 * env;
            s[i] = (short) (clip(v) * Short.MAX_VALUE);
        }
        return s;
    }

    private static short[] buildChirp() {
        double dur = 0.35;
        int n = (int) (RATE * dur);
        short[] s = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double f = 550 + 380 * Math.sin(Math.PI * t / dur)
                    + 40 * Math.sin(2 * Math.PI * 24 * t);
            phase += 2 * Math.PI * f / RATE;
            double env = Math.min(1, t / 0.02) * (1 - t / dur);
            s[i] = (short) (Math.sin(phase) * env * 0.30 * Short.MAX_VALUE);
        }
        return s;
    }

    private static short[] buildSplash() {
        double dur = 0.4;
        int n = (int) (RATE * dur);
        short[] s = new short[n];
        Random r = new Random(11);
        double[] starts = {0.0, 0.1, 0.18};
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double env = Math.min(1, t / 0.02) * (1 - t / dur);
            double v = (r.nextDouble() * 2 - 1) * env * 0.16;
            for (double st : starts) {
                double bt = t - st;
                if (bt >= 0 && bt < 0.12) {
                    double f = 250 + 550 * (bt / 0.12);
                    v += Math.sin(2 * Math.PI * f * bt) * (1 - bt / 0.12) * 0.14;
                }
            }
            s[i] = (short) (v * Short.MAX_VALUE);
        }
        return s;
    }
}
