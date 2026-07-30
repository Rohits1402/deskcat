## The pet: full state machine port of the Java app's behaviors.
## Idle / petting / dragging / startle / attack / kneading / patrol /
## return-home / sleep, particles, squash spring, and the ported PixelArt
## skins (cat / pikachu / squirtle — sprites generated in code, no assets).
extends Node2D

enum State { IDLE, PET, DRAG, STARTLE, ATTACK, KNEAD, PATROL, RETURN, SLEEP }

## Java scales: Small 3 / Normal 4 / Large 5 pixels per art pixel.
const PX_BASE := 3.0

const SLEEP_AFTER := 75.0
const PATROL_AFTER := 25.0
const PATROL_SPEED := 55.0
const STARTLE_CURSOR_SPEED := 2600.0  # px/s toward the pet
const CLICK_MAX_TIME := 0.22
const CLICK_MAX_MOVE := 6.0

var skin: PetSkin
var state := State.IDLE
var size_factor := 1.0  # tray: Small 1 / Normal 4/3 / Large 5/3
var t := 0.0
var idle_time := 0.0
var facing_left := false

## Patrol travels the whole screen perimeter: 0 bottom, 1 right, 2 top,
## 3 left. The pet rotates to stand on each edge (feet on the edge).
var edge := 0
const EDGE_ROT := [0.0, -PI / 2, PI, PI / 2]

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

static var _part_tex: Dictionary = {}


func px() -> float:
	return PX_BASE * size_factor


func body_w() -> float:
	return skin.body_size.x * px()


func body_h() -> float:
	return skin.body_size.y * px()


## Inward normal of the edge the pet stands on (points into the screen).
func edge_normal() -> Vector2:
	return [Vector2.UP, Vector2.LEFT, Vector2.DOWN, Vector2.RIGHT][edge]


func hit_rect() -> Rect2:
	if edge == 0:
		return Rect2(position - Vector2(body_w() / 2, body_h()),
				Vector2(body_w(), body_h()))
	# On other edges the body is rotated — use a square around its center.
	var center := position + edge_normal() * body_h() / 2
	var half := maxf(body_w(), body_h()) / 2
	return Rect2(center - Vector2(half, half), Vector2(half, half) * 2)


func dragging() -> bool:
	return state == State.DRAG


## Global cursor in overlay-local coords; works without focus (polled, not
## event-driven, because passthrough regions never deliver motion events).
func global_cursor() -> Vector2:
	return Vector2(DisplayServer.mouse_get_position()
			- DisplayServer.window_get_position(0))


## The pet stands on the work-area bottom (taskbar top), even though the
## overlay window covers the whole screen. Window-local coords.
func floor_y() -> float:
	var s := DisplayServer.SCREEN_PRIMARY
	return float(DisplayServer.screen_get_usable_rect(s).end.y
			- DisplayServer.screen_get_position(s).y)


func screen_w() -> float:
	return float(DisplayServer.window_get_size(0).x)


func _ready() -> void:
	texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST  # crisp pixel art
	var args := OS.get_cmdline_user_args()
	skin = PetSkin.load(args[0] if args.size() > 0 else "squirtle")
	if _part_tex.is_empty():
		_part_tex = {
			heart = ParticleArt.heart(),
			zzz = ParticleArt.zzz(),
			spark = ParticleArt.spark(),
			drop = ParticleArt.water_drop(),
			alert = ParticleArt.alert(),
		}
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
					Vector2(screen_w() - body_w() / 2, floor_y()))
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
			_patrol_step(delta, PATROL_SPEED)
			if fmod(t, 0.4) < delta:
				_spawn(&"dust", Vector2(
						(1.0 if facing_left else -1.0) * body_w() * 0.4, -4.0))
			if keys > 0 or _cursor_near():
				facing_left = not facing_left  # turn around, walk back
				state = State.RETURN
		State.RETURN:
			_patrol_step(delta, PATROL_SPEED * 1.4)
			if edge == 0 and absf(home_x - position.x) < 6.0:
				position.x = home_x
				facing_left = false
				state = State.IDLE
				idle_time = 0.0
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
		_spawn(&"alert", Vector2(0, -body_h() - 10))


