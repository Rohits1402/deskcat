## The pet: full state machine port of the Java app's behaviors.
## Idle / petting / dragging / startle / attack / kneading / patrol /
## return-home / sleep, plus particles and the squash spring.
## Placeholder procedural body until the PixelArt skins land.
extends Node2D

enum State { IDLE, PET, DRAG, STARTLE, ATTACK, KNEAD, PATROL, RETURN, SLEEP }

const BASE_W := 96.0
const BASE_H := 84.0

const SLEEP_AFTER := 75.0
const PATROL_AFTER := 25.0
const PATROL_SPEED := 55.0
const STARTLE_CURSOR_SPEED := 2600.0  # px/s toward the pet
const CLICK_MAX_TIME := 0.22
const CLICK_MAX_MOVE := 6.0

var state := State.IDLE
var size_factor := 1.0  # tray: Small 1.0 / Normal 1.35 / Large 1.7
var t := 0.0
var idle_time := 0.0
var facing_left := false

# --- interaction bookkeeping -------------------------------------------------
var drag_offset := Vector2.ZERO
var press_time := 0.0
var press_pos := Vector2.ZERO
var pressed := false
var pet_heat := 0.0          # builds while the cursor strokes the pet
var state_timer := 0.0       # generic countdown for STARTLE/ATTACK
var knead_left := 0.0
var home_x := 0.0
var blink := 0.0
var next_blink := 3.0

var _prev_cursor := Vector2.ZERO
var _cursor_vel := Vector2.ZERO

# Damped spring squash (port of Spring.java).
var squash := 0.0
var squash_vel := 0.0
const STIFF := 180.0
const DAMP := 12.0

# --- particles ---------------------------------------------------------------
# {pos: Vector2 (local), vel: Vector2, life: float, max: float, kind: String}
var particles: Array[Dictionary] = []


func body_w() -> float:
	return BASE_W * size_factor


func body_h() -> float:
	return BASE_H * size_factor


func hit_rect() -> Rect2:
	return Rect2(position - Vector2(body_w() / 2, body_h()),
			Vector2(body_w(), body_h()))


func dragging() -> bool:
	return state == State.DRAG


## Global cursor in overlay-local coords; works without focus (polled, not
## event-driven, because passthrough regions never deliver motion events).
func global_cursor() -> Vector2:
	return Vector2(DisplayServer.mouse_get_position()
			- DisplayServer.window_get_position(0))


func usable_size() -> Vector2:
	return Vector2(DisplayServer.screen_get_usable_rect().size)


func _ready() -> void:
	home_x = position.x


func _process(delta: float) -> void:
	t += delta
	_update_spring(delta)
	_update_cursor_tracking(delta)
	_update_blink(delta)

	var keys := NativeBridge.key_delta()

	match state:
		State.IDLE:
			idle_time += delta
			_check_petting(delta)
			_check_startle()
			if keys > 0:
				_enter_knead()
			elif idle_time > SLEEP_AFTER:
				state = State.SLEEP
			elif idle_time > PATROL_AFTER and randf() < delta * 0.08:
				_enter_patrol()
		State.PET:
			idle_time = 0.0
			_check_petting(delta)
			if pet_heat <= 0.0:
				state = State.IDLE
			elif randf() < delta * 3.0:
				_spawn(&"heart", Vector2(randf_range(-20, 20), -body_h()))
		State.DRAG:
			var mouse := get_viewport().get_mouse_position()
			position = (mouse + drag_offset).clamp(
					Vector2(body_w() / 2, body_h()),
					usable_size() - Vector2(body_w() / 2, 0))
			home_x = position.x
		State.STARTLE, State.ATTACK:
			state_timer -= delta
			if state_timer <= 0.0:
				state = State.IDLE
		State.KNEAD:
			idle_time = 0.0
			knead_left -= delta
			if keys > 0:
				knead_left = 1.6
			if knead_left <= 0.0:
				state = State.IDLE
		State.PATROL:
			_walk(delta)
			_wrap_around()
			if keys > 0 or _cursor_near():
				state = State.RETURN
		State.RETURN:
			var dx := home_x - position.x
			if absf(dx) < 4.0:
				position.x = home_x
				facing_left = false
				state = State.IDLE
				idle_time = 0.0
			else:
				facing_left = dx < 0
				position.x += signf(dx) * PATROL_SPEED * 1.4 * delta
		State.SLEEP:
			if randf() < delta * 0.8:
				_spawn(&"zzz", Vector2(body_w() * 0.3, -body_h() * 0.9))
			if keys > 0 or _cursor_near() or pressed:
				state = State.IDLE
				idle_time = 0.0
				squash_vel = 5.0  # wake-up boing

	_update_particles(delta)
	queue_redraw()


