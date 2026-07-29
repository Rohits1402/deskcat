package com.deskcat;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

/**
 * Tiny procedural chiptune-ish sound effects, synthesized at first use —
 * no audio assets, keeping with the everything-is-code principle.
 */
final class SoundFx {

    private static final int RATE = 22050;

    static volatile boolean enabled = true;

    private static short[] zapPcm, splashPcm, chirpPcm;

    private SoundFx() {
    }

    /** Crackling descending buzz for the thunderbolt. */
    static void zap() {
        if (zapPcm == null) {
            zapPcm = buildZap();
        }
        play(zapPcm);
    }

    /** Noisy burst with rising bubbles for the water gun. */
    static void splash() {
        if (splashPcm == null) {
            splashPcm = buildSplash();
        }
        play(splashPcm);
    }

    /** Soft mew-like sweep for startles and reminders. */
    static void chirp() {
        if (chirpPcm == null) {
            chirpPcm = buildChirp();
        }
        play(chirpPcm);
    }

    private static void play(final short[] pcm) {
        if (!enabled) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioDevice d = Gdx.audio.newAudioDevice(RATE, true);
                    d.writeSamples(pcm, 0, pcm.length);
                    d.dispose();
                } catch (Throwable ignored) {
                }
            }
        }, "deskcat-sfx");
        t.setDaemon(true);
        t.start();
    }

    private static short[] buildChirp() {
        float dur = 0.35f;
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

    private static short[] buildZap() {
        float dur = 0.22f;
        int n = (int) (RATE * dur);
        short[] s = new short[n];
        Random r = new Random(7);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double k = 1 - t / dur;
            double f = 30 + 140 * k;
            phase += 2 * Math.PI * f / RATE;
            double noise = (r.nextDouble() * 2 - 1) * k * k * 0.35;
            double buzz = Math.signum(Math.sin(phase)) * k * 0.18;
            s[i] = (short) ((noise + buzz) * Short.MAX_VALUE);
        }
        return s;
    }

    private static short[] buildSplash() {
        float dur = 0.4f;
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
                    double benv = 1 - bt / 0.12;
                    v += Math.sin(2 * Math.PI * f * bt) * benv * 0.14;
                }
            }
            s[i] = (short) (v * Short.MAX_VALUE);
        }
        return s;
    }
}
