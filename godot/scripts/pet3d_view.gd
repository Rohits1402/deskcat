## Proof-of-concept 3D-in-overlay: a transparent SubViewport with its own
## Camera3D + lights, shown as a texture inside the 2D overlay. Real 3D
## characters (Kenney CC0 GLBs etc.) load into `rig` the same way — this
## placeholder just builds a capsule-and-sphere mannequin from CSG.
extends SubViewportContainer

var rig: Node3D
var _t := 0.0


func _ready() -> void:
	stretch = true
	custom_minimum_size = Vector2(220, 220)
	size = custom_minimum_size

	var vp := SubViewport.new()
	vp.transparent_bg = true
	vp.msaa_3d = Viewport.MSAA_4X
	add_child(vp)

	var cam := Camera3D.new()
	cam.position = Vector3(0, 1.0, 3.2)
	vp.add_child(cam)

	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-40, 30, 0)
	vp.add_child(sun)

	rig = Node3D.new()
	vp.add_child(rig)
	_build_mannequin()


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
	rig.position.y = absf(sin(_t * 2.4)) * 0.06
