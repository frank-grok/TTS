package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NormalizationSettings

@Composable
fun NormalizationControlsCard(
    settings: NormalizationSettings,
    activeRulesCount: Int,
    onTogglePreprocessing: (Boolean) -> Unit,
    onToggleLiterarySymbols: (Boolean) -> Unit,
    onToggleConversational: (Boolean) -> Unit,
    onToggleBreathingPauses: (Boolean) -> Unit,
    onTogglePhoneticDictionary: (Boolean) -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Master Pre-processing Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = if (settings.isPreprocessingEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "Burmese Text Pre-processing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (settings.isPreprocessingEnabled) "Active (Normalizing & Optimizing)" else "Disabled (Raw text sent to TTS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = settings.isPreprocessingEnabled,
                    onCheckedChange = onTogglePreprocessing,
                    modifier = Modifier.testTag("preprocessing_master_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(visible = settings.isPreprocessingEnabled) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1. Literary Symbol Expansion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-expand Literary Symbols",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "၎င်း → လည်းကောင်း, ၌ → တွင်, ၍ → ပြီး, ၏ → ${if (settings.isConversationalStyle) "ရဲ့" else "၏"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(
                            checked = settings.autoExpandLiterarySymbols,
                            onCheckedChange = onToggleLiterarySymbols,
                            modifier = Modifier.testTag("literary_symbols_switch")
                        )
                    }

                    // 1b. Formal vs Conversational Toggle (when literary expansion is on)
                    if (settings.autoExpandLiterarySymbols) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (settings.isConversationalStyle) "Conversational Burmese (၏ → ရဲ့)" else "Formal Burmese (၏ → ၏)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (settings.isConversationalStyle) "Transforms written genitive '၏' into colloquial spoken 'ရဲ့'"
                                    else "Keeps formal literary genitive marker '၏'",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isConversationalStyle,
                                onCheckedChange = onToggleConversational,
                                modifier = Modifier.testTag("conversational_style_switch")
                            )
                        }
                    }

                    // 2. Breathing Pauses (၊ and ။)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Natural Breathing Pauses",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Punctuation '၊' (short pause) and '။' (full stop) insert natural speech cadences",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.enableNaturalBreathingPauses,
                            onCheckedChange = onToggleBreathingPauses,
                            modifier = Modifier.testTag("breathing_pauses_switch")
                        )
                    }

                    // 3. Pronunciation Dictionary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Pronunciation Dictionary",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                    Text(
                                        text = "$activeRulesCount active",
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "Replaces written text with spoken phonetics (e.g. လက်ဖက် → လဖက်, ပုဂံ → ပဂံ)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isPhoneticDictionaryEnabled,
                            onCheckedChange = onTogglePhoneticDictionary,
                            modifier = Modifier.testTag("phonetic_dictionary_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Action buttons: Manage Dictionary & Inspect Normalization Diffs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElevatedButton(
                            onClick = onOpenDictionary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_dictionary_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "Pronunciation Rules",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        ElevatedButton(
                            onClick = onOpenInspector,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_inspector_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "Inspect Text Diffs",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
