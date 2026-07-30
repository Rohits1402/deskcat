#include "audio_peak.h"

#include <endpointvolume.h>
#include <mmdeviceapi.h>
#include <windows.h>

using namespace godot;

namespace deskcat {

DeskCatAudioPeak::DeskCatAudioPeak() {
    com_ok = SUCCEEDED(CoInitializeEx(nullptr, COINIT_MULTITHREADED))
            || GetLastError() == RPC_E_CHANGED_MODE;
}

DeskCatAudioPeak::~DeskCatAudioPeak() {
    release_meter();
    if (com_ok) {
        CoUninitialize();
    }
}

void DeskCatAudioPeak::ensure_meter() {
    if (meter) {
        return;
    }
    IMMDeviceEnumerator *enumerator = nullptr;
    if (FAILED(CoCreateInstance(__uuidof(MMDeviceEnumerator), nullptr,
            CLSCTX_ALL, __uuidof(IMMDeviceEnumerator),
            reinterpret_cast<void **>(&enumerator)))) {
        return;
    }
    IMMDevice *device = nullptr;
    if (SUCCEEDED(enumerator->GetDefaultAudioEndpoint(eRender, eConsole,
            &device))) {
        IAudioMeterInformation *m = nullptr;
        if (SUCCEEDED(device->Activate(__uuidof(IAudioMeterInformation),
                CLSCTX_ALL, nullptr, reinterpret_cast<void **>(&m)))) {
            meter = m;
        }
        device->Release();
    }
    enumerator->Release();
}

void DeskCatAudioPeak::release_meter() {
    if (meter) {
        static_cast<IAudioMeterInformation *>(meter)->Release();
        meter = nullptr;
    }
}

float DeskCatAudioPeak::get_peak() {
    ensure_meter();
    if (!meter) {
        return 0.0f;
    }
    float peak = 0.0f;
    HRESULT hr = static_cast<IAudioMeterInformation *>(meter)
            ->GetPeakValue(&peak);
    if (FAILED(hr)) {
        // Default device may have changed (headphones plugged in) — drop the
        // meter and re-acquire next call.
        release_meter();
        return 0.0f;
    }
    return peak;
}

void DeskCatAudioPeak::_bind_methods() {
    ClassDB::bind_method(D_METHOD("get_peak"), &DeskCatAudioPeak::get_peak);
}

} // namespace deskcat
