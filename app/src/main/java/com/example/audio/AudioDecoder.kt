package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

data class DecodedAudioResult(
    val samples: FloatArray,
    val durationMs: Long,
    val fileName: String,
    val fileSizeBytes: Long,
    val waveformAmplitudes: List<Float>
)

object AudioDecoder {
    private const val TAG = "AudioDecoder"
    const val TARGET_SAMPLE_RATE = 16000

    suspend fun decodeAudioUri(
        context: Context,
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): DecodedAudioResult = withContext(Dispatchers.IO) {
        val fileName = getFileName(context, uri)
        val fileSizeBytes = getFileSize(context, uri)
        val durationMs = getDurationMs(context, uri)

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            // Fallback: Copy to temporary file and setDataSource
            val tempFile = copyUriToTempFile(context, uri)
            extractor.setDataSource(tempFile.absolutePath)
        }

        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        if (audioTrackIndex == -1 || format == null) {
            extractor.release()
            throw IllegalArgumentException("Tidak dapat menemukan trek audio pada berkas ini.")
        }

        extractor.selectTrack(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val originalSampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else {
            44100
        }
        val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            1
        }

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val rawPcmList = ArrayList<FloatArray>()
        var totalRawFloats = 0

        var isEOS = false
        val timeoutUs = 10000L
        val estimatedTotalUs = (durationMs * 1000).coerceAtLeast(1)

        while (true) {
            if (!isEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isEOS = true
                    } else {
                        val sampleTime = extractor.sampleTime
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                        extractor.advance()

                        val progress = (sampleTime.toFloat() / estimatedTotalUs.toFloat()).coerceIn(0f, 0.9f)
                        onProgress(progress)
                    }
                }
            }

            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

                    // Convert 16-bit PCM bytes to floats
                    val shortCount = bufferInfo.size / 2
                    val chunkFloats = FloatArray(shortCount)
                    val shortBuf = outputBuffer.asShortBuffer()
                    val tempShorts = ShortArray(shortCount)
                    shortBuf.get(tempShorts)

                    for (s in 0 until shortCount) {
                        chunkFloats[s] = tempShorts[s] / 32768.0f
                    }

                    rawPcmList.add(chunkFloats)
                    totalRawFloats += shortCount
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                Log.d(TAG, "Audio output format changed: $newFormat")
            } else if (isEOS && outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        onProgress(0.95f)

        // Combine all raw chunks
        val combinedRaw = FloatArray(totalRawFloats)
        var offset = 0
        for (chunk in rawPcmList) {
            System.arraycopy(chunk, 0, combinedRaw, offset, chunk.size)
            offset += chunk.size
        }

        // Convert channels to Mono if stereo/multichannel
        val monoFloats: FloatArray
        if (channelCount > 1) {
            val monoLength = combinedRaw.size / channelCount
            monoFloats = FloatArray(monoLength)
            for (i in 0 until monoLength) {
                var sum = 0f
                val base = i * channelCount
                for (ch in 0 until channelCount) {
                    sum += combinedRaw[base + ch]
                }
                monoFloats[i] = sum / channelCount
            }
        } else {
            monoFloats = combinedRaw
        }

        // Resample to 16,000 Hz if needed
        val targetSamples: FloatArray
        if (originalSampleRate != TARGET_SAMPLE_RATE && monoFloats.isNotEmpty()) {
            val ratio = originalSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
            val targetLength = (monoFloats.size / ratio).toInt()
            targetSamples = FloatArray(targetLength)

            for (i in 0 until targetLength) {
                val srcIndex = i * ratio
                val srcInt = srcIndex.toInt()
                val frac = (srcIndex - srcInt).toFloat()

                val s0 = monoFloats[srcInt.coerceIn(0, monoFloats.size - 1)]
                val s1 = monoFloats[(srcInt + 1).coerceIn(0, monoFloats.size - 1)]
                targetSamples[i] = s0 + frac * (s1 - s0)
            }
        } else {
            targetSamples = monoFloats
        }

        // Compute 60 waveform visualizer points
        val waveformPoints = computeWaveform(targetSamples, 60)
        onProgress(1.0f)

        DecodedAudioResult(
            samples = targetSamples,
            durationMs = if (durationMs > 0) durationMs else (targetSamples.size * 1000L / TARGET_SAMPLE_RATE),
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            waveformAmplitudes = waveformPoints
        )
    }

    private fun computeWaveform(samples: FloatArray, pointCount: Int): List<Float> {
        if (samples.isEmpty()) return List(pointCount) { 0.1f }
        val chunkSize = max(1, samples.size / pointCount)
        val result = mutableListOf<Float>()
        var maxVal = 0.01f

        for (p in 0 until pointCount) {
            val start = p * chunkSize
            val end = (start + chunkSize).coerceAtMost(samples.size)
            var sumSquare = 0.0
            var count = 0
            for (i in start until end) {
                val v = samples[i]
                sumSquare += v * v
                count++
            }
            val rms = if (count > 0) Math.sqrt(sumSquare / count).toFloat() else 0f
            result.add(rms)
            if (rms > maxVal) maxVal = rms
        }

        return result.map { (it / maxVal).coerceIn(0.1f, 1.0f) }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "audio_file"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.let { name = it }
        }
        return name
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
        }
        return 0L
    }

    private fun getDurationMs(context: Context, uri: Uri): Long {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            return durationStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            return 0L
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File {
        val tempFile = File.createTempFile("audio_decode_", ".tmp", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
