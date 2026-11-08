package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    fun playUri(context: Context, uri: Uri, scope: CoroutineScope) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong()
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker(scope)
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    progressJob?.cancel()
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            _isPlaying.value = false
        }
    }

    fun playFloatSamples(samples: FloatArray, scope: CoroutineScope) {
        stop()
        if (samples.isEmpty()) return

        val sampleRate = AudioRecorder.SAMPLE_RATE
        val totalMs = (samples.size * 1000L) / sampleRate
        _durationMs.value = totalMs
        _currentPositionMs.value = 0L
        _isPlaying.value = true

        progressJob = scope.launch(Dispatchers.IO) {
            val shortCount = samples.size
            val shorts = ShortArray(shortCount)
            for (i in 0 until shortCount) {
                shorts[i] = (samples[i].coerceIn(-1.0f, 1.0f) * 32767).toInt().toShort()
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            val chunkSize = 2048
            var offset = 0
            val startTime = System.currentTimeMillis()

            while (isActive && offset < shorts.size && _isPlaying.value) {
                val length = chunkSize.coerceAtMost(shorts.size - offset)
                track.write(shorts, offset, length)
                offset += length
                _currentPositionMs.value = (offset * 1000L) / sampleRate
            }

            track.stop()
            track.release()
            audioTrack = null
            _isPlaying.value = false
            _currentPositionMs.value = 0L
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying || mp.currentPosition > 0) {
                mp.seekTo(positionMs.toInt())
                _currentPositionMs.value = positionMs
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume(scope: CoroutineScope) {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                _isPlaying.value = true
                startProgressTracker(scope)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null

        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null

        audioTrack?.let { track ->
            try {
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
        audioTrack = null

        _isPlaying.value = false
        _currentPositionMs.value = 0L
    }

    private fun startProgressTracker(scope: CoroutineScope) {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPositionMs.value = mp.currentPosition.toLong()
                    }
                }
                delay(100)
            }
        }
    }
}
