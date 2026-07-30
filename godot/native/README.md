# deskcat_native — GDExtension (Win32 helpers)

Three tiny classes Godot has no API for:

| Class | Does | Privacy rule |
|---|---|---|
| `DeskCatWinTricks` | `WS_EX_TOOLWINDOW` — hide overlay from taskbar/Alt-Tab | — |
| `DeskCatKeyCounter` | global keystroke **counter** (`WH_KEYBOARD_LL`) | key identities never read — hook only increments an atomic int |
| `DeskCatAudioPeak` | system output **peak level** (WASAPI `IAudioMeterInformation`) | interface can only return a 0..1 float, never samples |

The GDScript app runs without the DLL — `NativeBridge` no-ops everything.

## Build (once per machine)

1. Install **VS Build Tools** with the *Desktop development with C++* workload.
2. `pip install scons`
3. ```
   cd godot/native
   git clone -b 4.7 --depth 1 https://github.com/godotengine/godot-cpp
   scons platform=windows target=template_release
   scons platform=windows target=template_debug   # for running in the editor
   ```
4. Output lands in `godot/bin/win64/`; `deskcat_native.gdextension` picks it up
   on next launch. `godot-cpp/` and `bin/` are gitignored — every dev builds
   locally; release DLLs are produced by whoever cuts the release.

The godot-cpp branch must match the engine minor version (4.7). When the
engine is upgraded, re-clone the matching branch and rebuild.
