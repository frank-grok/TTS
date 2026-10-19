package com.example.data.model

/**
 * Voice Acting & Tone Presets for Burmese Text-to-Speech synthesis.
 * Guides the Gemini TTS model to use authentic Myanmar intonation and emotional pacing.
 */
enum class BurmeseTonePreset(
    val id: String,
    val titleMy: String,
    val titleEn: String,
    val subtitle: String,
    val systemInstruction: String,
    val recommendedVoice: String
) {
    NATURAL_CONVERSATIONAL(
        id = "conversational",
        titleMy = "သဘာဝ စကားပြောသံ",
        titleEn = "Natural Conversational",
        subtitle = "နေ့စဉ် သဘာဝကျသော စကားပြောလေသံ",
        systemInstruction = "Speak this Burmese text with native Myanmar pronunciation, natural conversational pacing, relaxed Burmese tones, and authentic Yangon-style colloquial intonation.",
        recommendedVoice = "Puck"
    ),

    DRAMATIC_STORYTELLER(
        id = "storyteller",
        titleMy = "ဇာတ်လမ်း/ဝတ္ထု ပြောသံ",
        titleEn = "Dramatic Storyteller",
        subtitle = "ခံစားချက်ပါဝင်သော ရသစုံ စာဖတ်/ဇာတ်လမ်းလေသံ",
        systemInstruction = "Speak this Burmese text with native Myanmar pronunciation as an expressive, theatrical Burmese dramatic storyteller. Modulate vocal pitches dynamically, apply evocative pauses at sentence endings, and convey rich emotional depth.",
        recommendedVoice = "Aoede"
    ),

    NEWS_BROADCAST(
        id = "news_broadcast",
        titleMy = "သတင်းကြေညာသံ",
        titleEn = "News Broadcast",
        subtitle = "သန့်ရှင်းပီသသော တရားဝင် သတင်းကြေညာသံ",
        systemInstruction = "Speak this Burmese text with native Myanmar pronunciation in a professional, crisp, and authoritative news broadcast style. Maintain steady formal pacing, sharp Burmese consonant articulation, and clear sentence boundary demarcation.",
        recommendedVoice = "Fenrir"
    ),

    CALM_WARM_PODCAST(
        id = "warm_podcast",
        titleMy = "Podcast လေသံ",
        titleEn = "Calm & Warm Podcast",
        subtitle = "စိတ်အေးချမ်းစေသော နွေးထွေးသည့် အသံ",
        systemInstruction = "Speak this Burmese text with native Myanmar pronunciation in a warm, intimate, and soothing podcast host tone. Keep vocal pitch gentle, insert soft natural breathing pauses, and articulate soothingly.",
        recommendedVoice = "Kore"
    );

    val fullLabel: String
        get() = "$titleEn ($titleMy)"

    companion object {
        fun fromId(id: String): BurmeseTonePreset {
            return entries.firstOrNull { it.id == id } ?: NATURAL_CONVERSATIONAL
        }
    }
}
