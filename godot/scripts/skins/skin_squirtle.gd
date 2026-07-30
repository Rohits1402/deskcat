class_name SkinSquirtle
extends RefCounted

## The Squirtle skin, ported faithfully from the Java app's PixelArt.java /
## SkinAssets.java. Built procedurally from overlapping ellipses + outline
## pass (not a character map). Personal-use fan art — must be stripped
## before any public release (see project CLAUDE.md).

const PAW: PackedStringArray = [
	".KK.",
	"KAAK",
	"KAAK",
	".KK.",
]


## Sitting Squirtle, 30x27, same shapes as the approved preview.
static func body_image() -> Image:
	var k: int = PixelArt.PALETTE["K"].to_rgba32()
	var a: int = PixelArt.PALETTE["A"].to_rgba32()
	var h: int = PixelArt.PALETTE["H"].to_rgba32()
	var c: int = PixelArt.PALETTE["C"].to_rgba32()
	var gw: int = 30
	var g: PackedInt32Array = PackedInt32Array()
	g.resize(gw * 27)
	PixelArt.ell(g, gw, 15.0, 19.5, 9.0, 6.5, h)		# shell
	PixelArt.ell(g, gw, 5.5, 18.5, 2.5, 3.0, a)			# arms
	PixelArt.ell(g, gw, 24.5, 18.5, 2.5, 3.0, a)
	PixelArt.ell(g, gw, 15.0, 21.0, 5.5, 5.3, c)		# plastron, down to the ground
	PixelArt.ell(g, gw, 9.5, 24.5, 3.0, 2.0, a)			# feet on top of it
	PixelArt.ell(g, gw, 20.5, 24.5, 3.0, 2.0, a)
	PixelArt.ell(g, gw, 15.0, 9.5, 8.5, 7.0, a)			# head
	PixelArt.outline_pass(g, gw, k)
	for x in range(12, 18):
		g[13 * gw + x] = k								# smile
	g[12 * gw + 11] = k
	g[12 * gw + 18] = k
	return PixelArt.grid_to_image(g, gw)


static func tail_frame_image(ox: float, oy: float) -> Image:
	var k: int = PixelArt.PALETTE["K"].to_rgba32()
	var a: int = PixelArt.PALETTE["A"].to_rgba32()
	var gw: int = 12
	var g: PackedInt32Array = PackedInt32Array()
	g.resize(gw * 12)
	PixelArt.ell(g, gw, 2.5, 9.0, 3.0, 2.6, a)			# root, hidden by body
	PixelArt.ell(g, gw, 5.5 + ox, 5.5 + oy, 3.2, 3.0, a)	# curl
	PixelArt.outline_pass(g, gw, k)
	var cx: int = int(roundf(5.5 + ox))
	var cy: int = int(roundf(5.5 + oy))
	g[cy * gw + cx] = k									# spiral hint
	g[cy * gw + cx + 1] = k
	return PixelArt.grid_to_image(g, gw)


static func build() -> PetSkin:
	var s: PetSkin = PetSkin.new()
	s.eye_style = PetSkin.EYE_OUTLINED_BLOCK
	s.attack_type = PetSkin.ATTACK_WATER_GUN
	var body_img: Image = body_image()
	s.body = PixelArt.to_texture(body_img)
	s.body_size = Vector2i(body_img.get_width(), body_img.get_height())
	# three wag frames for the curly tail
	s.tail = [
		PixelArt.to_texture(tail_frame_image(0.0, 0.0)),
		PixelArt.to_texture(tail_frame_image(0.9, -0.7)),
		PixelArt.to_texture(tail_frame_image(-0.6, 0.8)),
	]
	s.paw = PixelArt.to_texture(PixelArt.from_map(PAW))
	s.tail_x = 26
	s.eye_lx = 11
	s.eye_rx = 20
	s.eye_w = 3
	s.eye_h = 4
	s.eye_y = 16
	s.eye_center = Vector2(16.5, 17.5)
	s.fur_color = Color("8CCFE8")
	s.iris_color = Color("5B3A26")
	return s
