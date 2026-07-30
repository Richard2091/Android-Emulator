package com.richard.retrohall.emulator

class LibretroHost {
    external fun nativeVersion(): String
    external fun loadCore(corePath: String): Boolean
    external fun unloadCore()
    external fun loadGame(romPath: String): Boolean
    external fun runFrame(): Boolean
    external fun reset()
    external fun serializeState(path: String): Boolean
    external fun unserializeState(path: String): Boolean
    external fun saveSram(path: String): Boolean
    external fun setInputState(actionName: String, pressed: Boolean)

    companion object {
        init {
            System.loadLibrary("retrohall_libretro_host")
        }
    }
}
