package com.deskcat;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;

import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.windows.MONITORINFOEX;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;

/**
 * Read-only lookups of other top-level windows, used so the pet can perch on
 * the title bar of the window you're working in and ride along when it moves,
 * and step aside while a fullscreen game, video or slideshow is in front.
 *
 * Only reads window geometry and identity - never sends input, changes state,
 * or touches the network or disk. GetWindowRect and friends come from LWJGL's
 * User32 binding; the few calls it lacks (GetForegroundWindow, IsWindow,
 * GetClassNameW, GetWindowThreadProcessId) are resolved from user32.dll and
 * invoked through LWJGL's generic JNI trampolines. Any failure, e.g. on a
 * non-Windows OS, just means "nothing to perch on".
 */
final class Win32Windows {

    private static final int GWL_EXSTYLE = -20;
    private static final long WS_EX_TOOLWINDOW = 0x00000080L;

    /** Shell surfaces that are never a sensible perch. */
    private static final String[] SHELL_CLASSES = {
            "Progman", "WorkerW", "Shell_TrayWnd", "Shell_SecondaryTrayWnd",
            "Windows.UI.Core.CoreWindow", "NotifyIconOverflowWindow",
            "TopLevelWindowForOverflowXamlIsland", "XamlExplorerHostIslandWindow",
    };

    private static boolean loaded, ok;
    private static long fnForeground, fnIsWindow, fnClassName, fnPid;
    private static int ownPid = -1;

    private Win32Windows() {
    }

    private static synchronized boolean init() {
        if (loaded) {
            return ok;
        }
        loaded = true;
        try {
            SharedLibrary u = User32.getLibrary();
            fnForeground = u.getFunctionAddress("GetForegroundWindow");
            fnIsWindow = u.getFunctionAddress("IsWindow");
            fnClassName = u.getFunctionAddress("GetClassNameW");
            fnPid = u.getFunctionAddress("GetWindowThreadProcessId");
            String name = ManagementFactory.getRuntimeMXBean().getName();
            ownPid = Integer.parseInt(name.substring(0, name.indexOf('@')));
            ok = fnForeground != 0 && fnIsWindow != 0 && fnClassName != 0
                    && fnPid != 0;
        } catch (Throwable t) {
            ok = false;
        }
        return ok;
    }

    /** The window the user is working in, or 0. */
    static long foreground() {
        if (!init()) {
            return 0;
        }
        try {
            return JNI.callP(fnForeground);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Fills {left, top, right, bottom}; false if the window is gone. */
    static boolean rect(long hwnd, int[] out) {
        if (hwnd == 0 || !init()) {
            return false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            RECT r = RECT.malloc(stack);
            if (!User32.GetWindowRect(hwnd, r)) {
                return false;
            }
            out[0] = r.left();
            out[1] = r.top();
            out[2] = r.right();
            out[3] = r.bottom();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Whether the window is an ordinary, visible, restored application window
     * belonging to some other program - something you could sit on.
     */
    static boolean perchable(long hwnd) {
        if (hwnd == 0 || !init()) {
            return false;
        }
        try {
            if (JNI.callPI(hwnd, fnIsWindow) == 0
                    || !User32.IsWindowVisible(hwnd)
                    || User32.IsIconic(hwnd)
                    || User32.IsZoomed(hwnd)) {
                return false;
            }
            if ((User32.GetWindowLongPtr(hwnd, GWL_EXSTYLE) & WS_EX_TOOLWINDOW) != 0) {
                return false;
            }
            int[] pid = new int[1];
            JNI.callPPI(hwnd, pid, fnPid);
            if (pid[0] == ownPid) {
                return false;   // our own pet, chat box, patch notes...
            }
            return !isShell(hwnd);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Whether the window in front is another program filling the whole
     * monitor that {@code ours} is on: a game, a fullscreen video or a
     * slideshow. A maximised app keeps its title bar and the desktop is a
     * shell surface, so neither counts.
     */
    static boolean fullscreenOver(long ours) {
        long hwnd = foreground();
        if (hwnd == 0 || ours == 0) {
            return false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (!User32.IsWindowVisible(hwnd) || User32.IsIconic(hwnd)) {
                return false;
            }
            long style = User32.GetWindowLongPtr(hwnd, User32.GWL_STYLE);
            if ((style & User32.WS_CAPTION) == User32.WS_CAPTION) {
                return false;   // an ordinary (maybe maximised) window
            }
            int[] pid = new int[1];
            JNI.callPPI(hwnd, pid, fnPid);
            if (pid[0] == ownPid || isShell(hwnd)) {
                return false;
            }
            long mon = User32.MonitorFromWindow(hwnd, User32.MONITOR_DEFAULTTONULL);
            if (mon == 0 || mon != User32.MonitorFromWindow(ours,
                    User32.MONITOR_DEFAULTTONEAREST)) {
                return false;   // fullscreen on another monitor: no bother
            }
            MONITORINFOEX mi = MONITORINFOEX.malloc(stack);
            mi.cbSize(MONITORINFOEX.SIZEOF);
            RECT r = RECT.malloc(stack);
            if (!User32.GetMonitorInfo(mon, mi) || !User32.GetWindowRect(hwnd, r)) {
                return false;
            }
            RECT m = mi.rcMonitor();
            return r.left() <= m.left() && r.top() <= m.top()
                    && r.right() >= m.right() && r.bottom() >= m.bottom();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isShell(long hwnd) {
        String cls = className(hwnd);
        for (String c : SHELL_CLASSES) {
            if (c.equals(cls)) {
                return true;
            }
        }
        return false;
    }

    /** Still there and still a fair perch (not closed, minimised, maximised). */
    static boolean stillPerchable(long hwnd) {
        if (hwnd == 0 || !init()) {
            return false;
        }
        try {
            return JNI.callPI(hwnd, fnIsWindow) != 0
                    && User32.IsWindowVisible(hwnd)
                    && !User32.IsIconic(hwnd)
                    && !User32.IsZoomed(hwnd);
        } catch (Throwable t) {
            return false;
        }
    }

    private static String className(long hwnd) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buf = stack.malloc(512);
            int n = JNI.callPPI(hwnd, MemoryUtil.memAddress(buf), 256, fnClassName);
            return n > 0 ? MemoryUtil.memUTF16(buf, n) : "";
        }
    }
}
