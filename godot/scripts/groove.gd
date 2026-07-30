## Port of the music-groove behavior from CatApp.java (SystemAudio.peak()
## is replaced by NativeBridge.audio_peak() — peak LEVEL only, never audio
## samples; see CLAUDE.md).
##
## Java mapping being replicated:
##   - poll cadence 0.2 s (the helper streamed one float per 200 ms)
##   - musicAmp  = lerp(musicAmp, min(1, peak * 6), 0.25) each frame
##   - music ON  when peak > 0.02 for more than 0.6 s
##   - music OFF when peak < 0.005 for more than 2.5 s
##   - while dancing: bob = |sin(t*7)| * 1.2 * amp, waddle = sin(t*7) * 3.5
##     * amp, with amp = 0.3 + 0.7 * musicAmp
##
## Each `beat` is one bob hump (a half-period of sin(t*7), ~0.449 s) and
## strength is that amp (0.3..1.0). Integration: the pet consumes
## beat(strength) by kicking its squash spring / hopping scaled by strength,
## and can read `level` for continuous sway. The integrator should only
## dance when idle (Java gated on: not sleeping/dragging/wandering/attacking
## /stretching/kneading) — gate that on the pet side or via `enabled`.
class_name Groove
extends Node

signal beat(strength: float)

const POLL_EVERY := 0.2
const PEAK_GAIN := 6.0
const SMOOTH := 0.25
const ON_PEAK := 0.02
const ON_AFTER := 0.6
const OFF_PEAK := 0.005
const OFF_AFTER := 2.5
const BOB_RATE := 7.0
const BOB_BASE := 0.3
const BOB_SPAN := 0.7

var enabled := true

## Smoothed loudness 0..1 (Java's musicAmp).
var level := 0.0

## True while music is considered playing (Java's musicOn hysteresis).
var music_on := false

var _peak := 0.0
var _poll_in := 0.0
var _above := 0.0
var _below := 0.0
var _phase := 0.0


func _process(delta: float) -> void:
	if not enabled:
		level = lerpf(level, 0.0, SMOOTH)
		music_on = false
		_above = 0.0
		_below = 0.0
		_phase = 0.0
		return
	_poll_in -= delta
	if _poll_in <= 0.0:
		_poll_in += POLL_EVERY
		_peak = NativeBridge.audio_peak()
	level = lerpf(level, minf(1.0, _peak * PEAK_GAIN), SMOOTH)
	if not music_on:
		_above = _above + delta if _peak > ON_PEAK else 0.0
		if _above > ON_AFTER:
			music_on = true
			_below = 0.0
	else:
		_below = _below + delta if _peak < OFF_PEAK else 0.0
		if _below > OFF_AFTER:
			music_on = false
			_above = 0.0
	if music_on:
		var prev := _phase
		_phase += delta * BOB_RATE
		if int(prev / PI) != int(_phase / PI):
			beat.emit(BOB_BASE + BOB_SPAN * level)
	else:
		_phase = 0.0
