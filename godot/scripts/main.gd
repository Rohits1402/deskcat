## One screen-sized transparent always-on-top overlay window. Everything
## (pet, later: peers/bubbles/pellets) lives inside it as Node2Ds.
## Click-through everywhere EXCEPT over interactive sprites, via
## DisplayServer.window_set_mouse_passthrough (rebuilt every frame).
extends Node2D

const WINDOW_ID := 0

## Passthrough rects are grown by this margin so a fast cursor can't outrun
## the once-per-frame polygon update and leak clicks to the window below.
const HIT_MARGIN := 28.0

@onready var pet: Node2D = $Pet

var _tray: StatusIndicator
var _pet3d: Node = null
var reminders: Reminders
var groove: Groove
var net: NetController
var chat: ChatInput
var _peer_menu: PopupMenu = null
var _last_polygon := PackedVector2Array()
var _desktop_poll := 0.0
var _desktop_poll_supported := true


func _ready() -> void:
	# Perf: hard 60 fps cap; _process drops it to 30 while the pet sleeps.
	# (2D MSAA stays off — nearest-filtered pixel art gains nothing from it.)
	Engine.max_fps = 60
	# Fully transparent clear color — anything else leaves ghost outlines
	# smeared behind moving sprites on the transparent framebuffer.
	get_viewport().transparent_bg = true
	RenderingServer.set_default_clear_color(Color(0, 0, 0, 0))
	_setup_overlay_window()
	NativeBridge.hide_from_taskbar(WINDOW_ID)
	_setup_tray()
	# Pet coords are window-LOCAL: subtract the screen origin (non-zero on
	# multi-monitor setups where the primary isn't the leftmost screen).
	var origin := DisplayServer.screen_get_position(DisplayServer.SCREEN_PRIMARY)
	var usable := DisplayServer.screen_get_usable_rect(DisplayServer.SCREEN_PRIMARY)
	pet.position = Vector2(usable.end - origin) - Vector2(220, 0)

	# Settings hub: seed from current state, then follow changes.
	AppSettings.skin_id = pet.skin_name
	AppSettings.bus().changed.connect(_on_setting)

	reminders = Reminders.new()
	add_child(reminders)
	reminders.remind.connect(func(_kind: StringName, text: String) -> void:
		pet.say_local(text))
	groove = Groove.new()
	add_child(groove)
	groove.beat.connect(func(strength: float) -> void:
		if pet.state == pet.State.IDLE:
			pet.spring_vel += 2.0 * strength)

	net = NetController.new()
	net.user_name = AppSettings.user_name
	net.skin = pet.skin_name
	add_child(net)
	chat = ChatInput.new()
	add_child(chat)
	chat.submitted.connect(_on_chat_submitted)
	net.chat_own_bubble.connect(pet.show_own_bubble)
	net.shot_incoming.connect(func(from_peer) -> void:
		pet.react_to_shot(from_peer))
	net.shot_fired.connect(func(_target) -> void:
		pet.attack_flash())
	net.whipped_incoming.connect(func(_from_peer) -> void:
		pet.react_to_whip())
	net.peer_menu_requested.connect(_show_peer_menu)
	pet.chat_requested.connect(func() -> void:
		chat.open(pet.position - Vector2(0, pet.body_h() + 8)))
	pet.menu_requested.connect(_show_own_menu)


func _show_own_menu(screen_pos: Vector2) -> void:
	if _peer_menu:
		_peer_menu.queue_free()
	_peer_menu = PopupMenu.new()
	add_child(_peer_menu)
	_peer_menu.add_item("Say…", 1)
	if pet.own_bubble.is_active(Time.get_ticks_msec()):
		_peer_menu.add_item("Dismiss bubble", 2)
	var idx := 10
	var actions := {}
	for peer in net.registry.peers():
		var display: String = peer.name if peer.name != "" else "?"
		_peer_menu.add_item("Shoot %s" % display, idx)
		actions[idx] = ["shoot", peer]
		_peer_menu.add_item("Whip %s" % display, idx + 1)
		actions[idx + 1] = ["whip", peer]
		idx += 2
	_peer_menu.id_pressed.connect(_on_own_menu.bind(actions))
	_peer_menu.position = Vector2i(screen_pos)
	_peer_menu.popup()


func _on_own_menu(id: int, actions: Dictionary) -> void:
	match id:
		1:
			chat.open(pet.position - Vector2(0, pet.body_h() + 8))
		2:
			pet.own_bubble.clear()
		_:
			if actions.has(id):
				var a: Array = actions[id]
				if a[0] == "shoot":
					net.shoot(a[1])
				else:
					net.whip(a[1])


