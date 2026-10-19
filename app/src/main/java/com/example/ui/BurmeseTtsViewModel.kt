package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BurmeseTonePreset
import com.example.data.model.NormalizationSettings
import com.example.data.model.PronunciationRule
import com.example.data.model.TtsEngineType
import com.example.data.repository.PronunciationRepository
import com.example.domain.BurmeseTextNormalizer
import com.example.domain.NormalizationResult
import com.example.service.AudioPlaybackManager
import com.example.service.GeminiTtsService
import com.example.service.PlaybackState
import com.example.service.TtsAudioResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SampleTextType(val title: String, val text: String, val recommendedTone: BurmeseTonePreset) {
    BAGAN_AND_LAPHET(
        title = "ပုဂံခရီးနှင့် လက်ဖက်ရည် (စကားပြော)",
        text = "မင်္ဂလာပါရှင်။ ပုဂံမြို့ ၌ ခရီးသွားရင်း လက်ဖက်ရည် သောက်၍ အေးချမ်းစွာ အနားယူကြပါစို့။ မြန်မာ့ရိုးရာ အလှတရားသည် အလွန်ပင် စိတ်နှလုံးကို ရွှင်လန်းစေပါသည်။",
        recommendedTone = BurmeseTonePreset.NATURAL_CONVERSATIONAL
    ),
    FORMAL_NEWS_REPORT(
        title = "တရားဝင် သတင်းကြေညာချက်",
        text = "ပြည်ထောင်စုသမ္မတ မြန်မာနိုင်ငံတော်၌ ၎င်း စီမံကိန်းကို ဆောလျင်စွာ အကောင်အထည်ဖော်မည် ဖြစ်ကြောင်း၊ သင်္ဘောဆိပ်ကမ်းသို့ ကွင်းဆင်းရောက်ရှိ၍ စစ်ဆေးခဲ့ကြောင်း သိရသည်။",
        recommendedTone = BurmeseTonePreset.NEWS_BROADCAST
    ),
    DRAMATIC_NOVEL(
        title = "ဝတ္ထု ဇာတ်လမ်းပြောသံ",
        text = "ညသန်းခေါင်ယံ အချိန်၌ ... လေပြင်းများ တဝူးဝူး တိုက်ခတ်နေခဲ့သည်။ ၎င်း သစ်ပင်ရိပ်အောက်တွင် မည်သူမျှ မရှိတော့ချေ။ သို့သော်လည်း ... တစ်စုံတစ်ရာ၏ ခြေသံကို ကြားလိုက်ရသည်။",
        recommendedTone = BurmeseTonePreset.DRAMATIC_STORYTELLER
    ),
    WARM_PODCAST(
        title = "နွေးထွေးသော Podcast မိတ်ဆက်",
        text = "မင်္ဂလာရှိသော ညနေခင်းလေးမှာ ကြိုဆိုပါတယ်ရှင်။ ဒီနေ့ညမှာတော့ ... ကျွန်မတို့ ၏ နေ့စဉ်ဘဝမှာ စိတ်ဖိစီးမှုတွေကို ဘယ်လို လျှော့ချကြမလဲ ဆိုတာကို အေးအေးဆေးဆေး ဆွေးနွေးသွားပါမယ်။",
        recommendedTone = BurmeseTonePreset.CALM_WARM_PODCAST
    )
}

class BurmeseTtsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PronunciationRepository(application)
    private val ttsService = GeminiTtsService(application.cacheDir)
    val playbackManager = AudioPlaybackManager(application)

    val rules: StateFlow<List<PronunciationRule>> = repository.rules
    val settings: StateFlow<NormalizationSettings> = repository.settings
    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState
    val amplitudes: StateFlow<List<Float>> = playbackManager.amplitudes

    private val _inputField = MutableStateFlow(
        TextFieldValue(text = SampleTextType.BAGAN_AND_LAPHET.text)
    )
    val inputField: StateFlow<TextFieldValue> = _inputField.asStateFlow()

    private val _userApiKey = MutableStateFlow("")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Dynamically calculate normalization preview whenever text, rules, or settings change
    val normalizationResult: StateFlow<NormalizationResult> = combine(
        inputField,
        rules,
        settings
    ) { field, ruleList, currentSettings ->
        BurmeseTextNormalizer.normalize(
            inputText = field.text,
            settings = currentSettings,
            customRules = ruleList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BurmeseTextNormalizer.normalize(
            inputText = _inputField.value.text,
            settings = settings.value,
            customRules = rules.value
        )
    )

    fun onInputTextChanged(newValue: TextFieldValue) {
        _inputField.value = newValue
    }

    /**
     * Inserts quick pause markers or Burmese punctuation at the current cursor position.
     */
    fun insertSymbolAtCursor(symbol: String) {
        val current = _inputField.value
        val selection = current.selection
        val text = current.text

        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)

        val newText = text.substring(0, start) + symbol + text.substring(end)
        val newCursor = start + symbol.length

        _inputField.value = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    fun selectTonePreset(preset: BurmeseTonePreset) {
        repository.updateSettings(
            settings.value.copy(
                selectedTonePreset = preset,
                selectedVoiceName = preset.recommendedVoice
            )
        )
    }

    fun togglePreprocessing(enabled: Boolean) {
        repository.updateSettings(settings.value.copy(isPreprocessingEnabled = enabled))
    }

    fun toggleLiterarySymbols(enabled: Boolean) {
        repository.updateSettings(settings.value.copy(autoExpandLiterarySymbols = enabled))
    }

    fun toggleConversationalStyle(isConversational: Boolean) {
        repository.updateSettings(settings.value.copy(isConversationalStyle = isConversational))
    }

    fun toggleBreathingPauses(enabled: Boolean) {
        repository.updateSettings(settings.value.copy(enableNaturalBreathingPauses = enabled))
    }

    fun togglePhoneticDictionary(enabled: Boolean) {
        repository.updateSettings(settings.value.copy(isPhoneticDictionaryEnabled = enabled))
    }

    fun setSpeed(speed: Float) {
        repository.updateSettings(settings.value.copy(speechSpeed = speed))
    }

    fun setVoice(voice: String) {
        repository.updateSettings(settings.value.copy(selectedVoiceName = voice))
    }

    fun setEngineType(engine: TtsEngineType) {
        repository.updateSettings(settings.value.copy(engineType = engine))
    }

    fun setUserApiKey(key: String) {
        _userApiKey.value = key
    }

    fun loadSample(sample: SampleTextType) {
        _inputField.value = TextFieldValue(text = sample.text, selection = TextRange(sample.text.length))
        selectTonePreset(sample.recommendedTone)
        _uiMessage.value = "Loaded: ${sample.title}"
    }

    fun clearText() {
        _inputField.value = TextFieldValue("")
    }

    // Pronunciation Dictionary Actions
    fun addRule(originalWord: String, phoneticReplacement: String, description: String = "") {
        if (originalWord.isBlank() || phoneticReplacement.isBlank()) {
            _uiMessage.value = "Original word and phonetic replacement cannot be empty."
            return
        }
        repository.addRule(originalWord, phoneticReplacement, description)
        _uiMessage.value = "Added rule: $originalWord → $phoneticReplacement"
    }

    fun updateRule(rule: PronunciationRule) {
        repository.updateRule(rule)
    }

    fun toggleRule(ruleId: String) {
        repository.toggleRule(ruleId)
    }

    fun deleteRule(ruleId: String) {
        repository.deleteRule(ruleId)
        _uiMessage.value = "Rule deleted."
    }

    fun resetRulesToDefaults() {
        repository.resetRulesToDefaults()
        _uiMessage.value = "Dictionary reset to default 5 Burmese rules."
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    // Speech Synthesis & Playback
    fun speak() {
        val result = normalizationResult.value
        val textToSpeak = result.normalizedText
        if (textToSpeak.isBlank()) {
            _uiMessage.value = "Please enter some Burmese text to synthesize."
            return
        }

        val currentSettings = settings.value

        if (currentSettings.engineType == TtsEngineType.DEVICE_TTS) {
            playbackManager.speakWithDeviceTts(textToSpeak, currentSettings.speechSpeed)
            _uiMessage.value = "Speaking with on-device TTS engine..."
            return
        }

        // Gemini Neural Audio Generation
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val ttsResult = ttsService.synthesizeSpeech(
                    promptWithInstruction = result.finalTtsPrompt,
                    voiceName = currentSettings.selectedVoiceName,
                    customApiKey = _userApiKey.value.ifBlank { null }
                )

                _isGenerating.value = false

                when (ttsResult) {
                    is TtsAudioResult.Success -> {
                        _uiMessage.value = "Speech generated successfully! Playing..."
                        playbackManager.playAudioFile(ttsResult.audioFile)
                    }
                    is TtsAudioResult.Error -> {
                        _uiMessage.value = ttsResult.message
                        // If Gemini API fails (e.g. no internet or key), fallback option offered
                        Log.w("BurmeseTtsViewModel", "Gemini synthesis error: ${ttsResult.message}")
                    }
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _uiMessage.value = "Synthesis failed: ${e.localizedMessage}"
            }
        }
    }

    fun pauseAudio() = playbackManager.pause()
    fun resumeAudio() = playbackManager.resume()
    fun stopAudio() = playbackManager.stop()
    fun seekAudio(positionMs: Int) = playbackManager.seekTo(positionMs)

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
