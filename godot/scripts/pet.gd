## The pet: full state machine port of the Java app's behaviors.
## Idle / petting / dragging / startle / attack / kneading / patrol /
## return-home / sleep, particles, squash spring, and the ported PixelArt
## skins (cat / pikachu / squirtle — sprites generated in code, no assets).
extends Node2D

enum State { IDLE, PET, DRAG, STARTLE, ATTACK, KNEAD, PATROL, RETURN, SLEEP,
		FALL }

const GRAVITY := 2400.0

## Java scales: Small 3 / Normal 4 / Large 5 pixels per art pixel.
const PX_BASE := 3.0

const SLEEP_AFTER := 75.0
const PATROL_AFTER := 25.0
const PATROL_SPEED := 55.0
const STARTLE_CURSOR_SPEED := 2600.0  # px/s toward the pet
const CLICK_MAX_TIME := 0.22
const CLICK_MAX_MOVE := 6.0

var skin: PetSkin
var skin_name := "squirtle"
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
var fall_vel := 0.0

# Spring.java port: scale_y spring around 1.0, stiffness 160, damping 12,
# clamped [0.7, 1.5]; scale_x = 1 - (scale_y - 1) * 0.55.
var spring_val := 1.0
var spring_vel := 0.0
const STIFF := 160.0
const DAMP := 12.0

# Tail wag: TAIL_CYCLE ping-pong, cadence per state (java-render-spec §6).
const TAIL_CYCLE := [0, 1, 2, 1]
var tail_frame := 0
var tail_t := 0.0

# --- particles ---------------------------------------------------------------
# {pos: Vector2 (local), vel: Vector2, life: float, max: float, kind: String}
var particles: Array[Dictionary] = []

static var _part_tex: Dictionary = {}


func px() -> float:
	var base := PX_BASE
	if skin and skin.px_scale_override > 0.0:
		base = skin.px_scale_override
	return base * size_factor


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
	set_skin(args[0] if args.size() > 0 else "squirtle")
	if _part_tex.is_empty():
		_part_tex = {
			heart = ParticleArt.heart(),
			zzz = ParticleArt.zzz(),
			spark = ParticleArt.spark(),
			drop = ParticleArt.water_drop(),
			alert = ParticleArt.alert(),
		}
	home_x = position.x


func set_skin(id: String) -> void:
	skin_name = id.to_lower()
	skin = PetSkin.load(skin_name)
	# Pixel skins stay crisp; downscaled image skins need smoothing.
	texture_filter = (CanvasItem.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
			if skin.eye_style == PetSkin.EYE_BAKED
			else CanvasItem.TEXTURE_FILTER_NEAREST)
	spring_vel += 3.0  # little boing on change


func _process(delta: float) -> void:
	t += delta
	_update_spring(delta)
	_update_cursor_tracking(delta)
	_update_blink(delta)
	_update_tail(delta)

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
		State.FALL:
			fall_vel += GRAVITY * delta
			position.y += fall_vel * delta
			if position.y >= floor_y():
				position.y = floor_y()
				state = State.IDLE
				idle_time = 0.0
				spring_vel += clampf(-fall_vel / 250.0, -6.0, -2.0)  # land squish
				for i in 3:
					_spawn(&"dust", Vector2(randf_range(-1, 1) * body_w() * 0.5,
							-4.0))
		State.SLEEP:
			if randf() < delta * 0.8:
				_spawn(&"zzz", Vector2(body_w() * 0.3, -body_h() * 0.9))
			if keys > 0 or _cursor_near() or pressed:
				state = State.IDLE
				idle_time = 0.0
				spring_vel += 2.0  # wake-up boing (Java un-hide kick)

	_update_particles(delta)
	queue_redraw()


func _update_spring(delta: float) -> void:
	var target := 1.0
	if state == State.DRAG:
		target = 1.2
	elif state == State.FALL:
		target = 1.12
	spring_vel += (target - spring_val) * STIFF * delta
	spring_vel -= spring_vel * DAMP * delta
	spring_val = clampf(spring_val + spring_vel * delta, 0.7, 1.5)


