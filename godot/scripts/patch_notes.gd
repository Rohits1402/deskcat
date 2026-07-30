class_name PatchNotes
extends Window

## "Patch notes" window, ported from the Java app's PatchNotes.java.
## Real top-level OS window (the overlay is NO_FOCUS, so this must not be
## embedded). Update NOTES when cutting a release (newest entry on top).

const NOTES := "DeskCat " + AppSettings.VERSION + " — patch notes\n" + """
Godot port (unreleased)
-----------------------
* Full rewrite in Godot 4: one screen-sized transparent
  overlay window, click-through except over the sprites
* Patrol walks the whole screen perimeter — edges and
  ceiling, not just the taskbar
* Experimental 3D pets (transparent SubViewport pipeline)
* Do Not Disturb: hide remote pets, ignore incoming
  chat/shoot
* Overlay follows you across Windows virtual desktops

v1.2.0
------
* Chat commands: /big /huge /small /size N, /shake, /rainbow,
  /color <name> — chainable, everyone sees the same bubble
* Huge /size text opens a crisp screen-sized overlay bubble
* /shoot (chat or right-click menus): a pellet flies between
  pets; the target squishes flat or falls over, then recovers
* Speech bubbles word-wrap; the chat box grows while you type
* Patrol wraps around the screen edges instead of turning
* Friends' pets fade in; tray Peer fade sets their opacity
* One tray icon per machine; Patch notes window in the tray

v1.1.0
------
* LAN presence: see teammates' pets with name labels
* Chat with speech bubbles, broadcasts and @name DMs
* Right-click menus: say, dismiss, hide behind taskbar
* Size control (Small/Normal/Large) and taskbar gap
* Stretch/water reminders, procedural sound, music groove
* Start with Windows, auto-updater, single runnable jar

v1.0.0
------
* Squirtle, Pikachu and cat skins with click attacks
* Eye tracking, mochi drag, petting, kneading, patrol, sleep
"""

static var _open: PatchNotes = null


## Opens (or refocuses) the single patch-notes window. Safe to call from
## anywhere — parents itself to the scene root.
static func show_window() -> void:
	if _open != null and is_instance_valid(_open):
		_open.show()
		_open.grab_focus()
		return
	_open = PatchNotes.new()
	var tree := Engine.get_main_loop() as SceneTree
	tree.root.add_child(_open)
	_open.move_to_center()


func _init() -> void:
	title = "DeskCat — patch notes"
	size = Vector2i(460, 420)
	min_size = Vector2i(320, 240)
	# The overlay embeds subwindows (project setting); force a native OS
	# window so it gets real decorations and focus.
	force_native = true
	always_on_top = true
	transparent = false


func _ready() -> void:
	# The overlay sets a fully transparent default clear color; give this
	# window an opaque dark backdrop.
	var bg := ColorRect.new()
	bg.color = Color("26202a")
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(bg)

	var text := TextEdit.new()
	text.text = NOTES
	text.editable = false
	text.set_anchors_preset(Control.PRESET_FULL_RECT)

	var mono := SystemFont.new()
	mono.font_names = PackedStringArray(
			["Consolas", "Courier New", "monospace"])
	text.add_theme_font_override("font", mono)
	text.add_theme_font_size_override("font_size", 13)
	text.add_theme_color_override("font_color", Color.WHITE)
	text.add_theme_color_override("font_readonly_color", Color.WHITE)
	var style := StyleBoxFlat.new()
	style.bg_color = Color("26202a")
	style.set_content_margin_all(12.0)
	text.add_theme_stylebox_override("normal", style)
	text.add_theme_stylebox_override("read_only", style)
	text.add_theme_stylebox_override("focus", style)
	add_child(text)

	close_requested.connect(queue_free)
	window_input.connect(_on_window_input)


func _on_window_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed \
			and event.keycode == KEY_ESCAPE:
		queue_free()
