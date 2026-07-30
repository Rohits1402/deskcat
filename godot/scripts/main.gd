## One screen-sized transparent always-on-top overlay window. Everything
## (pet, later: peers/bubbles/pellets) lives inside it as Node2Ds.
## Click-through everywhere EXCEPT over interactive sprites, via
## DisplayServer.window_set_mouse_passthrough (rebuilt every frame).
extends Node2D

const WINDOW_ID := 0

## Passthrough rects are grown by this margin so a fast cursor can't outrun
## the once-per-frame polygon update and leak clicks to the window below.
const HIT_MARGIN := 28.0

@onready var pet: Node2D = $Pet

## Do Not Disturb: hide remote pets, ignore incoming chat/shoot.
## (Own presence still broadcasts; enforced by the LAN layer when it lands.)
var dnd := false

var _tray: StatusIndicator
var _pet3d: Node = null
var _last_polygon := PackedVector2Array()
var _desktop_poll := 0.0
var _desktop_poll_supported := true


func _ready() -> void:
	# Perf: hard 60 fps cap; _process drops it to 30 while the pet sleeps.
	# (2D MSAA stays off — nearest-filtered pixel art gains nothing from it.)
	Engine.max_fps = 60
	# Fully transparent clear color — anything else leaves ghost outlines
	# smeared behind moving sprites on the transparent framebuffer.
	get_viewport().transparent_bg = true
	RenderingServer.set_default_clear_color(Color(0, 0, 0, 0))
	_setup_overlay_window()
	NativeBridge.hide_from_taskbar(WINDOW_ID)
	_setup_tray()
	# Pet coords are window-LOCAL: subtract the screen origin (non-zero on
	# multi-monitor setups where the primary isn't the leftmost screen).
	var origin := DisplayServer.screen_get_position(DisplayServer.SCREEN_PRIMARY)
	var usable := DisplayServer.screen_get_usable_rect(DisplayServer.SCREEN_PRIMARY)
	pet.position = Vector2(usable.end - origin) - Vector2(220, 0)


func _setup_overlay_window() -> void:
	# Cover the FULL primary screen (not just the work area) so particles/
	# pellets can fly over the taskbar instead of cutting off at the window
	# edge. The pet's floor is still the work-area bottom (see Pet.floor_y()).
	# Explicit SCREEN_PRIMARY: with multiple monitors the window otherwise
	# opens on whichever screen the OS picks, while our coords assume primary.
	# +1 px height: a borderless window EXACTLY the screen size gets promoted
	# to fullscreen-optimized by Windows, which kills per-pixel transparency.
	var s := DisplayServer.SCREEN_PRIMARY
	DisplayServer.window_set_position(
			DisplayServer.screen_get_position(s), WINDOW_ID)
	DisplayServer.window_set_size(
			DisplayServer.screen_get_size(s) + Vector2i(0, 1), WINDOW_ID)
	DisplayServer.window_set_flag(
			DisplayServer.WINDOW_FLAG_ALWAYS_ON_TOP, true, WINDOW_ID)
	DisplayServer.window_set_flag(
			DisplayServer.WINDOW_FLAG_NO_FOCUS, true, WINDOW_ID)


func _setup_tray() -> void:
	_tray = StatusIndicator.new()
	_tray.icon = _tray_icon()
	_tray.tooltip = "DeskCat (Godot)"
	var menu := PopupMenu.new()
	add_child(menu)
	menu.add_radio_check_item("Size: Small", 10)
	menu.add_radio_check_item("Size: Normal", 11)
	menu.add_radio_check_item("Size: Large", 12)
	menu.set_item_checked(menu.get_item_index(10), true)
	menu.add_separator()
	menu.add_check_item("Do Not Disturb (only my pet)", 30)
	menu.add_check_item("3D test pet", 20)
	menu.add_separator()
	menu.add_item("Quit", 0)
	menu.id_pressed.connect(_on_tray)
	add_child(_tray)
	_tray.menu = menu.get_path()


