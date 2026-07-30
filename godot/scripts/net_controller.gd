class_name NetController
extends Node
## LAN orchestration (port of CatApp.updateNet + sendChat/doSendChat/shoot):
## owns the LanClient and PeerRegistry, keeps one RemotePet child per live
## peer, broadcasts our own STATE, and turns chat text / shoot requests into
## datagrams. The integrator adds this node to the overlay, feeds
## set_self_state() (and `skin`) every frame, and connects the signals —
## own-pet effects (bubble, attack flash, KO/pellets) stay in pet.gd.

## Our own chat was accepted for sending: show it on the own pet's bubble.
## parsed is the ChatCommands.parse() dict {text, scale, effect, color_hex}.
signal chat_own_bubble(parsed: Dictionary)
## Someone shot us (broadcast or targeted DM-shot). from_peer may be null
## when the shooter is unknown — Java then skips the pellet, hit only.
signal shot_incoming(from_peer: Peer)
signal whipped_incoming(from_peer: Peer)
## We fired — play the own pet's attack as the muzzle flash. target is null
## when shooting everyone.
signal shot_fired(target: Peer)
## Right-click on a remote pet (relayed from RemotePet.menu_requested).
signal peer_menu_requested(peer: Peer, screen_pos: Vector2)

## Own-state broadcast rate. The Java client sent 60 Hz — harmless on a LAN
## but overkill: remotes ease toward targets anyway, 20 Hz looks identical.
const SEND_HZ := 20.0

## Display name carried in every packet; shown under the remote pet.
var user_name: String = OS.get_environment("USERNAME")
## Skin id broadcast with our state (set alongside pet.set_skin()).
var skin := "squirtle"
## Do Not Disturb: remote pets removed, incoming CHAT/ACTION dropped; our
## own STATE still broadcasts so others keep seeing us.
var dnd := false:
	set(v):
		dnd = v
		if v:
			_clear_pets()
## Global remote-pet translucency (CatApp.peerAlpha); tray-driven.
var peer_alpha := 0.9:
	set(v):
		peer_alpha = v
		for p: RemotePet in _pets.values():
			p.peer_alpha = v

var self_id: String
var registry: PeerRegistry
## False when the multicast socket could not be opened (solo mode).
var online := false

var _client: LanClient
var _send_left := 0.0
var _sx := 0.0
var _sy := 1.0
var _sface := false
var _sanim := LanMsg.ANIM_IDLE
var _pets: Dictionary = {}   # peer id -> RemotePet child


func _ready() -> void:
	self_id = _uuid()
	registry = PeerRegistry.new(self_id)
	_client = LanClient.new(self_id)
	online = _client.start()
	if not online:
		push_warning("DeskCat LAN unavailable — running solo")


func _exit_tree() -> void:
	_client.close()   # broadcasts BYE before closing the socket


## Feed the own pet's pose each frame: fractions of the work area (feet
## anchor, clamped 0..1) plus facing and a LanMsg.ANIM_* state.
func set_self_state(x_frac: float, y_frac: float, facing_left: bool,
		anim: int) -> void:
	_sx = clampf(x_frac, 0.0, 1.0)
	_sy = clampf(y_frac, 0.0, 1.0)
	_sface = facing_left
	_sanim = anim


func _process(delta: float) -> void:
	var now := Time.get_ticks_msec()
	for m in _client.poll():
		if dnd and (m.type == LanMsg.CHAT or m.type == LanMsg.ACTION):
			continue   # DND drops chatter/shots but keeps presence updates
		registry.on_message(m, now)
		if m.type == LanMsg.ACTION \
				and (m.target.is_empty() or m.target == self_id):
			if m.action == "shoot":
				shot_incoming.emit(registry.by_id(m.id))
			elif m.action == "whip":
				whipped_incoming.emit(registry.by_id(m.id))
	registry.prune(now)
	_sync_pets()

	_send_left -= delta
	if _send_left <= 0.0:
		_send_left = 1.0 / SEND_HZ
		_client.send(LanProtocol.encode_state(self_id, user_name, skin,
				_sx, _sy, _sface, _sanim))


## "@name message" DMs the peer with that display name; "/shoot" fires;
## other /commands style the bubble (ChatCommands). Port of CatApp.sendChat.
func broadcast_chat(raw: String) -> void:
	var text := raw
	var target := ""
	if raw.begins_with("@"):
		var sp := raw.find(" ")
		if sp > 1:
			var p := registry.by_name(raw.substr(1, sp - 1))
			if p != null:
				target = p.id
				text = raw.substr(sp + 1).strip_edges()
	_send_chat(text, target)


## DM straight to one peer (remote pet "Message …" menu).
func send_chat_to(target_peer: Peer, text: String) -> void:
	_send_chat(text, target_peer.id)


## Fire at one peer, or everyone when target_peer is null.
func shoot(target_peer: Peer) -> void:
	_shoot_id("" if target_peer == null else target_peer.id)


## Whip a peer: their pet gets knocked flying sideways, then walks back.
func whip(target_peer: Peer) -> void:
	_client.send(LanProtocol.encode_action(self_id, user_name, "whip",
			"" if target_peer == null else target_peer.id))


## Overlay-local rects the integrator merges into the passthrough polygon so
## remote pets stay right-clickable.
func pet_hit_rects() -> Array[Rect2]:
	var out: Array[Rect2] = []
	for p: RemotePet in _pets.values():
		out.append(p.hit_rect())
	return out


## The RemotePet child for a peer id, or null (e.g. DND / same host).
func pet_for(peer_id: String) -> RemotePet:
	return _pets.get(peer_id)


func _send_chat(text: String, target_id: String) -> void:
	if text.strip_edges().to_lower().begins_with("/shoot"):
		_shoot_id(target_id)
		return
	if text.strip_edges().to_lower().begins_with("/whip"):
		_client.send(LanProtocol.encode_action(self_id, user_name, "whip",
				target_id))
		return
	var p := ChatCommands.parse(text)
	if (p.text as String).is_empty():
		return
	_client.send(LanProtocol.encode_chat(self_id, user_name, p.text,
			target_id, p.scale, p.effect, p.color_hex))
	chat_own_bubble.emit(p)


func _shoot_id(target_id: String) -> void:
	_client.send(LanProtocol.encode_action(self_id, user_name, "shoot",
			target_id))
	shot_fired.emit(null if target_id.is_empty()
			else registry.by_id(target_id))


## One RemotePet child per live, remote-machine peer (same-host instances
## and DND get none). Peers pruned or gone → child freed.
func _sync_pets() -> void:
	var live := {}
	if not dnd:
		for peer: Peer in registry.peers():
			if peer.same_host:
				continue
			live[peer.id] = true
			if not _pets.has(peer.id):
				var pet := RemotePet.new()
				pet.peer = peer
				pet.peer_alpha = peer_alpha
				pet.menu_requested.connect(
						func(p: Peer, sp: Vector2) -> void:
							peer_menu_requested.emit(p, sp))
				add_child(pet)
				_pets[peer.id] = pet
	for id in _pets.keys():
		if not live.has(id):
			_pets[id].queue_free()
			_pets.erase(id)


func _clear_pets() -> void:
	for p: RemotePet in _pets.values():
		p.queue_free()
	_pets.clear()


## Random 128-bit id (Java: UUID.randomUUID().toString()).
static func _uuid() -> String:
	randomize()
	return "%08x-%08x-%08x-%08x" % [randi(), randi(), randi(), randi()]
