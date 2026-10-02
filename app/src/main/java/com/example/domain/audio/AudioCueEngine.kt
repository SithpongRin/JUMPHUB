package com.example.domain.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

enum class AudioPriority {
    CRITICAL, // Countdown, Start, Finish, Safety, Rest start/end
    HIGH,     // Target reached, Personal record, 10 seconds remaining
    NORMAL,   // Target milestone, Every N jumps, Time interval
    LOW       // Every jump count, Rhythm metronome
}

data class AudioCueItem(
    val priority: AudioPriority,
    val textEn: String,
    val textKm: String,
    val isBeep: Boolean = false,
    val beepFrequency: Int = 0 // 1 = low, 2 = high
)

class AudioCueEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Configurable state
    var voiceLanguage: String = "en"
    var voiceEnabled: Boolean = true
    var voiceVolume: Float = 1.0f
    var vibrationEnabled: Boolean = true

    // Priority queue management
    private val cueQueue = ConcurrentLinkedQueue<AudioCueItem>()
    private var isCurrentlySpeaking = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w("AudioCueEngine", "TTS initialization failed: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            updateTtsLocale()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isCurrentlySpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isCurrentlySpeaking = false
                    mainHandler.post { processNextInQueue() }
                }

                override fun onError(utteranceId: String?) {
                    isCurrentlySpeaking = false
                    mainHandler.post { processNextInQueue() }
                }
            })
        }
    }

    private fun updateTtsLocale() {
        if (!isTtsInitialized) return
        val targetLocale = if (voiceLanguage.equals("km", ignoreCase = true)) {
            Locale("km", "KH")
        } else {
            Locale.US
        }
        val result = textToSpeech?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech?.language = Locale.US
        }
    }

    /**
     * Enqueues or plays an audio cue based on priority.
     * High/Critical priority can interrupt low priority.
     */
    fun postCue(cue: AudioCueItem) {
        if (!voiceEnabled && !cue.isBeep) return

        if (vibrationEnabled && (cue.priority == AudioPriority.CRITICAL || cue.priority == AudioPriority.HIGH)) {
            triggerVibration()
        }

        if (cue.priority == AudioPriority.CRITICAL) {
            // Immediate interruption of lower priority
            cueQueue.clear()
            cueQueue.add(cue)
            if (isCurrentlySpeaking) {
                textToSpeech?.stop()
            }
            processNextInQueue()
        } else {
            // If low priority and queue is backed up, skip to avoid stale audio
            if (cue.priority == AudioPriority.LOW && cueQueue.size >= 2) {
                return
            }
            cueQueue.add(cue)
            if (!isCurrentlySpeaking) {
                processNextInQueue()
            }
        }
    }

    private fun processNextInQueue() {
        val next = cueQueue.poll() ?: return
        speakItem(next)
    }

    private fun speakItem(item: AudioCueItem) {
        isCurrentlySpeaking = true
        requestAudioFocus()

        val textToSpeak = if (voiceLanguage.equals("km", ignoreCase = true)) {
            item.textKm.ifBlank { item.textEn }
        } else {
            item.textEn
        }

        if (isTtsInitialized && textToSpeech != null) {
            updateTtsLocale()
            val utteranceId = "cue_${System.currentTimeMillis()}"
            textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            // Fallback: complete after short delay
            mainHandler.postDelayed({
                isCurrentlySpeaking = false
                processNextInQueue()
            }, 600L)
        }
    }

    private fun requestAudioFocus() {
        try {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        } catch (_: Exception) { }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) { }
    }

    fun release() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            soundPool.release()
        } catch (_: Exception) { }
    }
}
