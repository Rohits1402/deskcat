#pragma once

#include <godot_cpp/classes/ref_counted.hpp>

namespace deskcat {

// System output PEAK level via WASAPI IAudioMeterInformation.
//
// PRIVACY GUARANTEE (mirrors the Java app's PowerShell helper): this meter
// interface can only report a 0..1 peak float — it is structurally incapable
// of returning audio samples. Replaces the whole subprocess with ~30 lines.
class DeskCatAudioPeak : public godot::RefCounted {
    GDCLASS(DeskCatAudioPeak, godot::RefCounted)

public:
    DeskCatAudioPeak();
    ~DeskCatAudioPeak() override;

    // Current output peak 0..1; 0.0 when no device / init failed.
    float get_peak();

protected:
    static void _bind_methods();

private:
    void ensure_meter();
    void release_meter();
    void *meter = nullptr;   // IAudioMeterInformation*
    bool com_ok = false;
};

} // namespace deskcat
