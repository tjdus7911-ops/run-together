package com.runtogether.data.pleos

import ai.pleos.playground.stt.SpeechToText
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.runtogether.data.VoiceAnnouncer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import ai.pleos.playground.tts.TextToSpeech as PleosTextToSpeech

/**
 * Pleos TextToSpeech SDK 로 읽어 준다. 연결이 안 되거나 오류가 나면 안드로이드 기본 TTS(fallback)로 넘긴다.
 * 자막은 엔진과 상관없이 같은 방식으로 보여 준다.
 */
class PleosTtsAnnouncer(context: Context, private val fallback: VoiceAnnouncer) : VoiceAnnouncer {

    private val main = Handler(Looper.getMainLooper())
    private val _caption = MutableStateFlow<String?>(null)
    override val caption: StateFlow<String?> = _caption.asStateFlow()

    private var ready = false
    private var usable = true
    private var lastText: String? = null

    private val tts: PleosTextToSpeech? = try {
        PleosTextToSpeech(context.applicationContext)
    } catch (t: Throwable) {
        Log.w(TAG, "Pleos TTS unavailable", t)
        null
    }

    init {
        val engine = tts
        if (engine == null) {
            usable = false
        } else {
            try {
                engine.initialize(
                    PleosBridge.ttsInit(
                        { main.post { ready = true; safely { engine.setLocale(Locale.KOREAN) } } },
                        { main.post { usable = false } },
                    ),
                )
                engine.addEventListener(
                    PleosBridge.ttsEvents(
                        { main.postDelayed({ _caption.value = null }, 600) },
                        {
                            main.post {
                                // Pleos 음성 엔진이 실패하면 이후로는 기본 TTS 로 읽는다
                                usable = false
                                lastText?.let { fallback.speak(it) }
                            }
                        },
                    ),
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Pleos TTS initialize failed", t)
                usable = false
            }
        }
    }

    override fun speak(text: String) {
        lastText = text
        _caption.value = text
        val engine = tts
        if (ready && usable && engine != null) {
            try {
                engine.speak(text)
                return
            } catch (t: Throwable) {
                Log.w(TAG, "Pleos TTS speak failed", t)
                usable = false
            }
        }
        fallback.speak(text)
        main.postDelayed({ if (_caption.value == text) _caption.value = null }, 2_500L + text.length * 90L)
    }

    override fun shutdown() {
        main.removeCallbacksAndMessages(null)
        safely { tts?.stop() }
        safely { tts?.release() }
        fallback.shutdown()
    }

    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(TAG, "Pleos TTS call failed", t)
        }
    }

    private companion object {
        const val TAG = "PleosTts"
    }
}

/**
 * Pleos SpeechToText SDK 로 “좋아요 / 괜찮아요”를 말로 답하게 한다.
 * 운전 중에는 화면을 누르지 않고 투표할 수 있다.
 */
class PleosVoiceAnswer(context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var onAnswer: ((Boolean) -> Unit)? = null

    private val stt: SpeechToText? = try {
        SpeechToText(context.applicationContext)
    } catch (t: Throwable) {
        Log.w(TAG, "Pleos STT unavailable", t)
        null
    }

    init {
        val engine = stt
        if (engine != null) {
            try {
                engine.initialize(
                    PleosBridge.sttInit(
                        { main.post { ready = true; safely { engine.setLocale(Locale.KOREAN) } } },
                        { main.post { ready = false } },
                    ),
                )
                engine.addListener(
                    PleosBridge.sttResults(object : PleosBridge.SpeechSink {
                        override fun onText(text: String?, isFinal: Boolean) {
                            if (isFinal) interpret(text)
                        }

                        override fun onFinished() = Unit
                    }),
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Pleos STT initialize failed", t)
            }
        }
    }

    /** 말로 답을 듣기 시작한다. 알아들으면 한 번만 [onAnswer] 를 부른다. */
    fun listen(onAnswer: (Boolean) -> Unit) {
        val engine = stt ?: return
        if (!ready) return
        this.onAnswer = onAnswer
        safely { engine.request() }
    }

    fun cancel() {
        onAnswer = null
        safely { stt?.stop() }
    }

    fun release() {
        cancel()
        safely { stt?.release() }
    }

    private fun interpret(text: String?) {
        val answer = VoiceAnswerParser.parse(text) ?: return
        main.post {
            val callback = onAnswer ?: return@post
            onAnswer = null
            callback(answer)
        }
    }

    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(TAG, "Pleos STT call failed", t)
        }
    }

    private companion object {
        const val TAG = "PleosStt"
    }
}

/** 말로 한 답을 좋아요(true) / 괜찮아요(false)로 읽는다. 모르겠으면 null. */
object VoiceAnswerParser {
    fun parse(text: String?): Boolean? {
        val t = text?.filter { it.isLetterOrDigit() }.orEmpty()
        if (t.isEmpty()) return null
        return when {
            listOf("괜찮", "아니", "싫어", "그냥가", "안쉬").any { it in t } -> false
            listOf("좋아", "좋습니다", "그래", "쉬어가", "쉬자", "콜").any { it in t } || t == "네" || t == "응" -> true
            else -> null
        }
    }
}
