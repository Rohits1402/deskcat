class_name ParticleArt
extends RefCounted

## Particle/overlay sprites, ported verbatim from the Java app's
## PixelArt.java. Colors come from PixelArt.PALETTE. There is no "dust"
## sprite — the Java app has none; dust-like effects are drawn with the
## tinted 1x1 PixelArt.pixel() texture.

const HEART: PackedStringArray = [
	".KK.KK.",
	"KRRKRRK",
	"KRRRRRK",
	".KRRRK.",
	"..KRK..",
	"...K...",
]

const ZZZ: PackedStringArray = [
	"BBBB",
	"..B.",
	".B..",
	"BBBB",
]

const SPARK: PackedStringArray = [
	".Y.",
	"YWY",
	".Y.",
]

const WATER_DROP: PackedStringArray = [
	".A.",
	"AWA",
	".A.",
]

const NOTE: PackedStringArray = [
	"....K.",
	"....K.",
	"....K.",
	"....K.",
	".BBBK.",
	"BBBBK.",
	".BBB..",
]

const ALERT: PackedStringArray = [
	"RRR",
	"RRR",
	"RRR",
	"RRR",
	"RRR",
	"...",
	"RRR",
	"RRR",
]


static func heart() -> ImageTexture:
	return PixelArt.to_texture(PixelArt.from_map(HEART))


static func zzz() -> ImageTexture:
	return PixelArt.to_texture(PixelArt.from_map(ZZZ))


static func spark() -> ImageTexture:
	return PixelArt.to_texture(PixelArt.from_map(SPARK))


static func water_drop() -> ImageTexture:
	return PixelArt.to_texture(PixelArt.from_map(WATER_DROP))


static func note() -> ImageTexture:
	return PixelArt.to_texture(PixelArt.from_map(NOTE))


static func alert() -> ImageTexture:
	return PixelArt.to_texture(PixelArt.from_map(ALERT))
