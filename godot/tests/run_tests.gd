extends SceneTree
## Runs every res://tests/test_*.gd script's static run() and exits 0/1.
##
##   godot --headless --path godot --script res://tests/run_tests.gd


func _initialize() -> void:
	var total_passed := 0
	var total_failed := 0
	var files: Array[String] = []
	var dir := DirAccess.open("res://tests")
	if dir != null:
		for f in dir.get_files():
			if f.begins_with("test_") and f.ends_with(".gd"):
				files.append("res://tests/" + f)
	files.sort()
	if files.is_empty():
		push_error("no test files found under res://tests")
		total_failed = 1
	for path in files:
		var script: GDScript = load(path)
		var r: Array = script.run()
		total_passed += r[0]
		total_failed += r[1]
		print("%s: %d passed, %d failed" % [path, r[0], r[1]])
	print("TOTAL: %d passed, %d failed" % [total_passed, total_failed])
	quit(1 if total_failed > 0 else 0)
