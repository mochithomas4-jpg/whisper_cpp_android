package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun StaticWaveform(
    amplitudes: List<Float>,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val progress = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(totalDurationMs) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((fraction * totalDurationMs).toLong())
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val count = amplitudes.size.coerceAtLeast(1)
            val barWidth = (size.width / count) * 0.7f
            val spacing = (size.width / count) * 0.3f
            val progressX = size.width * progress

            for (i in amplitudes.indices) {
                val amp = amplitudes[i].coerceIn(0.1f, 1.0f)
                val barHeight = size.height * amp * 0.9f
                val x = i * (barWidth + spacing)
                val y = (size.height - barHeight) / 2f

                val color = if (x <= progressX) activeColor else inactiveColor

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }

            // Draw progress indicator line
            if (totalDurationMs > 0) {
                drawLine(
                    color = activeColor,
                    start = Offset(progressX, 0f),
                    end = Offset(progressX, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun LiveWaveform(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseValue"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalBars = 36
        val barWidth = (size.width / totalBars) * 0.65f
        val spacing = (size.width / totalBars) * 0.35f

        val displayAmps = if (amplitudes.isEmpty() || !isRecording) {
            List(totalBars) { 0.15f }
        } else {
            val list = amplitudes.takeLast(totalBars)
            if (list.size < totalBars) {
                List(totalBars - list.size) { 0.15f } + list
            } else {
                list
            }
        }

        for (i in 0 until totalBars) {
            val baseAmp = displayAmps.getOrElse(i) { 0.15f }
            val dynamicAmp = if (isRecording) {
                (baseAmp * (if (i % 2 == 0) pulse else (2.0f - pulse))).coerceIn(0.12f, 1.0f)
            } else {
                0.15f
            }

            val barHeight = size.height * dynamicAmp * 0.95f
            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                color = barColor.copy(alpha = if (isRecording) 0.95f else 0.4f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
