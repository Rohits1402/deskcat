## Port of LanProtocolTest.java. No framework: run() returns [passed, failed]
## and reports failures via push_error.

static var _passed := 0
static var _failed := 0


static func run() -> Array[int]:
	_passed = 0
	_failed = 0
	_state_round_trip()
	_chat_round_trip_with_escaping()
	_broadcast_chat_has_empty_target()
	_chat_style_round_trip()
	_legacy_chat_without_style_gets_defaults()
	_action_round_trip()
	_bye_round_trip()
	_fractions_are_clamped()
	_rejects_garbage()
	_exact_wire_bytes()
	return [_passed, _failed]


static func _check(cond: bool, what: String) -> void:
	if cond:
		_passed += 1
	else:
		_failed += 1
		push_error("FAIL lan_protocol: " + what)


static func _approx(a: float, b: float) -> bool:
	return absf(a - b) < 1e-6


static func _state_round_trip() -> void:
	var raw := LanProtocol.encode_state("id-1", "Rajat", "squirtle",
			0.25, 0.75, true, LanMsg.ANIM_WALK)
	var m := LanProtocol.decode(raw)
	_check(m != null, "state decodes")
	if m == null:
		return
	_check(m.type == LanMsg.STATE, "state type")
	_check(m.id == "id-1", "state id")
	_check(m.name == "Rajat", "state name")
	_check(m.skin == "squirtle", "state skin")
	_check(_approx(m.x_frac, 0.25), "state xFrac")
	_check(_approx(m.y_frac, 0.75), "state yFrac")
	_check(m.facing_left, "state facingLeft")
	_check(m.anim == LanMsg.ANIM_WALK, "state anim")


static func _chat_round_trip_with_escaping() -> void:
	var nasty := "hi | there \\ friend |\\| ok"
	var raw := LanProtocol.encode_chat("id-2", "A|B\\C", nasty, "peer-9")
	var m := LanProtocol.decode(raw)
	_check(m != null, "chat decodes")
	if m == null:
		return
	_check(m.type == LanMsg.CHAT, "chat type")
	_check(m.name == "A|B\\C", "chat name escaping")
	_check(m.text == nasty, "chat text escaping")
	_check(m.target == "peer-9", "chat target")


static func _broadcast_chat_has_empty_target() -> void:
	var m := LanProtocol.decode(LanProtocol.encode_chat("id", "n", "hello", ""))
	_check(m != null and m.target == "", "broadcast chat empty target")


static func _chat_style_round_trip() -> void:
	var m := LanProtocol.decode(LanProtocol.encode_chat(
			"id", "n", "hey", "", 2.5, 1, "E5312E"))
	_check(m != null, "styled chat decodes")
	if m == null:
		return
	_check(_approx(m.chat_scale, 2.5), "chat scale")
	_check(m.chat_effect == 1, "chat effect")
	_check(m.chat_color == "E5312E", "chat color")


static func _legacy_chat_without_style_gets_defaults() -> void:
	var m := LanProtocol.decode("DC1|C|id|n|hello|")
	_check(m != null, "legacy chat decodes")
	if m == null:
		return
	_check(_approx(m.chat_scale, 1.0), "legacy chat scale default")
	_check(m.chat_effect == 0, "legacy chat effect default")
	_check(m.chat_color == "", "legacy chat color default")


static func _action_round_trip() -> void:
	var m := LanProtocol.decode(LanProtocol.encode_action(
			"id-4", "Ana", "shoot", "id-9"))
	_check(m != null, "action decodes")
	if m != null:
		_check(m.type == LanMsg.ACTION, "action type")
		_check(m.action == "shoot", "action verb")
		_check(m.target == "id-9", "action target")
	var all := LanProtocol.decode(LanProtocol.encode_action(
			"id-4", "Ana", "shoot", ""))
	_check(all != null and all.target == "", "action broadcast empty target")


static func _bye_round_trip() -> void:
	var m := LanProtocol.decode(LanProtocol.encode_bye("id-3"))
	_check(m != null, "bye decodes")
	if m != null:
		_check(m.type == LanMsg.BYE, "bye type")
		_check(m.id == "id-3", "bye id")


static func _fractions_are_clamped() -> void:
	var m := LanProtocol.decode(LanProtocol.encode_state(
			"id", "n", "cat", -3.0, 42.0, false, 0))
	_check(m != null, "clamp state decodes")
	if m == null:
		return
	_check(_approx(m.x_frac, 0.0), "xFrac clamped low")
	_check(_approx(m.y_frac, 1.0), "yFrac clamped high")
	_check(not m.facing_left, "facingLeft false")


static func _rejects_garbage() -> void:
	_check(LanProtocol.decode(null) == null, "rejects null")
	_check(LanProtocol.decode("") == null, "rejects empty")
	_check(LanProtocol.decode("hello world") == null, "rejects non-DC1")
	_check(LanProtocol.decode("XX9|S|id|n|cat|0|0|0|0") == null, "rejects wrong magic")
	_check(LanProtocol.decode("DC1|S|id|n|cat|zero|0|0|0") == null, "rejects bad float")
	_check(LanProtocol.decode("DC1|S|id") == null, "rejects truncated")
	_check(LanProtocol.decode("DC1|Z|id") == null, "rejects unknown type")
	_check(LanProtocol.decode("DC1|S||n|cat|0|0|0|0") == null, "rejects empty id")


## Not in the Java suite: pins the exact bytes so cross-client compat holds
## (Java Float.toString prints "0.25" / "1.0").
static func _exact_wire_bytes() -> void:
	_check(LanProtocol.encode_state("id-1", "Rajat", "squirtle",
			0.25, 0.75, true, LanMsg.ANIM_WALK)
			== "DC1|S|id-1|Rajat|squirtle|0.25|0.75|1|1", "state exact bytes")
	_check(LanProtocol.encode_chat("id", "n", "hello", "")
			== "DC1|C|id|n|hello||1.0|0|", "chat exact bytes")
	_check(LanProtocol.encode_bye("id-3") == "DC1|B|id-3", "bye exact bytes")
