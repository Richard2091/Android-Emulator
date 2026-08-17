package com.richard.retrohall.emulator

interface LibretroCoreHost {
    fun nativeVersion(): String
    fun loadCore(corePath: String): Boolean
    fun unloadCore()
    fun loadGame(romPath: String): Boolean
    fun runFrame(): Boolean
    fun reset()
    fun serializeState(path: String): Boolean
    fun unserializeState(path: String): Boolean
    fun saveSram(path: String): Boolean
    fun setInputState(actionName: String, pressed: Boolean)
}

class LibretroHost : LibretroCoreHost {
    external override fun nativeVersion(): String
    external override fun loadCore(corePath: String): Boolean
    external override fun unloadCore()
    external override fun loadGame(romPath: String): Boolean
    external override fun runFrame(): Boolean
    external override fun reset()
    external override fun serializeState(path: String): Boolean
    external override fun unserializeState(path: String): Boolean
    external override fun saveSram(path: String): Boolean
    external override fun setInputState(actionName: String, pressed: Boolean)

    companion object {
        init {
            System.loadLibrary("retrohall_libretro_host")
        }
    }
}
