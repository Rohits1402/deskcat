#include "win_tricks.h"

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

void DeskCatWinTricks::_bind_methods() {
    ClassDB::bind_method(D_METHOD("hide_from_taskbar", "hwnd"),
            &DeskCatWinTricks::hide_from_taskbar);
}

} // namespace deskcat
