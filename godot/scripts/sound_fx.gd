## Port of SoundFx.java — tiny procedural chiptune-ish one-shots synthesized
## at first use; no audio assets (everything-is-code principle).
##
## Same waveforms, frequencies, envelopes and durations as the Java build,
## including a faithful java.util.Random so the noise-based sounds come out
## sample-identical.
##
##   SoundFx.play(&"chirp")   # soft mew-like sweep (startles, reminders)
##   SoundFx.play(&"zap")     # crackling descending buzz (thunderbolt)
##   SoundFx.play(&"splash")  # noisy burst with rising bubbles (water gun)
##   SoundFx.enabled = false  # global mute (session-only, like Java)
class_name SoundFx

const RATE := 22050

## The full sound set, matching Java's zap()/splash()/chirp().
const SOUNDS: Array[StringName] = [&"chirp", &"zap", &"splash"]

## Durations as Java float32 literals promoted to double, so the envelope
## math matches (int)(RATE * dur) and t/dur exactly.
const _CHIRP_DUR := 0.3499999940395355   # 0.35f
const _ZAP_DUR := 0.2199999988079071     # 0.22f
const _SPLASH_DUR := 0.4000000059604645  # 0.4f

## Exact Java sample counts: (int)(RATE * durF).
const CHIRP_SAMPLES := 7717
const ZAP_SAMPLES := 4850
const SPLASH_SAMPLES := 8820

## Global mute flag; play() becomes a no-op when false.
static var enabled := true

static var _cache: Dictionary = {}
static var _players: Array[AudioStreamPlayer] = []

const _MAX_PLAYERS := 6


## Plays a cached one-shot through a lazily grown AudioStreamPlayer pool
## attached to the scene tree root. Unknown ids warn and do nothing.
static func play(id: StringName) -> void:
	if not enabled:
		return
	var stream := build_stream(id)
	if stream == null:
		push_warning("SoundFx: unknown sound id '%s'" % id)
		return
	var p := _free_player()
	if p == null:
		return
	p.stream = stream
	p.play()


## Builds (once) and returns the AudioStreamWAV for a sound id, or null for
## unknown ids. 22050 Hz, 16-bit, mono — same PCM the Java AudioDevice got.
static func build_stream(id: StringName) -> AudioStreamWAV:
	if _cache.has(id):
		return _cache[id]
	var pcm := PackedByteArray()
	match id:
		&"chirp":
			pcm = _build_chirp()
		&"zap":
			pcm = _build_zap()
		&"splash":
			pcm = _build_splash()
		_:
			return null
	var wav := AudioStreamWAV.new()
	wav.format = AudioStreamWAV.FORMAT_16_BITS
	wav.mix_rate = RATE
	wav.stereo = false
	wav.data = pcm
	_cache[id] = wav
	return wav


static func _free_player() -> AudioStreamPlayer:
	var tree := Engine.get_main_loop() as SceneTree
	if tree == null or tree.root == null:
		return null
	for i in range(_players.size() - 1, -1, -1):
		if not is_instance_valid(_players[i]):
			_players.remove_at(i)
	for p in _players:
		if not p.playing:
			return p
	if _players.size() >= _MAX_PLAYERS:
		var oldest: AudioStreamPlayer = _players[0]
		oldest.stop()
		return oldest
	var p := AudioStreamPlayer.new()
	p.name = "SoundFx%d" % _players.size()
	_players.append(p)
	tree.root.add_child(p)
	return p


static func _to_s16(v: float) -> int:
	return clampi(int(v * 32767.0), -32768, 32767)


## Soft mew-like sweep: 550 Hz carrier bent +-380 Hz over a half-sine,
## 24 Hz warble, 20 ms attack, linear decay, 0.30 gain. 0.35 s.
static func _build_chirp() -> PackedByteArray:
	var n := CHIRP_SAMPLES
	var out := PackedByteArray()
	out.resize(n * 2)
	var phase := 0.0
	for i in n:
		var t := i / float(RATE)
		var f := 550.0 + 380.0 * sin(PI * t / _CHIRP_DUR) \
				+ 40.0 * sin(TAU * 24.0 * t)
		phase += TAU * f / RATE
		var env := minf(1.0, t / 0.02) * (1.0 - t / _CHIRP_DUR)
		out.encode_s16(i * 2, _to_s16(sin(phase) * env * 0.30))
	return out


## Crackling descending buzz: square wave falling 170 -> 30 Hz plus white
## noise, both fading with k = 1 - t/dur (noise as k^2 * 0.35, buzz as
## k * 0.18). Random seed 7. 0.22 s.
static func _build_zap() -> PackedByteArray:
	var n := ZAP_SAMPLES
	var out := PackedByteArray()
	out.resize(n * 2)
	var r := _JavaRandom.new(7)
	var phase := 0.0
	for i in n:
		var t := i / float(RATE)
		var k := 1.0 - t / _ZAP_DUR
		var f := 30.0 + 140.0 * k
		phase += TAU * f / RATE
		var noise := (r.next_double() * 2.0 - 1.0) * k * k * 0.35
		var buzz := signf(sin(phase)) * k * 0.18
		out.encode_s16(i * 2, _to_s16(noise + buzz))
	return out


## Noisy burst (0.16 gain) with three rising bubble chirps starting at
## 0 / 0.1 / 0.18 s, each sweeping 250 -> 800 Hz over 0.12 s at 0.14 gain.
## Random seed 11. 0.4 s.
static func _build_splash() -> PackedByteArray:
	var n := SPLASH_SAMPLES
	var out := PackedByteArray()
	out.resize(n * 2)
	var r := _JavaRandom.new(11)
	var starts: Array[float] = [0.0, 0.1, 0.18]
	for i in n:
		var t := i / float(RATE)
		var env := minf(1.0, t / 0.02) * (1.0 - t / _SPLASH_DUR)
		var v := (r.next_double() * 2.0 - 1.0) * env * 0.16
		for st in starts:
			var bt := t - st
			if bt >= 0.0 and bt < 0.12:
				var f := 250.0 + 550.0 * (bt / 0.12)
				var benv := 1.0 - bt / 0.12
				v += sin(TAU * f * bt) * benv * 0.14
		out.encode_s16(i * 2, _to_s16(v))
	return out


## Exact java.util.Random (48-bit LCG) so zap/splash noise matches the Java
## PCM bit-for-bit. Godot ints are 64-bit and wrap, which is all this needs.
class _JavaRandom:
	const _MASK := 0xFFFFFFFFFFFF  # 2^48 - 1
	var _seed: int

	func _init(s: int) -> void:
		_seed = (s ^ 0x5DEECE66D) & _MASK

	func _next(bits: int) -> int:
		_seed = (_seed * 0x5DEECE66D + 0xB) & _MASK
		return _seed >> (48 - bits)

	func next_double() -> float:
		return float((_next(26) << 27) + _next(27)) / 9007199254740992.0
