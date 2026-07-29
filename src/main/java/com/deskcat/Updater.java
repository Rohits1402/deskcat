package com.deskcat;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Checks GitHub Releases for a newer build and swaps the running jar.
 * These are the only network calls in the app: one API request per check,
 * one asset download when the user installs. Nothing is ever sent.
 */
final class Updater {

    static final String REPO = "Rohits1402/deskcat";
    private static final String API =
            "https://api.github.com/repos/" + REPO + "/releases/latest";

    static final class Release {
        String version;
        String jarUrl;
        String pageUrl;
    }

    private Updater() {
    }

    /** Latest release, or null if unreachable / none published. */
    static Release fetchLatest() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
            c.setRequestProperty("User-Agent", "DeskCat/" + CatApp.VERSION);
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            InputStream in = c.getInputStream();
            Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A");
            String body = s.hasNext() ? s.next() : "";
            s.close();
            JsonValue root = new JsonReader().parse(body);
            Release r = new Release();
            r.version = root.getString("tag_name", "").replaceFirst("^v", "");
            r.pageUrl = root.getString("html_url",
                    "https://github.com/" + REPO + "/releases");
            JsonValue assets = root.get("assets");
            if (assets != null) {
                for (JsonValue a = assets.child; a != null; a = a.next) {
                    if (a.getString("name", "").endsWith(".jar")) {
                        r.jarUrl = a.getString("browser_download_url", null);
                        break;
                    }
                }
            }
            return r.version.isEmpty() ? null : r;
        } catch (Throwable t) {
            return null;   // offline, rate-limited, or TLS too old: no update UI
        }
    }

    static boolean isNewer(String latest, String current) {
        try {
            String[] a = latest.split("\\.");
            String[] b = current.split("\\.");
            for (int i = 0; i < Math.max(a.length, b.length); i++) {
                int x = i < a.length ? Integer.parseInt(a[i].trim()) : 0;
                int y = i < b.length ? Integer.parseInt(b[i].trim()) : 0;
                if (x != y) {
                    return x > y;
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    /** The jar this class runs from, or null in dev mode (gradle classes). */
    static File runningJar() {
        try {
            File f = new File(Updater.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return f.isFile() && f.getName().endsWith(".jar") ? f : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Download the release jar to a temp file; null on failure. */
    static File download(Release r) {
        if (r.jarUrl == null) {
            return null;
        }
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(r.jarUrl).openConnection();
            c.setRequestProperty("User-Agent", "DeskCat/" + CatApp.VERSION);
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(8000);
            c.setReadTimeout(30000);
            File out = File.createTempFile("deskcat-update-", ".jar");
            InputStream in = c.getInputStream();
            OutputStream os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            os.close();
            in.close();
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Spawn a detached script that waits for this JVM to exit, replaces the
     * running jar with the downloaded one, and relaunches it with the same
     * JVM. The caller must exit the app afterwards.
     */
    static boolean stageSwap(File newJar, File currentJar) {
        try {
            File script = File.createTempFile("deskcat-update-", ".cmd");
            String javaw = new File(new File(System.getProperty("java.home"),
                    "bin"), "javaw.exe").getAbsolutePath();
            PrintWriter w = new PrintWriter(script, "US-ASCII");
            w.println("@echo off");
            w.println("timeout /t 3 /nobreak >nul");
            w.println("copy /y \"" + newJar.getAbsolutePath() + "\" \""
                    + currentJar.getAbsolutePath() + "\" >nul");
            w.println("del \"" + newJar.getAbsolutePath() + "\" >nul 2>nul");
            w.println("start \"\" \"" + javaw + "\" -jar \""
                    + currentJar.getAbsolutePath() + "\"");
            w.println("del \"%~f0\"");
            w.close();
            Runtime.getRuntime().exec(new String[] {
                    "cmd", "/c", "start", "/min", "",
                    script.getAbsolutePath()});
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static void openReleasePage(Release r) {
        try {
            Desktop.getDesktop().browse(new URI(r.pageUrl));
        } catch (Throwable ignored) {
        }
    }
}
