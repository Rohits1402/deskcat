## 3D-in-overlay: a transparent SubViewport with its own Camera3D + lights,
## shown as a texture inside the 2D overlay. Loads a Kenney Cube Pets cat
## (CC0, see assets/kenney/) and plays its idle animation; falls back to a
## CSG mannequin if the model is missing.
extends SubViewportContainer

const MODEL_DIR := "res://assets/kenney/cube-pets/"

var rig: Node3D
var _t := 0.0
var _has_model := false


static func available_models() -> PackedStringArray:
	var out := PackedStringArray()
	var dir := DirAccess.open(MODEL_DIR)
	if dir:
		for f in dir.get_files():
			# exported builds list "x.glb.remap"; strip to the resource name
			var clean := f.trim_suffix(".remap").trim_suffix(".import")
			if clean.ends_with(".glb") and not out.has(clean):
				out.append(clean)
	return out


func _ready() -> void:
	stretch = true
	custom_minimum_size = Vector2(220, 220)
	size = custom_minimum_size

	var vp := SubViewport.new()
	vp.transparent_bg = true
	vp.msaa_3d = Viewport.MSAA_4X
	add_child(vp)

	var cam := Camera3D.new()
	cam.position = Vector3(0, 0.9, 2.6)
	cam.rotation_degrees = Vector3(-12, 0, 0)
	vp.add_child(cam)

	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-40, 30, 0)
	vp.add_child(sun)

	rig = Node3D.new()
	vp.add_child(rig)

	var models := available_models()
	if models.size() > 0:
		var file: String = models[randi() % models.size()]
		var scene: PackedScene = load(MODEL_DIR + file)
		if scene:
			var model := scene.instantiate()
			rig.add_child(model)
			_has_model = true
			_toonify(model)
			var anim: AnimationPlayer = model.find_child(
					"AnimationPlayer", true, false)
			if anim and anim.get_animation_list().size() > 0:
				var list := anim.get_animation_list()
				var pick: StringName = list[0]
				for anim_name in list:
					if String(anim_name).to_lower().contains("idle"):
						pick = anim_name
				anim.get_animation(pick).loop_mode = Animation.LOOP_LINEAR
				anim.play(pick)
	if not _has_model:
		_build_mannequin()


## Flat cartoon look: toon diffuse/specular on every material, keeping the
## original albedo/colormap texture.
func _toonify(model: Node) -> void:
	for mi in model.find_children("*", "MeshInstance3D", true, false):
		var mesh: Mesh = mi.mesh
		if mesh == null:
			continue
		for s in mesh.get_surface_count():
			var mat := mi.get_active_material(s)
			if mat is StandardMaterial3D:
				var toon: StandardMaterial3D = mat.duplicate()
				toon.diffuse_mode = BaseMaterial3D.DIFFUSE_TOON
				toon.specular_mode = BaseMaterial3D.SPECULAR_TOON
				toon.roughness = 1.0
				mi.set_surface_override_material(s, toon)


func _build_mannequin() -> void:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color("2e8b8b")

	var body := CSGCylinder3D.new()
	body.radius = 0.45
	body.height = 1.0
	body.material = mat
	body.position = Vector3(0, 0.5, 0)
	rig.add_child(body)

	var head := CSGSphere3D.new()
	head.radius = 0.42
	head.material = mat
	head.position = Vector3(0, 1.35, 0)
	rig.add_child(head)

	var eye_mat := StandardMaterial3D.new()
	eye_mat.albedo_color = Color.WHITE
	for side in [-1.0, 1.0]:
		var eye := CSGSphere3D.new()
		eye.radius = 0.09
		eye.material = eye_mat
		eye.position = Vector3(side * 0.16, 1.42, 0.36)
		rig.add_child(eye)


func _process(delta: float) -> void:
	_t += delta
	rig.rotation.y = sin(_t * 0.9) * 0.8
	if not _has_model:   # model animates itself; mannequin gets a bob
		rig.position.y = absf(sin(_t * 2.4)) * 0.06