func _on_chat_submitted(text: String) -> void:
	if _chat_target_id != "":
		var peer = net.pet_for(_chat_target_id)
		if peer:
			net.send_chat_to(peer.peer, text)
		else:
			net.broadcast_chat(text)
		_chat_target_id = ""
	else:
		net.broadcast_chat(text)


var _chat_target_id := ""


func _show_peer_menu(peer, screen_pos: Vector2) -> void:
	if _peer_menu:
		_peer_menu.queue_free()
	_peer_menu = PopupMenu.new()
	add_child(_peer_menu)
	var display: String = peer.name if peer.name != "" else "them"
	_peer_menu.add_item("Message %s…" % display, 1)
	_peer_menu.add_item("Shoot %s" % display, 2)
	_peer_menu.add_item("Dismiss bubble", 3)
	_peer_menu.id_pressed.connect(_on_peer_menu.bind(peer, screen_pos))
	_peer_menu.position = Vector2i(screen_pos)
	_peer_menu.popup()


func _on_peer_menu(id: int, peer, screen_pos: Vector2) -> void:
	match id:
		1:
			_chat_target_id = peer.id
			chat.open(screen_pos)
		2:
			net.shoot(peer)
		3:
			peer.bubble.clear()


func _on_setting(key: String) -> void:
	match key:
		"skin_id":
			pet.set_skin(AppSettings.skin_id)
		"size_index":
			pet.size_factor = AppSettings.size_factor()
		"sound_on":
			SoundFx.enabled = AppSettings.sound_on
		"music_groove":
			groove.enabled = AppSettings.music_groove
		"reminders_stretch", "stretch_interval_min":
			reminders.stretch_enabled = AppSettings.reminders_stretch
			reminders.stretch_interval_min = AppSettings.stretch_interval_min
		"reminders_water", "water_interval_min":
			reminders.water_enabled = AppSettings.reminders_water
			reminders.water_interval_min = AppSettings.water_interval_min
		"dnd":
			net.dnd = AppSettings.dnd
		"peer_alpha":
			net.peer_alpha = AppSettings.peer_alpha
		"user_name":
			net.user_name = AppSettings.user_name


func _setup_overlay_window() -> void:
	# Cover the FULL primary screen (not just the work area) so particles/
	# pellets can fly over the taskbar instead of cutting off at the window
	# edge. The pet's floor is still the work-area bottom (see Pet.floor_y()).
	# Explicit SCREEN_PRIMARY: with multiple monitors the window otherwise
	# opens on whichever screen the OS picks, while our coords assume primary.
	# +1 px height: a borderless window EXACTLY the screen size gets promoted
	# to fullscreen-optimized by Windows, which kills per-pixel transparency.
	var s := DisplayServer.SCREEN_PRIMARY
	DisplayServer.window_set_position(
			DisplayServer.screen_get_position(s), WINDOW_ID)
	DisplayServer.window_set_size(
			DisplayServer.screen_get_size(s) + Vector2i(0, 1), WINDOW_ID)
	DisplayServer.window_set_flag(
			DisplayServer.WINDOW_FLAG_ALWAYS_ON_TOP, true, WINDOW_ID)
	DisplayServer.window_set_flag(
			DisplayServer.WINDOW_FLAG_NO_FOCUS, true, WINDOW_ID)


func _setup_tray() -> void:
	_tray = StatusIndicator.new()
	_tray.icon = _tray_icon()
	_tray.tooltip = "DeskCat (Godot)"
	var menu := PopupMenu.new()
	add_child(menu)
	menu.add_item("Settings…", 50)
	menu.add_item("Patch notes", 51)
	menu.add_separator()
	menu.add_check_item("3D test pet", 20)
	menu.add_separator()
	menu.add_item("Quit", 0)
	menu.id_pressed.connect(_on_tray)
	add_child(_tray)
	_tray.menu = menu.get_path()


func _on_tray(id: int) -> void:
	var menu: PopupMenu = _tray.get_node(_tray.menu)
	match id:
		0:
			get_tree().quit()
		20:
			var idx := menu.get_item_index(20)
			var on := not menu.is_item_checked(idx)
			menu.set_item_checked(idx, on)
			_toggle_pet3d(on)
		50:
			SettingsWindow.show_window(self)
		51:
			PatchNotes.show_window()


## Proof-of-concept 3D pet: a SubViewport with transparent background
## composited into the overlay — the pipeline real 3D characters
## (e.g. Kenney CC0 GLBs) will use.
func _toggle_pet3d(on: bool) -> void:
	if not on:
		if _pet3d:
			_pet3d.queue_free()
			_pet3d = null
		return
	var script: GDScript = load("res://scripts/pet3d_view.gd")
	_pet3d = script.new()
	add_child(_pet3d)
	var origin := DisplayServer.screen_get_position(DisplayServer.SCREEN_PRIMARY)
	var usable := DisplayServer.screen_get_usable_rect(DisplayServer.SCREEN_PRIMARY)
	_pet3d.position = Vector2(usable.end - origin) - Vector2(480, 260)


