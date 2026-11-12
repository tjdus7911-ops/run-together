package com.runtogether.data

import com.runtogether.domain.VoicePriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceQueueTest {

    /** 1초 동안 읽고 자막을 지우는 가짜 음성 엔진 */
    private class FakeEngine(private val scope: CoroutineScope, private val speakMs: Long = 1_000L) : VoiceAnnouncer {
        val spoken = mutableListOf<String>()
        private val _caption = MutableStateFlow<String?>(null)
        override val caption: StateFlow<String?> = _caption

        override fun speak(text: String) {
            spoken += text
            _caption.value = text
            scope.launch {
                delay(speakMs)
                _caption.value = null
            }
        }

        override fun shutdown() = Unit
    }

    @Test
    fun readsOneAtATimeAndSafetyFirst() = runTest {
        val engine = FakeEngine(backgroundScope)
        val queue = VoiceQueue(engine, MutableStateFlow(false), backgroundScope, now = { currentTime })
        queue.say("민지님이 메시지를 보냈어요. 카페 들렀다 가요.", VoicePriority.MESSAGE)
        queue.say("민지님이 졸리대요.", VoicePriority.SAFETY)
        runCurrent()
        assertEquals(listOf("민지님이 졸리대요."), engine.spoken)
        assertEquals("민지님이 졸리대요.", queue.caption.value)

        // 읽는 동안에는 다음 안내를 겹쳐 읽지 않는다
        advanceTimeBy(600)
        runCurrent()
        assertEquals(1, engine.spoken.size)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf("민지님이 졸리대요.", "민지님이 메시지를 보냈어요. 카페 들렀다 가요."), engine.spoken)
    }

    @Test
    fun waitsUntilNaviGuidanceEnds() = runTest {
        val engine = FakeEngine(backgroundScope)
        val navi = MutableStateFlow(true)
        val queue = VoiceQueue(engine, navi, backgroundScope, now = { currentTime })
        queue.say("수아님이 메시지를 보냈어요.", VoicePriority.MESSAGE)
        advanceTimeBy(3_000)
        runCurrent()
        assertTrue(engine.spoken.isEmpty())
        assertNull(queue.caption.value)

        navi.value = false
        runCurrent()
        assertEquals(listOf("수아님이 메시지를 보냈어요."), engine.spoken)
    }

    @Test
    fun givesUpSpeakingAfterTheMaxWaitButStillShowsTheCaption() = runTest {
        val engine = FakeEngine(backgroundScope)
        val queue = VoiceQueue(engine, MutableStateFlow(true), backgroundScope, now = { currentTime })
        queue.say("모두에게 ‘졸려요’를 알렸어요.", VoicePriority.CONFIRM)
        advanceTimeBy(VoicePriority.CONFIRM.maxWaitMs + 100)
        runCurrent()
        assertTrue(engine.spoken.isEmpty())
        assertEquals("모두에게 ‘졸려요’를 알렸어요.", queue.caption.value)
    }

    @Test
    fun readAloudOffShowsCaptionOnly() = runTest {
        val engine = FakeEngine(backgroundScope)
        val queue = VoiceQueue(engine, MutableStateFlow(false), backgroundScope, now = { currentTime })
        val text = "지원님이 메시지를 보냈어요. 사진 찍고 갈게요."
        queue.say(text, VoicePriority.MESSAGE, speakerId = "jiwon", speak = false)
        runCurrent()
        assertTrue(engine.spoken.isEmpty())
        assertEquals(text, queue.caption.value)
        assertEquals("jiwon", queue.current.value?.speakerId)

        advanceTimeBy(VoiceQueue.captionMs(text) + 10)
        runCurrent()
        assertNull(queue.caption.value)
    }
}
