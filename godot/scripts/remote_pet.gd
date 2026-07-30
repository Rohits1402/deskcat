class_name RemotePet
extends Node2D
## Mirror of one remote Peer inside the overlay (port of RemotePetWindow.java
## minus the extra OS window): body + tail + simplified eyes from the peer's
## skin, name label underneath, speech bubble above, WALK bob / SLEEP eyes /
## DRAG stretch / KO lying on its side, 0.4 s fade-in, all times the global
## peer fade. Created and fed by NetController — position is driven from the
## peer's x/y fractions mapped onto THIS screen's work area (feet anchor).

signal menu_requested(peer: Peer, screen_pos: Vector2)

## Art px per texture px (Java "Small"); NetController leaves it alone today.
const PX := 3.0
const APPEAR_S := 0.4
const TAIL_CYCLE := [0, 1, 2, 1]
const C_DARK := Color("26202a")
const LABEL_SIZE := 13
const BUBBLE_BASE_SIZE := 16

var peer: Peer
## Tray "Peer fade" translucency (CatApp.peerAlpha equivalent); the
## integrator sets it on NetController which forwards it here.
var peer_alpha := 0.9
## Local KO override: while Time.get_ticks_msec() < ko_until_ms the pet
## renders knocked out even before its own STATE says ANIM_KO.
var ko_until_ms := 0

var _skin: PetSkin
var _skin_name := ""
var _t := 0.0
var _appear := 0.0
var _placed := false
var _blink_in := 3.0
var _blink_left := 0.0
var _tail_frame := 0
var _tail_time := 0.0


func _ready() -> void:
	texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST


func _process(delta: float) -> void:
	if peer == null:
		return
	var dt := minf(delta, 1.0 / 20.0)
	_t += dt
	_appear = minf(1.0, _appear + dt / APPEAR_S)
	if peer.skin != _skin_name:
		_skin_name = peer.skin
		_skin = PetSkin.load(_skin_name)
	_move(dt)
	_update_blink(dt)
	_update_tail(dt)
	queue_redraw()


## Overlay-local bounds of the standing pet; merge (grown) into the overlay's
## passthrough polygon so right-clicks reach [method _unhandled_input].
func hit_rect() -> Rect2:
	if _skin == null:
		return Rect2(position, Vector2.ZERO)
	var w := _skin.body_size.x * PX
	var h := _skin.body_size.y * PX
	return Rect2(position - Vector2(w / 2.0, h), Vector2(w, h))


func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed \
			and event.button_index == MOUSE_BUTTON_RIGHT \
			and hit_rect().grow(6.0).has_point(event.position):
		var screen: Vector2 = Vector2(DisplayServer.window_get_position(0)) \
				+ event.position
		menu_requested.emit(peer, screen)
		get_viewport().set_input_as_handled()


func _move(dt: float) -> void:
	var target := _target_pos()
	if not _placed or target.distance_to(position) > _work_rect().size.x / 2.0:
		# first placement, or the peer wrapped a screen edge — snap
		_placed = true
		position = target
	else:
		# light easing over the state stream smooths packet jitter (Java k)
		position += (target - position) * minf(1.0, dt * 30.0)


## This screen's work area in overlay-local coords.
static func _work_rect() -> Rect2:
	var s := DisplayServer.SCREEN_PRIMARY
	var origin := DisplayServer.screen_get_position(s)
	var usable := DisplayServer.screen_get_usable_rect(s)
	return Rect2(Vector2(usable.position - origin), Vector2(usable.size))


## Feet anchor: fractions span the work area, so y_frac 1 = standing on the
## taskbar top, mirroring the Java window mapping.
func _target_pos() -> Vector2:
	var r := _work_rect()
	return r.position + Vector2(peer.x_frac * r.size.x, peer.y_frac * r.size.y)


func _is_ko() -> bool:
	return peer.anim == LanMsg.ANIM_KO or Time.get_ticks_msec() < ko_until_ms


func _update_blink(dt: float) -> void:
	if _blink_left > 0.0:
		_blink_left -= dt
		return
	_blink_in -= dt
	if _blink_in <= 0.0:
		_blink_left = 0.12
		_blink_in = randf_range(2.5, 6.0)


