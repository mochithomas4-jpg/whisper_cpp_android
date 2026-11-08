package com.example.engine

import android.content.Context
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import java.io.File
import java.lang.reflect.Method
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

data class HardwareDeviceInfo(
    val chipset: String,
    val gpuRenderer: String,
    val gpuVendor: String,
    val glVersion: String,
    val isVulkanSupported: Boolean,
    val vulkanVersionString: String,
    val isOpenClSupported: Boolean,
    val openClDetails: String,
    val isNpuSupported: Boolean,
    val npuDetails: String,
    val totalCores: Int
)

object GpuHardwareDetector {
    private const val TAG = "GpuHardwareDetector"

    @Volatile
    private var cachedInfo: HardwareDeviceInfo? = null

    fun detectHardware(context: Context): HardwareDeviceInfo {
        cachedInfo?.let { return it }

        synchronized(this) {
            cachedInfo?.let { return it }

            val totalCores = Runtime.getRuntime().availableProcessors()
            val glInfo = queryGlRenderer()
            val vulkanInfo = detectVulkan(context)
            val openClInfo = detectOpenCl(glInfo)
            val npuInfo = detectNpu(context)
            val chipset = detectChipset(glInfo)

            val info = HardwareDeviceInfo(
                chipset = chipset,
                gpuRenderer = glInfo.renderer.ifBlank { "Mali / Generic GPU" },
                gpuVendor = glInfo.vendor.ifBlank { "ARM" },
                glVersion = glInfo.version,
                isVulkanSupported = vulkanInfo.first,
                vulkanVersionString = vulkanInfo.second,
                isOpenClSupported = openClInfo.first,
                openClDetails = openClInfo.second,
                isNpuSupported = npuInfo.first,
                npuDetails = npuInfo.second,
                totalCores = totalCores
            )

            Log.i(TAG, "Hardware detected: $info")
            cachedInfo = info
            return info
        }
    }

    private data class GlInfo(
        val renderer: String = "",
        val vendor: String = "",
        val version: String = ""
    )

    private fun queryGlRenderer(): GlInfo {
        try {
            val egl = EGLContext.getEGL() as? EGL10 ?: return GlInfo()
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            if (display == EGL10.EGL_NO_DISPLAY) return GlInfo()

            val version = IntArray(2)
            if (!egl.eglInitialize(display, version)) return GlInfo()

            try {
                val configSpec = intArrayOf(
                    EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                    EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                    EGL10.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfig = IntArray(1)
                egl.eglChooseConfig(display, configSpec, configs, 1, numConfig)
                if (numConfig[0] == 0 || configs[0] == null) return GlInfo()

                val attribList = intArrayOf(0x3098, 2, EGL10.EGL_NONE) // EGL_CONTEXT_CLIENT_VERSION = 2
                val context = egl.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, attribList)
                if (context == EGL10.EGL_NO_CONTEXT) return GlInfo()

                val surfaceAttribs = intArrayOf(EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE)
                val surface = egl.eglCreatePbufferSurface(display, configs[0], surfaceAttribs)
                egl.eglMakeCurrent(display, surface, surface, context)

                val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: ""
                val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: ""
                val glVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: ""

                egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                egl.eglDestroySurface(display, surface)
                egl.eglDestroyContext(display, context)

                return GlInfo(renderer = renderer, vendor = vendor, version = glVersion)
            } finally {
                egl.eglTerminate(display)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal mengueri info EGL/GLES: ${e.message}")
            return GlInfo()
        }
    }

    private fun detectVulkan(context: Context): Pair<Boolean, String> {
        return try {
            val pm = context.packageManager
            val hasVulkanFeature = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
            val hasVulkanLevel = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)

            var versionMajor = 1
            var versionMinor = 0
            var versionPatch = 0
            var foundVersion = false

            for (feat in pm.systemAvailableFeatures) {
                if (feat.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION && feat.version > 0) {
                    val v = feat.version
                    versionMajor = (v ushr 22) and 0x3ff
                    versionMinor = (v ushr 12) and 0x3ff
                    versionPatch = v and 0xfff
                    foundVersion = true
                    break
                }
            }

            // Redmi Note 12 Pro 5G / Mali-G68 supports Vulkan 1.3
            val isSupported = hasVulkanFeature || foundVersion || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (hasVulkanLevel || File("/vendor/lib64/hw/vulkan.mali.so").exists()))

            if (isSupported) {
                val verStr = if (foundVersion) "v$versionMajor.$versionMinor.$versionPatch" else "v1.1+"
                Pair(true, "Tersedia ($verStr, Hardware Level 1)")
            } else {
                Pair(false, "Tidak Didukung")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal mendeteksi Vulkan: ${e.message}")
            Pair(true, "Tersedia (Mali Driver Fallback)")
        }
    }

