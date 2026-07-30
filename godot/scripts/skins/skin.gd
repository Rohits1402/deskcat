class_name PetSkin
extends RefCounted

## Per-skin textures and geometry, ported from the Java app's SkinAssets.java.
## Eye/tail coordinates are in body-texture pixel space (top-left origin),
## identical to the Java constants so the pets look the same.

# eye styles
const EYE_IRIS_ON_WHITE: int = 0	# cat: iris on white patch
const EYE_SOLID_BEAD: int = 1		# pikachu: solid bead with glint
const EYE_OUTLINED_BLOCK: int = 2	# squirtle: outlined iris block
const EYE_BAKED: int = 3			# image skins: eyes are part of the art,
									# renderer must skip dynamic eyes/blink

# Kenney "Animal Pack" image skins (godot/assets/kenney/animals/, CC0).
const KENNEY_DIR: String = "res://assets/kenney/animals/"
const KENNEY_ANIMALS: PackedStringArray = [
	"penguin", "pig", "monkey", "snake",
	"panda", "rabbit", "parrot", "elephant",
]

## Rendered pet height target (window px at Small size) for image skins;
## px_scale_override is derived from it so ~300-370 px art lands ~120 px tall.
const IMAGE_TARGET_H: float = 120.0

# attack kinds (click attack)
const ATTACK_STARTLE: int = 0		# cat: startle only
const ATTACK_THUNDERBOLT: int = 1	# pikachu
const ATTACK_WATER_GUN: int = 2		# squirtle

var body: ImageTexture
var tail: Array[ImageTexture] = []	# 3 wag frames
var paw: ImageTexture
var body_size: Vector2i				# body texture pixel dimensions

var eye_style: int = EYE_IRIS_ON_WHITE
var attack_type: int = ATTACK_STARTLE
var display_name: String = ""

## Pixels-per-art-pixel at Small size for this skin; 0 = renderer default
## (PX_BASE). Image skins set a fractional value to shrink large PNGs.
var px_scale_override: float = 0.0

var tail_x: int = 0					# tail anchor x on the body
var eye_lx: int = 0					# left eye rect x
var eye_rx: int = 0					# right eye rect x
var eye_w: int = 0					# eye rect width
var eye_h: int = 0					# eye rect height
var eye_y: int = 0					# eye rect y (top)
var eye_center: Vector2 = Vector2.ZERO	# gaze pivot between the eyes

var fur_color: Color = Color.WHITE
var iris_color: Color = Color.WHITE


## Factory for "cat", "pikachu", "squirtle" plus "kenney:<animal>" image
## skins. Unknown names fall back to the cat, matching the Java
## SkinAssets.load() else-branch.
static func load(skin_name: String) -> PetSkin:
	var n: String = skin_name.to_lower()
	if n.begins_with("kenney:"):
		var animal: String = n.substr(7)
		if KENNEY_ANIMALS.has(animal):
			return from_image(KENNEY_DIR + animal + ".png", animal.capitalize())
		return SkinCat.build()
	match n:
		"squirtle":
			return SkinSquirtle.build()
		"pikachu":
			return SkinPikachu.build()
		_:
			return SkinCat.build()


## Build a skin from a single ready-made PNG (eyes and all baked into the
## art). No tail frames, startle-only attack; px_scale_override shrinks the
## large source image so the pet renders ~IMAGE_TARGET_H px tall at Small.
static func from_image(path: String, p_display_name: String) -> PetSkin:
	var tex: Texture2D = ResourceLoader.load(path)
	if tex == null:
		push_error("PetSkin.from_image: cannot load %s" % path)
		return SkinCat.build()
	var img: Image = tex.get_image()
	var s: PetSkin = PetSkin.new()
	s.display_name = p_display_name
	s.body = ImageTexture.create_from_image(img)
	s.body_size = Vector2i(img.get_width(), img.get_height())
	s.eye_style = EYE_BAKED
	s.attack_type = ATTACK_STARTLE
	s.px_scale_override = IMAGE_TARGET_H / float(img.get_height())
	return s


## Every selectable skin: the 3 generated pixel skins plus the Kenney image
## skins. Each entry: {id: String, label: String, kind: "pixel"|"image"};
## id goes straight back into PetSkin.load().
static func catalog() -> Array[Dictionary]:
	var out: Array[Dictionary] = [
		{"id": "cat", "label": "Cat", "kind": "pixel"},
		{"id": "pikachu", "label": "Pikachu", "kind": "pixel"},
		{"id": "squirtle", "label": "Squirtle", "kind": "pixel"},
	]
	for animal in KENNEY_ANIMALS:
		out.append({
			"id": "kenney:" + animal,
			"label": animal.capitalize(),
			"kind": "image",
		})
	return out
