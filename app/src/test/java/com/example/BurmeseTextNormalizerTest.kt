package com.example

import com.example.data.model.BurmeseTonePreset
import com.example.data.model.NormalizationSettings
import com.example.data.model.PronunciationRule
import com.example.domain.BurmeseTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BurmeseTextNormalizerTest {

    @Test
    fun testLiterarySymbolsExpansion() {
        val input = "၎င်း မြို့၌ ခရီးသွား၍ အလှတရား၏ တန်ဖိုးကို သိရှိရသည်။"
        val settings = NormalizationSettings(
            isPreprocessingEnabled = true,
            autoExpandLiterarySymbols = true,
            isConversationalStyle = true,
            enableNaturalBreathingPauses = false,
            isPhoneticDictionaryEnabled = false
        )

        val result = BurmeseTextNormalizer.normalize(
            inputText = input,
            settings = settings,
            customRules = emptyList()
        )

        // Verify "၎င်း" -> "လည်းကောင်း"
        assertTrue("Expected ၎င်း to be replaced with လည်းကောင်း", result.normalizedText.contains("လည်းကောင်း"))
        // Verify "၌" -> "တွင်"
        assertTrue("Expected ၌ to be replaced with တွင်", result.normalizedText.contains("တွင်"))
        // Verify "၍" -> "ပြီး"
        assertTrue("Expected ၍ to be replaced with ပြီး", result.normalizedText.contains("ပြီး"))
        // Verify "၏" -> "ရဲ့" in conversational style
        assertTrue("Expected ၏ to be replaced with ရဲ့", result.normalizedText.contains("ရဲ့"))
    }

    @Test
    fun testFormalStylePreservesLiteraryE() {
        val input = "ပြည်သူတို့၏ ဆန္ဒ"
        val settings = NormalizationSettings(
            isPreprocessingEnabled = true,
            autoExpandLiterarySymbols = true,
            isConversationalStyle = false, // Formal
            enableNaturalBreathingPauses = false,
            isPhoneticDictionaryEnabled = false
        )

        val result = BurmeseTextNormalizer.normalize(
            inputText = input,
            settings = settings,
            customRules = emptyList()
        )

        assertEquals("ပြည်သူတို့၏ ဆန္ဒ", result.normalizedText)
    }

    @Test
    fun testDefaultPhoneticReplacements() {
        val input = "ပုဂံ သို့ သွားရောက်ပြီး လက်ဖက်ရည် သောက်ကာ ကုလားထိုင် ၌ ထိုင်မည်။"
        val settings = NormalizationSettings(
            isPreprocessingEnabled = true,
            autoExpandLiterarySymbols = true,
            isConversationalStyle = true,
            enableNaturalBreathingPauses = false,
            isPhoneticDictionaryEnabled = true
        )

        val result = BurmeseTextNormalizer.normalize(
            inputText = input,
            settings = settings,
            customRules = PronunciationRule.DEFAULT_RULES
        )

        // "ပုဂံ" -> "ပဂံ"
        assertTrue("Expected ပုဂံ to become ပဂံ", result.normalizedText.contains("ပဂံ"))
        // "လက်ဖက်" -> "လဖက်"
        assertTrue("Expected လက်ဖက် to become လဖက်", result.normalizedText.contains("လဖက်"))
        // "ကုလားထိုင်" -> "ကလားထိုင်"
        assertTrue("Expected ကုလားထိုင် to become ကလားထိုင်", result.normalizedText.contains("ကလားထိုင်"))
    }

    @Test
    fun testBreathingPausesNormalization() {
        val input = "မင်္ဂလာပါ၊နေကောင်းပါသလား။"
        val settings = NormalizationSettings(
            isPreprocessingEnabled = true,
            autoExpandLiterarySymbols = false,
            enableNaturalBreathingPauses = true,
            isPhoneticDictionaryEnabled = false
        )

        val result = BurmeseTextNormalizer.normalize(
            inputText = input,
            settings = settings,
            customRules = emptyList()
        )

        assertEquals("မင်္ဂလာပါ ၊ နေကောင်းပါသလား ။", result.normalizedText)
    }

    @Test
    fun testTonePresetsSystemInstruction() {
        val conversational = BurmeseTonePreset.NATURAL_CONVERSATIONAL
        val storyteller = BurmeseTonePreset.DRAMATIC_STORYTELLER
        val news = BurmeseTonePreset.NEWS_BROADCAST
        val podcast = BurmeseTonePreset.CALM_WARM_PODCAST

        val prompt = BurmeseTextNormalizer.buildGeminiPrompt("စမ်းသပ်စာသား", storyteller)

        assertTrue(prompt.contains(storyteller.systemInstruction))
        assertTrue(prompt.contains("စမ်းသပ်စာသား"))
        assertTrue(storyteller.titleEn == "Dramatic Storyteller")
        assertTrue(news.titleEn == "News Broadcast")
        assertTrue(podcast.titleEn == "Calm & Warm Podcast")
        assertTrue(conversational.titleEn == "Natural Conversational")
    }
}
