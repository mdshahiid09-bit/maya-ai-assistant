package com.maya.assistant.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class ElevenLabsService(
    private val context: Context,
    private val apiKey: String,
    private val voiceId: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var androidTts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        androidTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidTts?.language = Locale.getDefault()
                isTtsReady = true
            }
        }
    }

    suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            speakWithAndroidTts(text)
            return@withContext
        }

        val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"

        val bodyJson = JSONObject()
            .put("text", text)
            .put("model_id", "eleven_turbo_v2_5")
            .put("voice_settings", JSONObject()
                .put("stability", 0.5)
                .put("similarity_boost", 0.75)
            )

        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    speakWithAndroidTts(text)
                    return@withContext
                }

                val audioBytes = response.body?.bytes() ?: run {
                    speakWithAndroidTts(text)
                    return@withContext
                }

                val tempFile = File(context.cacheDir, "maya_speech.mp3")
                FileOutputStream(tempFile).use { it.write(audioBytes) }

                withContext(Dispatchers.Main) {
                    val player = MediaPlayer()
                    player.setDataSource(tempFile.absolutePath)
                    player.prepare()
                    player.start()
                    player.setOnCompletionListener {
                        player.release()
                        tempFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            speakWithAndroidTts(text)
        }
    }

    private fun speakWithAndroidTts(text: String) {
        if (isTtsReady) {
            androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_tts_utterance")
        }
    }

    fun release() {
        androidTts?.stop()
        androidTts?.shutdown()
    }
}
