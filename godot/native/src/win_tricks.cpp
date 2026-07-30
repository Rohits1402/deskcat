#include "win_tricks.h"

#include <shobjidl.h>
#include <windows.h>

using namespace godot;

namespace deskcat {

void DeskCatWinTricks::hide_from_taskbar(int64_t hwnd) {
    HWND h = reinterpret_cast<HWND>(hwnd);
    if (!h) {
        return;
    }
    // Style changes only apply cleanly while hidden; caller's window is
    // already visible, so hide/restyle/show in one burst (imperceptible).
    ShowWindow(h, SW_HIDE);
    LONG_PTR ex = GetWindowLongPtrW(h, GWL_EXSTYLE);
    SetWindowLongPtrW(h, GWL_EXSTYLE,
            (ex | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW);
    ShowWindow(h, SW_SHOWNOACTIVATE);
}

DeskCatWinTricks::~DeskCatWinTricks() {
    if (vdm) {
        static_cast<IVirtualDesktopManager *>(vdm)->Release();
    }
}

bool DeskCatWinTricks::ensure_on_current_desktop(int64_t hwnd) {
    HWND h = reinterpret_cast<HWND>(hwnd);
    if (!h) {
        return false;
    }
    if (!vdm && !vdm_tried) {
        vdm_tried = true;
        CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
        IVirtualDesktopManager *m = nullptr;
        if (SUCCEEDED(CoCreateInstance(__uuidof(VirtualDesktopManager),
                nullptr, CLSCTX_ALL, __uuidof(IVirtualDesktopManager),
                reinterpret_cast<void **>(&m)))) {
            vdm = m;
        }
    }
    if (!vdm) {
        return false;
    }
    IVirtualDesktopManager *m = static_cast<IVirtualDesktopManager *>(vdm);
    BOOL on_current = TRUE;
    if (FAILED(m->IsWindowOnCurrentVirtualDesktop(h, &on_current))) {
        return true;   // transient (e.g. desktop mid-switch); keep polling
    }
    if (!on_current) {
        // Move the overlay to whatever desktop the user switched to.
        GUID current;
        HWND fg = GetForegroundWindow();
        if (fg && SUCCEEDED(m->GetWindowDesktopId(fg, &current))) {
            m->MoveWindowToDesktop(h, current);
        }
    }
    return true;
}

void DeskCatWinTricks::_bind_methods() {
    ClassDB::bind_method(D_METHOD("hide_from_taskbar", "hwnd"),
            &DeskCatWinTricks::hide_from_taskbar);
    ClassDB::bind_method(D_METHOD("ensure_on_current_desktop", "hwnd"),
            &DeskCatWinTricks::ensure_on_current_desktop);
}

} // namespace deskcat
