package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.RiyaMood
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SweetVoiceManager(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pulseJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.28f) // Sweet young girl voice pitch
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val hindiLocale = Locale("hi", "IN")
                    val result = tts?.setLanguage(hindiLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.getDefault())
                    }
                    // Try selecting a female Hindi voice if available
                    try {
                        val voices = tts?.voices
                        val sweetFemaleVoice = voices?.firstOrNull { voice ->
                            voice.locale.language == "hi" &&
                                (voice.name.contains("female", ignoreCase = true) ||
                                    voice.name.contains("f", ignoreCase = true) ||
                                    !voice.isNetworkConnectionRequired)
                        }
                        if (sweetFemaleVoice != null) {
                            tts?.voice = sweetFemaleVoice
                        }
                    } catch (_: Exception) {
                    }
                    tts?.setPitch(_voicePitch.value)
                    tts?.setSpeechRate(0.98f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                    isTtsReady = true
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun setSweetPitch(newPitch: Float) {
        _voicePitch.value = newPitch.coerceIn(0.9f, 1.6f)
        tts?.setPitch(_voicePitch.value)
    }

    fun speakSweetly(text: String, mood: RiyaMood = RiyaMood.ROMANTIC) {
        val cleanText = cleanTextForSpeech(text)
        if (cleanText.isBlank()) return

        // Start visual speaking waveform pulse for Live Chat stage
        pulseJob?.cancel()
        val durationMs = (cleanText.length * 68L).coerceIn(2200L, 6500L)
        pulseJob = scope.launch {
            _isSpeaking.value = true
            delay(durationMs)
            _isSpeaking.value = false
        }

        if (isTtsReady) {
            try {
                val moodPitch = when (mood) {
                    RiyaMood.SHY -> (_voicePitch.value + 0.08f).coerceAtMost(1.55f)
                    RiyaMood.ROMANTIC, RiyaMood.PLAYFUL -> _voicePitch.value
                    RiyaMood.NAKHRE, RiyaMood.GUSSA -> (_voicePitch.value - 0.06f).coerceAtLeast(1.1f)
                    RiyaMood.MISSING_YOU -> _voicePitch.value
                }
                val moodRate = when (mood) {
                    RiyaMood.GUSSA, RiyaMood.NAKHRE -> 1.06f
                    RiyaMood.SHY -> 0.94f
                    else -> 0.98f
                }
                tts?.setPitch(moodPitch)
                tts?.setSpeechRate(moodRate)
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "riya_utt_${System.currentTimeMillis()}")
            } catch (_: Exception) {
            }
        }
    }

    fun stopSpeaking() {
        pulseJob?.cancel()
        _isSpeaking.value = false
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }

    private fun cleanTextForSpeech(raw: String): String {
        // Remove *roleplay action text* and emojis so TTS reads Riya's spoken words smoothly
        val withoutActions = raw.replace(Regex("\\*[^*]+\\*"), " ")
        val withoutEmojis = withoutActions.replace(
            Regex("[\\p{So}\\p{Cn}~]"),
            " "
        )
        return withoutEmojis.replace(Regex("\\s+"), " ").trim()
    }
}
