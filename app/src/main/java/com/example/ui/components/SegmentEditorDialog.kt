package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.SubtitleSegment

@Composable
fun SegmentEditorDialog(
    segment: SubtitleSegment,
    onDismiss: () -> Unit,
    onSave: (SubtitleSegment) -> Unit
) {
    var text by remember { mutableStateOf(segment.text) }
    var startTimeMsText by remember { mutableStateOf(segment.startTimeMs.toString()) }
    var endTimeMsText by remember { mutableStateOf(segment.endTimeMs.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Segmen Subtitle #${segment.index}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Teks Subtitle") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_segment_text"),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = startTimeMsText,
                    onValueChange = { startTimeMsText = it },
                    label = { Text("Waktu Mulai (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endTimeMsText,
                    onValueChange = { endTimeMsText = it },
                    label = { Text("Waktu Selesai (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = startTimeMsText.toLongOrNull() ?: segment.startTimeMs
                    val e = endTimeMsText.toLongOrNull() ?: segment.endTimeMs
                    onSave(segment.copy(text = text, startTimeMs = s, endTimeMs = e))
                },
                modifier = Modifier.testTag("save_segment_button")
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
