package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max

class AudioRecorder {
    companion object {
        const val SAMPLE_RATE = 16000
    }

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val recordedSamples = Collections.synchronizedList(ArrayList<Float>())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope): Boolean {
        if (_isRecording.value) return true

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
        val bufferSize = max(minBufferSize * 2, 4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            recordedSamples.clear()
            _durationMs.value = 0L
            _amplitudeHistory.value = emptyList()
            _isRecording.value = true

            audioRecord?.startRecording()

            recordJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(bufferSize / 2)
                val history = ArrayDeque<Float>(40)

                while (isActive && _isRecording.value) {
                    val read = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (read > 0) {
                        var sumSquare = 0.0
                        var maxPeak = 0f

                        synchronized(recordedSamples) {
                            for (i in 0 until read) {
                                val floatVal = shortBuffer[i] / 32768.0f
                                recordedSamples.add(floatVal)
                                sumSquare += (floatVal * floatVal)
                                val a = abs(floatVal)
                                if (a > maxPeak) maxPeak = a
                            }
                        }

                        val rms = Math.sqrt(sumSquare / read).toFloat()
                        val normalizedAmp = (rms * 4.0f).coerceIn(0.05f, 1.0f)
                        _currentAmplitude.value = normalizedAmp

                        if (history.size >= 40) history.removeFirst()
                        history.addLast(normalizedAmp)
                        _amplitudeHistory.value = history.toList()

                        _durationMs.value = (recordedSamples.size * 1000L) / SAMPLE_RATE
                    }
                }
            }
            return true
        } catch (_: Exception) {
            stop()
            return false
        }
    }

    fun stop(): FloatArray {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null

        val samples = synchronized(recordedSamples) {
            val arr = FloatArray(recordedSamples.size)
            for (i in recordedSamples.indices) {
                arr[i] = recordedSamples[i]
            }
            arr
        }
        return samples
    }

    fun getSamplesSnapshot(): FloatArray {
        return synchronized(recordedSamples) {
            val arr = FloatArray(recordedSamples.size)
            for (i in recordedSamples.indices) {
                arr[i] = recordedSamples[i]
            }
            arr
        }
    }

    fun clear() {
        synchronized(recordedSamples) {
            recordedSamples.clear()
        }
        _durationMs.value = 0L
        _currentAmplitude.value = 0f
        _amplitudeHistory.value = emptyList()
    }
}
