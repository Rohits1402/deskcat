#include "key_counter.h"

#include <windows.h>

using namespace godot;

namespace deskcat {

// A WH_KEYBOARD_LL hook needs a thread that pumps messages; one hook per
// process is plenty, so the machinery is file-static.
namespace {
std::atomic<int> g_count{0};
HHOOK g_hook = nullptr;
HANDLE g_thread = nullptr;
DWORD g_thread_id = 0;

LRESULT CALLBACK hook_proc(int code, WPARAM w_param, LPARAM l_param) {
    // PRIVACY: l_param (KBDLLHOOKSTRUCT with the key identity) is
    // deliberately never dereferenced. Count key-downs only.
    if (code == HC_ACTION && (w_param == WM_KEYDOWN || w_param == WM_SYSKEYDOWN)) {
        g_count.fetch_add(1, std::memory_order_relaxed);
    }
    return CallNextHookEx(nullptr, code, w_param, l_param);
}

DWORD WINAPI hook_thread(LPVOID) {
    g_hook = SetWindowsHookExW(WH_KEYBOARD_LL, hook_proc,
            GetModuleHandleW(nullptr), 0);
    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0) > 0) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
    if (g_hook) {
        UnhookWindowsHookEx(g_hook);
        g_hook = nullptr;
    }
    return 0;
}
} // namespace

DeskCatKeyCounter::~DeskCatKeyCounter() {
    stop();
}

void DeskCatKeyCounter::start() {
    if (running || g_thread) {
        running = true;
        return;
    }
    g_thread = CreateThread(nullptr, 0, hook_thread, nullptr, 0, &g_thread_id);
    running = g_thread != nullptr;
}

void DeskCatKeyCounter::stop() {
    if (!running) {
        return;
    }
    running = false;
    if (g_thread_id) {
        PostThreadMessageW(g_thread_id, WM_QUIT, 0, 0);
    }
    if (g_thread) {
        WaitForSingleObject(g_thread, 1000);
        CloseHandle(g_thread);
        g_thread = nullptr;
        g_thread_id = 0;
    }
}

int DeskCatKeyCounter::get_and_reset() {
    return g_count.exchange(0, std::memory_order_relaxed);
}

void DeskCatKeyCounter::_bind_methods() {
    ClassDB::bind_method(D_METHOD("start"), &DeskCatKeyCounter::start);
    ClassDB::bind_method(D_METHOD("stop"), &DeskCatKeyCounter::stop);
    ClassDB::bind_method(D_METHOD("get_and_reset"),
            &DeskCatKeyCounter::get_and_reset);
}

} // namespace deskcat
