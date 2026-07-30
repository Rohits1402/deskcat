## Port of ChatCommandsTest.java. No framework: run() returns [passed, failed]
## and reports failures via push_error.

static var _passed := 0
static var _failed := 0


static func run() -> Array[int]:
	_passed = 0
	_failed = 0
	_plain_text_passes_through()
	_big_huge_small()
	_size_is_font_px_and_clamped()
	_effects()
	_named_colors()
	_commands_chain()
	_case_insensitive()
	_malformed_commands_stay_as_text()
	_command_with_no_text_gives_empty()
	return [_passed, _failed]


static func _check(cond: bool, what: String) -> void:
	if cond:
		_passed += 1
	else:
		_failed += 1
		push_error("FAIL chat_commands: " + what)


static func _approx(a: float, b: float) -> bool:
	return absf(a - b) < 1e-4


static func _plain_text_passes_through() -> void:
	var p := ChatCommands.parse("hello world")
	_check(p.text == "hello world", "plain text")
	_check(_approx(p.scale, 1.0), "plain scale")
	_check(p.effect == ChatCommands.EFFECT_NONE, "plain effect")
	_check(p.color_hex == "", "plain color")


static func _big_huge_small() -> void:
	_check(_approx(ChatCommands.parse("/big hi").scale, 1.6), "big scale")
	_check(_approx(ChatCommands.parse("/huge hi").scale, 2.2), "huge scale")
	_check(_approx(ChatCommands.parse("/small hi").scale, 0.65),
			"small scale")
	_check(ChatCommands.parse("/big hi").text == "hi", "big text")


static func _size_is_font_px_and_clamped() -> void:
	_check(_approx(ChatCommands.parse("/size 50 hi").scale, 50.0 / 16.0),
			"size 50")
	_check(ChatCommands.parse("/size 50 hi").text == "hi", "size 50 text")
	_check(_approx(ChatCommands.parse("/size 1000 hi").scale, 1000.0 / 16.0),
			"size 1000")
	_check(_approx(ChatCommands.parse("/size 99999 hi").scale,
			ChatCommands.MAX_SCALE), "size clamps high")
	_check(_approx(ChatCommands.parse("/size 1 hi").scale,
			ChatCommands.MIN_SCALE), "size clamps low")


static func _effects() -> void:
	_check(ChatCommands.parse("/shake hi").effect
			== ChatCommands.EFFECT_SHAKE, "shake")
	_check(ChatCommands.parse("/rainbow hi").effect
			== ChatCommands.EFFECT_RAINBOW, "rainbow")


static func _named_colors() -> void:
	var p := ChatCommands.parse("/color red hi")
	_check(p.color_hex == "E5312E", "color red hex")
	_check(p.text == "hi", "color red text")
	_check(ChatCommands.parse("/colour blue hi").color_hex == "2E6BE5",
			"colour blue")


static func _commands_chain() -> void:
	var p := ChatCommands.parse("/big /shake /color red hello")
	_check(p.text == "hello", "chain text")
	_check(_approx(p.scale, 1.6), "chain scale")
	_check(p.effect == ChatCommands.EFFECT_SHAKE, "chain effect")
	_check(p.color_hex == "E5312E", "chain color")


static func _case_insensitive() -> void:
	_check(_approx(ChatCommands.parse("/BIG hi").scale, 1.6), "BIG")
	_check(ChatCommands.parse("/Color RED hi").color_hex == "E5312E",
			"Color RED")


static func _malformed_commands_stay_as_text() -> void:
	_check(ChatCommands.parse("/size nan hi").text == "/size nan hi",
			"size nan stays text")
	_check(ChatCommands.parse("/color neon hi").text == "/color neon hi",
			"unknown color stays text")
	_check(ChatCommands.parse("/dance hi").text == "/dance hi",
			"unknown command stays text")


static func _command_with_no_text_gives_empty() -> void:
	_check(ChatCommands.parse("/big").text == "", "bare /big")
	_check(ChatCommands.parse("/big ").text == "", "bare /big with space")
