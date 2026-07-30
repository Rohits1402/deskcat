class_name Bubbles
## Speech bubbles (port of com.deskcat.Bubbles): greedy word wrap plus
## CanvasItem draw calls. wrap()/fit_text() are pure — the width measurer is
## injectable so they are testable headless — and draw() renders the Java
## look: white rounded bubble, dark text, small tail triangle pointing down
## at the anchor. draw() must only be called from a CanvasItem's _draw().

const MAX_LINES := 4
const C_OUTLINE := Color("26202a")
const PAD_X := 8.0
const PAD_Y := 6.0
const CORNER := 6.0
const TAIL_H := 8.0
const TAIL_HALF_W := 6.0


## Greedy word wrap. Words wider than a whole line are hard-split; text that
## would exceed max_lines is cut with an ellipsis on the last line.
## measurer: optional Callable(String) -> float text-width oracle; defaults
## to font.get_string_size at font_size (tests inject a fake).
static func wrap(font: Font, font_size: int, text: String, max_w: float,
		max_lines: int, measurer := Callable()) -> PackedStringArray:
	var m := _measurer_or_default(font, font_size, measurer)
	var lines := PackedStringArray()
	var cur := ""
	for w in _words(text):
		var word: String = w
		# hard-split words that could never fit on one line
		while m.call(word) > max_w and word.length() > 1:
			var cut := word.length() - 1
			while cut > 1 and m.call(word.substr(0, cut)) > max_w:
				cut -= 1
			if not cur.is_empty():
				lines.append(cur)
				cur = ""
			lines.append(word.substr(0, cut))
			word = word.substr(cut)
		var candidate := word if cur.is_empty() else cur + " " + word
		if m.call(candidate) <= max_w or cur.is_empty():
			cur = candidate
		else:
			lines.append(cur)
			cur = word
	if not cur.is_empty():
		lines.append(cur)
	if lines.is_empty():
		lines.append("")
	if lines.size() > max_lines:
		var last := lines[max_lines - 1]
		lines.resize(max_lines)
		lines[max_lines - 1] = _fit(m, last + "…", max_w)
	return lines


## Truncate with an ellipsis so a single line always fits.
static func fit_text(font: Font, font_size: int, text: String, max_w: float,
		measurer := Callable()) -> String:
	return _fit(_measurer_or_default(font, font_size, measurer), text, max_w)


