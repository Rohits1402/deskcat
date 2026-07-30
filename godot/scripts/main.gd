## Phase-1 spike: one screen-sized transparent always-on-top overlay window.
## Everything (pet, later: peers/bubbles/pellets) lives inside it as Node2Ds.
## Click-through everywhere EXCEPT over interactive sprites, via
## DisplayServer.window_set_mouse_passthrough (rebuilt every frame).
extends Node2D

const WINDOW_ID := 0

@onready var pet: Node2D = $Pet

var _tray: StatusIndicator
var _last_polygon := PackedVector2Array()


func _ready() -> void:
	_setup_overlay_window()
	NativeBridge.hide_from_taskbar(WINDOW_ID)
	_setup_tray()
	var usable := DisplayServer.screen_get_usable_rect()
	# Window origin == usable-rect origin, so local coords map 1:1 to it.
	pet.position = Vector2(usable.size.x - 220, usable.size.y - 40)


func _setup_overlay_window() -> void:
	var usable := DisplayServer.screen_get_usable_rect()
	DisplayServer.window_set_size(usable.size, WINDOW_ID)
	DisplayServer.window_set_position(usable.position, WINDOW_ID)
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
	menu.add_item("Quit", 0)
	menu.id_pressed.connect(func(id: int) -> void:
		if id == 0:
			get_tree().quit())
	add_child(_tray)
	_tray.menu = menu.get_path()


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


func _process(_delta: float) -> void:
	_update_passthrough()


## The overlay must swallow clicks ONLY over the pet; the rest of the screen
## belongs to whatever is underneath. One polygon per window — disjoint
## regions (later: peers, bubbles) get stitched with zero-width bridges.
func _update_passthrough() -> void:
	var r: Rect2 = pet.hit_rect()
	var poly := PackedVector2Array([
		r.position,
		r.position + Vector2(r.size.x, 0),
		r.end,
		r.position + Vector2(0, r.size.y),
	])
	if poly != _last_polygon:
		_last_polygon = poly
		DisplayServer.window_set_mouse_passthrough(poly, WINDOW_ID)
