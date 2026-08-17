#include <jni.h>

#include <android/log.h>
#include <dlfcn.h>

#include <cstdarg>
#include <cstdint>
#include <fstream>
#include <mutex>
#include <string>
#include <vector>

#include "libretro.h"

namespace {

constexpr const char *kTag = "RetroHallLibretro";

using retro_set_environment_t = void (*)(retro_environment_t);
using retro_set_video_refresh_t = void (*)(retro_video_refresh_t);
using retro_set_audio_sample_t = void (*)(retro_audio_sample_t);
using retro_set_audio_sample_batch_t = void (*)(retro_audio_sample_batch_t);
using retro_set_input_poll_t = void (*)(retro_input_poll_t);
using retro_set_input_state_t = void (*)(retro_input_state_t);
using retro_init_t = void (*)();
using retro_deinit_t = void (*)();
using retro_api_version_t = unsigned (*)();
using retro_get_system_info_t = void (*)(retro_system_info *);
using retro_get_system_av_info_t = void (*)(retro_system_av_info *);
using retro_set_controller_port_device_t = void (*)(unsigned, unsigned);
using retro_reset_t = void (*)();
using retro_run_t = void (*)();
using retro_serialize_size_t = size_t (*)();
using retro_serialize_t = bool (*)(void *, size_t);
using retro_unserialize_t = bool (*)(const void *, size_t);
using retro_load_game_t = bool (*)(const retro_game_info *);
using retro_unload_game_t = void (*)();
using retro_get_memory_data_t = void *(*)(unsigned);
using retro_get_memory_size_t = size_t (*)(unsigned);

struct CoreApi {
    retro_set_environment_t set_environment = nullptr;
    retro_set_video_refresh_t set_video_refresh = nullptr;
    retro_set_audio_sample_t set_audio_sample = nullptr;
    retro_set_audio_sample_batch_t set_audio_sample_batch = nullptr;
    retro_set_input_poll_t set_input_poll = nullptr;
    retro_set_input_state_t set_input_state = nullptr;
    retro_init_t init = nullptr;
    retro_deinit_t deinit = nullptr;
    retro_api_version_t api_version = nullptr;
    retro_get_system_info_t get_system_info = nullptr;
    retro_get_system_av_info_t get_system_av_info = nullptr;
    retro_set_controller_port_device_t set_controller_port_device = nullptr;
    retro_reset_t reset = nullptr;
    retro_run_t run = nullptr;
    retro_serialize_size_t serialize_size = nullptr;
    retro_serialize_t serialize = nullptr;
    retro_unserialize_t unserialize = nullptr;
    retro_load_game_t load_game = nullptr;
    retro_unload_game_t unload_game = nullptr;
    retro_get_memory_data_t get_memory_data = nullptr;
    retro_get_memory_size_t get_memory_size = nullptr;
};

struct HostState {
    void *core_handle = nullptr;
    CoreApi api;
    bool core_loaded = false;
    bool game_loaded = false;
    unsigned pixel_format = RETRO_PIXEL_FORMAT_0RGB1555;
    uint32_t input_mask = 0;
    std::string core_path;
    std::string content_dir;
    std::string save_dir;
    std::string system_dir;
};

HostState g_host;
std::recursive_mutex g_host_mutex;

void log_info(const char *message) {
    __android_log_write(ANDROID_LOG_INFO, kTag, message);
}

void log_error(const char *message) {
    __android_log_write(ANDROID_LOG_ERROR, kTag, message);
}

void core_log(enum retro_log_level level, const char *fmt, ...) {
    int priority = ANDROID_LOG_INFO;
    if (level == RETRO_LOG_ERROR) {
        priority = ANDROID_LOG_ERROR;
    } else if (level == RETRO_LOG_WARN) {
        priority = ANDROID_LOG_WARN;
    } else if (level == RETRO_LOG_DEBUG) {
        priority = ANDROID_LOG_DEBUG;
    }

    va_list args;
    va_start(args, fmt);
    __android_log_vprint(priority, kTag, fmt, args);
    va_end(args);
}

bool file_exists(const std::string &path) {
    std::ifstream file(path, std::ios::binary);
    return file.good();
}

std::string parent_dir(const std::string &path) {
    const size_t slash = path.find_last_of("/\\");
    if (slash == std::string::npos) return ".";
    if (slash == 0) return "/";
    return path.substr(0, slash);
}

template <typename T>
bool load_symbol(void *handle, const char *name, T *target) {
    dlerror();
    void *symbol = dlsym(handle, name);
    const char *error = dlerror();
    if (error != nullptr || symbol == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "missing symbol %s: %s", name, error ? error : "null");
        return false;
    }
    *target = reinterpret_cast<T>(symbol);
    return true;
}

