## Port of the stretch/water reminder logic from CatApp.java.
##
## Settings are deliberately session-only — nothing is persisted (project
## rule; same as the Java tray checkboxes).
##
## Integration: add one Reminders node, connect remind(kind, text) and show
## the text as pet speech (Java showed a tray balloon + a 4 s tall stretch
## pose / 3 s water-drop shower). Set `paused` true while the pet is being
## dragged — Java froze both countdowns during drags. A chirp is played here
## on fire, exactly like Java.
class_name Reminders
extends Node

signal remind(kind: StringName, text: String)

## Java defaults: STRETCH_EVERY = 30 * 60 s, WATER_EVERY = 45 * 60 s.
const STRETCH_MINUTES_DEFAULT := 30.0
const WATER_MINUTES_DEFAULT := 45.0

## Same texts as the Java notifyTray() calls.
const STRETCH_TITLE := "Stretch time"
const STRETCH_TEXT := "DeskCat is stretching - join it for a moment."
const WATER_TITLE := "Water break"
const WATER_TEXT := "Time to drink some water."

## Freeze countdowns (integrator sets this while dragging, like Java's
## `!dragging` guard).
var paused := false

## Toggling (either way) resets the countdown, matching the Java tray
## listener which re-armed the full interval on every click.
var stretch_enabled := false:
	set(v):
		stretch_enabled = v
		_stretch_in = stretch_minutes * 60.0

var water_enabled := false:
	set(v):
		water_enabled = v
		_water_in = water_minutes * 60.0

## Interval changes also re-arm the timer.
var stretch_minutes := STRETCH_MINUTES_DEFAULT:
	set(v):
		stretch_minutes = maxf(0.1, v)
		_stretch_in = stretch_minutes * 60.0

var water_minutes := WATER_MINUTES_DEFAULT:
	set(v):
		water_minutes = maxf(0.1, v)
		_water_in = water_minutes * 60.0

var _stretch_in := STRETCH_MINUTES_DEFAULT * 60.0
var _water_in := WATER_MINUTES_DEFAULT * 60.0


func _process(delta: float) -> void:
	if paused:
		return
	if stretch_enabled:
		_stretch_in -= delta
		if _stretch_in <= 0.0:
			_stretch_in = stretch_minutes * 60.0
			SoundFx.play(&"chirp")
			remind.emit(&"stretch", STRETCH_TEXT)
	if water_enabled:
		_water_in -= delta
		if _water_in <= 0.0:
			_water_in = water_minutes * 60.0
			SoundFx.play(&"chirp")
			remind.emit(&"water", WATER_TEXT)
