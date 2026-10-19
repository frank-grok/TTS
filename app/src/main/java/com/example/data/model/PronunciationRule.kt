package com.example.data.model

import java.util.UUID

/**
 * Represents a phonetic replacement rule for Burmese Text-to-Speech.
 * Written Burmese words often have historical spellings that differ from their
 * natural conversational pronunciation (e.g. weakened vowel schwa or stacked consonants).
 */
data class PronunciationRule(
    val id: String = UUID.randomUUID().toString(),
    val originalWord: String,
    val phoneticReplacement: String,
    val description: String = "",
    val isEnabled: Boolean = true
) {
    companion object {
        /**
         * 5 standard default Burmese phonetic replacement rules
         */
        val DEFAULT_RULES = listOf(
            PronunciationRule(
                id = "rule_laphet",
                originalWord = "လက်ဖက်",
                phoneticReplacement = "လဖက်",
                description = "လက်ဖက်ရည်/လက်ဖက်သုပ် (လဖက် ဟု သဘာဝကျကျ အသံထွက်ရန်)",
                isEnabled = true
            ),
            PronunciationRule(
                id = "rule_bagan",
                originalWord = "ပုဂံ",
                phoneticReplacement = "ပဂံ",
                description = "ရှေးဟောင်းမြို့ ပုဂံ (အသံလျော့ ပဂံ ဟု အသံထွက်စေရန်)",
                isEnabled = true
            ),
            PronunciationRule(
                id = "rule_thinbaw",
                originalWord = "သင်္ဘော",
                phoneticReplacement = "သင်းဘော",
                description = "ကင်းစီးအစား သင်းဘော ဟု ပီသစွာ အသံထွက်စေရန်",
                isEnabled = true
            ),
            PronunciationRule(
                id = "rule_kalathaing",
                originalWord = "ကုလားထိုင်",
                phoneticReplacement = "ကလားထိုင်",
                description = "ကုလားထိုင် အား နေ့စဉ်ပြော ကလားထိုင် ဟု အသံထွက်ရန်",
                isEnabled = true
            ),
            PronunciationRule(
                id = "rule_thammada",
                originalWord = "သမ္မတ",
                phoneticReplacement = "သမ်မတ",
                description = "သမ္မတ (ဆင့်စာလုံးကို သမ်မတ ဟု ရှင်းလင်းစွာ အသံထွက်စေရန်)",
                isEnabled = true
            )
        )
    }
}
