class_name SettingsWindow
extends Window

## The settings window: one real top-level OS window (NOT embedded — the
## overlay is NO_FOCUS, so settings must be its own native window that the
## OS can focus) with every runtime option, two-way bound to AppSettings.
## Open it from the tray via SettingsWindow.show_window(self).

## PetSkin has no catalog() (checked skins/skin.gd), so the skin list comes
## from AppSettings.SKINS (squirtle / pikachu / cat fallback).
const SIZE_NAMES: PackedStringArray = ["Small", "Normal", "Large"]

const BG := Color("1b171f")
const HEADING := Color("8fd8c6")

static var _open: SettingsWindow = null

var _skin_opt: OptionButton
var _size_checks: Array[CheckBox] = []
var _name_edit: LineEdit
var _fade_slider: HSlider
var _dnd_check: CheckBox
var _stretch_check: CheckBox
var _stretch_spin: SpinBox
var _water_check: CheckBox
var _water_spin: SpinBox
var _sound_check: CheckBox
var _groove_check: CheckBox
var _startup_check: CheckBox
var _network_check: CheckBox


## Opens (or refocuses) the single settings window instance.
static func show_window(parent: Node) -> void:
	if _open != null and is_instance_valid(_open):
		_open.show()
		_open.grab_focus()
		return
	_open = SettingsWindow.new()
	parent.get_tree().root.add_child(_open)
	_open.move_to_center()


func _init() -> void:
	title = "DeskCat settings"
	size = Vector2i(420, 560)
	min_size = Vector2i(360, 420)
	# The overlay embeds subwindows (project setting); force a native OS
	# window so it gets real decorations and keyboard focus.
	force_native = true
	always_on_top = true
	transparent = false


func _ready() -> void:
	# Opaque backdrop: the app's default clear color is fully transparent.
	var bg := ColorRect.new()
	bg.color = BG
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(bg)

	var scroll := ScrollContainer.new()
	scroll.set_anchors_preset(Control.PRESET_FULL_RECT)
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	add_child(scroll)

	var margin := MarginContainer.new()
	margin.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	for side in ["margin_left", "margin_right", "margin_top", "margin_bottom"]:
		margin.add_theme_constant_override(side, 14)
	scroll.add_child(margin)

	var root := VBoxContainer.new()
	root.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	root.add_theme_constant_override("separation", 6)
	margin.add_child(root)

	_build_character(_section(root, "Character"))
	_build_size(_section(root, "Size"))
	_build_name(_section(root, "Name"))
	_build_friends(_section(root, "Friends"))
	_build_reminders(_section(root, "Reminders"))
	_build_sound(_section(root, "Sound"))
	_build_system(_section(root, "System"))
	_build_about(_section(root, "About"))

	_sync()
	AppSettings.bus().changed.connect(_on_setting_changed)
	close_requested.connect(hide)
	window_input.connect(_on_window_input)


func _on_window_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed \
			and event.keycode == KEY_ESCAPE:
		hide()


# --- sections ----------------------------------------------------------------

func _section(parent: VBoxContainer, heading: String) -> VBoxContainer:
	if parent.get_child_count() > 0:
		var sep := HSeparator.new()
		parent.add_child(sep)
	var head := Label.new()
	head.text = heading
	head.add_theme_color_override("font_color", HEADING)
	parent.add_child(head)
	var indent := MarginContainer.new()
	indent.add_theme_constant_override("margin_left", 10)
	parent.add_child(indent)
	var box := VBoxContainer.new()
	box.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	indent.add_child(box)
	return box


var _skin_ids: Array = []


func _build_character(box: VBoxContainer) -> void:
	_skin_opt = OptionButton.new()
	_skin_ids.clear()
	for entry in PetSkin.catalog():
		_skin_opt.add_item(entry.label)
		_skin_ids.append(entry.id)
	_skin_opt.item_selected.connect(func(i: int) -> void:
		AppSettings.skin_id = _skin_ids[i])
	box.add_child(_skin_opt)


func _build_size(box: VBoxContainer) -> void:
	var group := ButtonGroup.new()
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 12)
	box.add_child(row)
	for i in SIZE_NAMES.size():
		var c := CheckBox.new()
		c.text = SIZE_NAMES[i]
		c.button_group = group
		var idx := i
		c.toggled.connect(func(on: bool) -> void:
			if on:
				AppSettings.size_index = idx)
		row.add_child(c)
		_size_checks.append(c)


func _build_name(box: VBoxContainer) -> void:
	_name_edit = LineEdit.new()
	_name_edit.max_length = 24
	_name_edit.placeholder_text = "Shown to LAN friends"
	_name_edit.text_changed.connect(func(t: String) -> void:
		AppSettings.user_name = t)
	box.add_child(_name_edit)