void reset_state() {
    g_host.api = CoreApi{};
    g_host.core_loaded = false;
    g_host.game_loaded = false;
    g_host.pixel_format = RETRO_PIXEL_FORMAT_0RGB1555;
    g_host.input_mask = 0;
    g_host.core_path.clear();
    g_host.content_dir.clear();
    g_host.save_dir.clear();
    g_host.system_dir.clear();
}

bool load_required_symbols(void *handle) {
    CoreApi api;
    bool ok = true;
    ok &= load_symbol(handle, "retro_set_environment", &api.set_environment);
    ok &= load_symbol(handle, "retro_set_video_refresh", &api.set_video_refresh);
    ok &= load_symbol(handle, "retro_set_audio_sample", &api.set_audio_sample);
    ok &= load_symbol(handle, "retro_set_audio_sample_batch", &api.set_audio_sample_batch);
    ok &= load_symbol(handle, "retro_set_input_poll", &api.set_input_poll);
    ok &= load_symbol(handle, "retro_set_input_state", &api.set_input_state);
    ok &= load_symbol(handle, "retro_init", &api.init);
    ok &= load_symbol(handle, "retro_deinit", &api.deinit);
    ok &= load_symbol(handle, "retro_api_version", &api.api_version);
    ok &= load_symbol(handle, "retro_get_system_info", &api.get_system_info);
    ok &= load_symbol(handle, "retro_get_system_av_info", &api.get_system_av_info);
    ok &= load_symbol(handle, "retro_set_controller_port_device", &api.set_controller_port_device);
    ok &= load_symbol(handle, "retro_reset", &api.reset);
    ok &= load_symbol(handle, "retro_run", &api.run);
    ok &= load_symbol(handle, "retro_serialize_size", &api.serialize_size);
    ok &= load_symbol(handle, "retro_serialize", &api.serialize);
    ok &= load_symbol(handle, "retro_unserialize", &api.unserialize);
    ok &= load_symbol(handle, "retro_load_game", &api.load_game);
    ok &= load_symbol(handle, "retro_unload_game", &api.unload_game);
    ok &= load_symbol(handle, "retro_get_memory_data", &api.get_memory_data);
    ok &= load_symbol(handle, "retro_get_memory_size", &api.get_memory_size);
    if (!ok) return false;
    g_host.api = api;
    return true;
}

bool environment_callback(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *static_cast<bool *>(data) = true;
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            g_host.pixel_format = *static_cast<const unsigned *>(data);
            return true;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            static_cast<retro_log_callback *>(data)->log = core_log;
            return true;
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
            *static_cast<unsigned *>(data) = 2;
            return true;
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            static_cast<retro_variable *>(data)->value = nullptr;
            return false;
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *static_cast<bool *>(data) = false;
            return true;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            *static_cast<const char **>(data) = g_host.system_dir.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            *static_cast<const char **>(data) = g_host.save_dir.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY:
            *static_cast<const char **>(data) = g_host.content_dir.c_str();
            return true;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL:
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
        case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
        case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:
        case RETRO_ENVIRONMENT_SET_MESSAGE:
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
            return true;
        default:
            return false;
    }
}

void video_refresh_callback(const void *, unsigned, unsigned, size_t) {
    // Rendering is wired later; this frontend currently validates core execution headlessly.
}

void audio_sample_callback(int16_t, int16_t) {
}

