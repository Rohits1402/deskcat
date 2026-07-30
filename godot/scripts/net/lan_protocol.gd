class_name LanProtocol
## Wire format: pipe-delimited fields with backslash escaping, prefixed with a
## magic+version field so foreign datagrams on the port are ignored.
##
##   DC1|S|id|name|skin|xFrac|yFrac|facing|anim
##   DC1|C|id|name|text|target|scale|effect|color
##   DC1|A|id|name|action|target
##   DC1|B|id
##
## The three CHAT style fields are optional on decode (older senders omit
## them), defaulting to scale 1 / no effect / default color.
## MUST stay byte-identical to the Java client (com.deskcat.net.LanProtocol).

const MAGIC := "DC1"
## Receive-buffer size; datagrams beyond this are truncated and dropped.
const MAX_PACKET := 8192


static func encode_state(id: String, name: String, skin: String,
		x_frac: float, y_frac: float, facing_left: bool, anim: int) -> String:
	return _join([MAGIC, LanMsg.STATE, id, name, skin,
			_num(x_frac), _num(y_frac),
			"1" if facing_left else "0", str(anim)])


## Empty target = broadcast (Java passes null; the wire field is "" either way).
static func encode_chat(id: String, name: String, text: String,
		target: String, scale: float = 1.0, effect: int = 0,
		color_hex: String = "") -> String:
	return _join([MAGIC, LanMsg.CHAT, id, name, text, target,
			_num(scale), str(effect), color_hex])


static func encode_action(id: String, name: String, action: String,
		target: String) -> String:
	return _join([MAGIC, LanMsg.ACTION, id, name, action, target])


static func encode_bye(id: String) -> String:
	return _join([MAGIC, LanMsg.BYE, id])


## Returns null for anything malformed or non-DeskCat.
static func decode(raw: Variant) -> LanMsg:
	if not (raw is String):
		return null
	var f := _split(raw)
	if f.size() < 3 or f[0] != MAGIC or f[1].length() != 1:
		return null
	var m := LanMsg.new()
	m.type = f[1]
	m.id = f[2]
	if m.id.is_empty():
		return null
	match m.type:
		LanMsg.STATE:
			if f.size() < 9:
				return null
			if not f[5].is_valid_float() or not f[6].is_valid_float() \
					or not f[8].is_valid_int():
				return null
			m.name = f[3]
			m.skin = f[4]
			m.x_frac = clampf(f[5].to_float(), 0.0, 1.0)
			m.y_frac = clampf(f[6].to_float(), 0.0, 1.0)
			m.facing_left = f[7] == "1"
			m.anim = f[8].to_int()
			return m
		LanMsg.CHAT:
			if f.size() < 6:
				return null
			m.name = f[3]
			m.text = f[4]
			m.target = f[5]
			if f.size() >= 9:
				if not f[6].is_valid_float() or not f[7].is_valid_int():
					return null
				m.chat_scale = f[6].to_float()
				m.chat_effect = f[7].to_int()
				m.chat_color = f[8]
			return m
		LanMsg.ACTION:
			if f.size() < 6:
				return null
			m.name = f[3]
			m.action = f[4]
			m.target = f[5]
			return m
		LanMsg.BYE:
			return m
	return null


## Java's Float.toString always includes a decimal point ("1.0", "0.25");
## match that so encodes stay byte-identical for typical values. Scientific
## notation (only possible for extreme values) is still parseable by Java.
static func _num(f: float) -> String:
	var s := str(f)
	if not ("." in s or "e" in s or "E" in s or "inf" in s or "nan" in s):
		s += ".0"
	return s


static func _join(fields: Array) -> String:
	var sb := ""
	for i in fields.size():
		if i > 0:
			sb += "|"
		var s: String = "" if fields[i] == null else str(fields[i])
		for c in s:
			if c == "\\":
				sb += "\\\\"
			elif c == "|":
				sb += "\\p"
			else:
				sb += c
	return sb


static func _split(raw: String) -> PackedStringArray:
	var out := PackedStringArray()
	var cur := ""
	var esc := false
	for c in raw:
		if esc:
			cur += "|" if c == "p" else c
			esc = false
		elif c == "\\":
			esc = true
		elif c == "|":
			out.append(cur)
			cur = ""
		else:
			cur += c
	out.append(cur)
	return out
