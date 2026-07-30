# DeskCat — Godot port (work in progress)

Godot 4.7 port of the Java app, on the `godot-port` branch. Java stays the
shipping version until this reaches feature parity.

## Architecture (differs from Java on purpose)

**One screen-sized transparent overlay window** instead of one OS window per
entity. Own pet, remote pets, bubbles and pellets are all Node2Ds inside it.
Click-through is per-region: every frame the pet/UI rects are stitched into a
mouse-passthrough polygon (`DisplayServer.window_set_mouse_passthrough`), so
clicks land on the desktop everywhere except on interactive sprites. This
replaces the entire window-zoo (taskbar-flash dance, WS_EX_LAYERED pitfall,
overlap click-through toggling) from the Java version.

Win32 specials (taskbar hide, key counter, audio peak) live in a small
GDExtension — see [native/README.md](native/README.md). Without the built DLL
everything still runs; those three features just no-op.

## Run

Godot 4.7+ standard build (not .NET):

```
godot --path godot            # or open the folder in the editor
```

## Status / roadmap

- [x] Phase 1 spike: overlay + passthrough polygon + tray StatusIndicator +
      placeholder pet (drag, squash spring, eye tracking)
- [ ] Phase 2: port skins (PixelArt → generated Images or PNGs), full state
      machine (petting, startle, attack, knead, patrol/wraparound, sleep)
- [ ] Phase 3: LAN — DC1 protocol port (wire-compatible with Java clients),
      multicast PacketPeerUDP, peers as sprites
- [ ] Phase 4: chat input, bubbles + /commands, /shoot + KO
- [ ] Phase 5: reminders, sounds (AudioStreamGenerator), music groove,
      updater, startup toggle, patch notes, settings window
- [ ] Phase 6: Windows export preset + release pipeline

Wire format stays byte-identical to the Java client so both apps coexist on
one LAN during the transition.