size_t audio_sample_batch_callback(const int16_t *, size_t frames) {
    return frames;
}

void input_poll_callback() {
}

int16_t input_state_callback(unsigned port, unsigned device, unsigned, unsigned id) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (port != 0 || device != RETRO_DEVICE_JOYPAD || id > RETRO_DEVICE_ID_JOYPAD_R3) {
        return 0;
    }
    return (g_host.input_mask & (1u << id)) != 0 ? 1 : 0;
}

unsigned button_id_for_action(const std::string &action) {
    if (action == "Up") return RETRO_DEVICE_ID_JOYPAD_UP;
    if (action == "Down") return RETRO_DEVICE_ID_JOYPAD_DOWN;
    if (action == "Left") return RETRO_DEVICE_ID_JOYPAD_LEFT;
    if (action == "Right") return RETRO_DEVICE_ID_JOYPAD_RIGHT;
    if (action == "A") return RETRO_DEVICE_ID_JOYPAD_A;
    if (action == "B") return RETRO_DEVICE_ID_JOYPAD_B;
    if (action == "X") return RETRO_DEVICE_ID_JOYPAD_X;
    if (action == "Y") return RETRO_DEVICE_ID_JOYPAD_Y;
    if (action == "Start") return RETRO_DEVICE_ID_JOYPAD_START;
    if (action == "Select") return RETRO_DEVICE_ID_JOYPAD_SELECT;
    return RETRO_DEVICE_ID_JOYPAD_MASK;
}

void unload_core() {
    if (g_host.core_loaded && g_host.game_loaded && g_host.api.unload_game) {
        g_host.api.unload_game();
    }
    if (g_host.core_loaded && g_host.api.deinit) {
        g_host.api.deinit();
    }
    if (g_host.core_handle != nullptr) {
        dlclose(g_host.core_handle);
        g_host.core_handle = nullptr;
    }
    reset_state();
}

} // namespace

