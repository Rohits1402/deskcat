#pragma once

#include <atomic>

#include <godot_cpp/classes/ref_counted.hpp>

namespace deskcat {

// Global keystroke COUNTER via a low-level keyboard hook.
//
// PRIVACY GUARANTEE (mirrors the Java app): the hook callback increments an
// atomic int and nothing else. Key identity (vkCode/scanCode) is never read,
// stored, or forwarded — the KBDLLHOOKSTRUCT pointer is simply not touched.
class DeskCatKeyCounter : public godot::RefCounted {
    GDCLASS(DeskCatKeyCounter, godot::RefCounted)

public:
    ~DeskCatKeyCounter() override;

    void start();
    void stop();
    // Keys pressed since the previous call.
    int get_and_reset();

protected:
    static void _bind_methods();

private:
    bool running = false;
};

} // namespace deskcat
