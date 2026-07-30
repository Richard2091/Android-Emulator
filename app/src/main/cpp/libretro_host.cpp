#include <jni.h>
#include <fstream>
#include <string>

namespace {
bool coreLoaded = false;
bool gameLoaded = false;

bool file_exists(const char *path) {
    std::ifstream file(path);
    return file.good();
}
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_nativeVersion(
    JNIEnv *env,
    jobject /* this */
) {
    return env->NewStringUTF("retrohall-libretro-host/0.1");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_loadCore(
    JNIEnv *env,
    jobject /* this */,
    jstring corePath
) {
    const char *path = env->GetStringUTFChars(corePath, nullptr);
    const bool exists = file_exists(path);
    env->ReleaseStringUTFChars(corePath, path);
    coreLoaded = exists;
    if (!coreLoaded) {
        gameLoaded = false;
    }
    return coreLoaded ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_unloadCore(
    JNIEnv * /* env */,
    jobject /* this */
) {
    coreLoaded = false;
    gameLoaded = false;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_loadGame(
    JNIEnv *env,
    jobject /* this */,
    jstring romPath
) {
    const char *path = env->GetStringUTFChars(romPath, nullptr);
    const bool exists = file_exists(path);
    env->ReleaseStringUTFChars(romPath, path);
    gameLoaded = coreLoaded && exists;
    return gameLoaded ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_runFrame(
    JNIEnv * /* env */,
    jobject /* this */
) {
    return gameLoaded ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_reset(
    JNIEnv * /* env */,
    jobject /* this */
) {
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_serializeState(
    JNIEnv *env,
    jobject /* this */,
    jstring path
) {
    const char *target = env->GetStringUTFChars(path, nullptr);
    std::ofstream output(target, std::ios::binary);
    const bool ok = gameLoaded && output.good();
    if (ok) {
        const std::string marker = "retrohall-state-placeholder";
        output.write(marker.data(), static_cast<std::streamsize>(marker.size()));
    }
    env->ReleaseStringUTFChars(path, target);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_unserializeState(
    JNIEnv *env,
    jobject /* this */,
    jstring path
) {
    const char *target = env->GetStringUTFChars(path, nullptr);
    const bool ok = gameLoaded && file_exists(target);
    env->ReleaseStringUTFChars(path, target);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_saveSram(
    JNIEnv *env,
    jobject /* this */,
    jstring path
) {
    const char *target = env->GetStringUTFChars(path, nullptr);
    std::ofstream output(target, std::ios::binary);
    const bool ok = gameLoaded && output.good();
    if (ok) {
        const std::string marker = "retrohall-sram-placeholder";
        output.write(marker.data(), static_cast<std::streamsize>(marker.size()));
    }
    env->ReleaseStringUTFChars(path, target);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_richard_retrohall_emulator_LibretroHost_setInputState(
    JNIEnv * /* env */,
    jobject /* this */,
    jstring /* actionName */,
    jboolean /* pressed */
) {
}
