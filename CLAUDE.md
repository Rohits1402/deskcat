# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

DeskCat: a Windows desktop pixel pet (Java 8 + libGDX/LWJGL3). Transparent always-on-top window, no taskbar button, AWT tray icon for Exit. Six skins, selected by launch arg or live from the tray: `squirtle` (default), `pikachu`, `cat`, `blackcat`, `doge`, `goldfish`.

## Build / run

No Gradle wrapper is checked in and `gradle` may not be on PATH — the project was built with a cached Gradle 8.7 dist invoked by full path (check `~/.gradle/wrapper/dists`). Target is Java 8.

```
gradle run                      # default skin (squirtle)
gradle run --args="pikachu"
gradle run --args="cat"
```

- [gradle.properties](gradle.properties) points the build at a newer JRE 8's `cacerts` because the 2016-era JDK 8 truststore rejects Maven Central. If that hardcoded path (`C:/Program Files/Java/jre1.8.0_491/...`) doesn't exist on the current machine, dependency resolution will fail — update the path, don't delete the property.
- No tests exist. Verify changes by running the app; quit via the tray icon's Exit menu (the app has no window chrome).

## Architecture

Three source files, all in `com.deskcat`:

- [DesktopLauncher.java](src/main/java/com/deskcat/DesktopLauncher.java) — LWJGL3 config (transparent framebuffer, undecorated, positioned bottom-right). Ends with `System.exit(0)` because the AWT tray thread is non-daemon; removing it makes the JVM hang on exit.
- [CatApp.java](src/main/java/com/deskcat/CatApp.java) — the entire runtime: state machine (idle / petting / dragging / startle / attack / kneading / wander-patrol / return-home / sleep), rendering, particles, window movement. Skin is a static `CatApp.skin` string set before launch; per-skin config (textures, eye geometry, eye style, attack) branches on it in `create()`.
- [PixelArt.java](src/main/java/com/deskcat/PixelArt.java) — all sprites, generated in code; there are **no image assets**. Two authoring styles:
  - Character maps: `String[]` grids of chars → colors via `fromMap()` (cat, Pikachu, particles).
  - Procedural ellipse builder: `ell()` + `tri()` + `outlinePass()` + `toTexture()` (Squirtle, black cat, doge, goldfish). Prefer this for new complex skins — hand-typed maps for detailed characters failed twice (see DEVLOG §8, §10).
  - Check new art on a **dark** backdrop too: a dark skin outlined in near-black vanishes on dark wallpaper (the black cat needed the lighter `'1'` rim). And draw animals in the view people recognise them in — the goldfish read as a chick head-on and only works in profile (`SkinAssets.oneEye`).

Key mechanics that span the codebase:

