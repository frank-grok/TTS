package com.example.service

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale
import kotlin.random.Random

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Loading : PlaybackState
    data class Playing(val positionMs: Int, val durationMs: Int) : PlaybackState
    data class Paused(val positionMs: Int, val durationMs: Int) : PlaybackState
    data object Completed : PlaybackState
    data class Error(val message: String) : PlaybackState
}

class AudioPlaybackManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var androidTts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // 16 amplitude bars for audio visualizer animation (values 0.1f .. 1.0f)
    private val _amplitudes = MutableStateFlow<List<Float>>(List(16) { 0.15f })
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val pos = player.currentPosition
                    val dur = player.duration.coerceAtLeast(1)
                    _playbackState.value = PlaybackState.Playing(pos, dur)

                    // Generate dynamic wave amplitudes
                    val newAmps = List(16) {
                        Random.nextFloat() * 0.85f + 0.15f
                    }
                    _amplitudes.value = newAmps

                    mainHandler.postDelayed(this, 100)
                }
            }
        }
    }

    private val ttsVisualizerRunnable = object : Runnable {
        override fun run() {
            if (_playbackState.value is PlaybackState.Playing) {
                _amplitudes.value = List(16) {
                    Random.nextFloat() * 0.85f + 0.15f
                }
                mainHandler.postDelayed(this, 100)
            }
        }
    }

    init {
        initAndroidTts()
    }

    private fun initAndroidTts() {
        androidTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                // Try setting Myanmar locale or default
                val myLocale = Locale("my", "MM")
                val res = androidTts?.setLanguage(myLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("AudioPlaybackManager", "Burmese language pack missing on device TTS, using default voice.")
                }
            }
        }

        androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    _playbackState.value = PlaybackState.Playing(0, 3000)
                    mainHandler.post(ttsVisualizerRunnable)
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    resetVisualizer()
                    _playbackState.value = PlaybackState.Completed
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    resetVisualizer()
                    _playbackState.value = PlaybackState.Error("Device TTS synthesis failed.")
                }
            }
        })
    }

    fun playAudioFile(file: File) {
        stop()
        _playbackState.value = PlaybackState.Loading
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    val dur = mp.duration.coerceAtLeast(1)
                    _playbackState.value = PlaybackState.Playing(0, dur)
                    mainHandler.post(progressRunnable)
                }
                setOnCompletionListener {
                    resetVisualizer()
                    _playbackState.value = PlaybackState.Completed
                }
                setOnErrorListener { _, what, extra ->
                    resetVisualizer()
                    _playbackState.value = PlaybackState.Error("Media player playback error ($what, $extra)")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            resetVisualizer()
            _playbackState.value = PlaybackState.Error("Failed to play audio: ${e.localizedMessage}")
        }
    }

    fun speakWithDeviceTts(text: String, speed: Float = 1.0f) {
        stop()
        if (!isTtsInitialized || androidTts == null) {
            _playbackState.value = PlaybackState.Error("Device TTS is initializing, please try again in a moment.")
            return
        }
        androidTts?.setSpeechRate(speed)
        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "BURMESE_TTS_UTTERANCE")
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                mainHandler.removeCallbacks(progressRunnable)
                _playbackState.value = PlaybackState.Paused(player.currentPosition, player.duration)
                resetVisualizer()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            player.start()
            _playbackState.value = PlaybackState.Playing(player.currentPosition, player.duration)
            mainHandler.post(progressRunnable)
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs)
            if (player.isPlaying) {
                _playbackState.value = PlaybackState.Playing(positionMs, player.duration)
            } else {
                _playbackState.value = PlaybackState.Paused(positionMs, player.duration)
            }
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(progressRunnable)
        mainHandler.removeCallbacks(ttsVisualizerRunnable)
        resetVisualizer()

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            androidTts?.stop()
        } catch (_: Exception) {}

        _playbackState.value = PlaybackState.Idle
    }

    private fun resetVisualizer() {
        _amplitudes.value = List(16) { 0.15f }
    }

    fun release() {
        stop()
        try {
            androidTts?.shutdown()
        } catch (_: Exception) {}
        androidTts = null
    }
}
