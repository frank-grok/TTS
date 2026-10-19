package com.example.domain

import com.example.data.model.BurmeseTonePreset
import com.example.data.model.NormalizationSettings
import com.example.data.model.PronunciationRule

/**
 * Inspection detail of each transformation applied to Burmese text.
 */
data class WordReplacementDetail(
    val original: String,
    val replacement: String,
    val type: String // "Literary Symbol" or "Phonetic Dictionary"
)

data class NormalizationResult(
    val originalText: String,
    val afterLiteraryExpansions: String,
    val afterPhoneticReplacements: String,
    val normalizedText: String,
    val finalTtsPrompt: String,
    val systemInstruction: String,
    val replacementsApplied: List<WordReplacementDetail>,
    val characterCount: Int,
    val pauseCount: Int
)

object BurmeseTextNormalizer {

    // Burmese Literary Symbols
    private const val SYM_LAGAUNG = "၎င်း" // U+104E
    private const val SYM_HNAI = "၌"     // U+104C
    private const val SYM_YWE = "၍"       // U+104D
    private const val SYM_E = "၏"         // U+104F

    // Spoken Conversational & Formal Replacements
    private const val WORD_LAGAUNG = "လည်းကောင်း"
    private const val WORD_HNAI_FORMAL = "တွင်"
    private const val WORD_HNAI_CONVERSATIONAL = "တွင်"
    private const val WORD_YWE = "ပြီး"
    private const val WORD_E_CONVERSATIONAL = "ရဲ့"
    private const val WORD_E_FORMAL = "၏"

    // Burmese Punctuation
    private const val PUNC_COMMA = "၊"  // U+104A (Little section / short pause)
    private const val PUNC_PERIOD = "။" // U+104B (Section / full stop)

    /**
     * Normalizes Burmese text according to user settings and active pronunciation rules.
     */
    fun normalize(
        inputText: String,
        settings: NormalizationSettings,
        customRules: List<PronunciationRule>
    ): NormalizationResult {
        if (!settings.isPreprocessingEnabled || inputText.isBlank()) {
            val systemInstruction = settings.selectedTonePreset.systemInstruction
            val prompt = buildGeminiPrompt(inputText, settings.selectedTonePreset)
            return NormalizationResult(
                originalText = inputText,
                afterLiteraryExpansions = inputText,
                afterPhoneticReplacements = inputText,
                normalizedText = inputText,
                finalTtsPrompt = prompt,
                systemInstruction = systemInstruction,
                replacementsApplied = emptyList(),
                characterCount = inputText.length,
                pauseCount = countPauses(inputText)
            )
        }

        val replacementsApplied = mutableListOf<WordReplacementDetail>()
        var text = inputText

        // Step 1: Auto-expand Burmese literary symbols into spoken words
        var afterLiterary = text
        if (settings.autoExpandLiterarySymbols) {
            // ၎င်း -> လည်းကောင်း
            if (afterLiterary.contains(SYM_LAGAUNG)) {
                afterLiterary = afterLiterary.replace(SYM_LAGAUNG, WORD_LAGAUNG)
                replacementsApplied.add(
                    WordReplacementDetail(SYM_LAGAUNG, WORD_LAGAUNG, "Literary Symbol")
                )
            }

            // ၌ -> တွင်
            if (afterLiterary.contains(SYM_HNAI)) {
                val replacement = if (settings.isConversationalStyle) WORD_HNAI_CONVERSATIONAL else WORD_HNAI_FORMAL
                afterLiterary = afterLiterary.replace(SYM_HNAI, replacement)
                replacementsApplied.add(
                    WordReplacementDetail(SYM_HNAI, replacement, "Literary Symbol")
                )
            }

            // ၍ -> ပြီး
            if (afterLiterary.contains(SYM_YWE)) {
                afterLiterary = afterLiterary.replace(SYM_YWE, WORD_YWE)
                replacementsApplied.add(
                    WordReplacementDetail(SYM_YWE, WORD_YWE, "Literary Symbol")
                )
            }

            // ၏ -> ရဲ့ (Conversational) or ၏ (Formal)
            if (afterLiterary.contains(SYM_E)) {
                val replacement = if (settings.isConversationalStyle) WORD_E_CONVERSATIONAL else WORD_E_FORMAL
                if (replacement != SYM_E) {
                    afterLiterary = afterLiterary.replace(SYM_E, replacement)
                    replacementsApplied.add(
                        WordReplacementDetail(SYM_E, replacement, "Literary Symbol (Conversational)")
                    )
                }
            }
        }
        text = afterLiterary

        // Step 2: Custom Pronunciation Dictionary (Phonetic Replacer)
        var afterPhonetic = text
        if (settings.isPhoneticDictionaryEnabled && customRules.isNotEmpty()) {
            // Sort by descending word length so compound words match before sub-words
            val activeRules = customRules
                .filter { it.isEnabled && it.originalWord.isNotBlank() && it.phoneticReplacement.isNotBlank() }
                .sortedByDescending { it.originalWord.length }

            for (rule in activeRules) {
                if (afterPhonetic.contains(rule.originalWord)) {
                    val count = countOccurrences(afterPhonetic, rule.originalWord)
                    afterPhonetic = afterPhonetic.replace(rule.originalWord, rule.phoneticReplacement)
                    replacementsApplied.add(
                        WordReplacementDetail(
                            rule.originalWord,
                            rule.phoneticReplacement,
                            "Phonetic (${count}x)"
                        )
                    )
                }
            }
        }
        text = afterPhonetic

        // Step 3: Natural Breathing Pauses for Standard Burmese Punctuation ("၊" and "။")
        var normalized = text
        if (settings.enableNaturalBreathingPauses) {
            // In natural speech, "၊" represents a short breath pause (~250-350ms)
            // and "။" represents a sentence full stop (~600-800ms).
            // Burmese text often lacks whitespace, so TTS models rush.
            // Adding clean spacing around Burmese punctuation creates the acoustic breathing room.
            normalized = normalized
                .replace(Regex("""\s*။\s*"""), " ။ ")
                .replace(Regex("""\s*၊\s*"""), " ၊ ")
                .replace(Regex("""\s*\.{3,}\s*"""), " ... ")
                .replace(Regex("""[ \t]{2,}"""), " ")
                .trim()
        }

        val prompt = buildGeminiPrompt(normalized, settings.selectedTonePreset)

        return NormalizationResult(
            originalText = inputText,
            afterLiteraryExpansions = afterLiterary,
            afterPhoneticReplacements = afterPhonetic,
            normalizedText = normalized,
            finalTtsPrompt = prompt,
            systemInstruction = settings.selectedTonePreset.systemInstruction,
            replacementsApplied = replacementsApplied,
            characterCount = normalized.length,
            pauseCount = countPauses(normalized)
        )
    }

    /**
     * Builds the Gemini TTS prompt with tone preset system instruction prepended.
     */
    fun buildGeminiPrompt(text: String, tonePreset: BurmeseTonePreset): String {
        return "${tonePreset.systemInstruction}\n\nBurmese Script:\n$text"
    }

    private fun countOccurrences(text: String, target: String): Int {
        var count = 0
        var index = 0
        while (index != -1) {
            index = text.indexOf(target, index)
            if (index != -1) {
                count++
                index += target.length
            }
        }
        return count
    }

    private fun countPauses(text: String): Int {
        var count = 0
        for (c in text) {
            if (c == '၊' || c == '။') count++
        }
        if (text.contains("...")) count++
        return count
    }
}
