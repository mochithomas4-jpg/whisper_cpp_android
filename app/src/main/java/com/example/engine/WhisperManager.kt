package com.example.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.ComputeDevice
import com.example.model.LocalModelItem
import com.example.model.ModelDownloadState
import com.example.model.SubtitleSegment
import com.example.model.WhisperModelPreset
import com.example.model.WhisperSettings
import io.aatricks.llmedge.speech.stt.Whisper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class WhisperManager(private val context: Context) {
    companion object {
        private const val TAG = "WhisperManager"
        private const val MODELS_DIR = "whisper_models"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private var activeWhisper: Whisper? = null
    private var activeModelPath: String? = null
    private var currentLoadedSettings: WhisperSettings? = null

    val modelsDir: File
        get() = File(context.filesDir, MODELS_DIR).apply { if (!exists()) mkdirs() }

    fun getLocalModels(): List<LocalModelItem> {
        val dir = modelsDir
        val files = dir.listFiles { f -> f.isFile && (f.name.endsWith(".bin") || f.name.endsWith(".gguf")) } ?: emptyArray()

        return files.map { file ->
            val preset = WhisperModelPreset.ALL_PRESETS.find { it.filename == file.name }
            LocalModelItem(
                name = preset?.displayName ?: file.name,
                filePath = file.absolutePath,
                fileSizeBytes = file.length(),
                isPreset = preset != null,
                presetId = preset?.id,
                isLoaded = file.absolutePath == activeModelPath
            )
        }
    }

    fun isGpuAvailable(): Boolean {
        return try {
            Whisper.isOpenClAvailable() || Whisper.isVulkanBackendAvailable()
        } catch (_: Exception) {
            false
        }
    }

    fun isVulkanAvailable(): Boolean {
        return try {
            Whisper.isVulkanBackendAvailable()
        } catch (_: Exception) {
            false
        }
    }

    fun isOpenClAvailable(): Boolean {
        return try {
            Whisper.isOpenClAvailable()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun downloadPresetModel(
        preset: WhisperModelPreset,
        onProgress: (ModelDownloadState) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val targetFile = File(modelsDir, preset.filename)
        if (targetFile.exists() && targetFile.length() > 5 * 1024 * 1024) {
            onProgress(ModelDownloadState.Completed)
            return@withContext targetFile
        }

        val tempFile = File(modelsDir, "${preset.filename}.download")
        try {
            val request = Request.Builder()
                .url(preset.downloadUrl)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Gagal mengunduh: HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Respons kosong dari server")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L
            var speedKBps = 0.0

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32768)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        bytesSinceLastTime += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 400) {
                            speedKBps = (bytesSinceLastTime / 1024.0) / (elapsed / 1000.0)
                            lastTime = now
                            bytesSinceLastTime = 0L

                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt()
                            } else {
                                -1
                            }
                            onProgress(
                                ModelDownloadState.Downloading(
                                    progressPercent = percent,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedKBps = speedKBps
                                )
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.renameTo(targetFile)) {
                onProgress(ModelDownloadState.Completed)
                targetFile
            } else {
                throw IllegalStateException("Gagal menyimpan berkas model.")
            }
        } catch (e: Exception) {
            tempFile.delete()
            val msg = e.localizedMessage ?: "Terjadi kesalahan saat mengunduh"
            onProgress(ModelDownloadState.Error(msg))
            throw e
        }
    }

    suspend fun importCustomModel(uri: Uri): LocalModelItem = withContext(Dispatchers.IO) {
        val fileName = getDisplayNameFromUri(uri) ?: "imported_model_${System.currentTimeMillis()}.bin"
        val targetFile = File(modelsDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Tidak dapat membaca berkas yang dipilih")

        LocalModelItem(
            name = fileName,
            filePath = targetFile.absolutePath,
            fileSizeBytes = targetFile.length(),
            isPreset = false
        )
    }

    fun deleteModel(filePath: String): Boolean {
        if (filePath == activeModelPath) {
            unloadActiveModel()
        }
        val file = File(filePath)
        return file.exists() && file.delete()
    }

    suspend fun loadModel(
        filePath: String,
        settings: WhisperSettings = WhisperSettings()
    ): Whisper = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Berkas model tidak ditemukan: $filePath")
        }

        if (activeWhisper != null && activeModelPath == filePath && currentLoadedSettings == settings) {
            return@withContext activeWhisper!!
        }

        unloadActiveModel()

        val useGpu = settings.computeDevice == ComputeDevice.GPU || settings.computeDevice == ComputeDevice.NPU
        val nThreads = settings.threadCount.coerceIn(1, 16)
        val flashAttn = settings.flashAttention

        Log.i(TAG, "Memuat model Whisper: $filePath (device=${settings.computeDevice}, threads=$nThreads, flashAttn=$flashAttn)")

        val whisper = try {
            Whisper.load(
                filePath,
                useGpu,
                flashAttn,
                nThreads
            )
        } catch (e: Exception) {
            if (useGpu) {
                Log.w(TAG, "Gagal memuat dengan GPU/NPU (${e.message}), beralih ke mode CPU aman...")
                Whisper.load(
                    filePath,
                    false,
                    flashAttn,
                    nThreads
                )
            } else {
                throw e
            }
        }

        activeWhisper = whisper
        activeModelPath = filePath
        currentLoadedSettings = settings
        whisper
    }

    fun unloadActiveModel() {
        try {
            activeWhisper?.close()
        } catch (_: Exception) {
        }
        activeWhisper = null
        activeModelPath = null
        currentLoadedSettings = null
    }

    fun isModelLoaded(): Boolean = activeWhisper != null
    fun getActiveModelPath(): String? = activeModelPath

    suspend fun transcribeAudio(
        samples: FloatArray,
        language: String = "auto",
        translate: Boolean = false,
        settings: WhisperSettings = WhisperSettings(),
        onSegmentReceived: (SubtitleSegment) -> Unit = {},
        onProgress: (Int) -> Unit = {}
    ): List<SubtitleSegment> = withContext(Dispatchers.IO) {
        val whisper = activeWhisper ?: throw IllegalStateException("Model Whisper belum dimuat. Silakan pilih & muat model terlebih dahulu.")

        val detectedLang = if (language == "auto") {
            try {
                whisper.detectLanguage(samples, settings.threadCount)
            } catch (_: Exception) {
                "en"
            }
        } else {
            language
        }

        val params = Whisper.TranscribeParams().copy(
            nThreads = settings.threadCount.coerceIn(1, 16),
            language = if (language == "auto") (detectedLang ?: "en") else language,
            translate = translate,
            tokenTimestamps = settings.tokenTimestamps,
            beamSize = settings.beamSize.coerceIn(1, 5),
            temperature = settings.temperature.coerceIn(0.0f, 1.0f),
            printProgress = false
        )

        val rawSegments = whisper.transcribe(samples, params)
        val segments = rawSegments.mapIndexed { idx, s ->
            SubtitleSegment(
                index = idx + 1,
                startTimeMs = s.startTimeMs,
                endTimeMs = s.endTimeMs,
                text = s.text.trim()
            )
        }

        segments.forEach { onSegmentReceived(it) }
        onProgress(100)
        segments
    }

    private fun getDisplayNameFromUri(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }
}