func _enter_knead() -> void:
	state = State.KNEAD
	knead_left = 1.6
	idle_time = 0.0


func _enter_patrol() -> void:
	state = State.PATROL
	home_x = position.x
	facing_left = randf() < 0.5


## Walks the screen perimeter: bottom → right → top → left, feet on the
## edge (node rotation), clockwise when facing right, ccw when facing left.
func _patrol_step(delta: float, speed: float) -> void:
	var d := speed * delta
	var w := screen_w()
	var fy := floor_y()
	var cw := not facing_left
	match edge:
		0:  # bottom
			position = Vector2(position.x + (d if cw else -d), fy)
			if cw and position.x >= w:
				edge = 1
				position.x = w
			elif not cw and position.x <= 0:
				edge = 3
				position.x = 0
		1:  # right edge; clockwise climbs up
			position = Vector2(w, position.y + (-d if cw else d))
			if cw and position.y <= 0:
				edge = 2
				position.y = 0
			elif not cw and position.y >= fy:
				edge = 0
				position.y = fy
		2:  # top; clockwise walks left (hanging upside down)
			position = Vector2(position.x + (-d if cw else d), 0)
			if cw and position.x <= 0:
				edge = 3
				position.x = 0
			elif not cw and position.x >= w:
				edge = 1
				position.x = w
		3:  # left edge; clockwise climbs down
			position = Vector2(0, position.y + (d if cw else -d))
			if cw and position.y >= fy:
				edge = 0
				position.y = fy
			elif not cw and position.y <= 0:
				edge = 2
				position.y = 0
	rotation = EDGE_ROT[edge]


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
			edge = 0
			rotation = 0.0  # grabbing plucks the pet off whatever edge
		elif not event.pressed and pressed:
			pressed = false
			if state == State.DRAG:
				state = State.IDLE
				squash_vel = -6.0  # squish on drop
			elif press_time < CLICK_MAX_TIME:
				_attack()
			idle_time = 0.0
	elif event is InputEventMouseMotion and pressed and state != State.DRAG:
		if event.position.distance_to(press_pos) > CLICK_MAX_MOVE:
			state = State.DRAG
			squash_vel = 8.0  # stretch on grab
	if pressed and state != State.DRAG:
		press_time += get_process_delta_time()


## Click attack, flavored per skin like the Java app: squirtle water-gun,
## pikachu thunderbolt sparks, cat startle-pop.
func _attack() -> void:
	state = State.ATTACK
	state_timer = 0.35
	squash_vel = -5.0
	var dir := -1.0 if facing_left else 1.0
	match skin.attack_type:
		PetSkin.ATTACK_WATER_GUN:
			for i in 7:
				particles.append({
					pos = Vector2(dir * body_w() * 0.4, -body_h() * 0.55),
					vel = Vector2(dir * randf_range(160, 260),
							randf_range(-40, 10)),
					life = 0.0, max = 0.6, kind = &"drop",
				})
		PetSkin.ATTACK_THUNDERBOLT:
			for i in 6:
				_spawn(&"spark", Vector2(randf_range(-14, 14),
						-body_h() * randf_range(0.4, 1.1)))
		_:
			_spawn(&"alert", Vector2(0, -body_h() - 10))


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
		if p.kind != &"drop":
			p.vel.y -= 8.0 * delta   # drift up
		else:
			p.vel.y += 300.0 * delta  # water falls
	particles = particles.filter(func(p): return p.life < p.max)


# --- rendering ---------------------------------------------------------------

