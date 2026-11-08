package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AudioFileScreen
import com.example.ui.screens.LiveMicScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SrtResultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.WhisperViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WhisperViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val activeModel by viewModel.activeModel.collectAsState()
                val settings by viewModel.settings.collectAsState()
                val segments by viewModel.segments.collectAsState()

                BackHandler(enabled = currentScreen != AppScreen.AUDIO_FILE) {
                    viewModel.setScreen(AppScreen.AUDIO_FILE)
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Subtitles,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Whisper SRT",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = "${settings.computeDevice.name} • ${settings.threadCount}T",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            actions = {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (activeModel != null) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { viewModel.setScreen(AppScreen.MODELS) }
                                        .testTag("topbar_model_chip")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (activeModel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (activeModel != null) {
                                                activeModel!!.name.take(14)
                                            } else {
                                                "Pilih Model"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (activeModel != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.AUDIO_FILE,
                                onClick = { viewModel.setScreen(AppScreen.AUDIO_FILE) },
                                icon = {
                                    Icon(Icons.Default.AudioFile, contentDescription = "Berkas Audio")
                                },
                                label = { Text("Berkas") },
                                modifier = Modifier.testTag("nav_audio_file")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.LIVE_MIC,
                                onClick = { viewModel.setScreen(AppScreen.LIVE_MIC) },
                                icon = {
                                    Icon(Icons.Default.Mic, contentDescription = "Rekam Langsung")
                                },
                                label = { Text("Rekam") },
                                modifier = Modifier.testTag("nav_live_mic")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SRT_RESULT,
                                onClick = { viewModel.setScreen(AppScreen.SRT_RESULT) },
                                icon = {
                                    if (segments.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${segments.size}") } }) {
                                            Icon(Icons.Default.Subtitles, contentDescription = "Hasil & SRT")
                                        }
                                    } else {
                                        Icon(Icons.Default.Subtitles, contentDescription = "Hasil & SRT")
                                    }
                                },
                                label = { Text("Hasil SRT") },
                                modifier = Modifier.testTag("nav_srt_result")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.MODELS,
                                onClick = { viewModel.setScreen(AppScreen.MODELS) },
                                icon = {
                                    Icon(Icons.Default.Psychology, contentDescription = "Model Whisper")
                                },
                                label = { Text("Model") },
                                modifier = Modifier.testTag("nav_models")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SETTINGS,
                                onClick = { viewModel.setScreen(AppScreen.SETTINGS) },
                                icon = {
                                    Icon(Icons.Default.Settings, contentDescription = "Setting")
                                },
                                label = { Text("Setting") },
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            AppScreen.AUDIO_FILE -> AudioFileScreen(viewModel = viewModel)
                            AppScreen.LIVE_MIC -> LiveMicScreen(viewModel = viewModel)
                            AppScreen.SRT_RESULT -> SrtResultScreen(viewModel = viewModel)
                            AppScreen.MODELS -> ModelsScreen(viewModel = viewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
