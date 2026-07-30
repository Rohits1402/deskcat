class_name PeerRegistry
## Tracks live peers from decoded LAN messages. Pure logic (time is injected)
## so it is unit-testable; call it only from the main thread.

const TIMEOUT_MS := 10000

var _self_id: String
# id -> Peer; Godot Dictionaries preserve insertion order like LinkedHashMap.
var _peers: Dictionary = {}


func _init(self_id: String) -> void:
	_self_id = self_id


## Applies one message. Returns the peer if it is new, else null.
func on_message(m: LanMsg, now_ms: int) -> Peer:
	if m == null or m.id == _self_id:
		return null
	if m.type == LanMsg.BYE:
		_peers.erase(m.id)
		return null
	var p: Peer = _peers.get(m.id)
	var is_new := p == null
	if is_new:
		p = Peer.new(m.id)
		_peers[m.id] = p
	p.last_seen_ms = now_ms
	p.same_host = p.same_host or m.same_host   # sticky
	if m.type == LanMsg.STATE:
		p.name = m.name
		p.skin = m.skin
		p.x_frac = m.x_frac
		p.y_frac = m.y_frac
		p.facing_left = m.facing_left
		p.anim = m.anim
	elif m.type == LanMsg.CHAT:
		p.name = m.name
		# show broadcasts and DMs addressed to me; ignore others' DMs
		if m.target.is_empty() or m.target == _self_id:
			p.bubble.show(m.text, now_ms, m.chat_scale, m.chat_effect,
					m.chat_color)
	return p if is_new else null


## Drops silent peers; returns the removed ids.
func prune(now_ms: int) -> Array[String]:
	var removed: Array[String] = []
	for pid in _peers.keys():
		if now_ms - _peers[pid].last_seen_ms > TIMEOUT_MS:
			removed.append(pid)
			_peers.erase(pid)
	return removed


func by_id(id: String) -> Peer:
	return _peers.get(id)


func by_name(name: String) -> Peer:
	for p: Peer in _peers.values():
		if p.name.nocasecmp_to(name) == 0:
			return p
	return null


func peers() -> Array:
	return _peers.values()
