class_name ChatCommands
## Chat-message commands, chainable at the start of a message (port of
## com.deskcat.ChatCommands — behavior must match; tests mirror the Java ones):
##
##   /big /huge /small /size N   — bubble text scale (N = font px, 16 = normal)
##   /shake                      — jittering text
##   /rainbow                    — hue-cycling text
##   /color red | /colour red    — named text color
##
## e.g. "/big /shake /color red hello". Malformed or unknown commands leave
## the WHOLE remaining message as plain text (Java's break-out-of-loop).

const MIN_SCALE := 0.5
## Effectively uncapped — the screen size is the real limit.
const MAX_SCALE := 64.0
## Font px treated as scale 1.0 in "/size N".
const BASE_SIZE := 16.0

const EFFECT_NONE := 0
const EFFECT_SHAKE := 1
const EFFECT_RAINBOW := 2

## Same hexes as the Java COLORS map.
const COLORS := {
	"red": "E5312E",
	"orange": "F28C28",
	"yellow": "E8C400",
	"green": "3FA34D",
	"cyan": "1FA8A8",
	"blue": "2E6BE5",
	"purple": "8C4FD1",
	"pink": "E066A6",
	"brown": "8C5A2B",
	"black": "26202A",
	"white": "FAFAFA",
	"gray": "808080",
	"grey": "808080",
}


static func clamp_scale(s: float) -> float:
	return clampf(s, MIN_SCALE, MAX_SCALE)


## Returns {text: String, scale: float, effect: int, color_hex: String}.
## color_hex is RRGGBB, or "" for the default text color.
static func parse(raw: String) -> Dictionary:
	var t := raw.strip_edges()
	var scale := 1.0
	var effect := EFFECT_NONE
	var color_hex := ""

	while t.begins_with("/"):
		var sp := t.find(" ")
		var cmd := (t if sp < 0 else t.substr(0, sp)).to_lower()
		var rest := "" if sp < 0 else t.substr(sp + 1).strip_edges()

		if cmd == "/big":
			scale = 1.6
		elif cmd == "/huge":
			scale = 2.2
		elif cmd == "/small":
			scale = 0.65
		elif cmd == "/shake":
			effect = EFFECT_SHAKE
		elif cmd == "/rainbow":
			effect = EFFECT_RAINBOW
		elif cmd == "/size":
			var sp2 := rest.find(" ")
			var num := rest if sp2 < 0 else rest.substr(0, sp2)
			if not num.is_valid_float():
				break   # "/size notanumber ..." — treat as plain text
			scale = clamp_scale(num.to_float() / BASE_SIZE)
			rest = "" if sp2 < 0 else rest.substr(sp2 + 1).strip_edges()
		elif cmd == "/color" or cmd == "/colour":
			var sp2 := rest.find(" ")
			var cname := (rest if sp2 < 0 else rest.substr(0, sp2)).to_lower()
			if not COLORS.has(cname):
				break   # unknown color — treat as plain text
			color_hex = COLORS[cname]
			rest = "" if sp2 < 0 else rest.substr(sp2 + 1).strip_edges()
		else:
			break       # unknown command — leave the message as typed
		t = rest

	return {text = t, scale = scale, effect = effect, color_hex = color_hex}