func _draw() -> void:
	var bob := 0.0
	var sy_extra := 1.0
	match state:
		State.IDLE, State.PET:
			bob = sin(t * 2.2) * 0.8
		State.PATROL, State.RETURN:
			bob = absf(sin(t * 9.0)) * -1.2
		State.SLEEP:
			sy_extra = 0.8
		State.KNEAD:
			bob = sin(t * 12.0) * 0.6

	var flip := -1.0 if facing_left else 1.0
	var sx := (1.0 - squash * 0.5) * px()
	var sy := (1.0 + squash * 0.5) * px() * sy_extra

	var bw := float(skin.body_size.x)
	var bh := float(skin.body_size.y)

	# Everything body-related draws in art-pixel coords under one transform,
	# so squash/flip/scale hit all parts together (the Java FBO trick).
	draw_set_transform(Vector2(0, bob * px()), 0.0, Vector2(sx * flip, sy))

	# tail (behind body), 3 wag frames
	if skin.tail.size() == 3:
		var frame := 0
		if state != State.SLEEP:
			frame = int(t * 5.0) % 3
		var tail_tex: ImageTexture = skin.tail[frame]
		var ts := Vector2(tail_tex.get_size())
		draw_texture_rect(tail_tex, Rect2(
				skin.tail_x - bw / 2 - 2, -ts.y - 1, ts.x, ts.y), false)

	draw_texture_rect(skin.body, Rect2(-bw / 2, -bh, bw, bh), false)
	_draw_eyes(bw, bh)

	draw_set_transform(Vector2.ZERO, 0.0, Vector2.ONE)
	_draw_particles()


func _draw_eyes(bw: float, bh: float) -> void:
	var dark := Color("26202a")
	var closed := blink > 0.0 or state == State.SLEEP
	var happy := state == State.PET or state == State.KNEAD

	# gaze offset in art pixels, from the cursor direction
	var look_v := global_cursor() - global_position \
			+ Vector2(0, bh * px() * 0.6)
	var look := Vector2(clampf(look_v.x / 120.0, -1.0, 1.0),
			clampf(look_v.y / 120.0, -1.0, 1.0)).round()

	for ex in [skin.eye_lx, skin.eye_rx]:
		# local art coords: body spans x -bw/2..bw/2, y -bh..0
		var r := Rect2(ex - bw / 2, skin.eye_y - bh,
				skin.eye_w, skin.eye_h)
		if closed:
			draw_rect(Rect2(r.position, Vector2(r.size.x, r.size.y)),
					skin.fur_color)
			draw_rect(Rect2(r.position + Vector2(0, r.size.y - 1),
					Vector2(r.size.x, 1)), dark)
		elif happy:
			draw_rect(Rect2(r.position + Vector2(0, 1),
					Vector2(r.size.x, 1)), dark)
			draw_rect(Rect2(r.position + Vector2(0, 0),
					Vector2(1, 2)), dark)
			draw_rect(Rect2(r.position + Vector2(r.size.x - 1, 0),
					Vector2(1, 2)), dark)
		else:
			# Iris rides the gaze but always stays inside the white.
			match skin.eye_style:
				PetSkin.EYE_SOLID_BEAD:
					draw_rect(r, dark)
					draw_rect(Rect2(r.position + Vector2(
							clampf(1.0 + look.x, 0.0, r.size.x - 1.0), 0),
							Vector2.ONE), Color.WHITE)
				PetSkin.EYE_OUTLINED_BLOCK:
					draw_rect(r.grow(1.0), dark)
					draw_rect(r, Color.WHITE)
					draw_rect(Rect2(r.position + Vector2(
							clampf(1.0 + look.x, 0.0, r.size.x - 1.0),
							clampf(1.0 + look.y, 0.0, r.size.y - 2.0)),
							Vector2(1, 2)), skin.iris_color)
				_:
					draw_rect(r, Color.WHITE)
					draw_rect(Rect2(r.position + Vector2(
							clampf(1.0 + look.x, 0.0, r.size.x - 2.0),
							clampf(look.y * 0.5, 0.0, 1.0)),
							Vector2(2, r.size.y - 1)), skin.iris_color)


func _draw_particles() -> void:
	var s := px()
	for p in particles:
		var a: float = clampf(1.0 - p.life / p.max, 0.0, 1.0)
		var pos: Vector2 = p.pos
		if p.kind == &"dust":
			draw_circle(pos, 2.5 * (1.0 + p.life),
					Color(0.8, 0.8, 0.8, a * 0.5))
			continue
		var tex: ImageTexture = _part_tex.get(p.kind)
		if tex == null:
			continue
		var size := Vector2(tex.get_size()) * s
		draw_texture_rect(tex, Rect2(pos - size / 2, size), false,
				Color(1, 1, 1, a))
