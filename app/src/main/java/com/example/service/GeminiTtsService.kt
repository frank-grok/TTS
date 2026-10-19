package com.example.service

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed interface TtsAudioResult {
    data class Success(
        val audioFile: File,
        val mimeType: String,
        val durationMillis: Long
    ) : TtsAudioResult

    data class Error(val message: String) : TtsAudioResult
}

class GeminiTtsService(private val cacheDir: File) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun synthesizeSpeech(
        promptWithInstruction: String,
        voiceName: String = "Puck",
        customApiKey: String? = null
    ): TtsAudioResult = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return@withContext TtsAudioResult.Error(
                "Gemini API key is required. Please set GEMINI_API_KEY in the Secrets panel or provide it in Settings."
            )
        }

        try {
            // Using gemini-2.0-flash with AUDIO modality support
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", promptWithInstruction)
                            }
                            put(partObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    val responseModalities = JSONArray().apply {
                        put("AUDIO")
                    }
                    put("responseModalities", responseModalities)

                    val speechConfig = JSONObject().apply {
                        val voiceConfig = JSONObject().apply {
                            val prebuiltVoiceConfig = JSONObject().apply {
                                put("voiceName", voiceName)
                            }
                            put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                        }
                        put("voiceConfig", voiceConfig)
                    }
                    put("speechConfig", speechConfig)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseString)
                    errJson.getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseString"
                }
                return@withContext TtsAudioResult.Error("Gemini TTS Error: $errorMsg")
            }

            val rootJson = JSONObject(responseString)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext TtsAudioResult.Error("No audio candidate returned from Gemini.")
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return@withContext TtsAudioResult.Error("Audio content was empty.")
            }

            var base64Data: String? = null
            var mimeType = "audio/wav"

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("inlineData")) {
                    val inlineData = part.getJSONObject("inlineData")
                    base64Data = inlineData.optString("data")
                    mimeType = inlineData.optString("mimeType", "audio/wav")
                    break
                }
            }

            if (base64Data.isNullOrBlank()) {
                return@withContext TtsAudioResult.Error("Audio payload missing from response parts.")
            }

            val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val audioBytes = if (mimeType.contains("pcm", ignoreCase = true)) {
                // Wrap raw PCM 24000Hz 16-bit mono with standard RIFF WAV header
                addWavHeader(rawBytes, sampleRate = 24000, channels = 1, bitsPerSample = 16)
            } else {
                rawBytes
            }

            val outputFile = File(cacheDir, "burmese_tts_${System.currentTimeMillis()}.wav")
            FileOutputStream(outputFile).use { fos ->
                fos.write(audioBytes)
                fos.flush()
            }

            // Estimate duration in ms (for 24000 Hz, 16-bit mono = 48000 bytes/sec)
            val durationMs = if (audioBytes.size > 44) {
                ((audioBytes.size - 44).toLong() * 1000L) / (24000L * 2L)
            } else 1000L

            TtsAudioResult.Success(
                audioFile = outputFile,
                mimeType = "audio/wav",
                durationMillis = durationMs
            )

        } catch (e: Exception) {
            Log.e("GeminiTtsService", "Error during TTS synthesis", e)
            TtsAudioResult.Error(e.localizedMessage ?: "Failed to synthesize speech.")
        }
    }

    /**
     * Converts raw PCM bytes into a valid RIFF/WAV audio file.
     */
    private fun addWavHeader(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Short = 1,
        bitsPerSample: Short = 16
    ): ByteArray {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteArray(44)
        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat: 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = ((byteRate shr 8) and 0xffL).toByte()
        header[30] = ((byteRate shr 16) and 0xffL).toByte()
        header[31] = ((byteRate shr 24) and 0xffL).toByte()
        header[32] = blockAlign.toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // 'data' chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

        val output = ByteArrayOutputStream(44 + pcmData.size)
        output.write(header)
        output.write(pcmData)
        return output.toByteArray()
    }
}