func _update_spring(delta: float) -> void:
	var accel := -STIFF * squash - DAMP * squash_vel
	squash_vel += accel * delta
	squash = clampf(squash + squash_vel * delta, -0.6, 0.6)


func _update_cursor_tracking(delta: float) -> void:
	var cur := global_cursor()
	if delta > 0.0:
		_cursor_vel = (cur - _prev_cursor) / delta
	_prev_cursor = cur


func _update_blink(delta: float) -> void:
	blink = maxf(0.0, blink - delta)
	next_blink -= delta
	if next_blink <= 0.0:
		blink = 0.12
		next_blink = randf_range(2.5, 6.0)


func _check_petting(delta: float) -> void:
	var cur := global_cursor()
	var over := hit_rect().grow(6.0).has_point(cur)
	var slow := _cursor_vel.length() < 700.0
	if over and slow and not pressed and _cursor_vel.length() > 15.0:
		pet_heat = minf(pet_heat + delta * 2.5, 1.5)
	else:
		pet_heat = maxf(0.0, pet_heat - delta * 1.2)
	if pet_heat > 0.5 and state == State.IDLE:
		state = State.PET


func _check_startle() -> void:
	var cur := global_cursor()
	var to_pet := (position - Vector2(0, body_h() / 2)) - cur
	var closing := _cursor_vel.dot(to_pet.normalized())
	if to_pet.length() < 260.0 and closing > STARTLE_CURSOR_SPEED:
		state = State.STARTLE
		state_timer = 0.5
		squash_vel = 7.0  # jump stretch
		pet_heat = 0.0


func _enter_knead() -> void:
	state = State.KNEAD
	knead_left = 1.6
	idle_time = 0.0


func _enter_patrol() -> void:
	state = State.PATROL
	home_x = position.x
	facing_left = randf() < 0.5


func _walk(delta: float) -> void:
	var dir := -1.0 if facing_left else 1.0
	position.x += dir * PATROL_SPEED * delta
	if fmod(t, 0.4) < delta:
		_spawn(&"dust", Vector2(-dir * body_w() * 0.4, -4.0))


func _wrap_around() -> void:
	var w := usable_size().x
	if position.x < -body_w() / 2:
		position.x = w + body_w() / 2
	elif position.x > w + body_w() / 2:
		position.x = -body_w() / 2


func _cursor_near() -> bool:
	return global_cursor().distance_to(
			position - Vector2(0, body_h() / 2)) < 90.0


func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT:
		if event.pressed and hit_rect().has_point(event.position):
			pressed = true
			press_time = 0.0
			press_pos = event.position
			drag_offset = position - event.position
		elif not event.pressed and pressed:
			pressed = false
			if state == State.DRAG:
				state = State.IDLE
				squash_vel = -6.0  # squish on drop
			elif press_time < CLICK_MAX_TIME:
				_attack()
			idle_time = 0.0
	elif event is InputEventMouseMotion and pressed and state != State.DRAG:
		press_time += event.relative.length() * 0.0  # keep analyzer quiet
		if event.position.distance_to(press_pos) > CLICK_MAX_MOVE:
			state = State.DRAG
			squash_vel = 8.0  # stretch on grab
	if pressed and state != State.DRAG:
		press_time += get_process_delta_time()


func _attack() -> void:
	state = State.ATTACK
	state_timer = 0.35
	squash_vel = -5.0
	for i in 6:
		_spawn(&"spark", Vector2(randf_range(-10, 10), -body_h() * 0.5))


# --- particles ---------------------------------------------------------------

func _spawn(kind: StringName, local: Vector2) -> void:
	particles.append({
		pos = local, life = 0.0,
		max = 1.4 if kind == &"zzz" else 0.9,
		vel = Vector2(randf_range(-18, 18), -randf_range(22, 46)),
		kind = kind,
	})


func _update_particles(delta: float) -> void:
	for p in particles:
		p.life += delta
		p.pos += p.vel * delta
		p.vel.y -= 8.0 * delta  # drift up
	particles = particles.filter(func(p): return p.life < p.max)


