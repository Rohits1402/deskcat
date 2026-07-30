# deskcat_native — GDExtension (Win32 helpers)

Three tiny classes Godot has no API for:

| Class | Does | Privacy rule |
|---|---|---|
| `DeskCatWinTricks` | `WS_EX_TOOLWINDOW` — hide overlay from taskbar/Alt-Tab | — |
| `DeskCatKeyCounter` | global keystroke **counter** (`WH_KEYBOARD_LL`) | key identities never read — hook only increments an atomic int |
| `DeskCatAudioPeak` | system output **peak level** (WASAPI `IAudioMeterInformation`) | interface can only return a 0..1 float, never samples |

The GDScript app runs without the DLL — `NativeBridge` no-ops everything.

## Build (once per machine)

1. Install **VS Build Tools** with the *Desktop development with C++* workload
   (`winget install Microsoft.VisualStudio.2022.BuildTools` with the VCTools
   workload).
2. `pip install scons`
3. godot-cpp's release branches stop at 4.5 — for newer engines use master
   plus the API description dumped from the exact engine binary:
   ```
   cd godot/native
   git clone --depth 1 https://github.com/godotengine/godot-cpp
   <godot_console.exe> --headless --dump-extension-api
   scons platform=windows target=template_debug   custom_api_file=extension_api.json
   scons platform=windows target=template_release custom_api_file=extension_api.json
   ```
   (debug is what the editor / non-exported runs load; release ships)
4. Output lands in `godot/bin/win64/`; `deskcat_native.gdextension` picks it up
   on next launch. `godot-cpp/`, `extension_api.json` and `bin/` are
   gitignored — every dev builds locally; release DLLs are produced by
   whoever cuts the release.

When the engine is upgraded, re-dump the API json and rebuild.