func _update_cursor_tracking(delta: float) -> void:
	var cur := global_cursor()
	if delta > 0.0:
		_cursor_vel = (cur - _prev_cursor) / delta
	_prev_cursor = cur


## Java tail cadence: sleep 0.8s, startled 0.12, walking 0.15, else 0.3.
func _update_tail(delta: float) -> void:
	var interval := 0.3
	match state:
		State.SLEEP:
			interval = 0.8
		State.STARTLE:
			interval = 0.12
		State.PATROL, State.RETURN:
			interval = 0.15
	tail_t += delta
	if tail_t >= interval:
		tail_t = 0.0
		tail_frame = (tail_frame + 1) % TAIL_CYCLE.size()


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
		spring_vel += 5.0  # jump stretch
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
				if position.y < floor_y() - 2.0:
					state = State.FALL   # dropped mid-air: fall to taskbar
					fall_vel = 0.0
				else:
					state = State.IDLE
					spring_vel += 3.0  # Java drag-release kick
			elif press_time < CLICK_MAX_TIME:
				_attack()
			idle_time = 0.0
	elif event is InputEventMouseMotion and pressed and state != State.DRAG:
		if event.position.distance_to(press_pos) > CLICK_MAX_MOVE:
			state = State.DRAG  # spring target 1.2 does the stretch
	if pressed and state != State.DRAG:
		press_time += get_process_delta_time()


## Click attack, flavored per skin like the Java app: squirtle water-gun,
## pikachu thunderbolt sparks, cat startle-pop.
func _attack() -> void:
	state = State.ATTACK
	state_timer = 0.35
	spring_vel += 5.0  # Java attack-hop kick
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

# FBO layout constants from the Java app (java-render-spec.md §1):
# 40x30-unit canvas, origin at bottom-center (x=20), y-up; body at x=2.
const FBO_W := 40.0
const BODY_X := 2.0
const C_OUTLINE := Color("26202a")


## Convert a Java FBO rect (x, y measured y-UP from the feet) to local
## y-down coords under the draw transform.
static func fbo_rect(x: float, y_up: float, w: float, h: float) -> Rect2:
	return Rect2(x - FBO_W / 2, -(y_up + h), w, h)


func _draw() -> void:
	var bob := 0.0
	var waddle := 0.0
	match state:
		State.PATROL, State.RETURN:
			bob = absf(sin(t * 9.0)) * 0.8
			waddle = sin(t * 9.0) * 3.0
		_:
			pass

	var flip := -1.0 if facing_left else 1.0
	var scale_y := spring_val
	var scale_x := 1.0 - (scale_y - 1.0) * 0.55
	# Java petting: whole-body x jitter, eyes unchanged (spec §3.2).
	var shake := sin(t * 45.0) * 0.25 if state == State.PET else 0.0

	draw_set_transform(Vector2(0, -bob * px()), deg_to_rad(waddle) * flip,
			Vector2(px() * scale_x * flip, px() * scale_y))

	var bw := float(skin.body_size.x)
	var bh := float(skin.body_size.y)

	if skin.eye_style == PetSkin.EYE_BAKED:
		# Image skin: single texture, face baked in.
		draw_texture_rect(skin.body, Rect2(-bw / 2, -bh, bw, bh), false)
	else:
		# tail first (behind body), TAIL_CYCLE ping-pong frames
		if skin.tail.size() == 3:
			var tail_tex: ImageTexture = skin.tail[TAIL_CYCLE[tail_frame]]
			var ts := Vector2(tail_tex.get_size())
			draw_texture_rect(tail_tex,
					fbo_rect(skin.tail_x + shake, 0, ts.x, ts.y), false)
		draw_texture_rect(skin.body,
				fbo_rect(BODY_X + shake, 0, bw, bh), false)
		_draw_eyes(shake)
		if state == State.KNEAD and skin.paw:
			var left_up := sin(t * 14.0) > 0.0
			draw_texture_rect(skin.paw,
					fbo_rect(9 + shake, 1.0 if left_up else 0.0, 4, 4), false)
			draw_texture_rect(skin.paw,
					fbo_rect(18 + shake, 0.0 if left_up else 1.0, 4, 4), false)

	draw_set_transform(Vector2.ZERO, 0.0, Vector2.ONE)
	_draw_particles()


