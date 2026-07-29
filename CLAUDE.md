# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

DeskCat: a Windows desktop pixel pet (Java 8 + libGDX/LWJGL3). Transparent always-on-top window, no taskbar button, AWT tray icon for Exit. Three skins selected by launch arg: `squirtle` (default), `pikachu`, `cat`.

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
  - Procedural ellipse builder: `ell()` + `outlinePass()` + `toTexture()` (Squirtle). Prefer this for new complex skins — hand-typed maps for detailed characters failed twice (see DEVLOG §8, §10).

Key mechanics that span the codebase:

- **FBO squash-and-stretch**: the character renders into a small framebuffer (`FBO_W`×`FBO_H` world units), then the FBO texture is drawn scaled/squashed as one unit; a spring sim (`updateSpring`) drives the jelly physics. New body parts must be drawn in `renderCatToFbo()` to inherit the squash.
- **Global input without focus**: cursor via AWT `MouseInfo` (`globalCursor()`), keystrokes via JNativeHook. The keyboard listener only increments a counter — key identities must never be inspected or stored (stated privacy guarantee).
- **Window as position**: wandering/dragging moves the OS window itself (`Lwjgl3Window.setPosition`), not the sprite. Patrol paces the bottom work-area edge endlessly; any input triggers return-home to the exact pre-wander position.
- **Taskbar removal**: Win32 `WS_EX_TOOLWINDOW` set through LWJGL's User32 bindings in `hideFromTaskbar()`.

## Constraints

- Pokémon-inspired skins (Squirtle, Pikachu) are personal-use fan art — must be stripped before any public release; the cat is the only shippable original character.
- No telemetry, no network calls, no files written at runtime — keep it that way.
- [DEVLOG.md](DEVLOG.md) is the project history and backlog; append to it when making significant changes.
