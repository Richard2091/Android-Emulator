package com.richard.retrohall.emulator

import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.SystemClock
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.input.GameAction
import com.richard.retrohall.domain.save.SavePathResolver
import com.richard.retrohall.domain.save.SaveSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

class LibretroEmulatorSession(
    private val corePath: String,
    private val filesRoot: String,
    private val hostProvider: () -> LibretroCoreHost = { LibretroHost() },
) : EmulatorSession {
    private val hostLock = Any()
    private var host: LibretroCoreHost? = null
    private var loadedGame: LocalGame? = null
    private var frameThread: Thread? = null
    private var audioThread: Thread? = null

    private val _frames = MutableStateFlow<Bitmap?>(null)
    override val frames: StateFlow<Bitmap?> = _frames

    @Volatile
    override var state: EmulatorState = EmulatorState.Idle
        private set

    override var lastInput: GameAction? = null
        private set

    @Volatile
    private var running = false

    private var avFps = 60.0
    private var avSampleRate = 44100
    private var avBaseWidth = 256
    private var avBaseHeight = 224
    private var gameSpeed = 1f

    private var frameBuffer: ByteArray = ByteArray(1024 * 1024)
    private var audioBuffer: ByteArray = ByteArray(0)
    private var audioTrack: AudioTrack? = null
    private var frameLogCounter = 0

    private val frameIntervalMs: Double
        get() = 1000.0 / (avFps * gameSpeed)

    override fun load(game: LocalGame) {
        state = EmulatorState.Idle
        stopThreads()
        synchronized(hostLock) {
            runCatching { host?.unloadCore() }
        }
        host = null
        _frames.value = null
        loadedGame = game
        val nextHost = runCatching { hostProvider() }.getOrNull()
        val loaded = nextHost != null && synchronized(hostLock) {
            nextHost.loadCore(corePath) && nextHost.loadGame(game.romPath)
        }
        if (!loaded) {
            synchronized(hostLock) {
                runCatching { nextHost?.unloadCore() }
            }
            host = null
            state = EmulatorState.Error
            return
        }
        host = nextHost
        nextHost.getAvInfo()?.let { av ->
            if (av.fps > 0) avFps = av.fps
            if (av.sampleRate > 0) avSampleRate = av.sampleRate
            if (av.baseWidth > 0) avBaseWidth = av.baseWidth
            if (av.baseHeight > 0) avBaseHeight = av.baseHeight
        }
        state = EmulatorState.Loaded
    }

    override fun start() {
        if (state != EmulatorState.Loaded && state != EmulatorState.Paused) {
            state = EmulatorState.Error
            return
        }
        val firstFrameOk = synchronized(hostLock) {
            runCatching { host?.runFrame() == true }.getOrDefault(false)
        }
        if (!firstFrameOk) {
            state = EmulatorState.Error
            return
        }
        state = EmulatorState.Running
        startFrameLoop()
        startAudioLoop()
    }

    override fun pause() {
        if (state == EmulatorState.Running) {
            state = EmulatorState.Paused
            running = false
            synchronized(hostLock) { runCatching { audioTrack?.pause() } }
        }
    }

    override fun resume() {
        if (state == EmulatorState.Paused) {
            state = EmulatorState.Running
            synchronized(hostLock) { runCatching { audioTrack?.play() } }
            startFrameLoop()
            startAudioLoop()
        }
    }

    override fun reset() {
        val canReset = synchronized(hostLock) {
            host?.reset()
            host != null
        }
        if (canReset && loadedGame != null) {
            state = EmulatorState.Running
            running = true
            startFrameLoop()
            startAudioLoop()
        }
    }

    override fun stop() {
        state = EmulatorState.Stopped
        running = false
        stopThreads()
        synchronized(hostLock) {
            runCatching { audioTrack?.stop() }
            runCatching { audioTrack?.release() }
            audioTrack = null
            runCatching { host?.unloadCore() }
        }
        host = null
    }

    override fun setGameSpeed(speed: Float) {
        val next = speed.coerceIn(0.5f, 2f)
        if (next == gameSpeed) return
        gameSpeed = next
        synchronized(hostLock) {
            runCatching {
                audioTrack?.setPlaybackRate((avSampleRate * gameSpeed).toInt().coerceIn(4000, 96000))
            }
        }
    }

    override fun sendInput(action: GameAction, pressed: Boolean) {
        if (pressed) lastInput = action
        synchronized(hostLock) {
            host?.setInputState(action.toLibretroActionName(), pressed)
        }
    }

    override fun saveSram(): Boolean {
        val game = loadedGame ?: return false
        return writeWithParentDirs(SavePathResolver.sramPath(filesRoot, game.id)) { path ->
            synchronized(hostLock) { host?.saveSram(path) == true }
        }
    }

    override fun saveState(slot: SaveSlot): Boolean {
        val game = loadedGame ?: return false
        return writeWithParentDirs(SavePathResolver.statePath(filesRoot, game.id, slot)) { path ->
            synchronized(hostLock) { host?.serializeState(path) == true }
        }
    }

    override fun loadState(slot: SaveSlot): Boolean {
        val game = loadedGame ?: return false
        val path = SavePathResolver.statePath(filesRoot, game.id, slot)
        return File(path).isFile && synchronized(hostLock) { host?.unserializeState(path) == true }
    }

    private fun startFrameLoop() {
        if (frameThread?.isAlive == true) return
        running = true
        frameThread = Thread {
            var nextFrameAt = SystemClock.elapsedRealtime()
            while (running && (state == EmulatorState.Running || state == EmulatorState.Paused)) {
                val now = SystemClock.elapsedRealtime()
                val interval = frameIntervalMs
                if (state == EmulatorState.Paused) {
                    runCatching { Thread.sleep(max(8L, (interval / 2).toLong())) }
                    continue
                }
                val ok = synchronized(hostLock) {
                    runCatching { host?.runFrame() == true }.getOrDefault(false)
                }
                if (!ok) {
                    state = EmulatorState.Error
                    running = false
                    break
                }
                publishFrame()
                nextFrameAt += interval.toLong()
                val delay = nextFrameAt - SystemClock.elapsedRealtime()
                if (delay > 0) {
                    runCatching { Thread.sleep(delay) }
                } else {
                    nextFrameAt = SystemClock.elapsedRealtime()
                }
            }
        }.apply {
            name = "RetroHall-LibretroFrameLoop"
            isDaemon = true
            start()
        }
    }

    private fun publishFrame() {
        val h = host ?: return
        val info = runCatching { h.getFrameInfo() }.getOrNull() ?: return
        if (!info.ready || info.width <= 0 || info.height <= 0) return
        val needed = info.width * info.height * 4
        if (frameBuffer.size < needed) frameBuffer = ByteArray(needed)
        val written = runCatching { h.pollFrame(frameBuffer) }.getOrDefault(0)
        if (written <= 0) return
        val bitmap = runCatching {
            Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888).also { bmp ->
                bmp.copyPixelsFromBuffer(ByteBuffer.wrap(frameBuffer, 0, written))
            }
        }.getOrNull()
        if (bitmap != null) {
            _frames.value = bitmap
            if (frameLogCounter++ % 60 == 0) {
                android.util.Log.i("RetroHallFrame", "frame published ${info.width}x${info.height} bytes=$written")
            }
        }
    }

    private fun startAudioLoop() {
        if (audioThread?.isAlive == true) return
        val track = synchronized(hostLock) {
            audioTrack ?: runCatching {
                createAudioTrack().also { audioTrack = it }
            }.getOrNull()
        } ?: return
        if (track.state != AudioTrack.STATE_INITIALIZED) return
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            runCatching { track.play() }
        }
        audioThread = Thread {
            while (running && (state == EmulatorState.Running || state == EmulatorState.Paused)) {
                if (state == EmulatorState.Paused) {
                    runCatching { Thread.sleep(16) }
                    continue
                }
                val written = synchronized(hostLock) {
                    runCatching { host?.drainAudio(audioBuffer) }.getOrNull() ?: 0
                }
                if (written <= 0) {
                    runCatching { Thread.sleep(4) }
                    continue
                }
                runCatching { track.write(audioBuffer, 0, written) }
            }
        }.apply {
            name = "RetroHall-LibretroAudioLoop"
            isDaemon = true
            start()
        }
    }

    private fun createAudioTrack(): AudioTrack? {
        val minBuffer = AudioTrack.getMinBufferSize(
            avSampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return null
        audioBuffer = ByteArray(minBuffer)
        return AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(avSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuffer * 2, minBuffer))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .takeIf { it.state == AudioTrack.STATE_INITIALIZED }
    }

    private fun stopThreads() {
        running = false
        val frame = frameThread
        frameThread = null
        if (frame != null && frame !== Thread.currentThread()) {
            frame.interrupt()
            runCatching { frame.join(200) }
        }
        val audio = audioThread
        audioThread = null
        if (audio != null && audio !== Thread.currentThread()) {
            audio.interrupt()
            runCatching { audio.join(200) }
        }
    }

    private fun writeWithParentDirs(path: String, writer: (String) -> Boolean): Boolean {
        File(path).parentFile?.mkdirs()
        return writer(path)
    }

    private fun GameAction.toLibretroActionName(): String = when (this) {
        GameAction.Up -> "Up"
        GameAction.Down -> "Down"
        GameAction.Left -> "Left"
        GameAction.Right -> "Right"
        GameAction.Confirm,
        GameAction.NesA -> "A"
        GameAction.Back,
        GameAction.NesB -> "B"
        GameAction.Menu,
        GameAction.Start -> "Start"
        GameAction.Select -> "Select"
    }
}