func _tray_icon() -> Texture2D:
	# 16x16 teal cat-ish blob, generated in code (no assets — house style).
	var img := Image.create(16, 16, false, Image.FORMAT_RGBA8)
	var body := Color("2e8b8b")
	for y in range(4, 15):
		for x in range(2, 14):
			img.set_pixel(x, y, body)
	for p in [Vector2i(3, 1), Vector2i(4, 2), Vector2i(5, 3),
			Vector2i(12, 1), Vector2i(11, 2), Vector2i(10, 3)]:
		img.set_pixel(p.x, p.y, body)   # ears
	img.set_pixel(5, 8, Color.WHITE)
	img.set_pixel(10, 8, Color.WHITE)  # eyes
	return ImageTexture.create_from_image(img)


func _process(delta: float) -> void:
	_update_passthrough()
	_feed_net()
	# Follow the user across Windows virtual desktops (needs native ext).
	if _desktop_poll_supported:
		_desktop_poll += delta
		if _desktop_poll >= 1.0:
			_desktop_poll = 0.0
			_desktop_poll_supported = \
					NativeBridge.ensure_on_current_desktop(WINDOW_ID) \
					or not NativeBridge.available()
	# Perf: idle-down to 30 fps while asleep with nothing animating.
	var can_doze: bool = pet.state == pet.State.SLEEP \
			and pet.particles.is_empty() and _pet3d == null
	Engine.max_fps = 30 if can_doze else 60


## Own state → LAN, mapped to the wire's work-area fractions and anim ids.
func _feed_net() -> void:
	var work := RemotePet._work_rect()
	var fx := clampf((pet.position.x - work.position.x) / work.size.x, 0, 1)
	var fy := clampf((pet.position.y - work.position.y) / work.size.y, 0, 1)
	var anim := LanMsg.ANIM_IDLE
	if pet.ko_t > 0.0:
		anim = LanMsg.ANIM_KO
	elif pet.state == pet.State.DRAG:
		anim = LanMsg.ANIM_DRAG
	elif pet.state == pet.State.SLEEP:
		anim = LanMsg.ANIM_SLEEP
	elif pet.state == pet.State.PATROL or pet.state == pet.State.RETURN:
		anim = LanMsg.ANIM_WALK
	net.set_self_state(fx, fy, pet.facing_left, anim)
	net.skin = pet.skin_name


## The overlay must swallow clicks ONLY over interactive sprites; the rest
## of the screen belongs to whatever is underneath. One polygon per window —
## disjoint rects are stitched with zero-area bridges (even-odd fill drops
## the bridges). While dragging, the whole overlay turns interactive so fast
## cursor moves can't escape the region and drop clicks through mid-drag.
func _update_passthrough() -> void:
	var poly: PackedVector2Array
	if pet.dragging():
		var size := Vector2(DisplayServer.window_get_size(WINDOW_ID))
		poly = PackedVector2Array([Vector2.ZERO, Vector2(size.x, 0), size,
				Vector2(0, size.y)])
	else:
		var rects: Array[Rect2] = []
		var pr: Rect2 = pet.hit_rect().grow(HIT_MARGIN)
		# Never cover the taskbar: clicks under the pet's feet must reach it.
		if pet.edge == 0 and pet.state != pet.State.FALL:
			pr.size.y = maxf(8.0, pet.floor_y() - pr.position.y)
		rects.append(pr)
		if _pet3d:
			rects.append(Rect2(_pet3d.position, Vector2(220, 220)))
		if chat and chat.is_open():
			rects.append(chat.get_rect().grow(8.0))
		if net:
			for rr in net.pet_hit_rects():
				rects.append(rr.grow(HIT_MARGIN))
		poly = _rects_to_polygon(rects)
	if poly != _last_polygon:
		_last_polygon = poly
		DisplayServer.window_set_mouse_passthrough(poly, WINDOW_ID)


static func _rects_to_polygon(rects: Array[Rect2]) -> PackedVector2Array:
	var poly := PackedVector2Array()
	for r in rects:
		# 5 points per rect (closing back to its origin) — consecutive rects
		# connect via zero-area bridge edges that even-odd fill excludes.
		poly.append(r.position)
		poly.append(r.position + Vector2(r.size.x, 0))
		poly.append(r.end)
		poly.append(r.position + Vector2(0, r.size.y))
		poly.append(r.position)
	return poly
