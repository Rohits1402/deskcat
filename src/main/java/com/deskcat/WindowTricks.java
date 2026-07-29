package com.deskcat;

import org.lwjgl.glfw.GLFW;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;

/** Win32 window-style tweaks shared by the local pet and remote-peer windows. */
public final class WindowTricks {

    private static final int GWL_EXSTYLE = -20;
    private static final long WS_EX_TOOLWINDOW = 0x00000080L;
    private static final long WS_EX_APPWINDOW = 0x00040000L;

    private WindowTricks() {
    }

    /** Keep the window above everything else. */
    public static void floating(Lwjgl3Window window) {
        GLFW.glfwSetWindowAttrib(window.getWindowHandle(),
                GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
    }

    /**
     * Toggle click-through via GLFW's native mouse-passthrough (raw
     * WS_EX_LAYERED needs SetLayeredWindowAttributes or the window stops
     * rendering entirely — the pellet/bubble invisibility bug). Remote pet
     * windows turn this on while overlapping the local pet so it stays
     * draggable underneath them.
     */
    public static void setClickThrough(Lwjgl3Window window, boolean on) {
        try {
            GLFW.glfwSetWindowAttrib(window.getWindowHandle(),
                    GLFW.GLFW_MOUSE_PASSTHROUGH,
                    on ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        } catch (Throwable t) {
            // GLFW < 3.4: the window just stays clickable
        }
    }

    /**
     * Remove the taskbar button and, for view-only windows (pellets, big
     * bubbles), make the window click-through so it never steals mouse input.
     * No-op off Windows.
     */
    public static void applyExStyles(Lwjgl3Window window, boolean clickThrough) {
        try {
            long glfwWin = window.getWindowHandle();
            long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(glfwWin);
            GLFW.glfwHideWindow(glfwWin);
            long ex = org.lwjgl.system.windows.User32.GetWindowLongPtr(hwnd, GWL_EXSTYLE);
            ex = (ex | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW;
            org.lwjgl.system.windows.User32.SetWindowLongPtr(hwnd, GWL_EXSTYLE, ex);
            GLFW.glfwShowWindow(glfwWin);
        } catch (Throwable t) {
            // non-Windows or API unavailable: window keeps its taskbar button
        }
        if (clickThrough) {
            setClickThrough(window, true);
        }
    }
}
