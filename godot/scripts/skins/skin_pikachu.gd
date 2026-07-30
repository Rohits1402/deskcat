class_name SkinPikachu
extends RefCounted

## The Pikachu skin, ported verbatim from the Java app's PixelArt.java /
## SkinAssets.java. Personal-use fan art — must be stripped before any
## public release (see project CLAUDE.md).

# Sitting Pikachu, front view, 28x27. Eye area is left plain yellow;
# bead eyes with a white glint are drawn at runtime.
const BODY: PackedStringArray = [
	".KK......................KK.",
	".KMMK..................KMMK.",
	".KMMMK................KMMMK.",
	"..KMYYK..............KYYMK..",
	"..KYYYK..............KYYYK..",
	"...KYYYK............KYYYK...",
	"...KYYYYK..........KYYYYK...",
	"....KYYYYKKKKKKKKKKYYYYK....",
	"...KYYYYYYYYYYYYYYYYYYYYK...",
	"..KYYYYYYYYYYYYYYYYYYYYYYK..",
	"..KYYYYYYYYYYYYYYYYYYYYYYK..",
	"..KYYYYYYYYYYKKYYYYYYYYYYK..",
	"..KYYYYYYYYYYYYYYYYYYYYYYK..",
	"..KQQQYYYYYYYYYYYYYYYYQQQK..",
	"..KQQQYYYYYYYYYYYYYYYYQQQK..",
	"..KQQQYYYYYYYYYYYYYYYYQQQK..",
	"...KYYYYYYYYYYYYYYYYYYYYK...",
	"....KKYYYYYYYYYYYYYYYYKK....",
	"....KYYYYYYYYYYYYYYYYYYK....",
	"...KYYYYYYYYYYYYYYYYYYYYK...",
	"...KYKYYYYYYYYYYYYYYYYKYK...",
	"...KYKYYYYYYYYYYYYYYYYKYK...",
	"...KYYYYYYYYYYYYYYYYYYYYK...",
	"...KYYYYYYYYYYYYYYYYYYYYK...",
	"..KYYYYKYYYYYYYYYYYYKYYYYK..",
	"..KYYYYKYYYYYYYYYYYYKYYYYK..",
	"..KKKKKKKKKKKKKKKKKKKKKKKK..",
]

# Lightning-bolt tail, 12x14, three wag frames (upright / lean left / lean right)
const TAIL_A: PackedStringArray = [
	"......KKKKK.",
	".....KYYYYK.",
	"....KYYYYK..",
	"...KYYYYK...",
	"...KYYYYYK..",
	"....KYYYYYK.",
	".....KYYYYK.",
	"....KYYYYK..",
	"...KYYYYK...",
	"..KYYYYK....",
	"..KMMYK.....",
	".KMMMK......",
	".KMMK.......",
	".KKK........",
]

const TAIL_B: PackedStringArray = [
	".....KKKKK..",
	"....KYYYYK..",
	"...KYYYYK...",
	"..KYYYYK....",
	"..KYYYYYK...",
	"...KYYYYYK..",
	"....KYYYYK..",
	"....KYYYYK..",
	"...KYYYYK...",
	"..KYYYYK....",
	"..KMMYK.....",
	".KMMMK......",
	".KMMK.......",
	".KKK........",
]

const TAIL_C: PackedStringArray = [
	".......KKKKK",
	"......KYYYYK",
	".....KYYYYK.",
	"....KYYYYK..",
	"....KYYYYYK.",
	".....KYYYYYK",
	"......KYYYYK",
	"....KYYYYK..",
	"...KYYYYK...",
	"..KYYYYK....",
	"..KMMYK.....",
	".KMMMK......",
	".KMMK.......",
	".KKK........",
]

const PAW: PackedStringArray = [
	".KK.",
	"KYYK",
	"KYYK",
	".KK.",
]


static func build() -> PetSkin:
	var s: PetSkin = PetSkin.new()
	s.eye_style = PetSkin.EYE_SOLID_BEAD
	s.attack_type = PetSkin.ATTACK_THUNDERBOLT
	var body_img: Image = PixelArt.from_map(BODY)
	s.body = PixelArt.to_texture(body_img)
	s.body_size = Vector2i(body_img.get_width(), body_img.get_height())
	s.tail = [
		PixelArt.to_texture(PixelArt.from_map(TAIL_A)),
		PixelArt.to_texture(PixelArt.from_map(TAIL_B)),
		PixelArt.to_texture(PixelArt.from_map(TAIL_C)),
	]
	s.paw = PixelArt.to_texture(PixelArt.from_map(PAW))
	s.tail_x = 27
	s.eye_lx = 9
	s.eye_rx = 20
	s.eye_w = 3
	s.eye_h = 3
	s.eye_y = 15
	s.eye_center = Vector2(15.5, 16.0)
	s.fur_color = Color("F9D848")
	s.iris_color = Color("7CC46B")
	return s
