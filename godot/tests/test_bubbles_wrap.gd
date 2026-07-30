## Port of BubblesFitTextTest.java: wrap/fit_text against an injected fake
## measurer (7 px per char, like a simple monospace font) — no Font needed.

static var _passed := 0
static var _failed := 0

## Text-width oracle injected into Bubbles.wrap/fit_text.
static var MONO: Callable = func(s: String) -> float:
	return s.length() * 7.0


static func run() -> Array[int]:
	_passed = 0
	_failed = 0
	_short_text_unchanged()
	_exact_fit_unchanged()
	_long_text_truncated_with_ellipsis_and_fits()
	_tiny_limit_degrades_to_ellipsis()
	_wrap_short_text_single_line()
	_wrap_breaks_at_words_and_every_line_fits()
	_wrap_hard_splits_oversized_words()
	_wrap_caps_lines_with_ellipsis()
	_wrap_empty_text_gives_one_empty_line()
	return [_passed, _failed]


static func _check(cond: bool, what: String) -> void:
	if cond:
		_passed += 1
	else:
		_failed += 1
		push_error("FAIL bubbles_wrap: " + what)


static func _fit(text: String, max_w: float) -> String:
	return Bubbles.fit_text(null, 0, text, max_w, MONO)


static func _wrap(text: String, max_w: float, max_lines: int) \
		-> PackedStringArray:
	return Bubbles.wrap(null, 0, text, max_w, max_lines, MONO)


static func _short_text_unchanged() -> void:
	_check(_fit("hi", 100.0) == "hi", "short text unchanged")


static func _exact_fit_unchanged() -> void:
	_check(_fit("12345", 35.0) == "12345", "exact fit unchanged")


static func _long_text_truncated_with_ellipsis_and_fits() -> void:
	var fitted := _fit("hello wonderful world", 70.0)
	_check(fitted.ends_with("…"), "truncated ends with ellipsis")
	_check(MONO.call(fitted) <= 70.0, "truncated fits")
	_check(fitted == "hello wonderful world".substr(0, 9) + "…",
			"truncated exact")


static func _tiny_limit_degrades_to_ellipsis() -> void:
	_check(_fit("hello", 7.0) == "…", "tiny limit 7")
	_check(_fit("hello", 0.0) == "…", "tiny limit 0")


static func _wrap_short_text_single_line() -> void:
	var lines := _wrap("hi there", 100.0, 4)
	_check(lines.size() == 1 and lines[0] == "hi there",
			"short text single line")


static func _wrap_breaks_at_words_and_every_line_fits() -> void:
	var lines := _wrap("the quick brown fox jumps over", 80.0, 4)
	_check(lines.size() > 1, "breaks into multiple lines")
	for line in lines:
		_check(MONO.call(line) <= 80.0, "line fits: " + line)
	_check(" ".join(lines) == "the quick brown fox jumps over",
			"no words lost")


static func _wrap_hard_splits_oversized_words() -> void:
	var lines := _wrap("abcdefghijklmnop", 35.0, 4)
	_check(lines.size() > 1, "oversized word split")
	for line in lines:
		_check(MONO.call(line) <= 35.0, "split line fits: " + line)
	_check("".join(lines) == "abcdefghijklmnop", "no chars lost")


static func _wrap_caps_lines_with_ellipsis() -> void:
	var lines := _wrap(
			"one two three four five six seven eight nine ten", 35.0, 3)
	_check(lines.size() == 3, "capped to max lines")
	_check(lines[2].ends_with("…"), "last line ellipsis")
	_check(MONO.call(lines[2]) <= 35.0, "last line fits")


static func _wrap_empty_text_gives_one_empty_line() -> void:
	var lines := _wrap("", 35.0, 4)
	_check(lines.size() == 1 and lines[0] == "", "empty gives one empty line")