## Faithful port of drawOpenEye/drawClosedEye + gaze math
## (java-render-spec.md §4-5). All coords are FBO units, y-up.
func _draw_eyes(shake: float) -> void:
	var closed := blink > 0.0 or state == State.SLEEP \
			or (state == State.ATTACK and state_timer > 0.15)
	var startled := state == State.STARTLE
	var knead := state == State.KNEAD

	# Gaze: cursor delta from the eye center in SCREEN px, /240, clamped.
	var flip := -1.0 if facing_left else 1.0
	var eye_scr := global_position + Vector2(
			(skin.eye_center.x - FBO_W / 2) * px() * flip,
			-skin.eye_center.y * px())
	var cur := global_cursor()
	var gaze_x := clampf((cur.x - eye_scr.x) / 240.0, -1.0, 1.0)
	var gaze_y := clampf((eye_scr.y - cur.y) / 240.0, -1.0, 1.0)  # +1 above
	var pgx := 1 if gaze_x > 0.3 else (-1 if gaze_x < -0.3 else 0)
	if (state == State.PATROL or state == State.RETURN) and facing_left:
		pgx = -pgx  # canvas is mirrored while walking left
	var iris_y := float(skin.eye_y) \
			if (knead or gaze_y < -0.25) else float(skin.eye_y + 1)
	var gaze_down := -1.0 if knead else gaze_y

	var ew := float(skin.eye_w)
	var eh := float(skin.eye_h)
	var ey := float(skin.eye_y)

	for base_x in [skin.eye_lx, skin.eye_rx]:
		var ex := float(base_x) + shake
		if closed:
			if skin.eye_style == PetSkin.EYE_OUTLINED_BLOCK:
				draw_rect(fbo_rect(ex - 1, ey - 1, ew + 2, eh + 2),
						skin.fur_color)
				draw_rect(fbo_rect(ex - 1, ey + floorf(eh / 2), ew + 2, 1),
						C_OUTLINE)
			else:
				draw_rect(fbo_rect(ex, ey, ew, eh), skin.fur_color)
				draw_rect(fbo_rect(ex, ey + 1, ew, 1), C_OUTLINE)
			continue
		match skin.eye_style:
			PetSkin.EYE_IRIS_ON_WHITE:  # cat: white patch is baked in
				var irisx := ex + 1 + pgx
				if startled:
					draw_rect(fbo_rect(irisx, iris_y, 2, 2), C_OUTLINE)
				else:
					draw_rect(fbo_rect(irisx, iris_y, 2, 2), skin.iris_color)
					draw_rect(fbo_rect(
							irisx + (1 if pgx >= 0 else 0),
							iris_y + (1 if gaze_down >= 0.0 else 0),
							1, 1), C_OUTLINE)
			PetSkin.EYE_SOLID_BEAD:  # pikachu
				draw_rect(fbo_rect(ex, ey, ew, eh), C_OUTLINE)
				if not startled:
					draw_rect(fbo_rect(ex + 1 + pgx,
							ey + eh - 1 if gaze_down >= 0.0 else ey + eh - 2,
							1, 1), Color.WHITE)
			PetSkin.EYE_OUTLINED_BLOCK:  # squirtle
				draw_rect(fbo_rect(ex - 1, ey - 1, ew + 2, eh + 2), C_OUTLINE)
				draw_rect(fbo_rect(ex, ey, ew, eh),
						C_OUTLINE if startled else skin.iris_color)
				if not startled:
					draw_rect(fbo_rect(
							ex + clampf(1 + pgx, 0, ew - 1),
							ey + eh - 2 if gaze_down >= 0.0 else ey + eh - 3,
							1, 2), Color.WHITE)


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
