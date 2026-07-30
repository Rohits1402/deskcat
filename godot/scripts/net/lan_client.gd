class_name LanClient
## UDP-multicast transport. Everything — presence, chat, DMs — rides the one
## multicast group; DMs are filtered by target id on the receiving side.
## Poll-based: the main loop calls poll() once per frame; no threads.
## Wire-compatible with the Java client (same group/port/format).

const GROUP := "239.42.10.7"
const PORT := 42107

var _self_id: String
var _udp: PacketPeerUDP = null
var _local_addrs: Dictionary = {}   # ip string -> true


func _init(self_id: String) -> void:
	_self_id = self_id


## Returns false when the socket can't be opened (no network, port taken).
## Multicast loopback is the OS default (on), matching the Java client's
## setLoopbackMode(false): two instances on one PC see each other.
func start() -> bool:
	_udp = PacketPeerUDP.new()
	if _udp.bind(PORT, "0.0.0.0", LanProtocol.MAX_PACKET) != OK:
		_udp = null
		return false
	# Join on every interface so delivery works regardless of routing.
	var joined := false
	for ifc in IP.get_local_interfaces():
		if _udp.join_multicast_group(GROUP, ifc["name"]) == OK:
			joined = true
	if not joined:
		_udp.close()
		_udp = null
		return false
	for a in IP.get_local_addresses():
		_local_addrs[a] = true
	return true


## Thread-safe not required — call from the main loop.
func send(encoded: String) -> void:
	if _udp == null:
		return
	_udp.set_dest_address(GROUP, PORT)
	_udp.put_packet(encoded.to_utf8_buffer())


## Drain everything received since last frame, decoded and self-filtered,
## with same_host set from the sender address.
func poll() -> Array[LanMsg]:
	var out: Array[LanMsg] = []
	if _udp == null:
		return out
	while _udp.get_available_packet_count() > 0:
		var data := _udp.get_packet()
		if data.size() > LanProtocol.MAX_PACKET:
			continue   # oversized datagram dropped (Java: fixed recv buffer)
		var sender := _udp.get_packet_ip()
		var m := LanProtocol.decode(data.get_string_from_utf8())
		if m == null or m.id == _self_id:
			continue
		m.same_host = _is_local(sender)
		out.append(m)
	return out


func close() -> void:
	if _udp == null:
		return
	_udp.set_dest_address(GROUP, PORT)
	_udp.put_packet(LanProtocol.encode_bye(_self_id).to_utf8_buffer())
	_udp.close()
	_udp = null


func _is_local(ip: String) -> bool:
	return ip.begins_with("127.") or ip == "::1" \
			or ip.begins_with("::ffff:127.") or _local_addrs.has(ip)
