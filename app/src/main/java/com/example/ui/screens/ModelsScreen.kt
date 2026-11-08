package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocalModelItem
import com.example.model.ModelDownloadState
import com.example.model.WhisperModelPreset
import com.example.viewmodel.AppScreen
import com.example.viewmodel.WhisperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    viewModel: WhisperViewModel,
    modifier: Modifier = Modifier
) {
    val localModels by viewModel.localModels.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val isLoadingModel by viewModel.isLoadingModel.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }

    val categories = listOf("Semua", "Tiny", "Base", "Small", "Medium", "Large", "Terunduh")

    val customModelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importModel(uri)
        }
    }

    val filteredPresets = WhisperModelPreset.ALL_PRESETS.filter { preset ->
        val matchesCategory = when (selectedCategory) {
            "Semua" -> true
            "Terunduh" -> localModels.any { it.presetId == preset.id || it.filePath.endsWith(preset.filename) }
            else -> preset.category.equals(selectedCategory, ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() ||
                preset.id.contains(searchQuery, ignoreCase = true) ||
                preset.displayName.contains(searchQuery, ignoreCase = true) ||
                preset.filename.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Model & Backend Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (activeModel != null) "Model Aktif: ${activeModel?.name}" else "Belum Ada Model Aktif",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Backend: ${settings.computeDevice.name} • ${settings.threadCount} Threads",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setScreen(AppScreen.SETTINGS) },
                            modifier = Modifier.testTag("go_to_settings_chip")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Setting", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari dari 30 model Whisper (misal: turbo, q5, small)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_models_input")
            )
        }

        // Category Filter Chips
        item {
            val chipScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val count = when (cat) {
                        "Semua" -> WhisperModelPreset.ALL_PRESETS.size
                        "Terunduh" -> localModels.size
                        else -> WhisperModelPreset.ALL_PRESETS.count { it.category.equals(cat, ignoreCase = true) }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text("$cat ($count)") }
                    )
                }
            }
        }

        // Import Custom Model Button
        item {
            OutlinedButton(
                onClick = { customModelPicker.launch("*/*") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("import_custom_model_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Impor Berkas Model Kustom (.bin / .gguf)", fontSize = 13.sp)
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Model Whisper (${filteredPresets.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Hugging Face ggerganov/whisper.cpp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Presets list
        items(filteredPresets, key = { it.id }) { preset ->
            val downloadedItem = localModels.find { it.presetId == preset.id || it.filePath.endsWith(preset.filename) }
            val isLoaded = downloadedItem != null && downloadedItem.filePath == activeModel?.filePath
            val dlState = downloadStates[preset.id] ?: ModelDownloadState.Idle

            PresetModelCard(
                preset = preset,
                localItem = downloadedItem,
                isLoaded = isLoaded,
                isLoadingModel = isLoadingModel,
                downloadState = dlState,
                onDownload = { viewModel.downloadPreset(preset) },
                onLoad = { if (downloadedItem != null) viewModel.loadModel(downloadedItem) },
                onDelete = { if (downloadedItem != null) viewModel.deleteModel(downloadedItem) }
            )
        }

        // Custom imported models
        val customModels = localModels.filter { !it.isPreset }
        if (customModels.isNotEmpty() && (selectedCategory == "Semua" || selectedCategory == "Terunduh")) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Model Kustom Diimpor (${customModels.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            items(customModels, key = { it.filePath }) { model ->
                CustomModelCard(
                    model = model,
                    isLoaded = model.filePath == activeModel?.filePath,
                    onLoad = { viewModel.loadModel(model) },
                    onDelete = { viewModel.deleteModel(model) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PresetModelCard(
    preset: WhisperModelPreset,
    localItem: LocalModelItem?,
    isLoaded: Boolean,
    isLoadingModel: Boolean,
    downloadState: ModelDownloadState,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isLoaded) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (preset.isRecommended) {
                            Spacer(modifier = Modifier.width(6.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Rekomendasi", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = preset.filename,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (preset.isEnglishOnly) {
                            Text(
                                text = "• English Only",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Text(
                    text = if (preset.estimatedSizeMb >= 1000) String.format("%.1f GB", preset.estimatedSizeMb / 1000f) else "${preset.estimatedSizeMb} MB",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Download Progress
            if (downloadState is ModelDownloadState.Downloading) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (downloadState.progressPercent >= 0) "${downloadState.progressPercent}%" else "Mengunduh...",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = String.format("%.1f KB/s", downloadState.speedKBps),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (downloadState.progressPercent >= 0) {
                        LinearProgressIndicator(
                            progress = { downloadState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            } else if (downloadState is ModelDownloadState.Error) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gagal: ${downloadState.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (localItem == null) {
                    Button(
                        onClick = onDownload,
                        enabled = downloadState !is ModelDownloadState.Downloading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("download_model_${preset.id}")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unduh (${if (preset.estimatedSizeMb >= 1000) String.format("%.1f GB", preset.estimatedSizeMb / 1000f) else "${preset.estimatedSizeMb} MB"})", fontSize = 12.sp)
                    }
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Berkas",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isLoaded) {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sedang Aktif", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onLoad,
                            enabled = !isLoadingModel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("load_model_${preset.id}")
                        ) {
                            Text("Muat Model", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomModelCard(
    model: LocalModelItem,
    isLoaded: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = model.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = model.sizeFormatted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
            }
            if (isLoaded) {
                FilledTonalButton(onClick = {}, enabled = false) {
                    Text("Aktif", fontSize = 12.sp)
                }
            } else {
                Button(onClick = onLoad) {
                    Text("Muat", fontSize = 12.sp)
                }
            }
        }
    }
}
