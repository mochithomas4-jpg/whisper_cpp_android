package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioDecoder
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.DecodedAudioResult
import com.example.engine.WhisperManager
import com.example.model.ComputeDevice
import com.example.model.LocalModelItem
import com.example.model.ModelDownloadState
import com.example.model.SubtitleSegment
import com.example.model.WhisperModelPreset
import com.example.model.WhisperSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    AUDIO_FILE("Berkas Audio"),
    LIVE_MIC("Rekam Langsung"),
    SRT_RESULT("Hasil & SRT"),
    MODELS("Model Whisper"),
    SETTINGS("Pengaturan")
}

data class SupportedLanguage(val code: String, val label: String) {
    companion object {
        val LIST = listOf(
            SupportedLanguage("auto", "Deteksi Otomatis"),
            SupportedLanguage("id", "Bahasa Indonesia (id)"),
            SupportedLanguage("en", "English (en)"),
            SupportedLanguage("ja", "Jepang / 日本語 (ja)"),
            SupportedLanguage("ko", "Korea / 한국어 (ko)"),
            SupportedLanguage("zh", "Mandarin / 中文 (zh)"),
            SupportedLanguage("ar", "Arab / العربية (ar)"),
            SupportedLanguage("es", "Spanyol (es)"),
            SupportedLanguage("fr", "Prancis (fr)"),
            SupportedLanguage("de", "Jerman (de)")
        )
    }
}

class WhisperViewModel(application: Application) : AndroidViewModel(application) {
    private val whisperManager = WhisperManager(application)
    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer()

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.AUDIO_FILE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Settings State
    private val _settings = MutableStateFlow(WhisperSettings())
    val settings: StateFlow<WhisperSettings> = _settings.asStateFlow()

    val availableProcessors: Int = Runtime.getRuntime().availableProcessors()
    val isGpuAvailable: Boolean = whisperManager.isGpuAvailable()
    val isVulkanAvailable: Boolean = whisperManager.isVulkanAvailable()
    val isOpenClAvailable: Boolean = whisperManager.isOpenClAvailable()

    // Models State
    private val _localModels = MutableStateFlow<List<LocalModelItem>>(emptyList())
    val localModels: StateFlow<List<LocalModelItem>> = _localModels.asStateFlow()

    private val _activeModel = MutableStateFlow<LocalModelItem?>(null)
    val activeModel: StateFlow<LocalModelItem?> = _activeModel.asStateFlow()

    private val _isLoadingModel = MutableStateFlow(false)
    val isLoadingModel: StateFlow<Boolean> = _isLoadingModel.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

