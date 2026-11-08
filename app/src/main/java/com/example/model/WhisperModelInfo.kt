package com.example.model

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedKBps: Double
    ) : ModelDownloadState()
    object Completed : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

enum class ComputeDevice(val label: String, val subtitle: String) {
    CPU("CPU (Multi-Core ARM)", "Paling stabil & universal untuk semua perangkat Android"),
    GPU("GPU (OpenCL / Vulkan)", "Akselerasi grafis hardware untuk inferensi lebih cepat"),
    NPU("NPU / Neural Engine", "Unit pemrosesan neural / tensor akselerator")
}

data class WhisperSettings(
    val computeDevice: ComputeDevice = ComputeDevice.CPU,
    val threadCount: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 6),
    val flashAttention: Boolean = false,
    val tokenTimestamps: Boolean = true,
    val beamSize: Int = 1,
    val temperature: Float = 0.0f
)

data class WhisperModelPreset(
    val id: String,
    val displayName: String,
    val filename: String,
    val repoId: String = "ggerganov/whisper.cpp",
    val estimatedSizeMb: Int,
    val description: String,
    val category: String,
    val isEnglishOnly: Boolean = false,
    val isQuantized: Boolean = false,
    val isRecommended: Boolean = false
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$repoId/resolve/main/$filename"

    companion object {
        val ALL_PRESETS = listOf(
            // Tiny series
            WhisperModelPreset(
                id = "tiny",
                displayName = "Whisper Tiny",
                filename = "ggml-tiny.bin",
                estimatedSizeMb = 75,
                category = "Tiny",
                description = "FP16 multi-bahasa. Paling ringan, konsumsi RAM ~390MB.",
                isRecommended = true
            ),
            WhisperModelPreset(
                id = "tiny.en",
                displayName = "Whisper Tiny.en",
                filename = "ggml-tiny.en.bin",
                estimatedSizeMb = 75,
                category = "Tiny",
                description = "FP16 khusus Bahasa Inggris. Paling cepat untuk audio EN.",
                isEnglishOnly = true
            ),
            WhisperModelPreset(
                id = "tiny-q5_1",
                displayName = "Whisper Tiny Q5_1",
                filename = "ggml-tiny-q5_1.bin",
                estimatedSizeMb = 31,
                category = "Tiny",
                description = "Terkecil (31 MB)! Kuantisasi 5-bit, sangat hemat penyimpanan.",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "tiny.en-q5_1",
                displayName = "Whisper Tiny.en Q5_1",
                filename = "ggml-tiny.en-q5_1.bin",
                estimatedSizeMb = 31,
                category = "Tiny",
                description = "Kuantisasi 5-bit khusus Bahasa Inggris. Ukuran super mini.",
                isEnglishOnly = true,
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "tiny-q8_0",
                displayName = "Whisper Tiny Q8_0",
                filename = "ggml-tiny-q8_0.bin",
                estimatedSizeMb = 42,
                category = "Tiny",
                description = "Kuantisasi 8-bit multi-bahasa. Kualitas mendekati FP16 ukuran 42MB.",
                isQuantized = true
            ),

            // Base series
            WhisperModelPreset(
                id = "base",
                displayName = "Whisper Base",
                filename = "ggml-base.bin",
                estimatedSizeMb = 142,
                category = "Base",
                description = "Keseimbangan prima antara kecepatan dan akurasi multi-bahasa."
            ),
            WhisperModelPreset(
                id = "base.en",
                displayName = "Whisper Base.en",
                filename = "ggml-base.en.bin",
                estimatedSizeMb = 142,
                category = "Base",
                description = "Akurasi tinggi untuk percakapan Bahasa Inggris.",
                isEnglishOnly = true
            ),
            WhisperModelPreset(
                id = "base-q5_1",
                displayName = "Whisper Base Q5_1",
                filename = "ggml-base-q5_1.bin",
                estimatedSizeMb = 57,
                category = "Base",
                description = "Kuantisasi 5-bit base. Ukuran ramping 57 MB.",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "base.en-q5_1",
                displayName = "Whisper Base.en Q5_1",
                filename = "ggml-base.en-q5_1.bin",
                estimatedSizeMb = 57,
                category = "Base",
                description = "Kuantisasi 5-bit khusus Bahasa Inggris ukuran 57 MB.",
                isEnglishOnly = true,
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "base-q8_0",
                displayName = "Whisper Base Q8_0",
                filename = "ggml-base-q8_0.bin",
                estimatedSizeMb = 79,
                category = "Base",
                description = "Kuantisasi 8-bit base multi-bahasa ukuran 79 MB.",
                isQuantized = true
            ),

            // Small series
            WhisperModelPreset(
                id = "small",
                displayName = "Whisper Small",
                filename = "ggml-small.bin",
                estimatedSizeMb = 466,
                category = "Small",
                description = "Akurasi profesional untuk audio kompleks & istilah khusus."
            ),
            WhisperModelPreset(
                id = "small.en",
                displayName = "Whisper Small.en",
                filename = "ggml-small.en.bin",
                estimatedSizeMb = 466,
                category = "Small",
                description = "Model Small FP16 khusus Bahasa Inggris tingkat tinggi.",
                isEnglishOnly = true
            ),
            WhisperModelPreset(
                id = "small.en-tdrz",
                displayName = "Whisper Small.en TDRZ",
                filename = "ggml-small.en-tdrz.bin",
                repoId = "akashmjn/tinydiarize-whisper.cpp",
                estimatedSizeMb = 466,
                category = "Small",
                description = "Dilengkapi kemampuan deteksi pergantian pembicara (Tinydiarize).",
                isEnglishOnly = true
            ),
            WhisperModelPreset(
                id = "small-q5_1",
                displayName = "Whisper Small Q5_1",
                filename = "ggml-small-q5_1.bin",
                estimatedSizeMb = 182,
                category = "Small",
                description = "Kuantisasi 5-bit model Small multi-bahasa (182 MB).",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "small.en-q5_1",
                displayName = "Whisper Small.en Q5_1",
                filename = "ggml-small.en-q5_1.bin",
                estimatedSizeMb = 182,
                category = "Small",
                description = "Kuantisasi 5-bit model Small Bahasa Inggris (182 MB).",
                isEnglishOnly = true,
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "small-q8_0",
                displayName = "Whisper Small Q8_0",
                filename = "ggml-small-q8_0.bin",
                estimatedSizeMb = 253,
                category = "Small",
                description = "Kuantisasi 8-bit model Small multi-bahasa (253 MB).",
                isQuantized = true
            ),

            // Medium series
            WhisperModelPreset(
                id = "medium",
                displayName = "Whisper Medium",
                filename = "ggml-medium.bin",
                estimatedSizeMb = 1530,
                category = "Medium",
                description = "Akurasi sangat tinggi untuk transkripsi audio studio."
            ),
            WhisperModelPreset(
                id = "medium.en",
                displayName = "Whisper Medium.en",
                filename = "ggml-medium.en.bin",
                estimatedSizeMb = 1530,
                category = "Medium",
                description = "Model Medium FP16 khusus Bahasa Inggris (1.5 GB).",
                isEnglishOnly = true
            ),
            WhisperModelPreset(
                id = "medium-q5_0",
                displayName = "Whisper Medium Q5_0",
                filename = "ggml-medium-q5_0.bin",
                estimatedSizeMb = 515,
                category = "Medium",
                description = "Medium terkompresi 5-bit (515 MB). Hemat RAM.",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "medium.en-q5_0",
                displayName = "Whisper Medium.en Q5_0",
                filename = "ggml-medium.en-q5_0.bin",
                estimatedSizeMb = 515,
                category = "Medium",
                description = "Medium 5-bit khusus Bahasa Inggris (515 MB).",
                isEnglishOnly = true,
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "medium-q8_0",
                displayName = "Whisper Medium Q8_0",
                filename = "ggml-medium-q8_0.bin",
                estimatedSizeMb = 769,
                category = "Medium",
                description = "Medium terkompresi 8-bit (769 MB). Presisi tinggi.",
                isQuantized = true
            ),

            // Large series
            WhisperModelPreset(
                id = "large-v1",
                displayName = "Whisper Large v1",
                filename = "ggml-large-v1.bin",
                estimatedSizeMb = 2900,
                category = "Large",
                description = "Model Large generasi pertama (2.9 GB)."
            ),
            WhisperModelPreset(
                id = "large-v2",
                displayName = "Whisper Large v2",
                filename = "ggml-large-v2.bin",
                estimatedSizeMb = 2900,
                category = "Large",
                description = "Model Large generasi kedua FP16 (2.9 GB)."
            ),
            WhisperModelPreset(
                id = "large-v2-q5_0",
                displayName = "Whisper Large v2 Q5_0",
                filename = "ggml-large-v2-q5_0.bin",
                estimatedSizeMb = 1030,
                category = "Large",
                description = "Large v2 kuantisasi 5-bit (~1.0 GB).",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "large-v2-q8_0",
                displayName = "Whisper Large v2 Q8_0",
                filename = "ggml-large-v2-q8_0.bin",
                estimatedSizeMb = 1530,
                category = "Large",
                description = "Large v2 kuantisasi 8-bit (~1.5 GB).",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "large-v3",
                displayName = "Whisper Large v3",
                filename = "ggml-large-v3.bin",
                estimatedSizeMb = 2900,
                category = "Large",
                description = "Model Large v3 andalan OpenAI dengan akurasi terunggul (2.9 GB)."
            ),
            WhisperModelPreset(
                id = "large-v3-q5_0",
                displayName = "Whisper Large v3 Q5_0",
                filename = "ggml-large-v3-q5_0.bin",
                estimatedSizeMb = 1030,
                category = "Large",
                description = "Large v3 kuantisasi 5-bit (~1.0 GB). Kualitas v3 ukuran 1GB.",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "large-v3-turbo",
                displayName = "Whisper Large v3 Turbo",
                filename = "ggml-large-v3-turbo.bin",
                estimatedSizeMb = 1530,
                category = "Large",
                description = "Large v3 Turbo 4x lebih cepat dari Large v3 biasa (1.5 GB)."
            ),
            WhisperModelPreset(
                id = "large-v3-turbo-q5_0",
                displayName = "Whisper Large v3 Turbo Q5_0",
                filename = "ggml-large-v3-turbo-q5_0.bin",
                estimatedSizeMb = 547,
                category = "Large",
                description = "Large v3 Turbo 5-bit (547 MB). Cepat & akurat untuk smartphone.",
                isQuantized = true
            ),
            WhisperModelPreset(
                id = "large-v3-turbo-q8_0",
                displayName = "Whisper Large v3 Turbo Q8_0",
                filename = "ggml-large-v3-turbo-q8_0.bin",
                estimatedSizeMb = 809,
                category = "Large",
                description = "Large v3 Turbo 8-bit (809 MB). Kombinasi optimal v3 Turbo.",
                isQuantized = true
            )
        )
    }
}

data class LocalModelItem(
    val name: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val isPreset: Boolean,
    val presetId: String? = null,
    val isLoaded: Boolean = false
) {
    val sizeFormatted: String
        get() = String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0))
}
