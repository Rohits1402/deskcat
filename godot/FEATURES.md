# DeskCat — full feature inventory (Godot port checklist)

Everything the Java app (v1.2.0) does, plus the agreed roadmap. This is the
parity contract for the port: a feature isn't "done" until it matches the
Java behavior described here. Status: `[x]` ported, `[~]` partial, `[ ]` todo.

## 1. Core pet

- [~] **Skins** — squirtle (default), pikachu, cat; picked by launch arg;
      switchable from tray. Java generates all sprites in code (no image
      assets). Port plan: regenerate as `Image`s in GDScript, then move to
      the data-driven custom-character format (§6).
      *Now: procedural placeholder blob.*
- [~] **State machine** — idle, petting, dragging, startle, attack,
      kneading, wander-patrol, return-home, sleep.
  - [~] Idle: breathing bob, blink, occasional look-around.
  - [ ] Petting: slow cursor strokes over the pet → happy eyes + hearts.
  - [~] Dragging: grab & carry; jelly stretch while held, squish on drop.
  - [ ] Startle: fast cursor approach → jump/alert pose.
  - [ ] Attack: click → per-skin attack anim (water gun / thunder / swipe)
        + sparks.
  - [ ] Kneading: typing (global key COUNTER) → knead/typing reaction.
  - [ ] Wander-patrol: after idle a while, paces the bottom work-area edge;
        **wraps around** screen edges.
  - [ ] Return-home: any user input during patrol → walks back to the exact
        pre-wander spot.
  - [ ] Sleep: long idle → sleep pose + zzz particles; input wakes.
- [x] **Eye tracking** — pupils follow the global cursor (works unfocused).
- [x] **Squash & stretch** — damped-spring jelly physics on every pose
      (Java: FBO trick; Godot: node scale, spring ported).
- [ ] **Particles** — hearts (petting), sparks (attack/KO), zzz (sleep),
      dust (walking).
- [ ] **Sounds** — synthesized PCM chirps (no audio assets) via
      AudioStreamGenerator; tray toggle, session-only.
- [ ] **Music groove** — system output PEAK level (never samples) → pet
      bounces to music. Native ext `DeskCatAudioPeak`.
- [ ] **Reminders** — stretch/water nudges; intervals session-only
      (deliberately no config file).
- [ ] **Size control** — Small (default) / Normal / Large for self; applies
      live; remote pets render at sender's size.
- [ ] **Taskbar gap** — auto-detected from the OS work area (Godot:
      `screen_get_usable_rect`, free); manual override existed in Java tray.
- [ ] **Hide behind taskbar** — right-click option; pet sinks so only the
      top peeks above the taskbar; toggling restores.
- [ ] **Survive virtual-desktop switches** — pet stays visible on every
      Windows virtual desktop (native ext polls
      `IVirtualDesktopManager::IsWindowOnCurrentVirtualDesktop` and moves
      the overlay to the active desktop when it changes).
- [ ] **Start with Windows** — user-toggled HKCU Run entry via `reg.exe`
      (the ONLY registry access allowed).
- [x] **Tray icon** — StatusIndicator; menu: skins, size, peer fade,
      reminders, sound, patch notes, check updates, quit. *Now: Quit only.*
- [ ] **Single tray per machine** — second instance detects (Java: loopback
      port 42108 lock) and skips its tray; pet menus grow a Quit entry.
- [ ] **Auto-updater** — checks GitHub Releases `tag_name` vs version,
      downloads, swaps, restarts. Rework swap for Godot exe/pck.
- [ ] **Patch notes** — window from tray, newest on top.

## 2. LAN multiplayer (works with zero internet)

- [ ] **Protocol** — UDP multicast 239.42.10.7:42107, pipe-delimited `DC1`
      messages (STATE / CHAT / ACTION / BYE), escaping, MAX_PACKET 8192.
      **Keep byte-identical to Java** so both clients coexist on one LAN.
- [ ] **Presence** — 60 Hz state sync (position fractions, facing, anim);
      10 s timeout prune; BYE on clean exit.
- [ ] **Remote pets** — teammates' pets with name labels; fade-in on
      appear; tray "Peer fade" opacity; wrap-snap (no cross-screen slide
      when they wrap); KO pose mirrored. In Godot: sprites in the overlay —
      no extra windows, overlap click-through comes free.
- [ ] **Same-host suppression** — two instances on one PC don't mirror
      each other (packet source address vs local addresses, sticky).
- [~] **Do Not Disturb** — tray toggle: hide all remote pets and ignore
      incoming chat/shoot; only your own pet stays visible. You still
      broadcast presence. *Tray toggle exists; enforcement lands with LAN.*
- [ ] **Chat** — input box near pet (grows while typing, Enter sends,
      Esc dismisses); broadcast to all; `@name` DMs (delivered only to
      target); speech bubbles word-wrap, ~5 s, fade out; own bubble too.
- [ ] **Chat commands** (chainable): `/big` `/huge` `/small` `/size N`
      (N = font px, huge sizes allowed), `/shake`, `/rainbow`,
      `/color|/colour <name>`; malformed → plain text. Same rendering for
      every peer.
- [ ] **Giant bubbles** — sizes beyond the pet bubble render as a crisp
      screen-sized bubble (Godot dynamic fonts: free).
- [ ] **/shoot + menus** — pellet sprite flies shooter → target; target
      KOs: squish-flat or 90° topple, then recovers. Right-click menus:
      own pet (Say…, Dismiss bubble, Hide behind taskbar, Shoot <peer>,
      Quit when no tray) and remote pet (Message…, Shoot, Dismiss bubble).

## 3. Privacy / scope rules (carry over verbatim)

- Global keyboard listener **counts keystrokes only** — key identities are
  never read, stored, or sent (native hook never dereferences the key
  struct).
- System audio = **peak level float only**, never samples.
- No telemetry. Only sanctioned network: LAN/relay protocol + GitHub
  updater. Only file writes: updater temp + swap script. Only registry:
  the HKCU Run toggle.
- Reminder/sound settings session-only (no config file) — until the
  settings window (§5) deliberately changes this policy.
- Pokémon skins are personal-use fan art; strip from public releases —
  cat + new original characters only.

## 4. Distribution

- [ ] **Windows export** — single exe via Godot export template (~40 MB,
      no JVM). Release flow: bump version, patch notes, export, zip,
      `gh release create`.
- [ ] **Updater compatibility** — updater must handle the Godot layout.

## 5. Roadmap (new, post-parity)

- [ ] **Settings window** — full in-app window replacing the bloated tray:
      skin, size, name, peer fade, reminders, sound, music groove, startup,
      taskbar gap, network on/off. (Persistence policy decided then.)
- [ ] **Custom characters** — data-driven skin packs (spritesheet PNG +
      manifest: frames, anims, eye geometry, attack) drop-in folder;
      community-moddable like Shimeji/eSheep.
- [~] **3D characters** — transparent SubViewport pipeline composites 3D
      into the overlay; supports GLB models (Kenney CC0 assets approved as
      a source, 2D and 3D). *Now: CSG mannequin proof behind a tray toggle.*
- [ ] **Internet mode** — WebSocket relay with room codes next to LAN mode
      (multicast doesn't cross routers); DC1 over WS; lower sync rate +
      interpolation.
- [ ] **Chat images/stickers** — undecided: sticker set vs real image
      thumbnails (chunked). Awaiting call.

## 6. Explicitly out (Java-only quirks not ported)

- FBO squash trick, WS_EX_LAYERED workarounds, per-entity OS windows,
  Swing focus hacks, PowerShell audio helper subprocess, JNativeHook —
  all replaced by engine features or the native extension.