    private val _statusMessage = MutableStateFlow("Pilih atau unduh model Whisper untuk memulai.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Audio File State
    private val _selectedAudioUri = MutableStateFlow<Uri?>(null)
    val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri.asStateFlow()

    private val _decodedAudio = MutableStateFlow<DecodedAudioResult?>(null)
    val decodedAudio: StateFlow<DecodedAudioResult?> = _decodedAudio.asStateFlow()

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding: StateFlow<Boolean> = _isDecoding.asStateFlow()

    private val _decodeProgress = MutableStateFlow(0f)
    val decodeProgress: StateFlow<Float> = _decodeProgress.asStateFlow()

    // Audio Player State
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying
    val playerCurrentMs: StateFlow<Long> = audioPlayer.currentPositionMs
    val playerDurationMs: StateFlow<Long> = audioPlayer.durationMs

    // Live Mic State
    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording
    val recordingDurationMs: StateFlow<Long> = audioRecorder.durationMs
    val liveAmplitudes: StateFlow<List<Float>> = audioRecorder.amplitudeHistory
    private val _recordedAudioSamples = MutableStateFlow<FloatArray?>(null)
    val recordedAudioSamples: StateFlow<FloatArray?> = _recordedAudioSamples.asStateFlow()

    // Transcribe Settings
    private val _selectedLanguage = MutableStateFlow("auto")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _translateToEnglish = MutableStateFlow(false)
    val translateToEnglish: StateFlow<Boolean> = _translateToEnglish.asStateFlow()

    // Transcription Execution State
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcribeProgress = MutableStateFlow(0)
    val transcribeProgress: StateFlow<Int> = _transcribeProgress.asStateFlow()

    private val _transcribeSpeedText = MutableStateFlow("")
    val transcribeSpeedText: StateFlow<String> = _transcribeSpeedText.asStateFlow()

    private val _segments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val segments: StateFlow<List<SubtitleSegment>> = _segments.asStateFlow()

    private val _fullSrtText = MutableStateFlow("")
    val fullSrtText: StateFlow<String> = _fullSrtText.asStateFlow()

    private val _fullPlainText = MutableStateFlow("")
    val fullPlainText: StateFlow<String> = _fullPlainText.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _editingSegment = MutableStateFlow<SubtitleSegment?>(null)
    val editingSegment: StateFlow<SubtitleSegment?> = _editingSegment.asStateFlow()

    init {
        refreshLocalModels()
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setLanguage(langCode: String) {
        _selectedLanguage.value = langCode
    }

    fun setTranslateToEnglish(translate: Boolean) {
        _translateToEnglish.value = translate
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Settings actions
    fun setComputeDevice(device: ComputeDevice) {
        _settings.value = _settings.value.copy(computeDevice = device)
        val current = _activeModel.value
        if (current != null) {
            loadModel(current)
        }
    }

    fun setThreadCount(count: Int) {
        val clamped = count.coerceIn(1, 16)
        _settings.value = _settings.value.copy(threadCount = clamped)
        val current = _activeModel.value
        if (current != null) {
            loadModel(current)
        }
    }

    fun setFlashAttention(enabled: Boolean) {
        _settings.value = _settings.value.copy(flashAttention = enabled)
        val current = _activeModel.value
        if (current != null) {
            loadModel(current)
        }
    }

    fun setTokenTimestamps(enabled: Boolean) {
        _settings.value = _settings.value.copy(tokenTimestamps = enabled)
    }

    fun setBeamSize(size: Int) {
        _settings.value = _settings.value.copy(beamSize = size.coerceIn(1, 5))
    }

    fun setTemperature(temp: Float) {
        _settings.value = _settings.value.copy(temperature = temp.coerceIn(0.0f, 1.0f))
    }

    fun resetSettings() {
        val defaultThreads = availableProcessors.coerceIn(2, 6)
        _settings.value = WhisperSettings(
            computeDevice = ComputeDevice.CPU,
            threadCount = defaultThreads,
            flashAttention = false,
            tokenTimestamps = true,
            beamSize = 1,
            temperature = 0.0f
        )
        val current = _activeModel.value
        if (current != null) {
            loadModel(current)
        }
    }

    fun refreshLocalModels() {
        val models = whisperManager.getLocalModels()
        _localModels.value = models
        val activePath = whisperManager.getActiveModelPath()
        _activeModel.value = models.find { it.filePath == activePath }

        if (_activeModel.value == null && models.isNotEmpty() && !_isLoadingModel.value) {
            val autoPick = models.find { it.presetId == "tiny" } ?: models.first()
            loadModel(autoPick)
        }
    }

    fun downloadPreset(preset: WhisperModelPreset) {
        viewModelScope.launch {
            try {
                whisperManager.downloadPresetModel(preset) { state ->
                    val map = _downloadStates.value.toMutableMap()
                    map[preset.id] = state
                    _downloadStates.value = map
                }
                refreshLocalModels()
                val loadedItem = _localModels.value.find { it.presetId == preset.id }
                if (loadedItem != null) {
                    loadModel(loadedItem)
                }
            } catch (e: Exception) {
                val map = _downloadStates.value.toMutableMap()
                map[preset.id] = ModelDownloadState.Error(e.message ?: "Gagal mengunduh")
                _downloadStates.value = map
            }
        }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            try {
                _isLoadingModel.value = true
                _statusMessage.value = "Mengimpor model dari berkas..."
                val imported = whisperManager.importCustomModel(uri)
                refreshLocalModels()
                loadModel(imported)
            } catch (e: Exception) {
                _statusMessage.value = "Gagal mengimpor: ${e.message}"
            } finally {
                _isLoadingModel.value = false
            }
        }
    }

    fun loadModel(model: LocalModelItem) {
        viewModelScope.launch {
            _isLoadingModel.value = true
            val dev = _settings.value.computeDevice.name
            val threads = _settings.value.threadCount
            _statusMessage.value = "Memuat model ${model.name} [$dev, $threads Threads]..."
            try {
                whisperManager.loadModel(model.filePath, _settings.value)
                refreshLocalModels()
                _statusMessage.value = "Model ${model.name} aktif di $dev ($threads Threads)"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal memuat model: ${e.message}"
            } finally {
                _isLoadingModel.value = false
            }
        }
    }

    fun deleteModel(model: LocalModelItem) {
        whisperManager.deleteModel(model.filePath)
        refreshLocalModels()
    }

    // Audio File Handling
    fun selectAudioUri(uri: Uri, context: Context) {
        _selectedAudioUri.value = uri
        audioPlayer.stop()
        viewModelScope.launch {
            _isDecoding.value = true
            _decodeProgress.value = 0f
            _statusMessage.value = "Mendekode berkas audio..."
            try {
                val result = AudioDecoder.decodeAudioUri(context, uri) { progress ->
                    _decodeProgress.value = progress
                }
                _decodedAudio.value = result
                _statusMessage.value = "Audio siap: ${result.fileName} (${String.format("%.1f", result.durationMs / 1000f)}s)"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal membaca audio: ${e.message}"
                Toast.makeText(context, "Gagal memuat audio: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isDecoding.value = false
            }
        }
    }

    fun playSelectedAudio(context: Context) {
        val uri = _selectedAudioUri.value
        if (uri != null) {
            if (audioPlayer.isPlaying.value) {
                audioPlayer.pause()
            } else {
                audioPlayer.playUri(context, uri, viewModelScope)
            }
        }
    }

    fun seekPlayer(posMs: Long) {
        audioPlayer.seekTo(posMs)
    }

    // Live Recording Handling
    fun toggleRecording() {
        if (audioRecorder.isRecording.value) {
            val samples = audioRecorder.stop()
            _recordedAudioSamples.value = samples
            _statusMessage.value = "Rekaman selesai: ${String.format("%.1f", samples.size / 16000f)}s"
        } else {
            audioPlayer.stop()
            _recordedAudioSamples.value = null
            val started = audioRecorder.start(viewModelScope)
            if (started) {
                _statusMessage.value = "Merekam suara..."
            } else {
                _statusMessage.value = "Gagal menginisialisasi mikrofon"
            }
        }
    }

    fun playRecordedAudio() {
        val samples = _recordedAudioSamples.value ?: return
        if (audioPlayer.isPlaying.value) {
            audioPlayer.stop()
        } else {
            audioPlayer.playFloatSamples(samples, viewModelScope)
        }
    }

    // Transcription Execution
    fun startTranscriptionForFile() {
        val audio = _decodedAudio.value?.samples ?: return
        startTranscription(audio)
    }

    fun startTranscriptionForRecording() {
        val samples = _recordedAudioSamples.value ?: return
        startTranscription(samples)
    }

    private fun startTranscription(samples: FloatArray) {
        if (!whisperManager.isModelLoaded()) {
            _statusMessage.value = "Peringatan: Harap pilih dan muat model Whisper terlebih dahulu."
            _currentScreen.value = AppScreen.MODELS
            return
        }

        viewModelScope.launch {
            _isTranscribing.value = true
            _transcribeProgress.value = 5
            _segments.value = emptyList()
            _fullSrtText.value = ""
            _fullPlainText.value = ""
            val dev = _settings.value.computeDevice.name
            _statusMessage.value = "Mentranskripsikan audio secara lokal ($dev)..."

            val startTime = System.currentTimeMillis()
            val incomingSegments = mutableListOf<SubtitleSegment>()

            try {
                val result = whisperManager.transcribeAudio(
                    samples = samples,
                    language = _selectedLanguage.value,
                    translate = _translateToEnglish.value,
                    settings = _settings.value,
                    onSegmentReceived = { seg ->
                        incomingSegments.add(seg)
                        _segments.value = incomingSegments.toList()
                        _fullSrtText.value = SubtitleSegment.toFullSrt(incomingSegments)
                        _fullPlainText.value = SubtitleSegment.toPlainText(incomingSegments)
                    },
                    onProgress = { progress ->
                        _transcribeProgress.value = progress
                    }
                )

                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                val audioDurationSec = samples.size / 16000.0
                val speedRatio = if (elapsed > 0) audioDurationSec / elapsed else 1.0

                _transcribeSpeedText.value = String.format("Selesai dalam %.1fs (Kecepatan: %.1fx real-time via %s)", elapsed, speedRatio, dev)
                _statusMessage.value = "Transkripsi sukses! ${result.size} segmen SRT dihasilkan."
                _currentScreen.value = AppScreen.SRT_RESULT
            } catch (e: Exception) {
                _statusMessage.value = "Transkripsi gagal: ${e.message}"
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    // Subtitle editing
    fun startEditSegment(segment: SubtitleSegment) {
        _editingSegment.value = segment
    }

    fun cancelEditSegment() {
        _editingSegment.value = null
    }

    fun saveEditedSegment(updated: SubtitleSegment) {
        val list = _segments.value.toMutableList()
        val index = list.indexOfFirst { it.index == updated.index }
        if (index != -1) {
            list[index] = updated
            _segments.value = list
            _fullSrtText.value = SubtitleSegment.toFullSrt(list)
            _fullPlainText.value = SubtitleSegment.toPlainText(list)
        }
        _editingSegment.value = null
    }

    fun deleteSegment(segment: SubtitleSegment) {
        val list = _segments.value.filter { it.index != segment.index }
            .mapIndexed { idx, s -> s.copy(index = idx + 1) }
        _segments.value = list
        _fullSrtText.value = SubtitleSegment.toFullSrt(list)
        _fullPlainText.value = SubtitleSegment.toPlainText(list)
    }

    fun shareSrt(context: Context) {
        val srt = _fullSrtText.value
        if (srt.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Whisper_Transcript.srt")
            putExtra(Intent.EXTRA_TEXT, srt)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Subtitle SRT"))
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        audioRecorder.stop()
        whisperManager.unloadActiveModel()
    }
}
