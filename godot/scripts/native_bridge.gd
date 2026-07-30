## Thin wrapper around the deskcat_native GDExtension (Win32 helpers).
## Every call degrades to a no-op when the DLL isn't built/present, so the
## pure-GDScript app always runs. Build instructions: ../native/README.md
class_name NativeBridge

static var _warned := false


static func available() -> bool:
	var ok := ClassDB.class_exists("DeskCatWinTricks")
	if not ok and not _warned:
		_warned = true
		print("[deskcat] native extension not loaded — taskbar-hide, ",
				"key counter and audio peak are disabled")
	return ok


## Removes the overlay window from the taskbar/Alt-Tab (WS_EX_TOOLWINDOW).
static func hide_from_taskbar(window_id: int) -> void:
	if not available():
		return
	var hwnd := DisplayServer.window_get_native_handle(
			DisplayServer.WINDOW_HANDLE, window_id)
	ClassDB.instantiate("DeskCatWinTricks").hide_from_taskbar(hwnd)


## Keeps the overlay on whatever virtual desktop the user switches to.
## Call ~1x/second; returns false when unsupported (stop polling then).
static var _win_tricks: Object = null

static func ensure_on_current_desktop(window_id: int) -> bool:
	if not available():
		return false
	if _win_tricks == null:
		_win_tricks = ClassDB.instantiate("DeskCatWinTricks")
	var hwnd := DisplayServer.window_get_native_handle(
			DisplayServer.WINDOW_HANDLE, window_id)
	return _win_tricks.ensure_on_current_desktop(hwnd)


## Global keystroke COUNTER (privacy: key identities are never read — the
## native hook only increments an int). Returns keys pressed since last call.
static var _key_counter: Object = null

static func key_delta() -> int:
	if not available():
		return 0
	if _key_counter == null:
		_key_counter = ClassDB.instantiate("DeskCatKeyCounter")
		_key_counter.start()
	return _key_counter.get_and_reset()


## System output PEAK level 0..1 (never audio samples — see CLAUDE.md).
static var _audio_peak: Object = null

static func audio_peak() -> float:
	if not available():
		return 0.0
	if _audio_peak == null:
		_audio_peak = ClassDB.instantiate("DeskCatAudioPeak")
	return _audio_peak.get_peak()
