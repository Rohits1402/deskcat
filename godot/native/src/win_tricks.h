#pragma once

#include <godot_cpp/classes/ref_counted.hpp>

namespace deskcat {

// One-shot Win32 window tricks the Godot API doesn't expose.
class DeskCatWinTricks : public godot::RefCounted {
    GDCLASS(DeskCatWinTricks, godot::RefCounted)

public:
    // WS_EX_TOOLWINDOW & ~WS_EX_APPWINDOW: no taskbar button, no Alt-Tab.
    void hide_from_taskbar(int64_t hwnd);

    // Keeps the overlay visible across Windows virtual-desktop switches.
    // Call periodically (~1 s): if the window landed on an inactive desktop,
    // it is moved to the current one via IVirtualDesktopManager (documented
    // COM interface, Win10+). Returns false when the interface is
    // unavailable (very old Win10 builds) — callers can stop polling then.
    bool ensure_on_current_desktop(int64_t hwnd);

    ~DeskCatWinTricks() override;

protected:
    static void _bind_methods();

private:
    void *vdm = nullptr;   // IVirtualDesktopManager*
    bool vdm_tried = false;
};

} // namespace deskcat
