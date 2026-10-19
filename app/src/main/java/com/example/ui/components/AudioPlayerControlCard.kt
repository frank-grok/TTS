package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TtsEngineType
import com.example.service.PlaybackState

@Composable
fun AudioPlayerControlCard(
    playbackState: PlaybackState,
    amplitudes: List<Float>,
    isGenerating: Boolean,
    engineType: TtsEngineType,
    selectedVoice: String,
    speechSpeed: Float,
    onSpeak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Int) -> Unit,
    onChangeEngine: (TtsEngineType) -> Unit,
    onChangeVoice: (String) -> Unit,
    onChangeSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Engine Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = engineType == TtsEngineType.GEMINI_NEURAL,
                    onClick = { onChangeEngine(TtsEngineType.GEMINI_NEURAL) },
                    label = { Text("Gemini Neural Voice (AI Studio)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("engine_gemini_chip")
                )
                FilterChip(
                    selected = engineType == TtsEngineType.DEVICE_TTS,
                    onClick = { onChangeEngine(TtsEngineType.DEVICE_TTS) },
                    label = { Text("Device TTS Engine") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("engine_device_chip")
                )
            }

            // Voice Choice & Speed Row
            if (engineType == TtsEngineType.GEMINI_NEURAL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Actor:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Puck", "Aoede", "Kore", "Fenrir", "Charon").forEach { voice ->
                            val isSel = voice == selectedVoice
                            Surface(
                                onClick = { onChangeVoice(voice) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                tonalElevation = if (isSel) 2.dp else 0.dp,
                                modifier = Modifier.testTag("voice_select_$voice")
                            ) {
                                Text(
                                    text = voice,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Speed Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Speed: ${"%.2f".format(speechSpeed)}x",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(76.dp)
                )
                Slider(
                    value = speechSpeed,
                    onValueChange = onChangeSpeed,
                    valueRange = 0.75f..1.5f,
                    steps = 2,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("speed_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Waveform Audio Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    amplitudes.forEachIndexed { index, amp ->
                        val animHeight = remember { Animatable(amp) }
                        LaunchedEffect(amp) {
                            animHeight.animateTo(
                                targetValue = amp,
                                animationSpec = tween(120, easing = FastOutSlowInEasing)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height((36.dp * animHeight.value).coerceIn(4.dp, 36.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // Progress Bar (when playing via MediaPlayer)
            if (playbackState is PlaybackState.Playing || playbackState is PlaybackState.Paused) {
                val pos = when (playbackState) {
                    is PlaybackState.Playing -> playbackState.positionMs
                    is PlaybackState.Paused -> playbackState.positionMs
                    else -> 0
                }
                val dur = when (playbackState) {
                    is PlaybackState.Playing -> playbackState.durationMs
                    is PlaybackState.Paused -> playbackState.durationMs
                    else -> 1
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = pos.toFloat(),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..dur.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .testTag("playback_seek_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(pos), style = MaterialTheme.typography.labelSmall)
                        Text(formatMs(dur), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Primary Speak / Play / Pause Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Speak Button
                Button(
                    onClick = {
                        if (playbackState is PlaybackState.Paused) onResume()
                        else onSpeak()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("synthesize_speak_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Synthesizing...")
                    } else if (playbackState is PlaybackState.Playing) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Speaking Natural Burmese...")
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Synthesize & Speak (အသံထွက် နားဆင်မည်)")
                    }
                }

                // Pause Button
                if (playbackState is PlaybackState.Playing) {
                    FilledTonalIconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pause_audio_button")
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
                    }
                }

                // Stop Button
                if (playbackState is PlaybackState.Playing || playbackState is PlaybackState.Paused) {
                    FilledTonalIconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stop_audio_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