extern "C"
JNIEXPORT jstring JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_nativeVersion(JNIEnv *env, jobject) {
    return env->NewStringUTF("retrohall-libretro-host/0.2");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_loadCore(JNIEnv *env, jobject, jstring corePath) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    const char *raw_path = env->GetStringUTFChars(corePath, nullptr);
    const std::string path(raw_path ? raw_path : "");
    env->ReleaseStringUTFChars(corePath, raw_path);

    unload_core();
    if (path.empty() || !file_exists(path)) {
        log_error("core file does not exist");
        return JNI_FALSE;
    }

    void *handle = dlopen(path.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (handle == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "dlopen failed: %s", dlerror());
        return JNI_FALSE;
    }

    g_host.core_handle = handle;
    g_host.core_path = path;
    g_host.system_dir = parent_dir(path);
    g_host.save_dir = parent_dir(path);
    if (!load_required_symbols(handle)) {
        unload_core();
        return JNI_FALSE;
    }

    if (g_host.api.api_version() != RETRO_API_VERSION) {
        log_error("unsupported libretro API version");
        unload_core();
        return JNI_FALSE;
    }

    g_host.api.set_environment(environment_callback);
    g_host.api.set_video_refresh(video_refresh_callback);
    g_host.api.set_audio_sample(audio_sample_callback);
    g_host.api.set_audio_sample_batch(audio_sample_batch_callback);
    g_host.api.set_input_poll(input_poll_callback);
    g_host.api.set_input_state(input_state_callback);
    g_host.api.init();
    g_host.api.set_controller_port_device(0, RETRO_DEVICE_JOYPAD);
    g_host.core_loaded = true;
    log_info("core loaded");
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_unloadCore(JNIEnv *, jobject) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    unload_core();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_loadGame(JNIEnv *env, jobject, jstring romPath) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (!g_host.core_loaded) return JNI_FALSE;

    const char *raw_path = env->GetStringUTFChars(romPath, nullptr);
    const std::string path(raw_path ? raw_path : "");
    env->ReleaseStringUTFChars(romPath, raw_path);

    if (path.empty() || !file_exists(path)) {
        log_error("ROM file does not exist");
        return JNI_FALSE;
    }

    retro_system_info system_info{};
    g_host.api.get_system_info(&system_info);

    std::vector<char> rom_data;
    retro_game_info game_info{};
    game_info.path = path.c_str();
    game_info.meta = nullptr;

    if (!system_info.need_fullpath) {
        std::ifstream input(path, std::ios::binary);
        rom_data.assign(std::istreambuf_iterator<char>(input), std::istreambuf_iterator<char>());
        if (rom_data.empty()) {
            log_error("ROM read failed");
            return JNI_FALSE;
        }
        game_info.data = rom_data.data();
        game_info.size = rom_data.size();
    }

    g_host.content_dir = parent_dir(path);
    const bool loaded = g_host.api.load_game(&game_info);
    g_host.game_loaded = loaded;
    if (!loaded) {
        log_error("retro_load_game failed");
        return JNI_FALSE;
    }

    retro_system_av_info av_info{};
    g_host.api.get_system_av_info(&av_info);
    log_info("game loaded");
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_runFrame(JNIEnv *, jobject) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (!g_host.core_loaded || !g_host.game_loaded) return JNI_FALSE;
    g_host.api.run();
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_reset(JNIEnv *, jobject) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (g_host.core_loaded && g_host.game_loaded) {
        g_host.api.reset();
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_serializeState(JNIEnv *env, jobject, jstring path) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (!g_host.core_loaded || !g_host.game_loaded) return JNI_FALSE;
    const size_t size = g_host.api.serialize_size();
    if (size == 0) return JNI_FALSE;

    std::vector<uint8_t> buffer(size);
    if (!g_host.api.serialize(buffer.data(), buffer.size())) return JNI_FALSE;

    const char *raw_path = env->GetStringUTFChars(path, nullptr);
    std::ofstream output(raw_path ? raw_path : "", std::ios::binary);
    if (output.good()) {
        output.write(reinterpret_cast<const char *>(buffer.data()), static_cast<std::streamsize>(buffer.size()));
    }
    const bool ok = output.good();
    env->ReleaseStringUTFChars(path, raw_path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_unserializeState(JNIEnv *env, jobject, jstring path) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (!g_host.core_loaded || !g_host.game_loaded) return JNI_FALSE;

    const char *raw_path = env->GetStringUTFChars(path, nullptr);
    std::ifstream input(raw_path ? raw_path : "", std::ios::binary);
    std::vector<uint8_t> buffer{
        std::istreambuf_iterator<char>(input),
        std::istreambuf_iterator<char>()
    };
    env->ReleaseStringUTFChars(path, raw_path);
    if (buffer.empty()) return JNI_FALSE;
    return g_host.api.unserialize(buffer.data(), buffer.size()) ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_saveSram(JNIEnv *env, jobject, jstring path) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (!g_host.core_loaded || !g_host.game_loaded) return JNI_FALSE;
    void *data = g_host.api.get_memory_data(RETRO_MEMORY_SAVE_RAM);
    const size_t size = g_host.api.get_memory_size(RETRO_MEMORY_SAVE_RAM);
    if (data == nullptr || size == 0) return JNI_FALSE;

    const char *raw_path = env->GetStringUTFChars(path, nullptr);
    std::ofstream output(raw_path ? raw_path : "", std::ios::binary);
    if (output.good()) {
        output.write(static_cast<const char *>(data), static_cast<std::streamsize>(size));
    }
    const bool ok = output.good();
    env->ReleaseStringUTFChars(path, raw_path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_setInputState(
    JNIEnv *env,
    jobject,
    jstring actionName,
    jboolean pressed
) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    const char *raw_action = env->GetStringUTFChars(actionName, nullptr);
    const unsigned button = button_id_for_action(raw_action ? raw_action : "");
    env->ReleaseStringUTFChars(actionName, raw_action);
    if (button > RETRO_DEVICE_ID_JOYPAD_R3) return;

    const uint32_t mask = 1u << button;
    if (pressed == JNI_TRUE) {
        g_host.input_mask |= mask;
    } else {
        g_host.input_mask &= ~mask;
    }
}