func _build_friends(box: VBoxContainer) -> void:
	var lbl := Label.new()
	lbl.text = "Peer fade (friends' pet opacity)"
	box.add_child(lbl)
	_fade_slider = HSlider.new()
	_fade_slider.min_value = 0.0
	_fade_slider.max_value = 1.0
	_fade_slider.step = 0.05
	_fade_slider.custom_minimum_size = Vector2(180, 0)
	_fade_slider.value_changed.connect(func(v: float) -> void:
		AppSettings.peer_alpha = v)
	box.add_child(_fade_slider)
	_dnd_check = _check("Do Not Disturb (only my pet)",
			func(on: bool) -> void: AppSettings.dnd = on)
	box.add_child(_dnd_check)


func _build_reminders(box: VBoxContainer) -> void:
	var stretch := HBoxContainer.new()
	box.add_child(stretch)
	_stretch_check = _check("Stretch every",
			func(on: bool) -> void: AppSettings.reminders_stretch = on)
	stretch.add_child(_stretch_check)
	_stretch_spin = _minutes_spin(func(v: float) -> void:
		AppSettings.stretch_interval_min = int(v))
	stretch.add_child(_stretch_spin)
	stretch.add_child(_minutes_label())

	var water := HBoxContainer.new()
	box.add_child(water)
	_water_check = _check("Drink water every",
			func(on: bool) -> void: AppSettings.reminders_water = on)
	water.add_child(_water_check)
	_water_spin = _minutes_spin(func(v: float) -> void:
		AppSettings.water_interval_min = int(v))
	water.add_child(_water_spin)
	water.add_child(_minutes_label())


func _build_sound(box: VBoxContainer) -> void:
	_sound_check = _check("Sound effects",
			func(on: bool) -> void: AppSettings.sound_on = on)
	box.add_child(_sound_check)
	_groove_check = _check("Groove to music",
			func(on: bool) -> void: AppSettings.music_groove = on)
	box.add_child(_groove_check)


func _build_system(box: VBoxContainer) -> void:
	_startup_check = _check("Start with Windows",
			func(on: bool) -> void:
				AppSettings.start_with_windows = on
				# The setter refuses silently if reg.exe failed; show truth.
				_startup_check.set_pressed_no_signal(
						AppSettings.start_with_windows))
	box.add_child(_startup_check)
	_network_check = _check("LAN presence and chat",
			func(on: bool) -> void: AppSettings.network_on = on)
	box.add_child(_network_check)


func _build_about(box: VBoxContainer) -> void:
	var ver := Label.new()
	ver.text = "DeskCat %s — Godot port" % AppSettings.VERSION
	box.add_child(ver)
	var notes := Button.new()
	notes.text = "Patch notes"
	notes.size_flags_horizontal = Control.SIZE_SHRINK_BEGIN
	notes.pressed.connect(PatchNotes.show_window)
	box.add_child(notes)


# --- helpers -----------------------------------------------------------------

func _check(label: String, on_toggle: Callable) -> CheckBox:
	var c := CheckBox.new()
	c.text = label
	c.toggled.connect(on_toggle)
	return c


func _minutes_spin(on_change: Callable) -> SpinBox:
	var s := SpinBox.new()
	s.min_value = 5
	s.max_value = 240
	s.step = 5
	s.value_changed.connect(on_change)
	return s


func _minutes_label() -> Label:
	var l := Label.new()
	l.text = "min"
	return l


# --- two-way sync ------------------------------------------------------------

func _on_setting_changed(_key: String) -> void:
	_sync()


## Pushes AppSettings state into every control without re-firing their
## change signals (the AppSettings setters no-op on equal values anyway,
## so even a stray echo can't loop).
func _sync() -> void:
	_skin_opt.select(maxi(_skin_ids.find(AppSettings.skin_id), 0))
	for i in _size_checks.size():
		_size_checks[i].set_pressed_no_signal(i == AppSettings.size_index)
	if _name_edit.text != AppSettings.user_name:
		_name_edit.text = AppSettings.user_name
	_fade_slider.set_value_no_signal(AppSettings.peer_alpha)
	_dnd_check.set_pressed_no_signal(AppSettings.dnd)
	_stretch_check.set_pressed_no_signal(AppSettings.reminders_stretch)
	_stretch_spin.set_value_no_signal(AppSettings.stretch_interval_min)
	_water_check.set_pressed_no_signal(AppSettings.reminders_water)
	_water_spin.set_value_no_signal(AppSettings.water_interval_min)
	_sound_check.set_pressed_no_signal(AppSettings.sound_on)
	_groove_check.set_pressed_no_signal(AppSettings.music_groove)
	_startup_check.set_pressed_no_signal(AppSettings.start_with_windows)
	_network_check.set_pressed_no_signal(AppSettings.network_on)
