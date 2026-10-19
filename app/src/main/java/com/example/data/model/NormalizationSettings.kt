package com.example.data.model

enum class TtsEngineType {
    GEMINI_NEURAL,
    DEVICE_TTS
}

/**
 * Settings configuration for Burmese text normalization and TTS synthesis.
 */
data class NormalizationSettings(
    val isPreprocessingEnabled: Boolean = true,
    val autoExpandLiterarySymbols: Boolean = true,
    val isConversationalStyle: Boolean = true, // Conversational: ၏ -> ရဲ့, ၌ -> တွင်, ၍ -> ပြီး
    val enableNaturalBreathingPauses: Boolean = true, // Adds natural rhythm & pauses for ၊ and ။
    val isPhoneticDictionaryEnabled: Boolean = true,
    val selectedTonePreset: BurmeseTonePreset = BurmeseTonePreset.NATURAL_CONVERSATIONAL,
    val selectedVoiceName: String = "Puck",
    val speechSpeed: Float = 1.0f,
    val engineType: TtsEngineType = TtsEngineType.GEMINI_NEURAL
)
