# DeskCat — development log

Everything that happened in this project, in order. Session date: 2026-07-29.

## 1. ComNyang research

The project began as research into [Comnyang](https://comnyang.com) — a $3.90
pixel-cat desktop pet for macOS/Windows.

- Catalogued its full public feature set (18 features: eye follow, mouse hunt,
  keyboard kneading, mochi drag, stretch/water reminders, Pomodoro, AI-agent
  reactions for Claude Code CLI / Codex CLI / Cursor / Antigravity / Kiro,
  multi-device license via Google account, Lemon Squeezy checkout, no
  telemetry).
- Reviewed third-party coverage (BestFreeAI review, LaunchingNext listing) —
  the main criticism was "minimal documentation."
- Found an **unaffiliated Solana memecoin** ("AI Agent Comnyang") trading on
  the app's name — flagged as a thing the real developer should know about.
- Drafted a help/FAQ page for the product
  ([docs/comnyang-help.md](docs/comnyang-help.md)) before the project pivoted.

## 2. Pivot: build our own desk pet in libGDX

Decision: build a ComNyang-inspired desktop pet — **"just the pixel art
stuff"** — using Java + libGDX. Scope explicitly excluded licensing,
reminders, Pomodoro, and AI-agent integration.

Environment found on the machine: JDK 8 (1.8.0_111), no system Gradle but a
cached **Gradle 8.7** in `~/.gradle/wrapper/dists`, which is invoked directly.
Later, adding a dependency exposed that the 2016-era JDK truststore no longer
trusts Maven Central — fixed by pointing the build at the newer JRE 8u491
certificate store via [gradle.properties](gradle.properties).

## 3. Version 1 — the orange tabby

Core rig, still the foundation of everything:

- **Window tricks**: transparent framebuffer + undecorated + GLFW `FLOATING`
  → a cat that appears to sit directly on the desktop, always on top.
- **All pixel art generated in code** from character maps (`PixelArt.java`) —
  zero image assets.
- **FBO squash-and-stretch**: the whole character renders into a small
  framebuffer, which is drawn scaled/squashed as one unit; a spring
  simulation drives the mochi/jelly physics.
- **Eye tracking** via AWT `MouseInfo` (works without window focus), blinking,
  3-frame tail animation, petting with heart particles and purr shiver,
  startle ("!") on fast cursor fly-bys, sleep with floating Zzz after idle.
- Drag-to-carry moves the OS window itself.

## 4. Pikachu skin + the skin system

- Added a second character map set (yellow body, black-tipped ears, red
  cheeks, lightning-bolt tail with 3 wag frames).
- Introduced per-skin configuration: body/tail textures, eye geometry, eye
  *style* (cat iris vs. bead-with-glint), fur color. Skins picked by launch
  argument.

## 5. Tray icon, taskbar removal, thunderbolt

- **No taskbar button**: swapped the window to Win32 `WS_EX_TOOLWINDOW` using
  LWJGL's built-in User32 bindings.
- **System tray icon** (AWT) with an Exit menu — plus a `System.exit(0)` at
  loop end because the AWT tray thread is non-daemon and would keep the JVM
  alive.
- **Click vs. drag detection** (movement threshold / hold time), enabling the
  first click attack: Pikachu's **thunderbolt** — three flickering procedural
  lightning bolts, spark burst, cheek flash, attack face, excited hop.

## 6. Keyboard kneading + screen-edge wandering

- **Kneading**: global keyboard hook via JNativeHook. The listener only
  increments a counter — key identities are never inspected or stored. While
  typing, paw overlays knead in rhythm and the eyes look down.
- **Wandering**: after 18–40 s of being left alone the pet falls (gravity +
  landing squash) to the bottom of the work area and walks along it with a
  bob-and-waddle gait, sprite mirrored to face its direction (with gaze
  correction through the mirror).

## 7. Patrol upgrade: endless + return home

Reworked per feedback: the walk became an **endless patrol** — pacing between
screen edges indefinitely. Any input (cursor movement above the wake
threshold, or any keystroke) makes it hurry back at 1.6× speed to the exact
position it left, then **hop up** to its original height with a settle wobble.

## 8. Charizard: attempted and removed

Built a Charizard skin (wings, horns, flame-tipped tail, fireball click
attack with an ember burst). The hand-authored art didn't resemble Charizard
well enough and the whole skin was removed. Lesson learned for later: don't
hand-type complex sprites.

## 9. Choosing the next skin

Generated preview art for **Bulbasaur, Squirtle, Psyduck, Jigglypuff** in two
styles — chunky pixel (procedurally rasterized ellipses + auto-outline) and
smooth vector — rendered as inline widgets for comparison. Decision: pixel
art, Squirtle first.

## 10. Squirtle skin

- First attempt used hand-typed character maps again — and looked boxy and
  wrong next to the preview (square head, slab shell, floating tail).
- **Rebuilt procedurally**: ported the preview's generator into
  `PixelArt.java` — bodies built from overlapping ellipses with an automatic
  outline pass (`ell()` / `outlinePass()` / `toTexture()`), so the in-app
  sprite is constructed from the exact shapes of the approved preview.
- Third eye style added: big outlined brown iris blocks with a tall wandering
  glint.
- Curly tail generated as three wag frames, root tucked behind the shell.
- Click attack: **water gun** — a fountain of glinting droplets with
  per-particle gravity (engine addition), arcing up and falling past it.
- Post-launch fix: the brown shell rim showed as a pointed wedge between the
  feet; the cream plastron ellipse was extended to the ground line and the
  feet reordered on top of it.

## 11. Git and GitHub

- `git init` (branch `main`), [.gitignore](.gitignore),
  [README.md](README.md), initial commit of the full project.
- User installed GitHub CLI and authenticated as **Rohits1402**; the private
  repo **[Rohits1402/deskcat](https://github.com/Rohits1402/deskcat)** was
  created and pushed via `gh repo create --push`.
- **RajatGurnani** invited as a collaborator with write permission.
- Commit authorship corrected to
  `Rohits1402 <87677690+Rohits1402@users.noreply.github.com>` (links to the
  GitHub profile), and the AI co-author trailer removed at the user's request
  — history rewritten and force-pushed while the repo had a single commit.

## 12. Going public, collaborator onboarding, tray skin switcher

- The repo was flipped from private to **public** at the user's request (the
  README's personal-use disclaimer for the Pokémon skins is the operating
  posture).
- **Rajat Gurnani** accepted the collaborator invite and pushed the first
  external commits: [CLAUDE.md](CLAUDE.md) (project guidance for Claude Code
  sessions, including the plain-commit-message rule) and this devlog moved to
  the repo root.
- **Squirtle belly fix**: the brown shell rim showed as a pointed wedge
  between the feet; the cream plastron ellipse was extended to the ground
  line and the feet reordered on top of it.
- **Tray skin switcher**: the tray menu gained a *Skin* submenu (Squirtle /
  Pikachu / Cat, checkmark on the active one) that swaps the character live —
  skin setup was extracted from `create()` into `applySkin()`, which disposes
  the old skin's textures, cancels in-flight attacks, and runs on the GL
  thread via `postRunnable`. Quit stays as its own item below a separator;
  the tray tooltip shows the active skin.
- `.gitignore` extended with IntelliJ artifacts (`.idea/`, `local.properties`).

## 13. Packaging and auto-updater

- Added `fatJar` Gradle task producing a single runnable
  `deskcat-X.Y.Z.jar` (all deps + LWJGL natives merged), and
  `CatApp.VERSION` kept in sync with it.
- New `Updater.java`: checks GitHub Releases (latest tag vs. running
  version) quietly ~8 s after startup and on demand via the tray's new
  "Check for updates" item. On a newer release: tray balloon + the item
  becomes "Install update vX" → downloads the jar asset, spawns a script
  that waits for exit, swaps the jar in place, and relaunches with the same
  JVM. In dev mode (gradle classes, no jar) it opens the release page
  instead. All failures degrade silently to "no update UI."
- This carves the one sanctioned exception into the no-network rule: the
  user-visible update check/download. Still no telemetry — nothing is sent.
- First packaged release: **v1.0.0** on the repo's releases page.

## 14. Windows executable

- Downloaded Temurin JDK 21 (to `~/.jdks`) purely as a packaging toolchain —
  the app still targets Java 8.
- `jpackage --type app-image` wraps the fat jar into
  `build/dist/DeskCat/DeskCat.exe` with a trimmed, bundled Java 21 runtime:
  double-click to run, no Java required on the machine.
- `DeskCat-1.0.0-win64.zip` (~67 MB) uploaded to the v1.0.0 release
  alongside the jar; README documents both install flavors.
- The auto-updater still functions inside the app image (it swaps the jar
  under `app/`), provided the folder sits somewhere user-writable.

## 15. Reminders, sound effects, launch at startup

- **Stretch/water reminders** (tray > Reminders, opt-in, session-only):
  every 30 min the pet performs a long tall stretch (spring target 1.5,
  eyes closed mid-stretch) so the user stretches along; every 45 min it
  hops and fountains water droplets overhead. Both post a tray balloon,
  chirp, and call the pet home first if it's out patrolling.
- **SoundFx**: procedurally synthesized PCM — thunderbolt zap (noise +
  descending square), water-gun splash (noise + rising bubble sweeps),
  and a mew-like chirp (vibrato sine sweep) for startles and reminders.
  No audio files; tray "Sound" checkbox mutes.
- **Start with Windows**: tray checkbox writing the per-user
  `HKCU\...\Run` key via reg.exe — registers the packaged exe (preferred)
  or `javaw -jar`; dev runs explain they can't be registered. Unticking
  deletes the value.

## Current state

- **Skins**: Squirtle (default, procedural), Pikachu (character maps), and
  the original orange tabby — switchable live from the tray's Skin submenu,
  or at launch via `gradle run --args="pikachu"` etc.
- **Behaviors**: eye tracking, blink, tail animation, mochi drag, petting
  hearts, keyboard kneading, startle, click attacks (water gun /
  thunderbolt / startle), endless bottom-edge patrol with return-home, sleep,
  tray menu (skin switcher + quit), no taskbar button.
- **Repo**: public at
  [github.com/Rohits1402/deskcat](https://github.com/Rohits1402/deskcat);
  collaborators: Rohits1402 (owner), Rajat Gurnani (write).

## Backlog / ideas

- Remaining skins via the ellipse builder: Jigglypuff (sing attack),
  Psyduck (headache), Bulbasaur (leaf volley).
- Procedural zap/splash sound effects (libGDX `AudioDevice`, no asset files).
- Launch-at-startup, packaged double-clickable EXE (jpackage).
- Per-pixel click-through on transparent areas (Win32 layered window work).
- Note: Pokémon skins are personal-use fan art — strip them before any
  public release; the cat is the original, shippable character.
