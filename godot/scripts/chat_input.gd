class_name ChatInput
extends Control
## In-overlay chat input (port of ChatInput.java's Swing field): a dark
## monospace LineEdit summoned near the pet. Enter emits [signal submitted],
## Esc / clicking elsewhere / the window losing OS focus closes it.
##
## The overlay window normally runs NO_FOCUS + click-through, so open()
## temporarily makes the window focusable (and restores NO_FOCUS on close).
## The integrator must merge get_rect() into the passthrough polygon while
## [method is_open] — otherwise clicks fall through the field.
##
## Add one instance at the overlay root (position (0,0)); open()/close() it.

signal submitted(text: String)
signal closed

const WINDOW_ID := 0
## Field width in characters: starts at MIN_COLS, grows with the text.
const MIN_COLS := 30
const MAX_COLS := 70
const FONT_SIZE := 15

var _line: LineEdit
var _char_w := 9.0
var _open := false


func _ready() -> void:
	visible = false
	_line = LineEdit.new()
	var f := SystemFont.new()
	f.font_names = PackedStringArray(
			["Consolas", "Courier New", "Lucida Console"])
	_line.add_theme_font_override("font", f)
	_line.add_theme_font_size_override("font_size", FONT_SIZE)
	_line.add_theme_color_override("font_color", Color.WHITE)
	_line.add_theme_color_override("caret_color", Color.WHITE)
	# Java look: dark field, 2 px yellow border.
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color("26202a")
	sb.border_color = Color("f9d848")
	sb.set_border_width_all(2)
	sb.content_margin_left = 8.0
	sb.content_margin_right = 8.0
	sb.content_margin_top = 5.0
	sb.content_margin_bottom = 5.0
	for st in ["normal", "focus"]:
		_line.add_theme_stylebox_override(st, sb)
	add_child(_line)
	_char_w = f.get_string_size("0",
			HORIZONTAL_ALIGNMENT_LEFT, -1, FONT_SIZE).x
	_line.text_submitted.connect(_on_submit)
	_line.text_changed.connect(func(_t: String) -> void: _resize_to_text())
	_line.gui_input.connect(_on_line_input)


func is_open() -> bool:
	return _open


## Show the field with its bottom edge just above `at` (overlay-local px),
## make the overlay window focusable, and grab keyboard focus.
func open(at: Vector2) -> void:
	_open = true
	_line.text = ""
	_resize_to_text()
	position = Vector2(at.x - size.x / 2.0, at.y - size.y - 6.0)
	_clamp_to_screen()
	visible = true
	DisplayServer.window_set_flag(
			DisplayServer.WINDOW_FLAG_NO_FOCUS, false, WINDOW_ID)
	DisplayServer.window_move_to_foreground(WINDOW_ID)
	_line.call_deferred("grab_focus")


## Hide and restore the overlay's NO_FOCUS. Safe to call when not open.
func close() -> void:
	if not _open:
		return
	_open = false
	visible = false
	_line.release_focus()
	DisplayServer.window_set_flag(
			DisplayServer.WINDOW_FLAG_NO_FOCUS, true, WINDOW_ID)
	closed.emit()


func _on_submit(text: String) -> void:
	var t := text.strip_edges()
	close()
	if not t.is_empty():
		submitted.emit(t)


func _on_line_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed \
			and event.keycode == KEY_ESCAPE:
		get_viewport().set_input_as_handled()
		close()


## Click anywhere outside the field dismisses it (Java: focus-lost dispose).
func _input(event: InputEvent) -> void:
	if _open and event is InputEventMouseButton and event.pressed \
			and not get_rect().has_point(event.position):
		close()


func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_WINDOW_FOCUS_OUT and _open:
		close()


## Grow with the text (30..70 columns), clamped to the screen's right edge.
func _resize_to_text() -> void:
	var cols := clampi(_line.text.length() + 2, MIN_COLS, MAX_COLS)
	_line.custom_minimum_size = Vector2(cols * _char_w + 20.0, 0)
	_line.reset_size()
	size = _line.size
	_clamp_to_screen()


func _clamp_to_screen() -> void:
	var win := Vector2(DisplayServer.window_get_size(WINDOW_ID))
	position.x = clampf(position.x, 0.0, maxf(0.0, win.x - size.x))
	position.y = clampf(position.y, 0.0, maxf(0.0, win.y - size.y))
