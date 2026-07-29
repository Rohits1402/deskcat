package com.deskcat;

import org.lwjgl.glfw.GLFW;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;

/** Win32 window-style tweaks shared by the local pet and remote-peer windows. */
public final class WindowTricks {

    private static final int GWL_EXSTYLE = -20;
    private static final long WS_EX_TOOLWINDOW = 0x00000080L;
    private static final long WS_EX_APPWINDOW = 0x00040000L;
    private static final long WS_EX_LAYERED = 0x00080000L;
    private static final long WS_EX_TRANSPARENT = 0x00000020L;

    private WindowTricks() {
    }

    /** Keep the window above everything else. */
    public static void floating(Lwjgl3Window window) {
        GLFW.glfwSetWindowAttrib(window.getWindowHandle(),
                GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
    }

    /**
     * Remove the taskbar button and, for view-only windows (remote pets),
     * make the window click-through so it never steals mouse input.
     * No-op off Windows.
     */
    public static void applyExStyles(Lwjgl3Window window, boolean clickThrough) {
        try {
            long glfwWin = window.getWindowHandle();
            long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(glfwWin);
            GLFW.glfwHideWindow(glfwWin);
            long ex = org.lwjgl.system.windows.User32.GetWindowLongPtr(hwnd, GWL_EXSTYLE);
            ex = (ex | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW;
            if (clickThrough) {
                ex |= WS_EX_LAYERED | WS_EX_TRANSPARENT;
            }
            org.lwjgl.system.windows.User32.SetWindowLongPtr(hwnd, GWL_EXSTYLE, ex);
            GLFW.glfwShowWindow(glfwWin);
        } catch (Throwable t) {
            // non-Windows or API unavailable: window keeps its taskbar button
        }
    }
}
