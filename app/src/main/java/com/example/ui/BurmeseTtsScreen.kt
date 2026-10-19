package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.AudioPlayerControlCard
import com.example.ui.components.NormalizationControlsCard
import com.example.ui.components.PronunciationDictionaryBottomSheet
import com.example.ui.components.QuickPauseToolbar
import com.example.ui.components.TextInspectionDialog
import com.example.ui.components.TonePresetSelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurmeseTtsScreen(
    viewModel: BurmeseTtsViewModel,
    modifier: Modifier = Modifier
) {
    val inputField by viewModel.inputField.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val normResult by viewModel.normalizationResult.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val amplitudes by viewModel.amplitudes.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dictionarySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDictionarySheet by remember { mutableStateOf(false) }
    var showInspectorDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showSampleMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Burmese TTS Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "သဘာဝကျသော မြန်မာ အသံထွက် ဓာတ်ခွဲခန်း",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Sample text loader menu
                    Box {
                        IconButton(
                            onClick = { showSampleMenu = true },
                            modifier = Modifier.testTag("sample_texts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Load Sample Texts"
                            )
                        }
                        DropdownMenu(
                            expanded = showSampleMenu,
                            onDismissRequest = { showSampleMenu = false }
                        ) {
                            Text(
                                text = "  Select Sample Text:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            SampleTextType.entries.forEach { sample ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(sample.title, fontWeight = FontWeight.Medium)
                                            Text(
                                                sample.recommendedTone.titleMy,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.loadSample(sample)
                                        showSampleMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Pronunciation dictionary badge button
                    IconButton(
                        onClick = { showDictionarySheet = true },
                        modifier = Modifier.testTag("topbar_dictionary_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text("${rules.count { it.isEnabled }}")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Open Pronunciation Dictionary"
                            )
                        }
                    }

                    // Settings / API key button
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("topbar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings and API Key"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Voice Acting & Tone Presets (System Instruction Builder)
            TonePresetSelector(
                selectedPreset = settings.selectedTonePreset,
                onSelectPreset = { viewModel.selectTonePreset(it) }
            )

            // 2. Quick Pause Insertion Toolbar (Above text input box)
            QuickPauseToolbar(
                onInsert = { symbol -> viewModel.insertSymbolAtCursor(symbol) },
                onClear = { viewModel.clearText() }
            )

            // 3. Burmese Text Input Field
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputField,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("burmese_input_text_field"),
                        placeholder = {
                            Text(
                                text = "မြန်မာ စာသား ရိုက်ထည့်ပါ ... (ဥပမာ: လက်ဖက်ရည် ၎င်း၌ သောက်၍ ...)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Text statistics & Quick preview badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${normResult.characterCount} chars | ${normResult.pauseCount} pause marks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (normResult.replacementsApplied.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                onClick = { showInspectorDialog = true },
                                modifier = Modifier.testTag("preview_replacements_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "${normResult.replacementsApplied.size} phonetic replacements",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Normalized speech preview snippet
                    if (settings.isPreprocessingEnabled && normResult.replacementsApplied.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Phonetic Spoken Preview:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = normResult.normalizedText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. Burmese Text Pre-processing & Normalization Controls
            NormalizationControlsCard(
                settings = settings,
                activeRulesCount = rules.count { it.isEnabled },
                onTogglePreprocessing = { viewModel.togglePreprocessing(it) },
                onToggleLiterarySymbols = { viewModel.toggleLiterarySymbols(it) },
                onToggleConversational = { viewModel.toggleConversationalStyle(it) },
                onToggleBreathingPauses = { viewModel.toggleBreathingPauses(it) },
                onTogglePhoneticDictionary = { viewModel.togglePhoneticDictionary(it) },
                onOpenDictionary = { showDictionarySheet = true },
                onOpenInspector = { showInspectorDialog = true }
            )

            // 5. Audio Synthesizer & Player Controls
            AudioPlayerControlCard(
                playbackState = playbackState,
                amplitudes = amplitudes,
                isGenerating = isGenerating,
                engineType = settings.engineType,
                selectedVoice = settings.selectedVoiceName,
                speechSpeed = settings.speechSpeed,
                onSpeak = { viewModel.speak() },
                onPause = { viewModel.pauseAudio() },
                onResume = { viewModel.resumeAudio() },
                onStop = { viewModel.stopAudio() },
                onSeek = { viewModel.seekAudio(it) },
                onChangeEngine = { viewModel.setEngineType(it) },
                onChangeVoice = { viewModel.setVoice(it) },
                onChangeSpeed = { viewModel.setSpeed(it) }
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Pronunciation Dictionary Bottom Sheet
    if (showDictionarySheet) {
        PronunciationDictionaryBottomSheet(
            sheetState = dictionarySheetState,
            rules = rules,
            onDismiss = {
                scope.launch { dictionarySheetState.hide() }
                showDictionarySheet = false
            },
            onAddRule = { orig, phon, desc -> viewModel.addRule(orig, phon, desc) },
            onToggleRule = { viewModel.toggleRule(it) },
            onDeleteRule = { viewModel.deleteRule(it) },
            onResetDefaults = { viewModel.resetRulesToDefaults() }
        )
    }

    // Normalization Text Inspector Dialog
    if (showInspectorDialog) {
        TextInspectionDialog(
            result = normResult,
            onDismiss = { showInspectorDialog = false }
        )
    }

    // Settings / API Key Dialog
    if (showApiKeyDialog) {
        var inputKey by remember { mutableStateOf(userApiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Gemini API Key Settings")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "The app uses Gemini 2.0 Flash Audio Generation for high-fidelity Burmese neural voice synthesis.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val buildConfigKey = BuildConfig.GEMINI_API_KEY
                    if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                        Text(
                            text = "✓ API key auto-injected from AI Studio Secrets panel.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "You can enter a custom Gemini API key below or configure GEMINI_API_KEY in the AI Studio Secrets panel:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("Custom Gemini API Key (Optional)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_input")
                    )

                    Text(
                        text = "Tip: You can also use the 'Device TTS Engine' toggle at any time to test on-device speech synthesis without an API key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserApiKey(inputKey)
                        showApiKeyDialog = false
                    },
                    modifier = Modifier.testTag("save_api_key_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
