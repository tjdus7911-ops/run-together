package com.runtogether.data

import com.runtogether.domain.AnnouncementQueue
import com.runtogether.domain.Utterance
import com.runtogether.domain.VoicePriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 음성 안내 차례 정하기. 음성 엔진(Pleos TTS / 안드로이드 TTS)은 그대로 두고 그 앞에서 순서만 정한다.
 * - 한 번에 하나만 읽는다 (다른 안내와 겹쳐 재생하지 않음)
 * - 안전 안내 → 모임 안내 → 메시지 → 내 확인 순서 ([VoicePriority])
 * - 내비 길 안내 음성이 나오는 중이면 끝날 때까지 기다린다. 너무 오래 기다리면 읽지 않고 자막만 (소식에는 남아 있다)
 * - 읽지 않는 것(메시지 읽어 주기 끔)은 자막만 보여 준다
 */
class VoiceQueue(
    private val engine: VoiceAnnouncer,
    private val naviGuidancePlaying: StateFlow<Boolean>,
    scope: CoroutineScope,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val queue = AnnouncementQueue()
    private val wake = Channel<Unit>(Channel.CONFLATED)

    private val _current = MutableStateFlow<Utterance?>(null)

    /** 지금 읽거나 자막으로 보여 주는 안내 */
    val current: StateFlow<Utterance?> = _current.asStateFlow()

    private val _caption = MutableStateFlow<String?>(null)

    /** 화면 자막 (읽는 동안만) */
    val caption: StateFlow<String?> = _caption.asStateFlow()

    private val job = scope.launch { loop() }

    fun say(text: String, priority: VoicePriority, speakerId: String? = null, speak: Boolean = true) {
        if (text.isBlank()) return
        queue.offer(Utterance(text, priority, now(), speakerId, speak))
        wake.trySend(Unit)
    }

    /** 기다리는 안내를 모두 버린다 (시연 단계를 손으로 넘길 때) */
    fun clearPending() = queue.clear()

    fun shutdown() {
        job.cancel()
        engine.shutdown()
    }

    private suspend fun loop() {
        while (true) {
            val next = queue.poll(now())
            if (next == null) {
                wake.receive()
                continue
            }
            if (next.speak && waitForNaviGuidance(next)) play(next) else show(next)
            delay(GAP_MS)
        }
    }

    /** 내비 길 안내가 끝날 때까지 기다린다. 제때 끝나면 true. */
    private suspend fun waitForNaviGuidance(u: Utterance): Boolean {
        if (!naviGuidancePlaying.value) return true
        val left = u.priority.maxWaitMs - (now() - u.createdAtMs)
        if (left <= 0) return false
        return withTimeoutOrNull(left) { naviGuidancePlaying.first { !it } } != null
    }

    private suspend fun play(u: Utterance) {
        _current.value = u
        _caption.value = u.text
        engine.speak(u.text)
        // 엔진 자막이 지워지면 다 읽은 것. 엔진이 끝을 알려 주지 않으면 글자 수로 계산한 시간 뒤 넘어간다.
        withTimeoutOrNull(maxSpeakMs(u.text)) { engine.caption.first { it == null } }
        _current.value = null
        _caption.value = null
    }

    private suspend fun show(u: Utterance) {
        _current.value = u
        _caption.value = u.text
        delay(captionMs(u.text))
        _current.value = null
        _caption.value = null
    }

    companion object {
        /** 안내 사이 틈 */
        const val GAP_MS = 350L

        /** 자막만 보여 줄 때 읽는 데 걸릴 만큼 */
        fun captionMs(text: String): Long = 2_500L + text.length * 90L

        /** 엔진이 끝을 알려 주지 않을 때 넘어가는 시간 */
        fun maxSpeakMs(text: String): Long = 4_000L + text.length * 160L
    }
}
