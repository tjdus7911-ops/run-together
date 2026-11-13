package com.runtogether.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyMessagesTest {

    private fun msg(id: String, text: String, order: Int = 0) = QuickMessage(id, text, MessageIcon.COFFEE, order)

    // ---------------- 문장 검사 ----------------

    @Test
    fun ordinarySentencePasses() {
        val check = QuickMessagePolicy.validate("  카페 들렀다   가요 ")
        assertTrue(check.ok)
        assertEquals("카페 들렀다 가요", check.text)
    }

    @Test
    fun lengthIsTwoToTwentyCharacters() {
        assertEquals(listOf(MessageProblem.EMPTY), QuickMessagePolicy.validate("   ").problems)
        assertEquals(listOf(MessageProblem.TOO_SHORT), QuickMessagePolicy.validate("네").problems)
        assertTrue(QuickMessagePolicy.validate("가".repeat(20)).ok)
        assertEquals(listOf(MessageProblem.TOO_LONG), QuickMessagePolicy.validate("가".repeat(21)).problems)
    }

    @Test
    fun emojiAndOddSymbolsAreRejectedBecauseIconsArePickedSeparately() {
        assertTrue(MessageProblem.UNSUPPORTED_CHARACTER in QuickMessagePolicy.validate("커피 ☕").problems)
        assertTrue(MessageProblem.UNSUPPORTED_CHARACTER in QuickMessagePolicy.validate("곧 가요 ♥").problems)
        assertTrue(QuickMessagePolicy.validate("10분 뒤 도착해요!").ok)
        assertTrue(QuickMessagePolicy.validate("주유·충전하고 갈게요~").ok)
    }

    @Test
    fun phoneNumbersAndLinksAreRejected() {
        assertTrue(MessageProblem.CONTACT_INFO in QuickMessagePolicy.validate("010-1234-5678").problems)
        assertTrue(MessageProblem.CONTACT_INFO in QuickMessagePolicy.validate("www.cafe.kr 여기요").problems)
    }

    @Test
    fun urgingOrUnsafeDrivingWordsAreRejectedEvenWithSpaces() {
        assertTrue(MessageProblem.UNSAFE in QuickMessagePolicy.validate("빨리 따라와요").problems)
        assertTrue(MessageProblem.UNSAFE in QuickMessagePolicy.validate("신호 무시하고 와").problems)
        assertTrue(MessageProblem.UNSAFE in QuickMessagePolicy.validate("추월해서 갈게요").problems)
        assertFalse(MessageProblem.UNSAFE in QuickMessagePolicy.validate("천천히 따라갈게요").problems)
    }

    @Test
    fun duplicatesDefaultsAndTheSixLimit() {
        val six = (1..6).map { msg("m$it", "메시지 $it", it) }
        assertTrue(MessageProblem.LIMIT_REACHED in QuickMessagePolicy.validate("새 메시지", six).problems)
        // 고칠 때는 개수 제한에 걸리지 않고, 자기 자신과는 중복이 아니다
        assertTrue(QuickMessagePolicy.validate("메시지 1", six, editingId = "m1").ok)
        assertTrue(MessageProblem.DUPLICATE in QuickMessagePolicy.validate("메시지 2", six, editingId = "m1").problems)
        assertTrue(MessageProblem.SAME_AS_DEFAULT in QuickMessagePolicy.validate("화장실 급해요").problems)
    }

    @Test
    fun spokenSentencesAllowFortyCharacters() {
        assertTrue(QuickMessagePolicy.validateSpoken("다음 휴게소에서 잠깐 쉬었다 갈게요").ok)
        assertEquals(listOf(MessageProblem.EMPTY), QuickMessagePolicy.validateSpoken(null).problems)
        assertEquals(listOf(MessageProblem.TOO_LONG_SPOKEN), QuickMessagePolicy.validateSpoken("가".repeat(41)).problems)
        assertTrue(MessageProblem.UNSAFE in QuickMessagePolicy.validateSpoken("좀 더 빨리 와 주세요").problems)
    }

    @Test
    fun carShowsAtMostSixInOrder() {
        val many = (8 downTo 1).map { msg("m$it", "메시지 $it", it) }
        assertEquals((1..6).map { "m$it" }, QuickMessagePolicy.forCar(many).map { it.id })
    }

    // ---------------- 보내기 제한 ----------------

    @Test
    fun doubleTapIsIgnored() {
        val guard = SendGuard()
        assertEquals(SendGuard.Decision.Allowed, guard.tryAcquire("custom:a", 0L))
        val second = guard.tryAcquire("custom:b", 800L)
        assertTrue(second is SendGuard.Decision.Blocked && second.reason == SendGuard.Reason.DOUBLE_TAP)
    }

    @Test
    fun sameMessageWaitsThirtySeconds() {
        val guard = SendGuard()
        guard.tryAcquire("custom:a", 0L)
        val again = guard.tryAcquire("custom:a", 10_000L)
        assertTrue(again is SendGuard.Decision.Blocked && again.reason == SendGuard.Reason.SAME_MESSAGE)
        assertEquals(20_000L, (again as SendGuard.Decision.Blocked).waitMs)
        assertEquals(setOf("custom:a"), guard.coolingKeys(10_000L))
        assertEquals(SendGuard.Decision.Allowed, guard.tryAcquire("custom:a", 30_000L))
    }

    @Test
    fun atMostFourPerMinuteAcrossAllKinds() {
        val guard = SendGuard()
        listOf("status:SLEEPY", "custom:a", "voice:곧 가요", "custom:b").forEachIndexed { i, key ->
            assertEquals(SendGuard.Decision.Allowed, guard.tryAcquire(key, i * 5_000L))
        }
        val fifth = guard.tryAcquire("custom:c", 20_000L)
        assertTrue(fifth is SendGuard.Decision.Blocked && fifth.reason == SendGuard.Reason.TOO_MANY)
        assertEquals(SendGuard.Decision.Allowed, guard.tryAcquire("custom:c", 60_000L))
    }

    @Test
    fun guardKeysTellMessagesApart() {
        val custom = GroupMessage.Custom("1", "me", 0L, "m1", "카페 들렀다 가요", MessageIcon.COFFEE)
        val voice = GroupMessage.Voice("2", "me", 0L, " 곧  가요 ")
        assertEquals("custom:m1", custom.guardKey)
        assertEquals("voice:곧 가요", voice.guardKey)
        assertEquals("status:TOILET", GroupMessage.Status("3", "me", 0L, StatusSignal.TOILET).guardKey)
    }

    // ---------------- 받는 쪽 ----------------

    @Test
    fun sameWordsFromTheSamePersonAreReadOnceAMinute() {
        val filter = RepeatFilter()
        assertTrue(filter.shouldAnnounce("minji", "카페 들렀다 가요", 0L))
        assertFalse(filter.shouldAnnounce("minji", "카페  들렀다 가요", 20_000L))
        assertTrue(filter.shouldAnnounce("sua", "카페 들렀다 가요", 20_000L))
        assertTrue(filter.shouldAnnounce("minji", "카페 들렀다 가요", 81_000L))
    }

    // ---------------- 읽을 차례 ----------------

    private fun u(text: String, p: VoicePriority, at: Long) = Utterance(text, p, at)

    @Test
    fun safetyFirstThenOldestFirst() {
        val q = AnnouncementQueue()
        q.offer(u("확인", VoicePriority.CONFIRM, 0L))
        q.offer(u("메시지 1", VoicePriority.MESSAGE, 1L))
        q.offer(u("졸려요", VoicePriority.SAFETY, 2L))
        q.offer(u("메시지 2", VoicePriority.MESSAGE, 3L))
        assertEquals(listOf("졸려요", "메시지 1", "메시지 2", "확인"), List(4) { q.poll(4L)?.text })
        assertNull(q.poll(5L))
    }

    @Test
    fun sameSentenceIsNotQueuedTwice() {
        val q = AnnouncementQueue()
        assertTrue(q.offer(u("민지님이 메시지를 보냈어요.", VoicePriority.MESSAGE, 0L)))
        assertFalse(q.offer(u("민지님이 메시지를 보냈어요.", VoicePriority.MESSAGE, 1L)))
        assertEquals(1, q.size)
    }

    @Test
    fun onlyThreeMessagesWaitAndStaleOnesAreDropped() {
        val q = AnnouncementQueue(maxMessages = 3)
        (1..4).forEach { q.offer(u("메시지 $it", VoicePriority.MESSAGE, it.toLong())) }
        q.offer(u("확인", VoicePriority.CONFIRM, 5L))
        assertEquals(4, q.size)
        assertEquals("메시지 2", q.poll(10L)?.text)
        // 확인 음성은 6초 넘게 기다리면 버린다
        assertEquals("메시지 3", q.poll(7_000L)?.text)
        assertEquals("메시지 4", q.poll(7_000L)?.text)
        assertNull(q.poll(7_000L))
    }

    // ---------------- 읽는 문장 ----------------

    @Test
    fun receivedMessageSentences() {
        assertEquals("민지님이 메시지를 보냈어요. 카페 들렀다 가요.", Announcements.customMessage("민지", "카페 들렀다 가요"))
        assertEquals("한 분이 메시지를 보냈어요. 사진 찍고 갈게요!", Announcements.customMessage("민지", "사진 찍고 갈게요!!", anonymous = true))
        assertEquals("수아님이 메시지를 보냈어요. 주유, 충전하고 갈게요.", Announcements.voiceMessage("수아", "주유·충전하고 갈게요~"))
        assertEquals("10분 뒤 도착해요. 이렇게 보낼까요?", Announcements.confirmSpoken("10분 뒤 도착해요"))
        assertEquals("모두에게 ‘카페 들렀다 가요’를 보냈어요.", Announcements.customSent("카페 들렀다 가요"))
    }
}