func _on_tray(id: int) -> void:
	var menu: PopupMenu = _tray.get_node(_tray.menu)
	match id:
		0:
			get_tree().quit()
		10, 11, 12:
			# Java scales: Small 3 / Normal 4 / Large 5 px per art pixel.
			pet.size_factor = [1.0, 4.0 / 3.0, 5.0 / 3.0][id - 10]
			for item_id in [10, 11, 12]:
				menu.set_item_checked(menu.get_item_index(item_id),
						item_id == id)
		20:
			var idx := menu.get_item_index(20)
			var on := not menu.is_item_checked(idx)
			menu.set_item_checked(idx, on)
			_toggle_pet3d(on)
		30:
			var idx := menu.get_item_index(30)
			dnd = not menu.is_item_checked(idx)
			menu.set_item_checked(idx, dnd)


## Proof-of-concept 3D pet: a SubViewport with transparent background
## composited into the overlay — the pipeline real 3D characters
## (e.g. Kenney CC0 GLBs) will use.
func _toggle_pet3d(on: bool) -> void:
	if not on:
		if _pet3d:
			_pet3d.queue_free()
			_pet3d = null
		return
	var script: GDScript = load("res://scripts/pet3d_view.gd")
	_pet3d = script.new()
	add_child(_pet3d)
	var origin := DisplayServer.screen_get_position(DisplayServer.SCREEN_PRIMARY)
	var usable := DisplayServer.screen_get_usable_rect(DisplayServer.SCREEN_PRIMARY)
	_pet3d.position = Vector2(usable.end - origin) - Vector2(480, 260)


func _tray_icon() -> Texture2D:
	# 16x16 teal cat-ish blob, generated in code (no assets — house style).
	var img := Image.create(16, 16, false, Image.FORMAT_RGBA8)
	var body := Color("2e8b8b")
	for y in range(4, 15):
		for x in range(2, 14):
			img.set_pixel(x, y, body)
	for p in [Vector2i(3, 1), Vector2i(4, 2), Vector2i(5, 3),
			Vector2i(12, 1), Vector2i(11, 2), Vector2i(10, 3)]:
		img.set_pixel(p.x, p.y, body)   # ears
	img.set_pixel(5, 8, Color.WHITE)
	img.set_pixel(10, 8, Color.WHITE)  # eyes
	return ImageTexture.create_from_image(img)


func _process(delta: float) -> void:
	_update_passthrough()
	# Perf: idle-down to 30 fps while asleep with nothing animating.
	var can_doze: bool = pet.state == pet.State.SLEEP \
			and pet.particles.is_empty() and _pet3d == null
	Engine.max_fps = 30 if can_doze else 60
	# Follow the user across Windows virtual desktops (needs native ext).
	if _desktop_poll_supported:
		_desktop_poll += delta
		if _desktop_poll >= 1.0:
			_desktop_poll = 0.0
			_desktop_poll_supported = \
					NativeBridge.ensure_on_current_desktop(WINDOW_ID) \
					or not NativeBridge.available()


## The overlay must swallow clicks ONLY over interactive sprites; the rest
## of the screen belongs to whatever is underneath. One polygon per window —
## while dragging, the whole overlay turns interactive so fast cursor moves
## can't escape the region and drop clicks through mid-drag.
func _update_passthrough() -> void:
	var poly: PackedVector2Array
	if pet.dragging():
		var size := Vector2(DisplayServer.window_get_size(WINDOW_ID))
		poly = PackedVector2Array([Vector2.ZERO, Vector2(size.x, 0), size,
				Vector2(0, size.y)])
	else:
		var r: Rect2 = pet.hit_rect().grow(HIT_MARGIN)
		if _pet3d:
			r = r.merge(Rect2(_pet3d.position, Vector2(220, 220)))
		poly = PackedVector2Array([
			r.position,
			r.position + Vector2(r.size.x, 0),
			r.end,
			r.position + Vector2(0, r.size.y),
		])
	if poly != _last_polygon:
		_last_polygon = poly
		DisplayServer.window_set_mouse_passthrough(poly, WINDOW_ID)
