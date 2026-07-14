package ar.motorfar.app.nav

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

private const val UTTERANCE_ID = "turn_hint"

/**
 * Anuncia giros por voz (TextToSpeech nativo de Android, offline). Pide foco
 * de audio transitorio "may duck" antes de cada frase y lo libera al
 * terminar -- ar.motorfar.app.radio.RadioAudioService se agacha de volumen
 * en vez de pelear por el foco (ver onRxAudioFocusChange ahí).
 */
class TurnAnnouncer(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("es", "AR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale("es"))
                }
                ready = true
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = abandonFocus()
            override fun onError(utteranceId: String?) = abandonFocus()
        })
    }

    fun speak(text: String) {
        if (!ready) return
        requestFocus()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    fun shutdown() {
        abandonFocus()
        tts.stop()
        tts.shutdown()
    }
}
