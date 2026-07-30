class_name SkinCat
extends RefCounted

## The cat skin, ported verbatim from the Java app's PixelArt.java /
## SkinAssets.java. Character maps are identical strings; palette lives in
## PixelArt.PALETTE.

# Sitting tabby, front view, 28x26. Eye whites are part of the map;
# iris/pupil/blink are drawn on top at runtime.
const BODY: PackedStringArray = [
	"............................",
	"...KK..............KK.......",
	"..KOOK............KOOK......",
	"..KOPOK..........KOPOK......",
	".KOOPPOK........KOPPOOK.....",
	".KOOOOOKKKKKKKKKKOOOOOK.....",
	".KOOOOOOOOOOOOOOOOOOOOK.....",
	"KODOOOOOOOOOOOOOOOOOODOK....",
	"KODOOWWWWOOOOOOOWWWWODOK....",
	"KOOOOWWWWOOOOOOOWWWWOOOK....",
	"KOOOOWWWWOOOOOOOWWWWOOOK....",
	"KOOOOOOOOOOKNNKOOOOOOOOK....",
	"KOOOOOOOOOKWWWWKOOOOOOOK....",
	".KOOOOOOOOKWWWWKOOOOOOK.....",
	".KOOOOOOOOOKKKKOOOOOOOK.....",
	"..KKOOOOOOOOOOOOOOOKKK......",
	"...KOOOOOOOOOOOOOOOK........",
	"...KOOWWOOOOOOOOOOOK........",
	"..KOOWWWWOOOOOOOOOOK........",
	"..KOOWWWWOOOOOOOOOOK........",
	"..KOOOWWOOOOOOOOOOOK........",
	"..KOOOOOOODOODOOOOOK........",
	"..KOOOOOOODOODOOOOOK........",
	"..KOWWOKOOOOOOKOWWOK........",
	"..KOWWOKOOOOOOKOWWOK........",
	"..KKKKKKKKKKKKKKKKKK........",
]

const TAIL_A: PackedStringArray = [
	"......KK..",
	".....KOOK.",
	".....KDOK.",
	".....KOOK.",
	"....KOOK..",
	"....KDOK..",
	"....KOOK..",
	"...KOOK...",
	"...KOOK...",
	"..KOOK....",
	".KOOK.....",
	".KKK......",
]

const TAIL_B: PackedStringArray = [
	"..........",
	"..........",
	"......KK..",
	".....KOOK.",
	".....KDOK.",
	"....KOOK..",
	"....KOOK..",
	"....KDOK..",
	"...KOOK...",
	"..KOOK....",
	".KOOK.....",
	".KKK......",
]

const TAIL_C: PackedStringArray = [
	"..........",
	"..........",
	"..........",
	"....KKK...",
	"...KOOOK..",
	"...KODOK..",
	"...KOOOK..",
	"...KOOK...",
	"..KOOK....",
	"..KOOK....",
	".KOOK.....",
	".KKK......",
]

const PAW: PackedStringArray = [
	".KK.",
	"KOOK",
	"KWWK",
	".KK.",
]


static func build() -> PetSkin:
	var s: PetSkin = PetSkin.new()
	s.eye_style = PetSkin.EYE_IRIS_ON_WHITE
	s.attack_type = PetSkin.ATTACK_STARTLE
	var body_img: Image = PixelArt.from_map(BODY)
	s.body = PixelArt.to_texture(body_img)
	s.body_size = Vector2i(body_img.get_width(), body_img.get_height())
	s.tail = [
		PixelArt.to_texture(PixelArt.from_map(TAIL_A)),
		PixelArt.to_texture(PixelArt.from_map(TAIL_B)),
		PixelArt.to_texture(PixelArt.from_map(TAIL_C)),
	]
	s.paw = PixelArt.to_texture(PixelArt.from_map(PAW))
	s.tail_x = 26
	s.eye_lx = 7
	s.eye_rx = 18
	s.eye_w = 4
	s.eye_h = 3
	s.eye_y = 15
	s.eye_center = Vector2(14.0, 16.0)
	s.fur_color = Color("F29A4B")
	s.iris_color = Color("7CC46B")
	return s