# --- rendering ---------------------------------------------------------------

func _draw() -> void:
	var w := body_w()
	var h := body_h() - 12.0
	var bob := 0.0
	match state:
		State.IDLE, State.PET:
			bob = sin(t * 2.2) * 3.0
		State.PATROL, State.RETURN:
			bob = absf(sin(t * 9.0)) * -4.0
		State.SLEEP:
			h *= 0.8  # curled down
		State.KNEAD:
			bob = sin(t * 12.0) * 2.0

	var sx := 1.0 - squash * 0.5
	var sy := 1.0 + squash * 0.5
	w *= sx
	h *= sy
	var center := Vector2(0, -h / 2 + bob)
	var flip := -1.0 if facing_left else 1.0

	var body := Color("2e8b8b")
	var dark := body.darkened(0.35)

	_draw_ellipse(center, Vector2(w / 2, h / 2), body, dark)
	# ears
	var ear_y := center.y - h / 2 + 4
	for side in [-1.0, 1.0]:
		draw_colored_polygon(PackedVector2Array([
				Vector2(side * w * 0.32, ear_y),
				Vector2(side * w * 0.14, ear_y),
				Vector2(side * w * 0.25, ear_y - 16 * size_factor)]), body)

	_draw_eyes(center, w, h, flip)
	_draw_particles()


func _draw_eyes(center: Vector2, w: float, h: float, flip: float) -> void:
	var closed := blink > 0.0 or state == State.SLEEP
	var happy := state == State.PET or state == State.KNEAD
	for side in [-1.0, 1.0]:
		var eye := center + Vector2(side * w * 0.18, -h * 0.08)
		var r := 7.0 * size_factor
		if closed:
			draw_line(eye + Vector2(-r, 0), eye + Vector2(r, 0),
					Color("1c1c24"), 2.0, true)
		elif happy:
			# ^ ^ arcs
			draw_arc(eye + Vector2(0, r * 0.4), r * 0.9, PI, TAU,
					10, Color("1c1c24"), 2.5, true)
		else:
			var look := (global_cursor() - (global_position + eye))
			look = look.limit_length(60.0) / 60.0 * (2.5 * size_factor)
			if state == State.STARTLE:
				r *= 1.35
			draw_circle(eye, r, Color.WHITE)
			draw_circle(eye + look, r * 0.5, Color("1c1c24"))


func _draw_particles() -> void:
	for p in particles:
		var a: float = clampf(1.0 - p.life / p.max, 0.0, 1.0)
		var pos: Vector2 = p.pos
		match p.kind:
			&"heart":
				var c := Color("e56aa6", a)
				var s := 4.0
				draw_circle(pos + Vector2(-s * 0.5, 0), s * 0.55, c)
				draw_circle(pos + Vector2(s * 0.5, 0), s * 0.55, c)
				draw_colored_polygon(PackedVector2Array([
						pos + Vector2(-s, 0.5), pos + Vector2(s, 0.5),
						pos + Vector2(0, s * 1.4)]), c)
			&"spark":
				var c := Color("ffd54a", a)
				draw_line(pos + Vector2(-3, 0), pos + Vector2(3, 0), c, 2.0)
				draw_line(pos + Vector2(0, -3), pos + Vector2(0, 3), c, 2.0)
			&"zzz":
				var c := Color("cfd8ff", a)
				var s: float = 5.0 + p.life * 3.0
				draw_line(pos + Vector2(-s, -s), pos + Vector2(s, -s), c, 1.5)
				draw_line(pos + Vector2(s, -s), pos + Vector2(-s, s), c, 1.5)
				draw_line(pos + Vector2(-s, s), pos + Vector2(s, s), c, 1.5)
			&"dust":
				draw_circle(pos, 2.5 * (1.0 + p.life), Color(0.8, 0.8, 0.8, a * 0.5))


func _draw_ellipse(c: Vector2, radii: Vector2, fill: Color,
		outline: Color) -> void:
	var pts := PackedVector2Array()
	for i in 48:
		var a := TAU * i / 48.0
		pts.append(c + Vector2(cos(a) * radii.x, sin(a) * radii.y))
	draw_colored_polygon(pts, fill)
	pts.append(pts[0])
	draw_polyline(pts, outline, 2.0, true)
