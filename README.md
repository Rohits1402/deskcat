# DeskCat

A tiny pixel pet that lives on your Windows desktop, built with Java 8 and
[libGDX](https://libgdx.com/). It sits in a transparent, always-on-top,
taskbar-free window and reacts to what you do — inspired by desktop pets like
[Comnyang](https://comnyang.com), Neko, and Shimeji.

## Features

- **Eye tracking** — the pet's eyes follow your cursor anywhere on screen,
  even when the window has no focus.
- **Mochi drag** — pick it up and it stretches like mochi; drop it and it
  wobbles like jelly.
- **Petting** — stroke its head with the cursor and hearts float up while it
  purrs.
- **Keyboard kneading** — type anywhere and it kneads along with tiny paws
  (global hook counts keystrokes only; keys are never inspected or stored).
- **Startle** — fling the cursor past it and it flinches with a "!".
- **Click attacks** — each skin has a signature move (see below).
- **Edge patrol** — leave it alone and it drops to the bottom of the screen
  and paces endlessly between the edges; any mouse or keyboard input sends it
  hurrying back to exactly where it was.
- **Sleep** — after a minute of idle it dozes off with floating Zzz.
- **System tray** — no taskbar button; a tray icon hosts the Exit menu.

## Skins

| Skin | Eyes | Click attack |
|------|------|--------------|
| `squirtle` (default) | outlined iris blocks | water gun — droplet fountain with gravity |
| `pikachu` | bead eyes with glint | thunderbolt — triple lightning strike |
| `cat` | green cat eyes | just gets startled (it's a cat) |

All sprites are generated in code — either authored as character maps or
built from overlapping ellipses with an automatic outline pass. There are no
image assets.

> The Pokémon-inspired skins are personal-use fan art. Do not distribute or
> sell builds containing them; the cat is the only original character.

## Running

Requires JDK 8+ and Gradle (a `gradle.properties` entry points the build at a
newer JRE's certificate store if your JDK 8 is too old for Maven Central).

```
gradle run                      # default skin (squirtle)
gradle run --args="pikachu"
gradle run --args="cat"
```

Right-click the tray icon (yellow bolt) and choose Exit to quit.

## How it works

- Transparent LWJGL3 window (`setTransparentFramebuffer`), undecorated,
  GLFW `FLOATING` for always-on-top, and Win32 `WS_EX_TOOLWINDOW` (via
  LWJGL's User32 bindings) to remove the taskbar button.
- The character is rendered into a small framebuffer, which is then drawn
  squash-and-stretched as one unit — a spring simulation drives the jelly
  physics.
- Global cursor position comes from AWT `MouseInfo`; global keystroke counts
  from JNativeHook; both work without window focus.
- Wandering moves the OS window itself along the bottom of the work area.

No telemetry, no network calls, no files written.
