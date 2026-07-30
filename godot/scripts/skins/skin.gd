class_name PetSkin
extends RefCounted

## Per-skin textures and geometry, ported from the Java app's SkinAssets.java.
## Eye/tail coordinates are in body-texture pixel space (top-left origin),
## identical to the Java constants so the pets look the same.

# eye styles
const EYE_IRIS_ON_WHITE: int = 0	# cat: iris on white patch
const EYE_SOLID_BEAD: int = 1		# pikachu: solid bead with glint
const EYE_OUTLINED_BLOCK: int = 2	# squirtle: outlined iris block

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

var tail_x: int = 0					# tail anchor x on the body
var eye_lx: int = 0					# left eye rect x
var eye_rx: int = 0					# right eye rect x
var eye_w: int = 0					# eye rect width
var eye_h: int = 0					# eye rect height
var eye_y: int = 0					# eye rect y (top)
var eye_center: Vector2 = Vector2.ZERO	# gaze pivot between the eyes

var fur_color: Color = Color.WHITE
var iris_color: Color = Color.WHITE


## Factory for "cat", "pikachu", "squirtle". Unknown names fall back to the
## cat, matching the Java SkinAssets.load() else-branch.
static func load(skin_name: String) -> PetSkin:
	match skin_name.to_lower():
		"squirtle":
			return SkinSquirtle.build()
		"pikachu":
			return SkinPikachu.build()
		_:
			return SkinCat.build()
