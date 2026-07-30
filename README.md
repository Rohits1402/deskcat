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
- **Stretch and water reminders** — opt-in from the tray: every 30 minutes
  the pet rises into a long tall stretch so you stretch along; every 45
  minutes it hops and fountains water droplets to remind you to drink.
  Both come with a tray notification and a chirp.
- **Voices and sound effects** — each skin has its own voice: a real
  public-domain (CC0) meow for the cat, and synthesized cries for Pikachu
  and Squirtle, plus chirp/splash effects. Tray → Sound opens a volume
  slider (0% is silence).
- **Grooves to your music** — when the system is playing audio, the pet
  bobs and sways with the actual loudness, its tail wags double-time, and
  musical notes float up. Detection reads only the output peak level (a
  single loudness number via Windows Core Audio) — no audio is ever
  captured or recorded.
- **Start with Windows** — one tray checkbox registers the packaged
  jar/exe in the per-user Run key (HKCU); untick to remove it.
- **System tray** — no taskbar button; the tray icon hosts the skin
  switcher, reminders, sound and startup toggles, updates, and quit.

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

Right-click the tray icon (yellow bolt) to switch skins on the fly
(Skin submenu), check for updates, or quit.

## Installing and updates

Packaged builds live on the
[releases page](https://github.com/Rohits1402/deskcat/releases) in two
flavors:

- **`DeskCat-x.y.z-win64.zip`** — unzip anywhere and double-click
  `DeskCat.exe`. A trimmed Java runtime is bundled inside; nothing needs to
  be installed.
- **`deskcat-x.y.z.jar`** — single runnable jar for machines that already
  have Java 8+:

```
java -jar deskcat-1.1.0.jar
```

The app checks GitHub Releases for a newer version shortly after startup
(and on demand via the tray's "Check for updates"). If one exists, a tray
notification appears and the menu item becomes "Install update" — clicking
it downloads the new jar, swaps it in place, and relaunches. This check is
the app's only network access; nothing is ever sent.

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

No telemetry. The only network access is the update check against GitHub
Releases; the only files written are the updater's own temp files; the only
registry touch is the optional user-toggled startup entry. Reminder settings
are session-only — nothing is persisted.
