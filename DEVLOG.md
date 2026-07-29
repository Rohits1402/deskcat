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

## 13. LAN presence, chat, size control, tests (`feature/lan-presence`)

Built on a feature branch; settings window and auto-updater are being built
separately by another dev — the LAN work reads the launch args for now
(`java -jar deskcat.jar <skin> <name>`).

- **LAN presence**: UDP multicast (`239.42.10.7:42107`), pipe-delimited
  escaped wire format (`LanProtocol`), 5 Hz state broadcasts (skin, position
  as work-area fractions, facing, idle/walk/sleep/drag), 10 s peer timeout,
  BYE on exit. Everything rides the one multicast group; DMs are filtered by
  target id on the receiving side.
- **Remote pet windows**: one small transparent always-on-top window per
  peer (`RemotePetsManager` / `RemotePetWindow`), same size as the local pet,
  no taskbar button, name label above the head, walk bob + facing mirror +
  blink, position eased between 5 Hz updates. Textures come from the shared
  `SkinAssets` cache (per-skin, disposed once at shutdown) — `applySkin()`
  no longer disposes on swap since remote pets may use the same skin.
- **Chat**: middle-click (or right-click → *Say…*) opens a small dark text
  field; Enter broadcasts, `@name message` DMs. Pixel speech bubbles with
  outline, tail nub, ellipsis truncation and fade render above the pet on
  every desktop. Bubble timing lives in a pure `Bubble` class shared by the
  local pet and peers.
- **Right-click menus** (`PetMenu`, Swing popup on an invisible anchor):
  own pet — Say… / Dismiss bubble / Hide behind taskbar / Quit (when no
  tray); remote pet — Message them… / Dismiss bubble. Remote windows accept
  clicks now (click-through was dropped for this).
- **Hide behind taskbar**: drops always-on-top and tucks the window into the
  taskbar area with the ears peeking out; clicking the peek or the menu
  brings it back to its old spot.
- **Size control**: `scale` (px per world unit) is adjustable from the
  tray's *Size* submenu (Small 3 — default, Normal 5, Large 7); the window,
  input math, bubble camera and all remote windows resize live. *Taskbar
  gap* submenu adds patrol clearance (Auto = screen insets, which already
  detect the taskbar; manual px for auto-hide setups).
- **Footprint**: `fatJar` task (everything-included ~14 MB jar) + tuned JVM
  defaults (`-Xms16m -Xmx48m -XX:+UseSerialGC`, metaspace cap) — two
  instances ran at ~150 MB working set each, no Gradle daemons.
- **Tests**: JUnit 4 wired in (`gradle test`, 26 tests): protocol round-trip
  /escaping/garbage rejection, peer registry add/update/prune/BYE/DM
  filtering, spring physics (extracted `Spring` class now drives the squash),
  bubble timing/fade, ellipsis fitting (measurer injected so no GL needed).
  Compile encoding pinned to UTF-8 (the `…` literal broke under Cp1252).
- Verified live: two instances (Squirtle "Rajat" + Pikachu "Dev2") found
  each other over multicast, remote windows rendered transparent with name
  labels.

## Current state

- **Skins**: Squirtle (default, procedural), Pikachu (character maps), and
  the original orange tabby — switchable live from the tray's Skin submenu,
  or at launch via `gradle run --args="pikachu"` etc.
- **Behaviors**: eye tracking, blink, tail animation, mochi drag, petting
  hearts, keyboard kneading, startle, click attacks (water gun /
  thunderbolt / startle), endless bottom-edge patrol with return-home, sleep,
  tray menu (skin switcher + size + taskbar gap + quit), no taskbar button.
- **LAN** (branch `feature/lan-presence`): peer discovery, remote pet
  windows with names, broadcast + DM chat with bubbles, right-click menus,
  hide-behind-taskbar, size control, unit tests, fat jar.
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
