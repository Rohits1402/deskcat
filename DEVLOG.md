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
- Post-testing fixes: chat input and pet menus moved to undecorated utility
  JFrames (ownerless JWindows can't take focus on Windows — typing and menu
  clicks were dead), remote windows turn click-through while overlapping the
  local pet so it stays draggable, same-machine peers are detected by source
  address and get no mirror window (no more duplicate pets when testing two
  instances on one PC), pose sync raised 5 Hz → 60 Hz, hide-behind-taskbar
  peek enlarged. 27 tests.

## 14. Packaging and auto-updater

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
- Merge note: the LAN branch and this work both added a `fatJar` task —
  unified into one (versioned name + `Implementation-Version` from main,
  tuned JVM defaults and UTF-8 compile encoding from the branch).

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

## 16. Grooving to system audio

- New `SystemAudio`: a PowerShell helper (inline C# COM interop against
  `IAudioMeterInformation`, compiled in memory, launched via
  `-EncodedCommand` so nothing touches disk) streams the system output
  peak level at 5 Hz; a daemon thread parses it into a volatile float.
  Peak level only — no audio capture, ever.
- When sound is sustained (~0.6 s above threshold, with hysteresis so
  brief dings don't trigger it), the pet starts dancing: bob + sway
  scaled by the smoothed loudness envelope, tail at double speed, and
  musical-note particles floating up. Music also blocks sleep — it
  grooves instead.
- Dancing yields to everything else: dragging, patrol, kneading,
  attacks, reminders.

## 17. Release v1.1.0

Everything since v1.0.0 in one release: stretch/water reminders, procedural
sound effects, launch-at-startup, grooving to system audio, and the merged
LAN presence + chat + size control work (PR #1) with the project's first
unit tests. Shipped as both the runnable jar and the jpackage exe zip;
v1.0.0 copies self-update via the tray.

## 18. Wraparound patrol, remote fade, wrapped bubbles, growing chat box

On `feature/wrap-fade-chat`:

- **Patrol wraparound**: instead of turning at the screen edges, the pet now
  walks fully off one side and reappears on the other, keeping direction.
  Remote copies snap (not slide) across when a peer wraps, and their mapped
  position stays clamped to the viewer's work area by the 0–1 fraction
  protocol, so a mid-wrap peer just holds at the edge.
- **Remote pet fading**: peers' pets fade in over ~0.4 s when they appear
  and render at 90% opacity so they read as visitors next to the local pet;
  name label and bubble fade with them.
- **Wrapped bubbles**: speech bubbles word-wrap up to 4 lines (hard-splitting
  oversized words), with an ellipsis only past that — replacing single-line
  truncation. `Bubbles.wrap()` is measurer-injected and unit-tested.
- **Chat input**: bigger (15 px font, 30 columns) and self-expanding — grows
  with the text up to 70 columns, clamped to the screen's right edge.
- Tests grow to 32 (wrap: single-line, word breaks, hard split, line cap +
  ellipsis, empty text).

## 19. Cat scratch, per-skin voices, volume flyout

- **Cat click attack**: the tabby no longer just startles — it slaps a paw
  out toward whichever side the cursor is on, twice over 0.6 s, with claw
  marks fading in at full reach, a lunge that carries the whole sprite, and
  its eyes tracking the swinging paw. Yields to dragging, patrol, kneading
  and reminders like the other attacks.
- **Per-skin voices**: the cat plays a real **CC0 / public-domain** meow
  ([BigSoundBank #1900](https://bigsoundbank.com/meow-cat-12-s1900.html),
  59 KB OGG at `src/main/resources/sfx/meow.ogg`) — the project's first
  bundled asset — with the synthesized meow as fallback if the resource is
  missing. That synthesized version follows published meow acoustics
  (~0.55 s, f0 rising to ~700 Hz then falling, two moving formants sliding
  nasal → "ee" → "ah" → "ow", light vibrato).
- Pikachu and Squirtle get **original synthesized cries** — a two-syllable
  "pi-ka" chirp with an electric crackle tail, and a warbling watery squeak
  ending in a bubble. Their real cries are copyrighted (Nintendo / Game
  Freak / Creatures) and are deliberately never bundled; no CC0 version of
  them exists to license. The old standalone zap was folded into the pika
  cry; the splash moved to the water reminder.
- **Volume flyout**: tray *Sound* now opens a bare Windows-style slider next
  to the cursor (`VolumePopup`) — drag to set 0–100% live, no preview sound,
  closes on focus loss or Escape. `SoundFx.enabled` was dropped entirely:
  volume is the single control and 0 is silence. Session-only, like the
  reminders.

## 20. Black cat, doge and goldfish

Three new procedural skins, each with its own click move, chosen with the
user up front:

- **Black cat** — slimmer than the tabby, amber eyes. Click: the back
  arches and the fur bristles (dark spikes off both flanks and a ridge on
  the skull, pupils blown wide) for 0.35 s, then the existing paw-swipe
  rig takes over. New `SoundFx.hiss()` — high-passed noise.
- **Doge** — a Shiba Inu. Click: digs frantically for 1.6 s — paws
  pumping, body jittering, eyes down, dirt clods flung up and out with
  gravity. New `SoundFx.bork()` — a falling formant-filtered woof.
- **Goldfish** — click: inflates to 1.3x like a pufferfish (ease in,
  hold, wobbling deflate) while blowing bubbles from its mouth. New
  `SoundFx.blub()` — two rising chirp bloops. It also **swims freely**:
  a new wander state 5 steers the window toward random points anywhere in
  the work area along lazy S-curves, instead of dropping to the floor and
  patrolling. Any input sends it swimming straight home on both axes.

Engine changes: `SkinAssets.swims` and `oneEye` flags; attack types 3–5;
`roaming()` / `moving()` helpers replace the scattered
`wanderState == 1 || 2` and `== 2 || 3` checks so every one of them now
covers swimming; the remote-pet renderer honours `oneEye` too. Skin menu
labels are explicit now ("Black cat", not "Blackcat").

Art was iterated against a scratch preview rendered on a light **and** a
dark backdrop before it went in, which caught three problems:

- The black cat vanished on dark wallpaper — near-black outline on
  near-black. Fixed with a lighter rim colour (`'1'`, #6B6178).
- The doge read first as a jackalope (forked-looking ears) and then as a
  fox. What makes it read as a Shiba: a wide face with puffy cream
  cheeks, small upright ears, cream eyebrow spots, and 3x2 squinting eyes
  for the smug look.
- The goldfish, drawn head-on to suit the two-eye renderer, read as a
  chick: the dorsal fin became a comb, the side fins wings. Redrawn in
  profile with a forked waving tail and a single eye.

## 21. Walk cycles, poses, quirks, pouncing and perching

Everything recommended for the original three skins, plus a Pikachu redraw:

- **Pose system** — a skin can now carry whole-frame poses alongside its
  sitting body: `walkTex` (4-frame side-view walk, facing right),
  `sleepTex` (2 breathing frames) and `rollTex` (belly-up, 2 frames), plus
  Squirtle's `shellTex`. They're built in the new `PoseArt`, which has no
  libGDX dependency so the scratch preview renders the exact shipping
  pixels — the preview/port duplication of §20 is gone. While a pose
  plays it replaces body, tail and runtime eyes. Peers' pets over LAN use
  the walk and sleep poses too.
- **Walk cycles** — legs actually step now; the sticker waddle is dropped
  for skins with real frames. 8 fps on patrol, 12 fps hurrying home.
- **Sleep poses** — the cat curls into a loaf, Pikachu flops on its
  tummy, Squirtle tucks into its shell with its eyes closed in the dark.
- **Belly-up** — about 3 s of continuous petting rolls it over, paws
  pedalling, with hearts coming twice as fast.
- **Idle quirks** (only while sitting quietly): cats groom — lick a
  raised paw, then wipe it over the face; Pikachu's cheeks flash and throw
  sparks; Squirtle ducks into its shell when startled or carried, peeks
  out, then pops back.
- **Pounce** (both cats) — a slowly moving cursor beside the pet for
  ~0.6 s starts a stalk: crouch (squashed low, pupils blown), a
  pre-pounce wiggle, then a parabolic window hop of up to 170 px toward
  the cursor ending in a swat. A fast cursor calls it off.
- **Overheat** — heat builds with keystrokes and cools over time
  (~8 keys/s sustained trips it, normal typing doesn't): the pet flushes
  red, kneads in a frenzy and steams; Pikachu crackles with static.
  Rate is derived from the existing keystroke *counter* — still no key
  identities.
- **Perching** — when it would wander, 55% of the time it instead hops onto
  the title bar of the foreground window (if that's an ordinary restored
  window with room above it) and rides along as you move it. It tumbles
  to the floor if the window closes, minimises or maximises, and hops home
  after 1–2.5 minutes. New `Win32Windows` does the read-only lookups;
  LWJGL's User32 lacks `GetForegroundWindow`/`IsWindow`/`GetClassNameW`/
  `GetWindowThreadProcessId`, so those are resolved from user32.dll and
  called through LWJGL's `JNI` trampolines. Probed against a real desktop
  before shipping: correct rects and class names, and a maximised window
  on the second monitor was correctly refused.
- **Pikachu redrawn** — the day-one hand-typed sprite is replaced by the
  procedural reference-matched body and slab bolt tail from the earlier
  art sessions, with a new round-bead runtime eye (`eyeStyle 4`).

Art went through the preview twice: the walking Pikachu first read as a
shouting chick (cheek where a mouth goes, stubby ears), its belly-up pose
showed both red cheeks where eyes belong, and Squirtle's shell with arm
and leg holes read as a frowning face.

## 22. Cats redrawn by hand (CatArt)

Studying ComNyang's skins showed why they look so much cleaner than ours:
every part is a hand-placed pixel staircase (they ship them as SVG paths),
fills are flat with a few deliberate pattern pixels, and one uniform 1px
outline is grown around the whole silhouette (an SVG `feMorphology`
dilate) in a per-skin colour - white on their black cats. All their cats
share one silhouette and differ only by colour regions.

Both our cats now work that way, in new `CatArt` (our own silhouette, not
theirs):

- Parts are authored as absolute `M/H/V/Z` path strings and filled at pixel
  centres (even-odd), so curves are exact staircases with no stray pixels.
  Symmetric parts are drawn once and mirrored.
- The canvas holds **zones** (fur, far-side fur, cream, inner ear, iris,
  ring, pattern, crease...), and a `Palette` maps them to colours. The
  tabby and the black cat are the same shapes through two palettes; the
  black cat gets a light outline, whiskers and closed-eye line.
- `mark()` paints stripes only over fur; `fillOver()` gives a part drawn
  in front a soft crease where it overlaps earlier parts (sleeping head
  against the body, tail along the loaf) instead of a hard inner outline.
- `render()` dilates one outline around everything (8-neighbour, so
  diagonal steps stay closed), then adds whiskers outside it.
- New sitting body (28x30, eyes big and low), 3-frame tail, 4-frame trot,
  2-frame sleeping loaf, 2-frame belly-up with folded front paws, and a
  palette-coloured paw. The black cat gained poses for the first time.
- Eye sockets (ring + iris) are baked in; new `eyeStyle 5` draws only the
  2x2 pupil and glint (tall blown pupils when startled), and closes the
  eye by covering the socket with fur and drawing a curve in the skin's
  `lineColor`. Hiss bristles moved out to the wider body and use
  `lineColor` so they show on the black cat.
- Removed: the tabby char map and tails, the procedural black cat, the
  old PoseArt cat poses, PAW_CAT/PAW_BLACK, and the unused eye style 0.

First pass problems caught in the preview: the sleeping cat read as a box
with an L-shaped crease (fixed by a rounder loaf and creases from
`fillOver`), the belly-up legs read as a picket fence (front paws now
fold short on the chest), and the walk's far ear was a round blob.

## 23. Every skin redrawn in the path format

After the cats (§22), the doge, goldfish, Pikachu and Squirtle were redrawn
the same way, and the format moved out of `CatArt` into a shared `PathArt`
(builder-style `Palette`, generic accent zones, `paint()` clipped to any
zone, mirrored `fillPair`/`fillOverPair`/`paintPair`). Each skin has its own
art file. Parts were authored as one span per row and converted to paths
with a small script, then iterated in a preview that composes frames the
way CatApp does (tail at `tailX`, body at `BODY_X`, runtime eyes).

- **Doge**: wide face, cream cheeks/muzzle/eyebrow dots, upright ears,
  curled tail with a cream swirl. New walk, sleep and belly-up poses reuse
  the cat's body paths with a dog head (the first sleeping head read as a
  cat until it got a snout; the first upside-down nose read as a moustache).
- **Pikachu**: long black-tipped ears, cheeks on the head's edge, bolt
  tail with a brown base, back stripes in side poses. Eyes widened to 4px -
  at 3px the centred glint made them look like hollow rings - and the tail
  moved out from behind the head where it read as a raised paw.
- **Squirtle**: big head, brown shell edge with a cream rim around a
  plated yellow belly, curled tail. The shell pose kept the opening where
  the runtime peek eyes go; its curved seams again read as frowning brows
  over a mouth, so it uses the walking shell's plate grid instead.
- **Goldfish**: profile body with lighter belly, gill curve, a few scale
  pixels, fins with rays, and a fuller forked fan tail whose lobes flutter
  in opposition (thin lobes looked like two separate fins).
- Runtime geometry (eye rects, Pikachu's cheek spark points, tail offsets)
  updated in `SkinAssets`; eye styles and all behaviour code unchanged.
- Removed: PixelArt's ellipse builder and the procedural Squirtle, doge
  and goldfish; PoseArt's Pikachu/Squirtle poses and drawing helpers
  (PoseArt is now just the pixel grid and the steam puff); paw char maps;
  unused palette letters.

### Pikachu, second pass

The first path-format Pikachu still looked wrong (flat-topped box head,
antenna ears, dot eyes, leaf tail), so it was redrawn to official
proportions, approved as a preview before going in:

- Onion-shaped head (narrow crown, widest at the cheeks), long leaf ears
  standing almost upright with small black tips, 4x5 runtime eyes, round
  cheeks lower on the face, an "w" mouth, a pear body, and a stepped bolt
  tail standing clear of the head.
- Volume: a shade tone (under the chin, right flank and cheekbone), a crown
  highlight, and a dark-brown outline instead of near-black. Arms are solid
  shaded shapes tucked under the chin - outlined arms read as noodles.
- Poses redone to match. Walk: on all fours, ears swept back, cheek behind
  the eye, back stripes, bolt tail. Sleep: on its tummy, chin on its paws,
  ears angled up and back so they break the silhouette (flat ears' black
  tips read as dirt). Belly-up: head in profile facing the sky - an
  upside-down face read as a frown and hanging black-tipped ears as boots -
  and a straight flag tail (the zigzag's notch read as an eye).

### Squirtle, second pass

After v1.4.0 Squirtle was the odd one out (blocky box eyes, a flat line
smile, a grid-of-chocolate belly), so it got the same treatment, again
approved as a preview first:

- The first draft's head was far too big; it was cut to ~15x20 (about half
  the height), with a taller shell and belly below and a smaller tail curl.
- Eyes switched to baked sockets + `eyeStyle 5` (like the cats): navy ring,
  reddish-brown iris, runtime pupil and glint, closed with a navy curve
  (`SkinAssets.lineColor`). Open smile with a tongue, crown highlight,
  shaded right side, shell shade (`ACC2_FAR`) and shine (`CREAM_FAR`),
  navy outline, arms angled out, toes.
- Walk is now upright on two legs, the way Squirtle walks: shell on its
  back with a cream rim along the front edge, plastron in front, a
  swinging arm, stepping legs and the tail curl at the back.
- Shell/sleep and belly-up restyled to match (the opening stays where the
  runtime peek eyes go; the belly-up tail now attaches to the shell).

## 24. Orphaned audio helpers after a hard kill

While packaging v1.4.1, jpackage couldn't delete `build\dist\DeskCat`:
seven `SystemAudio` PowerShell helpers were still running, each the child
of a DeskCat or dev JVM that had been force-killed earlier that day (one
from the morning), and one still held the folder as its working
directory. The documented safety net — "a broken stdout pipe ends it" —
never worked: .NET Framework's console stream silently ignores
ERROR_BROKEN_PIPE on write, so `Console.WriteLine` kept "succeeding" and
the loop ran forever.

Fix: the JVM passes its own PID (`RuntimeMXBean` name, Java 8-safe) into
the script, and `Meter.Run(parentPid)` loops only while that process is
alive, so a hard-killed app takes its helper with it within one 200 ms
tick. If the PID can't be read the helper runs unwatched as before.
Verified with the dev build: one helper under the app, `Stop-Process
-Force` on the JVM, helper gone within 0.5 s. A normal tray Exit still
stops it through `dispose()`.

## 25. Same animal in every state; real walk cycles; Bulbasaur

Two complaints drove this. First, "do you think Pikachu actually walks this
way?" - every walk was 4 frames of rigid post legs sliding sideways, and
Pikachu shuffled on all fours. Second, the cat and doge looked chunky
sitting but slim walking: the side views were drawn as realistic animals
with heads half the size of the sitting chibi. The rule now (CLAUDE.md):
every pose must read as the same animal as the sitting drawing.

- **Gait.java**: jointed limbs rasterised along polylines (front:
  shoulder-wrist-paw; hind: hip-knee-hock-paw), 8 phases per leg, a
  lateral-sequence walk for quadrupeds and heel-to-toe steps for bipeds;
  bob, head lag (a frame), ear lag (two) and tail sway baked in. In stance
  a foot moves back ~1.7 px a frame, so CatApp plays walks at
  (85 or 140 px/s / scale) / 1.7, clamped 6-24 fps, and planted feet stay
  put; the sticker bounce is off for skins with walk frames.
- **Walks** redrawn on each skin's sitting proportions: cat/black cat and
  doge chunky with the sitting head, eye and markings; Pikachu upright
  with its full-size head; Squirtle's head enlarged to match.
- **Sleep** = the sitting head (eyes closed) resting on its paws in front
  of a body loaf. **Belly-up** = the sitting drawing turned upside down,
  happy eyes, paws paddling (toe beans on the cats); Pikachu's long ears
  splay along the ground (straight down it looked like a headstand).
  First attempts at a side-view belly-up with the big head read as a blob.
- **Bulbasaur** (personal-use fan art): frog-like teal head, socketed red
  eyes (eyeStyle 5), wide open smile, spots, white claws; the bulb is the
  tail layer so it breathes and sways behind the head. Side view on its
  real profile (big head, squat body, onion bulb with curved leaf seams) -
  the first walk was a cat's body with a bush on it. Petting gives a
  delighted pose instead of a roll. Click: Vine Whip (attackType 6) - a
  vine rises out of each side of the bulb in window space above the pet,
  ripples, cracks with a burst of leaves and sinks back; synthesized
  "bul-ba" + whip crack. (Horizontal vines were too cramped beside the
  head; vertical ones are clearly visible.)
- Poses wider than the sitting body are clamped inside the FBO.

## 26. Fullscreen step-aside, click-through, multiplayer + quiet toggles

- **Fullscreen apps**: once a second `Win32Windows.fullscreenOver()` asks
  whether the foreground window is another program with no title bar
  filling the whole monitor the pet is on (games, fullscreen video,
  slideshows; maximised apps keep WS_CAPTION, the desktop is a shell
  class). While it is, the pet window is hidden (`WindowTricks.setShown`,
  which never takes focus when showing again), friends' pets close, incoming
  shots are ignored, sounds/wandering/perching/reminders pause and the pet
  isn't drawn at all. Read-only, like the perch lookups.
- **Click-through**: after drawing, `updateClickThrough()` reads a 5×5
  block of the back buffer under the cursor; no visible pixel there turns
  on GLFW mouse-passthrough, so clicks on the window's empty area reach the
  app underneath. Solid while the pet is pressed or dragged.
- **Tray "Multiplayer"** (session-only, **off at every launch**; no LAN
  socket is opened until it's switched on): off closes the LAN
  socket (the bye message removes our pet from friends' screens), drops
  their pets locally and hides chat (Say…, middle-click) and shooting; on
  opens a fresh socket.
- **Tray "Quiet mode"** (session-only): mutes all sounds (`SoundFx.muted`,
  the volume level is untouched), stops wandering and perching (a roaming
  or perched pet heads home) and pauses the reminder timers.

## 27. Per-skin reminder animations

- The stretch and water reminders no longer pop a Windows notification
  and do a generic tall-stretch / drop shower. Each skin has its own pair
  in [ReminderArt.java](src/main/java/com/deskcat/ReminderArt.java):
  cat play-bow + yawn / laps from a bowl; black cat Halloween arch with
  bristling fur + yawn / dips a paw in a glass and licks it; doge
  play-bow + full-body shake / messy bowl slurp; Pikachu tiptoe reach
  with cheek sparks / gulps a bottle; Squirtle shell then star pop /
  drinks its own fountain; Bulbasaur vines reaching overhead with
  drifting leaves / rain cloud waters the bulb; goldfish fin flare and
  bend / chases and gulps a falling drop, then bubbles.
- Frames are whole 44x42 window-space pictures (props like the bowl,
  glass, bottle and cloud fit beside and above the pet) built from the
  shipping sitting art with the runtime eyes baked in, plus new poses in
  the PathArt format. CatApp draws the current frame in place of the FBO
  while it plays; a "Stretch time!" / "Water break!" speech bubble shows
  instead of the tray balloon.
- A due reminder waits (`remindPending`) until the pet is sitting (home or
  perched, not hidden or under a fullscreen app), then plays for ~4-5 s;
  picking the pet up or switching skin ends it. Textures are built when it
  starts and disposed when it ends.
- Previewed as GIFs from a scratch harness (same pixels) before wiring
  in. Test hook: `DESKCAT_TEST_REMIND=stretch|water` fires one 3 s after
  launch.

## 28. Carried pets dangle instead of stretching

- The old mochi drag (the whole sprite stretched 1.2x) is replaced by a
  per-skin carry pose in [CarryArt.java](src/main/java/com/deskcat/CarryArt.java):
  the cat goes limp like a scruffed kitten, the black cat sulks (ears
  back, half-lidded glare, tail lashing), the doge is held under its front
  legs paddling, Pikachu is lifted under the arms with its feet kicking,
  Squirtle swims in the air with a sweat drop (it no longer hides in its
  shell when carried), Bulbasaur hangs from the cursor by its own vines,
  the goldfish flops and gasps. 4 frames, window space, looped at 0.14 s.
- On pick-up the grab offset snaps to the art's grab point, so the pet
  hangs from the cursor wherever you clicked it. A pendulum driven by the
  cursor's horizontal acceleration swings it about that point, plus a
  short springy stretch right after pick-up so it still feels soft; the
  release wobble is unchanged.
- The swing and stretch carry the art well past the 44x42 window, which
  clipped it, so while carried the window grows (`growForCarry()`: 26
  units each side, 12 below, 8 above) with the cameras widened around an
  unchanged world origin, and shrinks back on release
  (`shrinkAfterCarry()`), the pet staying put on screen both times.
- Pikachu's and Bulbasaur's first drafts were just their sitting pose
  with legs added and didn't read as "being held"; redone as a lifted
  baby and as hanging by the vines.

## 29. Click tricks and idle fidgets

- Clicking now plays a per-skin trick from
  [TrickArt.java](src/main/java/com/deskcat/TrickArt.java) (the user picked
  one of three ideas per pet): cat bats a yarn ball; black cat pushes a
  mug off a shelf with a half-lidded stare and it smashes; doge zoomies
  then flops belly-up; Pikachu charges, is struck by its own bolt with a
  flash, ends up sooty with x-x eyes and shakes it off; Squirtle Rapid
  Spin, popping out dizzy with circling stars; Bulbasaur Razor Leaf;
  goldfish leap with a full flip and a splash. The skin's voice plays at
  the start.
- The old quick attacks (`signatureAttack()`) stay only as the muzzle
  flash when shooting a friend's pet over LAN.
- Idle fidgets play every 25-60 s while the pet sits calmly: paw lick,
  face wash, ear scratch, cheek rub, shell polish, bulb droop and perk,
  bubble.
- Same window-space frame format as the reminders; `Anim` gained optional
  per-frame window offsets (`dx`/`dy`), so travelling tricks (zoomies,
  spin, leap) move the window, clamped to the work area, and put it back
  where it started. Pressing the pet or picking it up cuts a trick short.
  Shared eye modes grew RIGHT, SMUG, DIZZY and XED.
- Click spam: a click trick (and the annoyed reaction) always plays to
  the end; clicks during it don't restart it but are counted, and 3 or
  more make the pet annoyed once the trick finishes
  (`TrickArt.annoyed()`): the cats glare with an anger mark (the black cat
  hisses) and turn their backs (a new back view), doge flops down and
  sulks, Pikachu glares with crackling cheeks then folds its arms,
  Squirtle hides in its shell glaring out, Bulbasaur crosses its vines,
  the goldfish turns away. Clicks during the annoyed reaction are
  ignored. Fidgets still give way to a click. New ANGRY eye mode.
- Each skin's fidget, trick and annoyed frames are built once in
  `applySkin()` (`loadTricks()`) instead of on every click.
- Test hook: `DESKCAT_TEST_TRICK=click|fidget|spam` plays one 3 s after
  launch (`spam` = a click plus three more during it).

## 30. One-click update

- The tray's "Check for updates" used to only find an update (relabelling
  itself "Install update vX"), needing a second click to install. Now a
  manual check that finds a newer release goes straight on to download,
  swap and relaunch (`checkForUpdates(quiet, install)` →
  `installUpdate()`). The silent check at launch still only notifies and
  relabels the item, so nothing installs without a click. An `installing`
  flag stops a double click from downloading twice.

## 31. Always on top, for real

- GLFW's floating flag only puts the window in the topmost band; within
  it the most recently raised window wins, so the taskbar or any other
  always-on-top app could sit over the pet until something reordered
  them. Now `keepOnTop()` calls `SetWindowPos(HWND_TOPMOST, NOMOVE |
  NOSIZE | NOACTIVATE)` on our own window every 0.5 s
  (`WindowTricks.raiseTopmost()`), the way other desktop pets do. It
  never takes focus.
- Skipped while tucked behind the taskbar, while hidden for a fullscreen
  app, and while one of our own windows is in front
  (`Win32Windows.foregroundIsOurs()`: chat box, menus, patch notes), so
  the pet never covers them.
- Windows' own shell overlays (Start, Task view, the notification centre)
  live in a higher band no normal app can beat.

## Current state

- **Skins**: Squirtle (default), Pikachu, the original orange tabby, black
  cat, doge, goldfish and Bulbasaur, all drawn in the path format — switchable live from
  the tray's Skin submenu, or at launch via `gradle run --args="doge"` etc.
- **Behaviors**: eye tracking, blink, tail animation, mochi drag, petting
  hearts, keyboard kneading, startle, click attacks (water gun /
  thunderbolt / paw swipe / hiss / dig / puff) each with their own voice,
  endless bottom-edge patrol (free swimming for the goldfish) with
  return-home, sleep, tray menu (skin switcher + size + taskbar
  gap + volume + quit), no taskbar button.
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
- Note: Pokémon skins are personal-use fan art — strip them before any
  public release; the cat, black cat, doge and goldfish are original and
  shippable.
