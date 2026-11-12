package com.runtogether.state

import com.runtogether.demo.DemoScenario
import com.runtogether.domain.RestVoteEngine
import com.runtogether.domain.StopCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RestTransitionsTest {

    /** 08:55, 내 차는 106km 지점, 다음 집결지는 횡성(145km) */
    private val base = DemoScenario.step(DemoScenario.STEP_SLEEPY)

    private fun voteFor(stop: StopCandidate) =
        RestVoteEngine.start(DemoScenario.minji.id, stop, 15, base.members.map { it.member.id }, 0L)

    @Test
    fun newRestStopIsInsertedBeforeNextMeetingPointAndBecomesNext() {
        val after = base.acceptRest(voteFor(DemoScenario.munmak))
        assertEquals(base.course.size + 1, after.course.size)
        assertEquals(DemoScenario.munmak.id, after.nextStop?.stop?.id)
        assertEquals("휴식", after.nextStop?.label)
        assertEquals(DemoScenario.hoengseong.id, after.course[after.nextStopIndex + 1].stop.id)
        assertEquals("문막휴게소를 모두의 내비에 추가했어요", after.voteResult?.title)
        // 6km(5분) 뒤 09:00 도착, 15분 쉬고, 33km(25분) 더 가서 횡성
        assertEquals("09:00 도착 · 15분 쉬고 출발 · 횡성 도착 09:40", after.voteResult?.detail)
        assertNull(after.alert)
    }

    @Test
    fun stopAlreadyInCourseIsNotDuplicated() {
        val after = base.acceptRest(voteFor(DemoScenario.hoengseong))
        assertEquals(base.course, after.course)
        assertEquals(DemoScenario.hoengseong.id, after.nextStop?.stop?.id)
        assertEquals("09:24 도착 · 15분 쉬고 출발", after.voteResult?.detail)
    }

    @Test
    fun stopBeyondNextMeetingPointKeepsDistanceOrder() {
        val after = base.acceptRest(voteFor(DemoScenario.pyeongchang))
        assertEquals(DemoScenario.hoengseong.id, after.nextStop?.stop?.id)
        assertEquals(
            listOf("jamsil", "deokpyeong", "hoengseong", "pyeongchang", "anmok"),
            after.course.map { it.stop.id },
        )
    }

    @Test
    fun rejectedVoteMeetsAtThePlannedPoint() {
        val after = base.rejectRest()
        assertFalse(after.voteResult!!.accepted)
        assertEquals("횡성휴게소에서 만나요", after.voteResult?.detail)
        assertEquals(base.course, after.course)
    }

    @Test
    fun distanceIsShownWithOneDecimalOnlyWhenNeeded() {
        assertEquals("6", formatKm(6.0))
        assertEquals("1.5", formatKm(1.5))
        assertEquals("3.4", formatKm(3.44))
        assertEquals("10", formatKm(9.96))
        assertEquals("18", formatKm(18.4))
    }
}
