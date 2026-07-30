class_name AppSettings
extends RefCounted

## Central holder for all runtime options, replacing the growing pile of
## tray checkboxes. Everything is static and SESSION-ONLY by project rule
## (no config file, ever). The one exception is start_with_windows, which
## by nature persists in the Windows registry HKCU Run key — the ONLY
## sanctioned registry access in the app (via reg.exe, like the Java port).
##
## Subscribe to changes:
##	AppSettings.bus().changed.connect(func(key): ...)
## GDScript has no static signals, so bus() returns one shared instance
## that carries the changed(key) signal for the whole app.

const VERSION := "1.2.0"

const SKINS: PackedStringArray = ["squirtle", "pikachu", "cat"]

## Java scales: Small 3 / Normal 4 / Large 5 px per art pixel.
## size_factor() maps size_index onto Pet.size_factor.
## Java tray scales are 3 / 5 / 7 px per art pixel (PX_BASE 3 = factor 1).
const SIZE_FACTORS: Array[float] = [1.0, 5.0 / 3.0, 7.0 / 3.0]

const RUN_KEY := "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
const RUN_VALUE := "DeskCat"

signal changed(key: String)

static var _bus: AppSettings = null


static func bus() -> AppSettings:
	if _bus == null:
		_bus = AppSettings.new()
	return _bus


static func _emit(key: String) -> void:
	bus().changed.emit(key)


# --- options -----------------------------------------------------------------

static var skin_id: String = "squirtle":
	set(v):
		v = v.to_lower()
		if skin_id == v:
			return
		skin_id = v
		_emit("skin_id")

## 0 Small / 1 Normal / 2 Large.
static var size_index: int = 0:
	set(v):
		v = clampi(v, 0, 2)
		if size_index == v:
			return
		size_index = v
		_emit("size_index")

static var user_name: String = _default_user_name():
	set(v):
		if user_name == v:
			return
		user_name = v
		_emit("user_name")

## Opacity of remote (LAN peer) pets, 0..1.
static var peer_alpha: float = 1.0:
	set(v):
		v = clampf(v, 0.0, 1.0)
		if is_equal_approx(peer_alpha, v):
			return
		peer_alpha = v
		_emit("peer_alpha")

## Do Not Disturb: hide remote pets, ignore incoming chat/shoot.
static var dnd: bool = false:
	set(v):
		if dnd == v:
			return
		dnd = v
		_emit("dnd")

static var reminders_stretch: bool = false:
	set(v):
		if reminders_stretch == v:
			return
		reminders_stretch = v
		_emit("reminders_stretch")

static var stretch_interval_min: int = 45:
	set(v):
		v = maxi(v, 1)
		if stretch_interval_min == v:
			return
		stretch_interval_min = v
		_emit("stretch_interval_min")

static var reminders_water: bool = false:
	set(v):
		if reminders_water == v:
			return
		reminders_water = v
		_emit("reminders_water")

static var water_interval_min: int = 30:
	set(v):
		v = maxi(v, 1)
		if water_interval_min == v:
			return
		water_interval_min = v
		_emit("water_interval_min")

static var sound_on: bool = true:
	set(v):
		if sound_on == v:
			return
		sound_on = v
		_emit("sound_on")

static var music_groove: bool = true:
	set(v):
		if music_groove == v:
			return
		music_groove = v
		_emit("music_groove")

## LAN presence/chat on or off.
static var network_on: bool = true:
	set(v):
		if network_on == v:
			return
		network_on = v
		_emit("network_on")


# --- start with Windows (registry-backed, the persistence exception) ---------

static var _startup_cached := false
static var _startup_on := false

## Reads the real registry state on first access (lazy — reg.exe query is a
## subprocess). Setting writes/deletes the Run value; on failure the stored
## state stays at what the registry actually says.
static var start_with_windows: bool:
	get:
		return _query_startup()
	set(v):
		if _query_startup() == v:
			return
		if _apply_startup(v):
			_startup_on = v
		# Emit even on failure so any UI re-reads the true state.
		_emit("start_with_windows")


static func _query_startup() -> bool:
	if not _startup_cached:
		_startup_cached = true
		_startup_on = OS.execute(
				"reg", ["query", RUN_KEY, "/v", RUN_VALUE]) == 0
	return _startup_on


static func _apply_startup(on: bool) -> bool:
	if on:
		return OS.execute("reg", ["add", RUN_KEY, "/v", RUN_VALUE,
				"/t", "REG_SZ", "/d", get_launch_command(), "/f"]) == 0
	# Delete is best-effort: exit code is nonzero if the value was absent,
	# which still means "disabled".
	OS.execute("reg", ["delete", RUN_KEY, "/v", RUN_VALUE, "/f"])
	return true


## Command line stored in the Run key. From the editor / a dev run the Godot
## binary needs "--path <project>" to find this project; an EXPORTED build is
## just the exe path (the pack is embedded), so no --path is appended there.
static func get_launch_command() -> String:
	var exe := OS.get_executable_path()
	if OS.has_feature("editor"):
		var proj := ProjectSettings.globalize_path("res://").rstrip("/")
		return "\"%s\" --path \"%s\"" % [exe, proj]
	return "\"%s\"" % exe


# --- helpers -----------------------------------------------------------------

static func size_factor() -> float:
	return SIZE_FACTORS[size_index]


static func _default_user_name() -> String:
	var n := OS.get_environment("USERNAME")
	return n if not n.is_empty() else "DeskCat"
