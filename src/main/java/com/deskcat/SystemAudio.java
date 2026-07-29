package com.deskcat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Reads the system's audio output peak level so the pet can react to music.
 * Java has no access to the Windows Core Audio COM API, so a small
 * PowerShell helper (inline C#, compiled in memory, no files) streams
 * IAudioMeterInformation.GetPeakValue five times a second.
 *
 * Privacy: the helper reads a single loudness float per tick. No audio is
 * ever captured, recorded, or stored.
 */
final class SystemAudio {

    private static volatile float peak;
    private static volatile Process proc;

    private SystemAudio() {
    }

    /** Current output peak, 0..1; 0 when silent or the helper is down. */
    static float peak() {
        return peak;
    }

    static void start() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                runLoop();
            }
        }, "deskcat-audio-meter");
        t.setDaemon(true);
        t.start();
    }

    static void stop() {
        Process p = proc;
        if (p != null) {
            p.destroy();
        }
    }

    private static void runLoop() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Process p = new ProcessBuilder("powershell", "-NoProfile",
                        "-NonInteractive", "-ExecutionPolicy", "Bypass",
                        "-EncodedCommand", encodedScript())
                        .redirectErrorStream(true).start();
                proc = p;
                BufferedReader r = new BufferedReader(new InputStreamReader(
                        p.getInputStream(), StandardCharsets.US_ASCII));
                String line;
                while ((line = r.readLine()) != null) {
                    try {
                        peak = Float.parseFloat(line.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
            peak = 0f;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static String encodedScript() {
        String cs = String.join("\n",
                "using System;",
                "using System.Runtime.InteropServices;",
                "namespace DeskCatMeter {",
                "[ComImport, Guid(\"BCDE0395-E52F-467C-8E3D-C4579291692E\")] class MMDeviceEnumeratorComObject { }",
                "[Guid(\"A95664D2-9614-4F35-A746-DE8DB63617E6\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]",
                "interface IMMDeviceEnumerator {",
                "    int NotImpl1();",
                "    int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice ppDevice);",
                "}",
                "[Guid(\"D666063F-1587-4E43-81F1-B948E807363F\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]",
                "interface IMMDevice {",
                "    int Activate(ref Guid iid, int clsCtx, IntPtr activationParams, [MarshalAs(UnmanagedType.IUnknown)] out object ppInterface);",
                "}",
                "[Guid(\"C02216F6-8C67-4B5B-9D00-D008E73E0064\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]",
                "interface IAudioMeterInformation {",
                "    int GetPeakValue(out float pfPeak);",
                "}",
                "public class Meter {",
                "    public static void Run() {",
                "        var en = (IMMDeviceEnumerator)(new MMDeviceEnumeratorComObject());",
                "        IMMDevice dev; en.GetDefaultAudioEndpoint(0, 1, out dev);",
                "        var iid = typeof(IAudioMeterInformation).GUID;",
                "        object o; dev.Activate(ref iid, 1, IntPtr.Zero, out o);",
                "        var m = (IAudioMeterInformation)o;",
                "        while (true) {",
                "            float p; m.GetPeakValue(out p);",
                "            Console.WriteLine(p.ToString(\"F4\", System.Globalization.CultureInfo.InvariantCulture));",
                "            Console.Out.Flush();",
                "            System.Threading.Thread.Sleep(200);",
                "        }",
                "    }",
                "}",
                "}");
        String ps = "$src = @'\n" + cs + "\n'@\n"
                + "Add-Type -TypeDefinition $src\n"
                + "[DeskCatMeter.Meter]::Run()\n";
        return Base64.getEncoder().encodeToString(
                ps.getBytes(StandardCharsets.UTF_16LE));
    }
}
