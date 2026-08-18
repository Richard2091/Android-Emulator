#include <jni.h>

#include <android/log.h>
#include <dlfcn.h>

#include <algorithm>
#include <cstdarg>
#include <cstdint>
#include <cstring>
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

    // 视频帧输出：统一转换到 XRGB8888（小端内存顺序 B,G,R,A）。
    std::vector<uint8_t> frame_buffer;
    unsigned frame_width = 0;
    unsigned frame_height = 0;
    bool frame_ready = false;

    // AV 信息：来自 retro_get_system_av_info。
    double av_fps = 60.0;
    unsigned av_sample_rate = 44100;
    unsigned av_channels = 2;
    unsigned av_base_width = 256;
    unsigned av_base_height = 224;

    // 音频环形缓冲：int16 立体声帧。
    std::vector<int16_t> audio_ring;
    size_t audio_capacity = 0;
    size_t audio_head = 0;  // 写位置
    size_t audio_tail = 0;  // 读位置
    size_t audio_fill = 0;  // 已占用帧数
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
    g_host.frame_buffer.clear();
    g_host.frame_width = 0;
    g_host.frame_height = 0;
    g_host.frame_ready = false;
    g_host.audio_ring.clear();
    g_host.audio_capacity = 0;
    g_host.audio_head = 0;
    g_host.audio_tail = 0;
    g_host.audio_fill = 0;
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

void convert_frame_to_xrgb(const void *data, unsigned width, unsigned height, size_t pitch) {
    const size_t needed = static_cast<size_t>(width) * height * 4;
    g_host.frame_buffer.resize(needed);
    const uint8_t *src = static_cast<const uint8_t *>(data);
    uint8_t *dst = g_host.frame_buffer.data();
    switch (g_host.pixel_format) {
        // 统一输出 R,G,B,A 字节序（Bitmap ARGB_8888 的 copyPixelsFromBuffer 期望）。
        case RETRO_PIXEL_FORMAT_XRGB8888: {
            // core 提供 XRGB8888（内存小端字节序 B,G,R,X），重排为 R,G,B,A。
            for (unsigned y = 0; y < height; ++y) {
                const uint8_t *row = src + static_cast<size_t>(y) * pitch;
                uint8_t *out = dst + static_cast<size_t>(y) * width * 4;
                for (unsigned x = 0; x < width; ++x) {
                    out[0] = row[x * 4 + 2];  // R
                    out[1] = row[x * 4 + 1];  // G
                    out[2] = row[x * 4 + 0];  // B
                    out[3] = 0xFF;            // A
                    out += 4;
                }
            }
            break;
        }
        case RETRO_PIXEL_FORMAT_RGB565: {
            // 标准 RGB565（小端）：bit11-15 = R，bit5-10 = G，bit0-4 = B。
            // 输出 R,G,B,A 字节序（Bitmap ARGB_8888 的 copyPixelsFromBuffer 期望）。
            for (unsigned y = 0; y < height; ++y) {
                const uint16_t *row = reinterpret_cast<const uint16_t *>(src + static_cast<size_t>(y) * pitch);
                uint8_t *out = dst + static_cast<size_t>(y) * width * 4;
                for (unsigned x = 0; x < width; ++x) {
                    const uint16_t p = row[x];
                    out[0] = static_cast<uint8_t>(((p >> 11) & 0x1F) * 255 / 31);      // R
                    out[1] = static_cast<uint8_t>(((p >> 5) & 0x3F) * 255 / 63);       // G
                    out[2] = static_cast<uint8_t>((p & 0x1F) * 255 / 31);              // B
                    out[3] = 0xFF;                                                     // A
                    out += 4;
                }
            }
            break;
        }
        case RETRO_PIXEL_FORMAT_0RGB1555: {
            // 标准 0RGB1555（小端）：bit14-10 = R，bit9-5 = G，bit4-0 = B，bit15 = 0。
            for (unsigned y = 0; y < height; ++y) {
                const uint16_t *row = reinterpret_cast<const uint16_t *>(src + static_cast<size_t>(y) * pitch);
                uint8_t *out = dst + static_cast<size_t>(y) * width * 4;
                for (unsigned x = 0; x < width; ++x) {
                    const uint16_t p = row[x];
                    out[0] = static_cast<uint8_t>(((p >> 10) & 0x1F) * 255 / 31);      // R
                    out[1] = static_cast<uint8_t>(((p >> 5) & 0x1F) * 255 / 31);       // G
                    out[2] = static_cast<uint8_t>((p & 0x1F) * 255 / 31);              // B
                    out[3] = 0xFF;                                                     // A
                    out += 4;
                }
            }
            break;
        }
        default: {
            std::fill(dst, dst + needed, 0x00);
            for (size_t i = 3; i < needed; i += 4) dst[i] = 0xFF;
            break;
        }
    }
}

