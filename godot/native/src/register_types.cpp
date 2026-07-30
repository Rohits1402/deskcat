#include <godot_cpp/core/class_db.hpp>
#include <godot_cpp/godot.hpp>

#include "audio_peak.h"
#include "key_counter.h"
#include "win_tricks.h"

using namespace godot;

namespace {

void initialize_module(ModuleInitializationLevel p_level) {
    if (p_level != MODULE_INITIALIZATION_LEVEL_SCENE) {
        return;
    }
    ClassDB::register_class<deskcat::DeskCatWinTricks>();
    ClassDB::register_class<deskcat::DeskCatKeyCounter>();
    ClassDB::register_class<deskcat::DeskCatAudioPeak>();
}

void uninitialize_module(ModuleInitializationLevel p_level) {
}

} // namespace

extern "C" GDExtensionBool GDE_EXPORT deskcat_init(
        GDExtensionInterfaceGetProcAddress p_get_proc_address,
        GDExtensionClassLibraryPtr p_library,
        GDExtensionInitialization *r_initialization) {
    GDExtensionBinding::InitObject init_obj(p_get_proc_address, p_library,
            r_initialization);
    init_obj.register_initializer(initialize_module);
    init_obj.register_terminator(uninitialize_module);
    init_obj.set_minimum_library_initialization_level(
            MODULE_INITIALIZATION_LEVEL_SCENE);
    return init_obj.init();
}
