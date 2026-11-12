package com.runtogether.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** 다른 차의 상황을 음성으로 읽어 준다. 같은 문장을 화면 자막으로도 보여 준다. */
interface VoiceAnnouncer {
    val caption: StateFlow<String?>
    fun speak(text: String)
    fun shutdown()
}

/**
 * 안드로이드 기본 TextToSpeech 구현.
 * Pleos Connect 의 TextToSpeech SDK 로 바꿔 끼울 수 있도록 VoiceAnnouncer 뒤에 둔다.
 */
class AndroidTtsAnnouncer(context: Context) : VoiceAnnouncer, TextToSpeech.OnInitListener {

    private val _caption = MutableStateFlow<String?>(null)
    override val caption: StateFlow<String?> = _caption.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = ArrayDeque<String>()
    private var initialized = false
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        initialized = true
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.KOREAN)
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    mainHandler.postDelayed({ _caption.value = null }, 600)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post { _caption.value = null }
                }
            })
        }
        if (!ready) Log.w(TAG, "Korean TTS unavailable, showing captions only")
        while (pending.isNotEmpty()) speakNow(pending.removeFirst())
    }

    override fun speak(text: String) {
        if (!initialized) {
            pending.addLast(text)
            _caption.value = text
            return
        }
        speakNow(text)
    }

    private fun speakNow(text: String) {
        _caption.value = text
        if (ready) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rt-${System.nanoTime()}")
        } else {
            // 소리를 낼 수 없으면 읽는 데 걸릴 만큼 자막만 보여 준다
            mainHandler.postDelayed({ if (_caption.value == text) _caption.value = null }, 2_500L + text.length * 90L)
        }
    }

    override fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "VoiceAnnouncer"
    }
}
