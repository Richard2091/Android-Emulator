package com.richard.retrohall.emulator

data class NativeFrameInfo(
    val width: Int,
    val height: Int,
    val ready: Boolean,
)

data class NativeAvInfo(
    val fps: Double,
    val sampleRate: Int,
    val channels: Int,
    val baseWidth: Int,
    val baseHeight: Int,
)

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

    /** 当前帧元数据；无帧时 ready=false。 */
    fun getFrameInfo(): NativeFrameInfo?

    /** 把最近一帧写入 dst（XRGB8888）。返回写入字节数；缓冲过小返回 -1。 */
    fun pollFrame(dst: ByteArray): Int

    /** AV 信息（fps/采样率/声道/基础分辨率）。 */
    fun getAvInfo(): NativeAvInfo?

    /** 拷贝可用的音频 PCM（16bit stereo）到 dst，返回字节数。 */
    fun drainAudio(dst: ByteArray): Int

    /** 清空音频缓冲，避免残留上一局声音。 */
    fun resetAudio()
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

    override fun getFrameInfo(): NativeFrameInfo? {
        val info = nativeGetFrameInfo() ?: return null
        return NativeFrameInfo(width = info[0], height = info[1], ready = info[2] == 1)
    }

    external fun nativeGetFrameInfo(): IntArray?
    external override fun pollFrame(dst: ByteArray): Int

    override fun getAvInfo(): NativeAvInfo? {
        val info = nativeGetAvInfo() ?: return null
        return NativeAvInfo(
            fps = info[0],
            sampleRate = info[1].toInt(),
            channels = info[2].toInt(),
            baseWidth = info[3].toInt(),
            baseHeight = info[4].toInt(),
        )
    }

    external fun nativeGetAvInfo(): DoubleArray?
    external override fun drainAudio(dst: ByteArray): Int
    external override fun resetAudio()

    companion object {
        init {
            System.loadLibrary("retrohall_libretro_host")
        }
    }
}
