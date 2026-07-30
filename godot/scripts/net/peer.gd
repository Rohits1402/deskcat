class_name Peer
## Live view of one remote pet. Written while draining the receive queue and
## read by the renderer on the same (main) thread, so no locking is needed.

var id: String
var name: String = ""
var skin: String = "cat"
var x_frac: float = 0.0
var y_frac: float = 0.0
var facing_left: bool = false
var anim: int = LanMsg.ANIM_IDLE
var last_seen_ms: int = 0
## Another instance on this same machine — no mirror window for it.
var same_host: bool = false

## Active speech bubble; empty when silent.
var bubble := Bubble.new()


func _init(p_id: String) -> void:
	id = p_id


## Timing/fade/style state of one speech bubble (port of com.deskcat.Bubble).
## Pure logic — time is injected — shared by the local pet and remote peers.
class Bubble:
	const DURATION_MS := 5000
	const FADE_MS := 500
	# ChatCommands.MIN_SCALE / MAX_SCALE in the Java client.
	const MIN_SCALE := 0.5
	const MAX_SCALE := 64.0

	var _text: String = ""
	var _until_ms: int = 0
	var _scale: float = 1.0
	var _effect: int = 0
	var _color_hex: String = ""

	func show(p_text: String, now_ms: int, p_scale: float = 1.0,
			p_effect: int = 0, p_color_hex: String = "") -> void:
		_text = p_text
		_until_ms = now_ms + DURATION_MS
		_scale = clampf(p_scale, MIN_SCALE, MAX_SCALE)
		_effect = p_effect
		_color_hex = p_color_hex

	func is_active(now_ms: int) -> bool:
		return not _text.is_empty() and now_ms < _until_ms

	## 1 while showing, fading to 0 over the final FADE_MS.
	func alpha(now_ms: int) -> float:
		if not is_active(now_ms):
			return 0.0
		return minf(1.0, float(_until_ms - now_ms) / float(FADE_MS))

	## Dismiss immediately (right-click menu).
	func clear() -> void:
		_text = ""
		_until_ms = 0

	func text() -> String:
		return _text

	func scale() -> float:
		return _scale

	func effect() -> int:
		return _effect

	func color_hex() -> String:
		return _color_hex

	## Expiry timestamp; doubles as an identity for the current message.
	func until_ms() -> int:
		return _until_ms