func _update_tail(dt: float) -> void:
	_tail_time += dt
	var interval := 0.8 if peer.anim == LanMsg.ANIM_SLEEP \
			else (0.15 if peer.anim == LanMsg.ANIM_WALK else 0.3)
	if _tail_time >= interval:
		_tail_time = 0.0
		_tail_frame = (_tail_frame + 1) % TAIL_CYCLE.size()


func _draw() -> void:
	if peer == null or _skin == null:
		return
	var ga := _appear * peer_alpha
	var walking := peer.anim == LanMsg.ANIM_WALK
	var sleeping := peer.anim == LanMsg.ANIM_SLEEP
	var ko := _is_ko()
	var bob := absf(sin(_t * 9.0)) * -2.0 if walking else 0.0
	var stretch := 1.15 if peer.anim == LanMsg.ANIM_DRAG else 1.0
	var flip := -1.0 if peer.facing_left else 1.0
	var rot := -PI / 2 if ko else 0.0
	var tint := Color(1, 1, 1, ga)

	var bw := float(_skin.body_size.x)
	var bh := float(_skin.body_size.y)

	# One transform for all body parts (same trick as pet.gd / the Java FBO):
	# origin = feet; KO rotates the whole pet onto its side.
	draw_set_transform(Vector2(0, bob), rot, Vector2(PX * flip, PX * stretch))

	if _skin.tail.size() == 3:
		var tail_tex: ImageTexture = _skin.tail[TAIL_CYCLE[_tail_frame]]
		var ts := Vector2(tail_tex.get_size())
		draw_texture_rect(tail_tex, Rect2(
				_skin.tail_x - bw / 2 - 2, -ts.y - 1, ts.x, ts.y), false, tint)
	draw_texture_rect(_skin.body, Rect2(-bw / 2, -bh, bw, bh), false, tint)
	if not ko:
		_draw_eyes(bw, bh, sleeping, ga)

	draw_set_transform(Vector2.ZERO, 0.0, Vector2.ONE)
	_draw_label()
	_draw_bubble(bh, ga)


## Simplified eyes (no gaze tracking): closed lid while sleeping/blinking/KO,
## else the skin's eye style like pet.gd.
func _draw_eyes(bw: float, bh: float, sleeping: bool, ga: float) -> void:
	var dark := Color(C_DARK, ga)
	var white := Color(1, 1, 1, ga)
	var closed := sleeping or _blink_left > 0.0
	for ex in [_skin.eye_lx, _skin.eye_rx]:
		var r := Rect2(ex - bw / 2, _skin.eye_y - bh,
				_skin.eye_w, _skin.eye_h)
		if closed:
			draw_rect(r, Color(_skin.fur_color, ga))
			draw_rect(Rect2(r.position + Vector2(0, r.size.y - 1),
					Vector2(r.size.x, 1)), dark)
			continue
		match _skin.eye_style:
			PetSkin.EYE_SOLID_BEAD:
				draw_rect(r, dark)
				draw_rect(Rect2(r.position + Vector2(1, 0), Vector2.ONE),
						white)
			PetSkin.EYE_OUTLINED_BLOCK:
				draw_rect(r.grow(1.0), dark)
				draw_rect(r, white)
				draw_rect(Rect2(r.position + Vector2(1, 1), Vector2(1, 2)),
						Color(_skin.iris_color, ga))
			_:
				draw_rect(r, white)
				draw_rect(Rect2(r.position + Vector2(1, 0),
						Vector2(2, r.size.y - 1)),
						Color(_skin.iris_color, ga))


func _draw_label() -> void:
	var label := "?" if peer.name.is_empty() else peer.name
	var font := ThemeDB.fallback_font
	var w := font.get_string_size(label,
			HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_SIZE).x
	var pos := Vector2(-w / 2.0, 6.0 + font.get_ascent(LABEL_SIZE))
	draw_string_outline(font, pos, label, HORIZONTAL_ALIGNMENT_LEFT, -1,
			LABEL_SIZE, 3, Color(C_DARK, _appear))
	draw_string(font, pos, label, HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_SIZE,
			Color(1, 1, 1, _appear))


func _draw_bubble(bh: float, ga: float) -> void:
	var now := Time.get_ticks_msec()
	if not peer.bubble.is_active(now):
		return
	Bubbles.draw(self, ThemeDB.fallback_font, BUBBLE_BASE_SIZE, peer.bubble,
			Vector2(0, -bh * PX - 10.0), peer.bubble.alpha(now) * ga)
