## Sanity checks for the SoundFx port. Same run() pattern as
## test_lan_protocol.gd: no framework, run() returns [passed, failed].

static var _passed := 0
static var _failed := 0

## Exact Java sample counts: (int)(22050 * durF) for 0.35f / 0.22f / 0.4f.
const _EXPECTED_SAMPLES := {
	&"chirp": 7717,
	&"zap": 4850,
	&"splash": 8820,
}


static func run() -> Array[int]:
	_passed = 0
	_failed = 0
	_each_sound_builds_expected_pcm()
	_streams_are_cached()
	_unknown_id_is_null()
	_muted_play_is_noop()
	return [_passed, _failed]


static func _check(cond: bool, what: String) -> void:
	if cond:
		_passed += 1
	else:
		_failed += 1
		push_error("FAIL sound_fx: " + what)


static func _each_sound_builds_expected_pcm() -> void:
	_check(SoundFx.SOUNDS.size() == 3, "sound set has 3 ids")
	for id in SoundFx.SOUNDS:
		var wav := SoundFx.build_stream(id)
		_check(wav != null, "%s builds" % id)
		if wav == null:
			continue
		_check(wav.format == AudioStreamWAV.FORMAT_16_BITS,
				"%s is 16-bit" % id)
		_check(wav.mix_rate == SoundFx.RATE, "%s mix rate 22050" % id)
		_check(not wav.stereo, "%s is mono" % id)
		var n: int = _EXPECTED_SAMPLES[id]
		_check(wav.data.size() == n * 2,
				"%s sample count %d (got %d bytes)"
				% [id, n, wav.data.size()])
		_check(_max_abs_sample(wav.data) > 1000, "%s is not silent" % id)


static func _max_abs_sample(data: PackedByteArray) -> int:
	var peak := 0
	var i := 0
	while i < data.size():
		peak = maxi(peak, absi(data.decode_s16(i)))
		i += 2
	return peak


static func _streams_are_cached() -> void:
	var a := SoundFx.build_stream(&"chirp")
	var b := SoundFx.build_stream(&"chirp")
	_check(a == b, "repeat build returns the cached stream")


static func _unknown_id_is_null() -> void:
	_check(SoundFx.build_stream(&"meow-nope") == null,
			"unknown id builds null")


static func _muted_play_is_noop() -> void:
	var was := SoundFx.enabled
	SoundFx.enabled = false
	SoundFx.play(&"chirp")  # must not throw or touch the tree
	SoundFx.enabled = was
	_check(true, "muted play is a no-op")
