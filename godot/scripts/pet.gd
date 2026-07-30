## Spike pet: procedural placeholder blob with the core interaction loop —
## idle bob, grab/drag with the mouse, spring squash on release.
## Real skins (ported PixelArt) replace _draw() in phase 2.
extends Node2D

const BODY_W := 96.0
const BODY_H := 84.0

enum State { IDLE, DRAG }

var state := State.IDLE
var t := 0.0
var drag_offset := Vector2.ZERO

# Damped spring for squash-and-stretch (port of Spring.java).
var squash := 0.0
var squash_vel := 0.0
const STIFF := 180.0
const DAMP := 12.0


func hit_rect() -> Rect2:
	return Rect2(position - Vector2(BODY_W / 2, BODY_H), Vector2(BODY_W, BODY_H))


func _process(delta: float) -> void:
	t += delta
	var accel := -STIFF * squash - DAMP * squash_vel
	squash_vel += accel * delta
	squash = clampf(squash + squash_vel * delta, -0.6, 0.6)

	match state:
		State.IDLE:
			pass  # bob rendered from t in _draw
		State.DRAG:
			var mouse := get_viewport().get_mouse_position()
			position = mouse + drag_offset
			var usable_size := Vector2(DisplayServer.screen_get_usable_rect().size)
			position = position.clamp(
					Vector2(BODY_W / 2, BODY_H),
					usable_size - Vector2(BODY_W / 2, 0))
	queue_redraw()


func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT:
		if event.pressed and hit_rect().has_point(event.position):
			state = State.DRAG
			drag_offset = position - event.position
			squash_vel = 8.0  # stretch kick on grab
		elif not event.pressed and state == State.DRAG:
			state = State.IDLE
			squash_vel = -6.0  # squish kick on drop


func _draw() -> void:
	var bob := sin(t * 2.2) * 3.0 if state == State.IDLE else 0.0
	var sx := 1.0 - squash * 0.5
	var sy := 1.0 + squash * 0.5
	var w := BODY_W * sx
	var h := (BODY_H - 12.0) * sy
	var center := Vector2(0, -h / 2 + bob)

	var body := Color("2e8b8b")
	var dark := body.darkened(0.35)

	draw_ellipse_approx(center, Vector2(w / 2, h / 2), body, dark)
	# ears
	var ear_y := center.y - h / 2 + 4
	draw_colored_polygon(PackedVector2Array([
			Vector2(-w * 0.32, ear_y), Vector2(-w * 0.14, ear_y),
			Vector2(-w * 0.26, ear_y - 16)]), body)
	draw_colored_polygon(PackedVector2Array([
			Vector2(w * 0.14, ear_y), Vector2(w * 0.32, ear_y),
			Vector2(w * 0.26, ear_y - 16)]), body)
	# eyes track the cursor a little
	var mouse := get_viewport().get_mouse_position()
	var look := (mouse - global_position).limit_length(60.0) / 60.0 * 2.5
	for side in [-1.0, 1.0]:
		var eye := center + Vector2(side * w * 0.18, -h * 0.08)
		draw_circle(eye, 7.0, Color.WHITE)
		draw_circle(eye + look, 3.5, Color("1c1c24"))


func draw_ellipse_approx(c: Vector2, radii: Vector2, fill: Color,
		outline: Color) -> void:
	var pts := PackedVector2Array()
	for i in 40:
		var a := TAU * i / 40.0
		pts.append(c + Vector2(cos(a) * radii.x, sin(a) * radii.y))
	draw_colored_polygon(pts, fill)
	pts.append(pts[0])
	draw_polyline(pts, outline, 2.0)
