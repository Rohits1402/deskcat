# Java render spec (extracted from CatApp.java / SkinAssets.java / PixelArt.java)

Source of truth: `src/main/java/com/deskcat/CatApp.java` (line refs below),
`SkinAssets.java`, `PixelArt.java`, `Spring.java`. All coordinates are **world
units** (1 unit = 1 art pixel), origin **bottom-left, y-up** (libGDX ortho cam,
`setToOrtho(false, …)`). Window is 44×42 units (`UNITS_W/UNITS_H`, CatApp.java:72-73),
rendered at `scale` px/unit (3/5/7).

Reality check vs. common guesses: there are **no** happy/^^ eyes, **no** blush
quads, **no** brows, **no** X-shaped KO eyes anywhere in the Java code. Petting
only causes a body shake + heart particles; KO is a whole-body rotate/squish of
the FBO quad — eyes keep their normal open/closed logic throughout;
mouth/nose/cheeks are **baked into the body textures**, never drawn dynamically
(single exception: Pikachu's cheek spark overlay during a bolt, below).

## 1. Canvas layout

- FBO: 40×30 units (`FBO_W`,`FBO_H`, CatApp.java:85-86), RGBA8888, **nearest**
  filtering, cleared to transparent each frame (1356-1359).
- The FBO quad is drawn into the window at `CAT_X = 2`, y = `bob`, i.e. the cat
  canvas occupies window units x∈[2,42) (1509-1510).
- `BODY_X = 2`: body texture position inside the FBO (87-88).
- FBO region is y-flipped once at creation (260) — standard libGDX FBO fixup.

## 2. Per-skin constants (SkinAssets.java:49-109)

| field | cat | pikachu | squirtle |
|---|---|---|---|
| eyeStyle | 0 (iris on baked white patch) | 1 (solid bead) | 2 (outlined iris block) |
| attackType | 0 startle | 1 thunderbolt | 2 water gun |
| bodyTex | `BODY` map 28×26 | `PIKA_BODY` map 28×27 | procedural 30×27 |
| tailTex | `TAIL_A/B/C` 10×12 | `PIKA_TAIL_A/B/C` 12×14 | procedural 12×12 ×3 |
| tailX | 26 | 27 | 26 |
| eyeLX / eyeRX | 7 / 18 | 9 / 20 | 11 / 20 |
| eyeW × eyeH | 4×3 | 3×3 | 3×4 |
| eyeY | 15 | 15 | 16 |
| eyeCenterX, eyeCenterY | 14, 16 | 15.5, 16 | 16.5, 17.5 |
| furColor | `F29A4B` | `F9D848` | `8CCFE8` |
| irisColor | `7CC46B` (green) | `7CC46B` (green — unused by style 1) | `5B3A26` (brown) |

Eye coords (`eyeLX/eyeRX/eyeY`) are **FBO coordinates, y-up** (body sits at
y=0 inside the FBO). Shared colors: outline `C_OUTLINE = 26202A`
(CatApp.java:111), glint = pure white.

Baked-in face detail (never redrawn at runtime):
- Cat: white eye patches (4×3 at the eye positions), pink nose + white muzzle +
  mouth, chest/paw markings — all in `PixelArt.BODY` (PixelArt.java:79-106).
- Pikachu: red `E23B2E` cheeks, tiny nose (KK at map row 11) — `PIKA_BODY`
  (155-183).
- Squirtle: smile = outline-color pixels x=12..17 at map row 13 plus corner
  pixels (11,12) and (18,12) — `squirtleBody()` (322-326).

## 3. FBO draw order (`renderCatToFbo()`, CatApp.java:1356-1402)

1. Clear transparent.
2. `shake = petActive ? sin(time*45)*0.25 : 0` — horizontal jitter applied to
   every element below (1363).
3. **Tail**: `tailTex[TAIL_CYCLE[tailFrame]]` at `(tailX + shake, 0)`, natural
   texture size (1365-1366). Drawn first → behind body.
4. **Body**: `bodyTex` at `(BODY_X + shake, 0)` = `(2 + shake, 0)` (1367-1368).
5. **Pikachu cheek spark** (only `attackType==1 && boltLeft>0 && sin(time*40)>0`):
   tint `(1, 1, 0.75, 1)`, two 3×3 quads at `(BODY_X+3+shake, 11)` and
   `(BODY_X+22+shake, 11)` (1371-1376). This is the only dynamic mouth/cheek
   element in the app.
6. **Eyes** (left then right — identical drawing, different x):
   - `closed = sleeping || blinkLeft > 0 || boltLeft > 0.15 || waterLeft > 0.2
     || (stretchLeft > 0.8 && stretchLeft < 3.4)` (1378-1380).
   - closed → `drawClosedEye(eyeLX+shake)` / `(eyeRX+shake)`.
   - open → compute `pgx`, `irisY` (§5) then `drawOpenEye(eyeX+shake, pgx, irisY)`.
7. **Kneading paws** (only `kneadNow`): `leftUp = sin(time*14) > 0`;
   `pawTex` 4×4 at `(9+shake, leftUp?1:0)` and `(18+shake, leftUp?0:1)`
   (1394-1398). Paw maps: `PAW_CAT`/`PAW_PIKA`/`PAW_SQUIRT`
   (PixelArt.java:352-377), each 4×4.

Nothing else goes into the FBO. Hearts, zzz, alert, notes, water drops, bolts
are window-space particles (§8).

## 4. Eye rendering, exact (all rects drawn with a tinted 1×1 white pixel `px`)

### 4.1 Open eyes (`drawOpenEye`, CatApp.java:1404-1443)

Inputs: `eyeX` (= eyeLX/eyeRX + shake), `pgx` ∈ {-1,0,1}, `irisY` ∈ {eyeY, eyeY+1}.
`gazeDown = kneadNow ? -1 : gazeY` (1405) — kneading forces looking down.

**Style 0 — cat** (1406-1417). White patch is baked into the body texture; only
iris + pupil are drawn:
- `irisX = eyeX + 1 + pgx`
- iris: irisColor 2×2 at `(irisX, irisY)`
- startled (`alertLeft > 0`): outline-color 2×2 drawn over the whole iris
  ("wide startled pupils" — a full black 2×2, no white pupil)
- normal pupil: outline color 1×1 at
  `(irisX + (pgx>=0 ? 1 : 0), irisY + (gazeDown>=0 ? 1 : 0))`

**Style 1 — pikachu** (1418-1428). Solid bead + wandering glint:
- bead: C_OUTLINE, `eyeW×eyeH` (3×3) at `(eyeX, eyeY)`
- glint (skipped while `alertLeft > 0`): white 1×1 at
  `x = eyeX + 1 + pgx`, `y = gazeDown>=0 ? eyeY+eyeH-1 : eyeY+eyeH-2`
  (glint x is not clamped, but for a 3-wide bead `eyeX+1±1` always stays
  inside: left column, center, or right column)

**Style 2 — squirtle** (1429-1440). Outlined iris block + tall glint:
- outline: C_OUTLINE `(eyeW+2)×(eyeH+2)` = 5×6 at `(eyeX-1, eyeY-1)`
- iris fill: `alertLeft>0 ? C_OUTLINE : irisColor`, `eyeW×eyeH` = 3×4 at
  `(eyeX, eyeY)` (startle = fully black eye)
- glint (skipped while `alertLeft > 0`): white 1×2 at
  `x = eyeX + clamp(1 + pgx, 0, eyeW-1)` (clamped, unlike style 1),
  `y = gazeDown>=0 ? eyeY+eyeH-2 : eyeY+eyeH-3`

Batch color reset to white after (1442).

### 4.2 Closed eyes (`drawClosedEye`, CatApp.java:1445-1458)

Used for sleep, blink, attack wind-up, mid-stretch — same shape for all of them
(there is no separate "sleeping" vs "blink" art).

- Style 2 (squirtle): furColor `(eyeW+2)×(eyeH+2)` = 5×6 at `(eyeX-1, eyeY-1)`
  (paints out the outline block), then C_OUTLINE horizontal line `(eyeW+2)×1`
  = 5×1 at `(eyeX-1, eyeY + eyeH/2)` = `(eyeX-1, eyeY+2)`.
- Styles 0 & 1 (cat, pikachu): furColor `eyeW×eyeH` at `(eyeX, eyeY)` (covers
  the baked white patch / erases the bead), then C_OUTLINE line `eyeW×1` at
  `(eyeX, eyeY+1)`.

So a closed eye is always: fur-colored patch + one 1-px-tall dark horizontal
lid line. No arc, no ^ shape.

## 5. Gaze math (CatApp.java:1022-1026, 1385-1391)

Per frame, from the global cursor (screen px, y-down):

```
eyeScrX = winX + (CAT_X + eyeCenterX) * scale
eyeScrY = winY + (UNITS_H - eyeCenterY) * scale
gazeX = clamp((cursor.x - eyeScrX) / 240, -1, 1)
gazeY = clamp((eyeScrY - cursor.y) / 240, -1, 1)     // +1 = cursor above
```

Quantization at draw time (1385-1389):

```
pgx   = gazeX >  0.3 ? +1 : (gazeX < -0.3 ? -1 : 0)     // horizontal iris/glint step
if (walking (wanderState 2|3) && facingLeft) pgx = -pgx  // FBO is mirrored
irisY = (kneadNow || gazeY < -0.25) ? eyeY : eyeY + 1    // cursor well below → iris drops 1px
```

Vertical detail inside the eye uses the *unquantized* sign:
`gazeDown = kneadNow ? -1 : gazeY`; `gazeDown >= 0` picks the upper pupil/glint
row, `< 0` the lower one (1405, 1415, 1426, 1438). Max iris travel is therefore
±1 px horizontally and 1 px vertically — nothing subpixel.

## 6. Blink, tail wag, squash/stretch

**Blink** (1318-1328): closed for `blinkLeft = 0.12 s`, then next blink in
`blinkIn = random(2.5, 6) s`. Initial `blinkIn = 3` (142).

**Tail** (147, 1330-1340): 3 textures cycled as `TAIL_CYCLE = {0,1,2,1}`
(A,B,C,B — ping-pong). Frame interval:

```
sleeping        0.8 s
alertLeft > 0   0.12 s
walking (2|3)   0.15 s
danceNow        0.18 s
else            0.3 s
```

Frames are whole alternate textures (no per-frame offset): cat/pika frames are
the char maps `TAIL_A/B/C`, `PIKA_TAIL_A/B/C` (PixelArt.java:108-151, 186-235);
squirtle frames are the procedural curl with curl-center offsets
`(0,0), (+0.9,-0.7), (-0.6,+0.8)` in map (y-down) coords
(PixelArt.java:344-350). All drawn at the same `(tailX, 0)`.

**Spring / squash** (Spring.java, CatApp.java:131, 1310-1316):

```
Spring(stiffness=160, damping=12, min=0.7, max=1.5), value starts at 1
update: vel += (target - value)*160*dt;  vel -= vel*12*dt;
        value += vel*dt;  value = clamp(value, 0.7, 1.5)
kick(i): vel += i
target = dragging ? 1.2 : falling(state1) ? 1.12 : hopping(state4) ? 1.08
       : stretchLeft>0 ? 1.5 : 1.0
scaleY = spring value
```

Kicks: release drag `+3` (349), un-hide `+2` (433), shot hit `-6` (566),
attack hop `+5` (928), water reminder `+4` (1077), wander landing `-5` (1233),
settle home `+2` (1269).

**Applied to the FBO quad** (`renderWindow`, 1466-1510):

```
scaleX = 1 - (scaleY - 1) * 0.55          // volume-ish preservation
walking: rate = state3 ? 12 : 9
         bob    = |sin(time*rate)| * 0.8
         waddle = sin(time*rate) * 3        (degrees)
         facingLeft → scaleX = -scaleX      (horizontal mirror)
dancing: amp = 0.3 + 0.7*musicAmp
         bob = |sin(time*7)| * 1.2 * amp;  waddle = sin(time*7) * 3.5 * amp
draw(fboRegion, x=CAT_X, y=bob, originX=FBO_W/2, originY=0,
     FBO_W, FBO_H, scaleX, drawScaleY, rotation=waddle)
```

Origin = bottom-center → the cat squashes/rotates about its feet.

**KO overlay on the same quad** (1483-1508), `KO_TOTAL = 1.6 s`, `e = elapsed`:
- mode 0 topple: rotation added to waddle, `rot*koDir` where
  `rot = 90*(e/0.25)` for e<0.25; `90` for e<1.1; `90*(1-(e-1.1)/0.5)` after.
- mode 1 pancake: `squish = 1 - 0.75*(e/0.15)` for e<0.15; `0.25` for e<1.0;
  `0.25 + 0.75*min(1,(e-1.0)/0.6)` after. `drawScaleY *= squish`;
  `scaleX *= 1 + (1-squish)*0.6`.
- Eyes during KO are unchanged by KO itself (no X eyes); the eye state follows
  the normal open/closed rules.

## 7. Eye-state truth table (local pet)

| state | trigger | rendering |
|---|---|---|
| normal | default | open eye per style, pupil/glint from gaze (§4.1, §5) |
| blink | every 2.5-6 s for 0.12 s | closed shape (§4.2) |
| sleeping | idle > 60 s | closed shape — identical to blink |
| attack wind-up | `boltLeft > 0.15` or `waterLeft > 0.2` | closed shape |
| stretch reminder | `0.8 < stretchLeft < 3.4` (4 s total) | closed shape |
| startled | `alertLeft > 0` | open, but: style 0 iris fully outline-color, styles 1/2 glint removed, style 2 iris painted outline-color |
| kneading | typing detected | open, iris raised (`irisY = eyeY`), pupil/glint forced to "looking down" row (`gazeDown = -1`) |
| KO | shot by peer | no eye change; whole quad topples/squishes |
| petting | cursor strokes head | **no eye change**; body shake 0.25 amplitude at sin(time*45) + heart particles |

## 8. Drawn outside the FBO (window space, `renderWindow` 1460-1562)

Order after the FBO quad:
1. **Thunderbolts** (`boltLeft > 0`, flicker gate `sin(time*55) > -0.4`):
   per bolt polyline (16 points, generated 966-979 from window-top down to
   y≈24), each point drawn twice — 2×2 tinted `(1, 0.92, 0.35, a)` then 1×1
   `(1, 1, 0.9, a)`, `a = clamp(boltLeft/0.3, 0, 1)` (1512-1525).
2. **Particles** (hearts, zzz, alert-less sparks, notes, water drops): texture
   at natural size × per-particle scale, alpha fades over last 0.5 s of life
   (1527-1533). Spawn positions/velocities: hearts 1176-1186, zzz 1195-1204,
   notes 1105-1116, water-gun drops 1138-1151, reminder drops 1121-1133,
   spark burst 948-963.
3. **Alert `!!` marker** (`alertLeft > 0`): `ALERT` texture (3×8 map) at
   `(CAT_X+13, 28 + |sin(time*14)|*1.5)` (1536-1540).
4. **Chat bubble**: pixel-space cam, only when active (1544-1561).

**No shadow** is drawn anywhere. **No name label** on the local pet — the name
label exists only on remote peers' windows (`RemotePetWindow.java:156-169`:
BitmapFont text centered at top, `ly = winH-4`, over a black 0.45-alpha rounded
rect of `layout+8` padding).