## Draw one speech bubble above `anchor` (in ci's local coords; the tail
## points down at it). bubble is a Peer.Bubble — scale/effect/color ride it.
## Font px = base_size * bubble.scale(); alpha multiplies everything.
static func draw(ci: CanvasItem, font: Font, base_size: int,
		bubble: Peer.Bubble, anchor: Vector2, alpha: float) -> void:
	if alpha <= 0.0:
		return
	var now := Time.get_ticks_msec()
	if not bubble.is_active(now):
		return
	var scale := bubble.scale()
	var fs := maxi(1, roundi(base_size * scale))
	var vp := ci.get_viewport_rect().size

	# Same line cap the Java client used for big scales.
	var max_lines := 2 if scale >= 2.0 else (3 if scale > 1.2 else MAX_LINES)
	var max_text_w := clampf(vp.x * 0.4, 200.0, 640.0)
	var lines := wrap(font, fs, bubble.text(), max_text_w, max_lines)

	var line_h := font.get_height(fs)
	var tw := 0.0
	for line in lines:
		tw = maxf(tw, font.get_string_size(line,
				HORIZONTAL_ALIGNMENT_LEFT, -1, fs).x)
	var bw := tw + PAD_X * 2.0
	var bh := lines.size() * line_h + PAD_Y * 2.0

	var bx := anchor.x - bw / 2.0
	var by := anchor.y - TAIL_H - bh
	# keep the bubble on screen horizontally (ci assumed unscaled/unrotated)
	var org := ci.get_global_transform().origin
	bx = clampf(bx, 4.0 - org.x, maxf(4.0 - org.x, vp.x - bw - 4.0 - org.x))
	by = maxf(by, 4.0 - org.y)

	var bg := Color(1, 1, 1, 0.92 * alpha)
	var outline := Color(C_OUTLINE, alpha)
	var body := _rounded_points(Rect2(bx, by, bw, bh),
			minf(CORNER, minf(bw, bh) / 2.0))
	ci.draw_colored_polygon(body, bg)
	var closed := body.duplicate()
	closed.append(body[0])
	ci.draw_polyline(closed, outline, 1.0)

	# tail nub pointing at the anchor
	var tx := clampf(anchor.x, bx + CORNER + TAIL_HALF_W,
			bx + bw - CORNER - TAIL_HALF_W)
	var tip := Vector2(tx, by + bh + TAIL_H)
	var tl := Vector2(tx - TAIL_HALF_W, by + bh - 1.0)
	var tr := Vector2(tx + TAIL_HALF_W, by + bh - 1.0)
	ci.draw_colored_polygon(PackedVector2Array([tl, tr, tip]), bg)
	ci.draw_line(tl, tip, outline, 1.0)
	ci.draw_line(tr, tip, outline, 1.0)

	# text: dark by default, hex-colored, shake jitter or rainbow per line
	var base_col := C_OUTLINE
	var hex := bubble.color_hex()
	if not hex.is_empty() and Color.html_is_valid(hex):
		base_col = Color.html(hex)
	var time := now / 1000.0
	var rid := ci.get_canvas_item()
	var asc := font.get_ascent(fs)
	for i in lines.size():
		var dx := 0.0
		var dy := 0.0
		var col := base_col
		if bubble.effect() == ChatCommands.EFFECT_SHAKE:
			dx = sin(time * 45.0 + i * 1.7) * 1.5 * scale
			dy = cos(time * 38.0 + i * 2.3) * 1.2 * scale
		elif bubble.effect() == ChatCommands.EFFECT_RAINBOW:
			col = Color.from_hsv(
					fmod(time * 120.0 + i * 40.0, 360.0) / 360.0, 0.8, 0.85)
		font.draw_string(rid,
				Vector2(bx + PAD_X + dx, by + PAD_Y + asc + i * line_h + dy),
				lines[i], HORIZONTAL_ALIGNMENT_LEFT, -1, fs, Color(col, alpha))


static func _measurer_or_default(font: Font, font_size: int,
		measurer: Callable) -> Callable:
	if measurer.is_valid():
		return measurer
	return func(s: String) -> float:
		return font.get_string_size(s, HORIZONTAL_ALIGNMENT_LEFT, -1,
				font_size).x


## Java split("\\s+") on trimmed text: any whitespace runs, no empties.
static func _words(text: String) -> PackedStringArray:
	var t := text.strip_edges().replace("\t", " ") \
			.replace("\n", " ").replace("\r", " ")
	return t.split(" ", false)


static func _fit(m: Callable, text: String, max_w: float) -> String:
	if m.call(text) <= max_w:
		return text
	for l in range(text.length() - 1, 0, -1):
		var candidate := text.substr(0, l) + "…"
		if m.call(candidate) <= max_w:
			return candidate
	return "…"


## Clockwise rounded-rect outline, 5 points per corner arc.
static func _rounded_points(r: Rect2, radius: float) -> PackedVector2Array:
	var pts := PackedVector2Array()
	var centers := [
		r.position + Vector2(radius, radius),
		Vector2(r.end.x - radius, r.position.y + radius),
		r.end - Vector2(radius, radius),
		Vector2(r.position.x + radius, r.end.y - radius),
	]
	var start := [PI, -PI / 2, 0.0, PI / 2]
	for c in 4:
		for i in 5:
			var a: float = start[c] + (PI / 2) * i / 4.0
			pts.append(centers[c] + Vector2(cos(a), sin(a)) * radius)
	return pts