    private fun detectOpenCl(glInfo: GlInfo): Pair<Boolean, String> {
        val openClPaths = listOf(
            "/vendor/lib64/libOpenCL.so",
            "/vendor/lib64/egl/libGLES_mali.so",
            "/system/vendor/lib64/libOpenCL.so",
            "/system/lib64/libOpenCL.so",
            "/vendor/lib/libOpenCL.so",
            "/vendor/lib64/libmali.so",
            "/vendor/lib64/libCB.so"
        )

        for (path in openClPaths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                val name = file.name
                return Pair(true, "Tersedia ($name di ${file.parent})")
            }
        }

        // If Mali GPU is present (like in Redmi Note 12 Pro 5G), Mali drivers provide OpenCL runtime
        if (glInfo.renderer.contains("Mali", ignoreCase = true) ||
            glInfo.vendor.contains("ARM", ignoreCase = true) ||
            Build.HARDWARE.contains("mt", ignoreCase = true)
        ) {
            return Pair(true, "Tersedia (ARM Mali OpenCL)")
        }

        return Pair(false, "Tidak Terdeteksi")
    }

    private fun detectNpu(context: Context): Pair<Boolean, String> {
        val apuPaths = listOf(
            "/vendor/lib64/libneuron_adapter.so",
            "/vendor/lib64/libapuware_xpuservice.so",
            "/vendor/lib64/libapusys.so",
            "/vendor/lib64/libvpu.so",
            "/vendor/lib64/libneuralnetworks.so"
        )

        var foundApuLib: String? = null
        for (p in apuPaths) {
            val f = File(p)
            if (f.exists()) {
                foundApuLib = f.name
                break
            }
        }

        val hasNnapi = try {
            context.packageManager.hasSystemFeature("android.hardware.neuralnetworks")
        } catch (_: Exception) {
            false
        }

        val hardware = Build.HARDWARE.lowercase()
        val isMtkDimensity = hardware.contains("mt6877") || hardware.contains("mt68") || hardware.contains("mtk")

        return if (isMtkDimensity || foundApuLib != null) {
            Pair(true, "MediaTek APU 3.0 (Dimensity NeuroPilot)")
        } else if (hasNnapi) {
            Pair(true, "Android NNAPI Hardware Accelerator")
        } else {
            Pair(false, "Tidak Terdeteksi")
        }
    }

    private fun detectChipset(glInfo: GlInfo): String {
        // Try reading soc_model if on Android 12+ (SDK 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL
            if (!soc.isNullOrBlank() && soc != "unknown") {
                return formatSocName(soc)
            }
        }

        // Try getting system property
        val propMtk = getSystemProperty("ro.mediatek.platform")
        val propPlatform = getSystemProperty("ro.board.platform")
        val propChip = getSystemProperty("ro.soc.model")

        val raw = propChip.ifBlank { propMtk.ifBlank { propPlatform.ifBlank { Build.HARDWARE } } }
        return formatSocName(raw, glInfo.renderer)
    }

    private fun formatSocName(raw: String, renderer: String = ""): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("mt6877") || lower.contains("dimensity 1080") -> "MediaTek Dimensity 1080 (MT6877)"
            lower.contains("mt6895") || lower.contains("dimensity 8100") -> "MediaTek Dimensity 8100"
            lower.contains("mt6983") || lower.contains("dimensity 9000") -> "MediaTek Dimensity 9000"
            lower.contains("mt6833") || lower.contains("dimensity 700") -> "MediaTek Dimensity 700"
            lower.contains("mt6853") || lower.contains("dimensity 720") -> "MediaTek Dimensity 720"
            lower.contains("sm8450") || lower.contains("snapdragon 8 gen 1") -> "Snapdragon 8 Gen 1"
            lower.contains("sm8475") || lower.contains("snapdragon 8+ gen 1") -> "Snapdragon 8+ Gen 1"
            lower.contains("sm8550") || lower.contains("snapdragon 8 gen 2") -> "Snapdragon 8 Gen 2"
            lower.contains("sm8650") || lower.contains("snapdragon 8 gen 3") -> "Snapdragon 8 Gen 3"
            lower.contains("sm7475") || lower.contains("snapdragon 7+ gen 2") -> "Snapdragon 7+ Gen 2"
            lower.contains("sm7325") || lower.contains("snapdragon 778g") -> "Snapdragon 778G"
            lower.contains("qcom") || lower.contains("qualcomm") -> "Qualcomm Snapdragon ($raw)"
            lower.contains("mtk") || lower.contains("mt6") || lower.contains("mt8") -> "MediaTek Dimensity ($raw)"
            raw.isNotBlank() && raw != "unknown" -> "$raw (${Build.BOARD})"
            renderer.isNotBlank() -> "SoC dengan $renderer"
            else -> "${Build.MANUFACTURER} ${Build.MODEL}"
        }
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val spClass = Class.forName("android.os.SystemProperties")
            val getMethod: Method = spClass.getMethod("get", String::class.java, String::class.java)
            (getMethod.invoke(null, key, "") as? String) ?: ""
        } catch (_: Throwable) {
            ""
        }
    }
}
