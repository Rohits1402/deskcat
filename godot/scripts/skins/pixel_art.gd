class_name PixelArt
extends RefCounted

## Sprite generation helpers, ported from the Java app's PixelArt.java.
## All sprites are authored as character maps or procedural ellipses so the
## whole app ships without binary assets. One char = one pixel; '.' (or space
## / any unmapped char) is transparent.

# K outline, O orange, D dark stripe, W warm white, P inner ear pink,
# N nose, R heart red, B zzz blue, G iris green
const PALETTE: Dictionary = {
	"K": Color("26202A"),
	"O": Color("F29A4B"),
	"D": Color("C9702E"),
	"W": Color("FFF4E3"),
	"P": Color("F2A3B3"),
	"N": Color("C75B77"),
	"R": Color("E5626E"),
	"B": Color("BFE3F2"),
	"G": Color("7CC46B"),
	"Y": Color("F9D848"),
	"M": Color("8C5A2B"),
	"Q": Color("E23B2E"),
	"T": Color("4FA8A0"),
	"F": Color("E8542F"),
	"A": Color("8CCFE8"),
	"H": Color("A9784C"),
	"C": Color("F2E3B8"),
}


## Character-map grid -> Image. Every row must be the same length.
## '.'/' '/unmapped chars stay transparent (Image.create starts fully
## transparent, so those cells are simply not written).
static func from_map(rows: PackedStringArray, palette: Dictionary = PALETTE) -> Image:
	var h: int = rows.size()
	var w: int = rows[0].length()
	for i in h:
		assert(rows[i].length() == w,
				"map row %d is %d chars, expected %d" % [i, rows[i].length(), w])
	var img: Image = Image.create(w, h, false, Image.FORMAT_RGBA8)
	for y in h:
		for x in w:
			var c: String = rows[y][x]
			if palette.has(c):
				img.set_pixel(x, y, palette[c])
	return img


## 1x1 white pixel, tint + scale it to draw eyes and rectangles.
static func pixel() -> ImageTexture:
	var img: Image = Image.create(1, 1, false, Image.FORMAT_RGBA8)
	img.set_pixel(0, 0, Color.WHITE)
	return to_texture(img)


# --- procedural sprite builder -------------------------------------------
# Bodies built from overlapping ellipses with an automatic outline pass —
# the same construction as the Java app's approved preview art, so shapes
# stay round. The grid is a flat PackedInt32Array of rgba32 ints (0 = empty),
# gw columns wide.

## Fill an ellipse into grid g (gw columns). color is Color.to_rgba32().
static func ell(g: PackedInt32Array, gw: int, cx: float, cy: float,
		rx: float, ry: float, color: int) -> void:
	var gh: int = g.size() / gw
	for y in gh:
		for x in gw:
			var dx: float = (x - cx) / rx
			var dy: float = (y - cy) / ry
			if dx * dx + dy * dy <= 1.0:
				g[y * gw + x] = color


## Recolor every filled cell that touches an empty cell (4-neighbourhood)
## or the grid border with the outline color.
static func outline_pass(g: PackedInt32Array, gw: int, outline: int) -> void:
	var gh: int = g.size() / gw
	var edge: PackedByteArray = PackedByteArray()
	edge.resize(g.size())
	for y in gh:
		for x in gw:
			if g[y * gw + x] == 0:
				continue
			if x == 0 or y == 0 or x == gw - 1 or y == gh - 1 \
					or g[y * gw + x - 1] == 0 or g[y * gw + x + 1] == 0 \
					or g[(y - 1) * gw + x] == 0 or g[(y + 1) * gw + x] == 0:
				edge[y * gw + x] = 1
	for i in g.size():
		if edge[i] == 1:
			g[i] = outline


## Procedural grid -> Image (0 cells stay transparent).
static func grid_to_image(g: PackedInt32Array, gw: int) -> Image:
	var gh: int = g.size() / gw
	var img: Image = Image.create(gw, gh, false, Image.FORMAT_RGBA8)
	for y in gh:
		for x in gw:
			var v: int = g[y * gw + x]
			if v != 0:
				img.set_pixel(x, y, Color.hex(v))
	return img


## Image -> ImageTexture. Godot 4 has no per-texture filter setting: for
## crisp pixels set `texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST` on
## the CanvasItem (Sprite2D etc.) that draws this texture, or project-wide
## via rendering/textures/canvas_textures/default_texture_filter = Nearest.
static func to_texture(img: Image) -> ImageTexture:
	return ImageTexture.create_from_image(img)
