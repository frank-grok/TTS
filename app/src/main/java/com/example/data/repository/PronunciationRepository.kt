package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.BurmeseTonePreset
import com.example.data.model.NormalizationSettings
import com.example.data.model.PronunciationRule
import com.example.data.model.TtsEngineType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PronunciationRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("burmese_tts_prefs", Context.MODE_PRIVATE)

    private val _rules = MutableStateFlow<List<PronunciationRule>>(emptyList())
    val rules: StateFlow<List<PronunciationRule>> = _rules.asStateFlow()

    private val _settings = MutableStateFlow(NormalizationSettings())
    val settings: StateFlow<NormalizationSettings> = _settings.asStateFlow()

    init {
        loadSettings()
        loadRules()
    }

    private fun loadRules() {
        val jsonString = prefs.getString(KEY_RULES, null)
        if (jsonString.isNullOrBlank()) {
            _rules.value = PronunciationRule.DEFAULT_RULES
            saveRulesToPrefs(PronunciationRule.DEFAULT_RULES)
        } else {
            try {
                val array = JSONArray(jsonString)
                val loadedList = mutableListOf<PronunciationRule>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedList.add(
                        PronunciationRule(
                            id = obj.getString("id"),
                            originalWord = obj.getString("originalWord"),
                            phoneticReplacement = obj.getString("phoneticReplacement"),
                            description = obj.optString("description", ""),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                    )
                }
                _rules.value = loadedList
            } catch (e: Exception) {
                _rules.value = PronunciationRule.DEFAULT_RULES
            }
        }
    }

    private fun saveRulesToPrefs(list: List<PronunciationRule>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalWord", item.originalWord)
                put("phoneticReplacement", item.phoneticReplacement)
                put("description", item.description)
                put("isEnabled", item.isEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RULES, array.toString()).apply()
    }

    fun addRule(originalWord: String, phoneticReplacement: String, description: String = "") {
        val newRule = PronunciationRule(
            originalWord = originalWord.trim(),
            phoneticReplacement = phoneticReplacement.trim(),
            description = description.trim(),
            isEnabled = true
        )
        val updated = _rules.value + newRule
        _rules.value = updated
        saveRulesToPrefs(updated)
    }

    fun updateRule(updatedRule: PronunciationRule) {
        val updated = _rules.value.map {
            if (it.id == updatedRule.id) updatedRule else it
        }
        _rules.value = updated
        saveRulesToPrefs(updated)
    }

    fun toggleRule(ruleId: String) {
        val updated = _rules.value.map {
            if (it.id == ruleId) it.copy(isEnabled = !it.isEnabled) else it
        }
        _rules.value = updated
        saveRulesToPrefs(updated)
    }

    fun deleteRule(ruleId: String) {
        val updated = _rules.value.filter { it.id != ruleId }
        _rules.value = updated
        saveRulesToPrefs(updated)
    }

    fun resetRulesToDefaults() {
        val defaults = PronunciationRule.DEFAULT_RULES
        _rules.value = defaults
        saveRulesToPrefs(defaults)
    }

    private fun loadSettings() {
        val isPre = prefs.getBoolean(KEY_IS_PREPROCESSING, true)
        val autoLit = prefs.getBoolean(KEY_AUTO_LITERARY, true)
        val isConversational = prefs.getBoolean(KEY_IS_CONVERSATIONAL, true)
        val naturalBreathing = prefs.getBoolean(KEY_NATURAL_BREATHING, true)
        val dictEnabled = prefs.getBoolean(KEY_DICT_ENABLED, true)
        val tonePresetId = prefs.getString(KEY_TONE_PRESET, BurmeseTonePreset.NATURAL_CONVERSATIONAL.id)
            ?: BurmeseTonePreset.NATURAL_CONVERSATIONAL.id
        val voiceName = prefs.getString(KEY_VOICE_NAME, "Puck") ?: "Puck"
        val speed = prefs.getFloat(KEY_SPEED, 1.0f)
        val engineStr = prefs.getString(KEY_ENGINE, TtsEngineType.GEMINI_NEURAL.name)
        val engine = try {
            TtsEngineType.valueOf(engineStr ?: TtsEngineType.GEMINI_NEURAL.name)
        } catch (_: Exception) {
            TtsEngineType.GEMINI_NEURAL
        }

        _settings.value = NormalizationSettings(
            isPreprocessingEnabled = isPre,
            autoExpandLiterarySymbols = autoLit,
            isConversationalStyle = isConversational,
            enableNaturalBreathingPauses = naturalBreathing,
            isPhoneticDictionaryEnabled = dictEnabled,
            selectedTonePreset = BurmeseTonePreset.fromId(tonePresetId),
            selectedVoiceName = voiceName,
            speechSpeed = speed,
            engineType = engine
        )
    }

    fun updateSettings(newSettings: NormalizationSettings) {
        _settings.value = newSettings
        prefs.edit().apply {
            putBoolean(KEY_IS_PREPROCESSING, newSettings.isPreprocessingEnabled)
            putBoolean(KEY_AUTO_LITERARY, newSettings.autoExpandLiterarySymbols)
            putBoolean(KEY_IS_CONVERSATIONAL, newSettings.isConversationalStyle)
            putBoolean(KEY_NATURAL_BREATHING, newSettings.enableNaturalBreathingPauses)
            putBoolean(KEY_DICT_ENABLED, newSettings.isPhoneticDictionaryEnabled)
            putString(KEY_TONE_PRESET, newSettings.selectedTonePreset.id)
            putString(KEY_VOICE_NAME, newSettings.selectedVoiceName)
            putFloat(KEY_SPEED, newSettings.speechSpeed)
            putString(KEY_ENGINE, newSettings.engineType.name)
            apply()
        }
    }

    companion object {
        private const val KEY_RULES = "pronunciation_rules_json"
        private const val KEY_IS_PREPROCESSING = "is_preprocessing_enabled"
        private const val KEY_AUTO_LITERARY = "auto_expand_literary"
        private const val KEY_IS_CONVERSATIONAL = "is_conversational"
        private const val KEY_NATURAL_BREATHING = "natural_breathing_pauses"
        private const val KEY_DICT_ENABLED = "dict_enabled"
        private const val KEY_TONE_PRESET = "tone_preset"
        private const val KEY_VOICE_NAME = "voice_name"
        private const val KEY_SPEED = "speech_speed"
        private const val KEY_ENGINE = "tts_engine"
    }
}
