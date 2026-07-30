#pragma once

#include <godot_cpp/classes/ref_counted.hpp>

namespace deskcat {

// One-shot Win32 window tricks the Godot API doesn't expose.
class DeskCatWinTricks : public godot::RefCounted {
    GDCLASS(DeskCatWinTricks, godot::RefCounted)

public:
    // WS_EX_TOOLWINDOW & ~WS_EX_APPWINDOW: no taskbar button, no Alt-Tab.
    void hide_from_taskbar(int64_t hwnd);

protected:
    static void _bind_methods();
};

} // namespace deskcat
