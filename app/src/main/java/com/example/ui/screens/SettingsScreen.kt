package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ComputeDevice
import com.example.viewmodel.WhisperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WhisperViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val settings by viewModel.settings.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Compute Backend (CPU, GPU, NPU)
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hardware,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Akselerasi Komputasi (Backend)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Pilih unit pemroses untuk inferensi Whisper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ComputeDevice.values().forEach { device ->
                    val isSelected = settings.computeDevice == device
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setComputeDevice(device) }
                            .testTag("backend_select_${device.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setComputeDevice(device) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                val subtitleText = when (device) {
                                    ComputeDevice.CPU -> "Multi-core CPU (${viewModel.availableProcessors} Cores ${Build.SUPPORTED_ABIS.firstOrNull() ?: "ARM"})"
                                    ComputeDevice.GPU -> if (viewModel.isGpuAvailable) {
                                        "GPU ${viewModel.hardwareInfo.gpuRenderer} (Vulkan / OpenCL)"
                                    } else {
                                        device.subtitle
                                    }
                                    ComputeDevice.NPU -> if (viewModel.isNpuAvailable) {
                                        viewModel.hardwareInfo.npuDetails
                                    } else {
                                        device.subtitle
                                    }
                                }
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Thread Count
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Jumlah Thread CPU",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Tersedia: ${viewModel.availableProcessors} core prosesor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${settings.threadCount} Threads",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val maxThreads = viewModel.availableProcessors.coerceAtLeast(4).coerceAtMost(16)
                Slider(
                    value = settings.threadCount.toFloat(),
                    onValueChange = { viewModel.setThreadCount(it.toInt()) },
                    valueRange = 1f..maxThreads.toFloat(),
                    steps = maxThreads - 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("threads_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "1 Thread (Hemat)", style = MaterialTheme.typography.labelSmall)
                    Text(text = "Optimal (${maxThreads / 2})", style = MaterialTheme.typography.labelSmall)
                    Text(text = "$maxThreads Threads (Maksimal)", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.setThreadCount(2) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("2 Threads", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.setThreadCount(4) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("4 Threads", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.setThreadCount(viewModel.availableProcessors) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Maks (${viewModel.availableProcessors})", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: Performance & Quality Flags
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Parameter Inferensi Subtitle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Flash Attention Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Flash Attention", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(text = "Optimasi self-attention untuk kecepatan ekstra & hemat RAM", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Switch(
                        checked = settings.flashAttention,
                        onCheckedChange = { viewModel.setFlashAttention(it) },
                        modifier = Modifier.testTag("flash_attention_toggle")
                    )
                }

                // Token Timestamps Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Token Timestamps (Presisi Tinggi)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(text = "Menghitung durasi level kata agar waktu subtitle lebih akurat", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = settings.tokenTimestamps,
                        onCheckedChange = { viewModel.setTokenTimestamps(it) },
                        modifier = Modifier.testTag("token_timestamps_toggle")
                    )
                }

                // Beam Search Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Beam Search Size", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            text = if (settings.beamSize == 1) "1 (Greedy / Tercepat)" else "${settings.beamSize} (Akurasi Tinggi)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.beamSize.toFloat(),
                        onValueChange = { viewModel.setBeamSize(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section: Hardware Telemetry
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Informasi Perangkat Keras",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                HardwareInfoRow("Perangkat", "${Build.MANUFACTURER.capitalize()} ${Build.MODEL}")
                HardwareInfoRow("SoC / Chipset", viewModel.hardwareInfo.chipset)
                HardwareInfoRow("GPU Renderer", viewModel.hardwareInfo.gpuRenderer)
                HardwareInfoRow("Vendor GPU", viewModel.hardwareInfo.gpuVendor)
                HardwareInfoRow("Core Prosesor", "${viewModel.availableProcessors} Cores (${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"})")
                HardwareInfoRow("Android", "Versi ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                HardwareInfoRow(
                    label = "Driver Vulkan",
                    value = viewModel.hardwareInfo.vulkanVersionString,
                    isSuccess = viewModel.isVulkanAvailable
                )
                HardwareInfoRow(
                    label = "Driver OpenCL",
                    value = viewModel.hardwareInfo.openClDetails,
                    isSuccess = viewModel.isOpenClAvailable
                )
                HardwareInfoRow(
                    label = "Akselerator NPU",
                    value = viewModel.hardwareInfo.npuDetails,
                    isSuccess = viewModel.isNpuAvailable
                )
                HardwareInfoRow("Engine STT", "whisper.cpp JNI Native (Offline)")
            }
        }

        // Reset Settings Button
        OutlinedButton(
            onClick = { viewModel.resetSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_settings_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kembalikan ke Pengaturan Default")
        }
    }
}

@Composable
private fun HardwareInfoRow(
    label: String,
    value: String,
    isSuccess: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSuccess != null) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isSuccess) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