void audio_write_samples(const int16_t *data, size_t frames) {
    if (g_host.audio_capacity == 0) return;
    if (frames > g_host.audio_capacity) {
        // 极端情况：一次写入超过整个缓冲，丢弃最老的。
        g_host.audio_tail = 0;
        g_host.audio_head = 0;
        g_host.audio_fill = 0;
        frames = g_host.audio_capacity;
    }
    const size_t avail = g_host.audio_capacity - g_host.audio_fill;
    if (frames > avail) {
        // 覆盖最老的音频（变速/快进时丢音，保持跟随游戏）。
        const size_t drop = frames - avail;
        g_host.audio_tail = (g_host.audio_tail + drop) % g_host.audio_capacity;
        g_host.audio_fill -= drop;
    }
    for (size_t i = 0; i < frames; ++i) {
        g_host.audio_ring[(g_host.audio_head + i) % g_host.audio_capacity] = data[i];
    }
    g_host.audio_head = (g_host.audio_head + frames) % g_host.audio_capacity;
    g_host.audio_fill += frames;
}

void video_refresh_callback(const void *data, unsigned width, unsigned height, size_t pitch) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (data == nullptr || width == 0 || height == 0 || pitch == 0) {
        // RETRO_ENVIRONMENT_GET_CAN_DUPE 已声明支持 dupe，data 为 null 时保留上一帧。
        return;
    }
    convert_frame_to_xrgb(data, width, height, pitch);
    g_host.frame_width = width;
    g_host.frame_height = height;
    g_host.frame_ready = true;
}

void audio_sample_callback(int16_t left, int16_t right) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    const int16_t sample[2] = {left, right};
    audio_write_samples(sample, 1);
}

size_t audio_sample_batch_callback(const int16_t *data, size_t frames) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (data == nullptr || frames == 0) return frames;
    audio_write_samples(data, frames);
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
    g_host.av_fps = av_info.timing.fps > 0.0 ? av_info.timing.fps : 60.0;
    g_host.av_sample_rate = av_info.timing.sample_rate > 0 ? av_info.timing.sample_rate : 44100;
    g_host.av_channels = 2;
    g_host.av_base_width = av_info.geometry.base_width > 0 ? av_info.geometry.base_width : 256;
    g_host.av_base_height = av_info.geometry.base_height > 0 ? av_info.geometry.base_height : 224;

    // 重置输出缓冲，避免上一局残留画面/音频。
    g_host.frame_buffer.clear();
    g_host.frame_width = 0;
    g_host.frame_height = 0;
    g_host.frame_ready = false;
    g_host.audio_capacity = 8192;
    g_host.audio_ring.assign(g_host.audio_capacity, 0);
    g_host.audio_head = 0;
    g_host.audio_tail = 0;
    g_host.audio_fill = 0;

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

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_nativeGetFrameInfo(JNIEnv *env, jobject) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    jintArray result = env->NewIntArray(3);
    if (result == nullptr) return nullptr;
    const jint values[3] = {
        static_cast<jint>(g_host.frame_width),
        static_cast<jint>(g_host.frame_height),
        g_host.frame_ready ? static_cast<jint>(1) : static_cast<jint>(0),
    };
    env->SetIntArrayRegion(result, 0, 3, values);
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_pollFrame(JNIEnv *env, jobject, jbyteArray dst) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (!g_host.game_loaded || !g_host.frame_ready) return 0;
    const size_t needed = static_cast<size_t>(g_host.frame_width) * g_host.frame_height * 4;
    const jsize cap = env->GetArrayLength(dst);
    if (static_cast<size_t>(cap) < needed) return -1;
    env->SetByteArrayRegion(
        dst,
        0,
        static_cast<jsize>(needed),
        reinterpret_cast<const jbyte *>(g_host.frame_buffer.data())
    );
    return static_cast<jint>(needed);
}

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_nativeGetAvInfo(JNIEnv *env, jobject) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    jdoubleArray result = env->NewDoubleArray(6);
    if (result == nullptr) return nullptr;
    const jdouble values[6] = {
        g_host.av_fps,
        static_cast<jdouble>(g_host.av_sample_rate),
        static_cast<jdouble>(g_host.av_channels),
        static_cast<jdouble>(g_host.av_base_width),
        static_cast<jdouble>(g_host.av_base_height),
        1.0, // 预留：aspect ratio
    };
    env->SetDoubleArrayRegion(result, 0, 6, values);
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_drainAudio(JNIEnv *env, jobject, jbyteArray dst) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    if (!g_host.game_loaded || g_host.audio_fill == 0) return 0;
    const jsize cap = env->GetArrayLength(dst);
    const size_t maxFrames = static_cast<size_t>(cap) / 2;
    if (maxFrames == 0) return 0;
    const size_t toRead = std::min(maxFrames, g_host.audio_fill);
    std::vector<int16_t> tmp(toRead);
    for (size_t i = 0; i < toRead; ++i) {
        tmp[i] = g_host.audio_ring[(g_host.audio_tail + i) % g_host.audio_capacity];
    }
    g_host.audio_tail = (g_host.audio_tail + toRead) % g_host.audio_capacity;
    g_host.audio_fill -= toRead;
    env->SetByteArrayRegion(
        dst,
        0,
        static_cast<jsize>(toRead * 2),
        reinterpret_cast<const jbyte *>(tmp.data())
    );
    return static_cast<jint>(toRead * 2);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_resetAudio(JNIEnv *, jobject) {
    std::lock_guard<std::recursive_mutex> lock(g_host_mutex);
    g_host.audio_head = 0;
    g_host.audio_tail = 0;
    g_host.audio_fill = 0;
}