- **FBO squash-and-stretch**: the character renders into a small framebuffer (`FBO_W`×`FBO_H` world units), then the FBO texture is drawn scaled/squashed as one unit; a spring sim (`updateSpring`) drives the jelly physics. New body parts must be drawn in `renderCatToFbo()` to inherit the squash.
- **Global input without focus**: cursor via AWT `MouseInfo` (`globalCursor()`), keystrokes via JNativeHook. The keyboard listener only increments a counter — key identities must never be inspected or stored (stated privacy guarantee).
- **Window as position**: wandering/dragging moves the OS window itself (`Lwjgl3Window.setPosition`), not the sprite. Patrol paces the bottom work-area edge endlessly; any input triggers return-home to the exact pre-wander position.
- **Taskbar removal**: Win32 `WS_EX_TOOLWINDOW` set through LWJGL's User32 bindings in `hideFromTaskbar()`.
- **Poses**: [PoseArt.java](src/main/java/com/deskcat/PoseArt.java) builds whole-frame poses (side-view walk cycles, sleep, belly-up roll, Squirtle's shell) and Pikachu's body/tail as plain RGBA8888 grids with **no libGDX dependency**, so a scratch preview can render the exact shipping pixels; `PixelArt.tex()` converts them. While a pose plays (`CatApp.poseFrame()`), it replaces body + tail + runtime eyes, so poses bake their eyes in. Walk frames face right and are centred on the FBO mirror axis.
- **Wander states**: 0 home, 1 falling, 2 floor patrol, 3 walking home, 4 hop up to home, 5 free swim (goldfish), 6 parabolic window hop (`hopTo`, used by pounces and perching), 7 perched. `seated()` (0 or 7) gates sit-still behaviours; `roaming()` and `moving()` group the others.
- **Perching**: [Win32Windows.java](src/main/java/com/deskcat/Win32Windows.java) reads the foreground window's rect/visibility/class/process via LWJGL's User32 plus `JNI` trampolines for the calls LWJGL doesn't bind. It refuses our own process, maximised/minimised/tool windows and shell surfaces (taskbar, desktop). All failures degrade to "nothing to perch on".

## Constraints

- Commits: no `Co-Authored-By` trailer, no Claude/AI as author or committer, no generated-with footers. Plain commit messages only.

- Pokémon-inspired skins (Squirtle, Pikachu) are personal-use fan art — must be stripped before any public release. The cat, black cat, doge (a generic Shiba Inu) and goldfish are original and shippable.
- No telemetry. The ONLY sanctioned network access is the GitHub Releases
  update check/download in [Updater.java](src/main/java/com/deskcat/Updater.java);
  the only runtime file writes are that updater's temp jar + swap script; the
  only registry access is the user-toggled `HKCU\...\Run` startup entry; the
  only look at other programs is `Win32Windows` reading window geometry
  and identity for perching (read-only: never input, focus or state changes)
  (via `reg.exe`). Never add anything beyond those. Reminder/sound settings
  are deliberately session-only — do not add a config file.
- Sounds are synthesized PCM in
  [SoundFx.java](src/main/java/com/deskcat/SoundFx.java); each play opens a
  short-lived `AudioDevice` on a daemon thread. The ONE bundled audio asset is
  `src/main/resources/sfx/meow.ogg`, a **CC0 / public-domain** cat recording
  from [BigSoundBank](https://bigsoundbank.com/meow-cat-12-s1900.html) (sound
  #1900), loaded via `Gdx.files.internal` with the synthesized meow as
  fallback. Any future audio asset must be CC0 or equivalently free —
  Pokémon cries are copyrighted (Nintendo / Game Freak / Creatures) and must
  never be bundled; the Pikachu and Squirtle voices stay original synthesis.
- [SystemAudio.java](src/main/java/com/deskcat/SystemAudio.java) spawns one
  PowerShell helper subprocess (inline C# via Add-Type, `-EncodedCommand`,
  nothing on disk) that streams the Core Audio output PEAK LEVEL — one float
  per 200 ms. It must never capture audio samples; peak level only. The
  helper dies with the app (dispose calls stop; a broken stdout pipe ends
  it if the JVM is hard-killed).
- Releases: bump `CatApp.VERSION` and the `fatJar` version in
  [build.gradle](build.gradle) together, move the "Unreleased" entries in
  `PatchNotes.NOTES` under the new version heading, run `gradle fatJar`, then
  `gh release create vX.Y.Z build/libs/deskcat-X.Y.Z.jar`. The updater
  compares `tag_name` against `CatApp.VERSION` and installs the first `.jar`
  asset. Run jar builds with JRE 8u491+ — the 2016 JDK 8 truststore may
  fail TLS to GitHub, which degrades the updater silently.
- Windows exe: `jpackage --type app-image --name DeskCat --input build/libs
  --main-jar deskcat-X.Y.Z.jar --dest build/dist --app-version X.Y.Z
  --vendor Rohits1402`, then zip `build/dist/DeskCat` as
  `DeskCat-X.Y.Z-win64.zip` and upload it to the release alongside the jar.
  jpackage lives in the Temurin 21 install at `~/.jdks/jdk-21*` (the app
  still compiles for Java 8; the exe bundles the 21 runtime). The
  auto-updater works inside the app image too — it swaps `app/*.jar` in
  place — but only when the unzipped folder is user-writable. The packaged
  exe must be closed before zipping; the launcher locks its own jar.
- [DEVLOG.md](DEVLOG.md) is the project history and backlog; append to it when making significant changes.