Remote peers use a simplified renderer (`RemotePetWindow.drawEye`, 198-225):
same three styles but with fixed centered pupil/glint (no gaze), body offset
`4 - cx`/`tailX + 2`, eyes at `eyeLX + 2`, stretch 1.15 while dragged, −90°
rotation while KO, `TAIL_CYCLE` identical. Do not copy its constants for the
local pet.

## 9. Misc constants a port needs

- Frame dt clamped to `min(dt, 1/20)` (983).
- `time` accumulates clamped dt; all `sin(time*k)` phases use it.
- Nearest filtering everywhere; all art must stay pixel-crisp.
- Palette (PixelArt.java:18-39): K `26202A`, O `F29A4B`, D `C9702E`,
  W `FFF4E3`, P `F2A3B3`, N `C75B77`, R `E5626E`, B `BFE3F2`, G `7CC46B`,
  Y `F9D848`, M `8C5A2B`, Q `E23B2E`, A `8CCFE8`, H `A9784C`, C `F2E3B8`.
- Squirtle body ellipses (PixelArt.java:310-327), grid 30×27, map coords y-down:
  shell H(15, 19.5, r 9×6.5); arms A(5.5, 18.5, 2.5×3) and A(24.5, 18.5, 2.5×3);
  plastron C(15, 21, 5.5×5.3); feet A(9.5, 24.5, 3×2), A(20.5, 24.5, 3×2);
  head A(15, 9.5, 8.5×7); then outline pass (edge pixels → K), then smile.
- Squirtle tail (330-341): root A(2.5, 9, 3×2.6) + curl A(5.5+ox, 5.5+oy,
  3.2×3), outline pass, 2-px K "spiral hint" at rounded curl center.
