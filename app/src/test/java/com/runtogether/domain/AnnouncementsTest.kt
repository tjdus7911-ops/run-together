package com.runtogether.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementsTest {

    @Test
    fun sleepyWithSuggestedStop() {
        assertEquals(
            "민지님이 졸리대요. 문막휴게소에서 15분 쉬었다 갈까요?",
            Announcements.forSignal(StatusSignal.SLEEPY, "민지", stopName = "문막휴게소"),
        )
    }

    @Test
    fun anonymousOptionHidesTheName() {
        val text = Announcements.forSignal(StatusSignal.SLEEPY, "민지", anonymous = true)
        assertTrue(text.startsWith("한 분이 졸리대요"))
        assertFalse(text.contains("민지"))
    }

    @Test
    fun particlesFollowTheLastSyllable() {
        assertEquals("를", Josa.eulReul("문막휴게소"))
        assertEquals("을", Josa.eulReul("주유·충전"))
        assertEquals("를", Josa.eulReul("쉬었다 갈까요?"))
        assertEquals("으로", Josa.euroRo("덕평"))
        assertEquals("로", Josa.euroRo("덕평자연휴게소"))
        assertEquals("로", Josa.euroRo("강릉 안목해변길"))
        assertEquals("예요", Josa.ieyo("횡성휴게소"))
        assertEquals("이에요", Josa.ieyo("안목해변"))
    }

    @Test
    fun voteResultSentences() {
        assertEquals(
            "과반이 좋다고 해서 문막휴게소를 모두의 경로에 넣었어요. 15분 쉬고 출발해요.",
            Announcements.voteAccepted("문막휴게소", 15),
        )
        assertEquals("모두에게 ‘졸려요’를 알렸어요.", Announcements.sentByMe(StatusSignal.SLEEPY))
    }

    @Test
    fun drivingPolicy() {
        assertTrue(DrivingPolicy.isDriving(VehicleSnapshot(gear = Gear.DRIVE)))
        assertFalse(DrivingPolicy.isDriving(VehicleSnapshot(gear = Gear.PARK, speedKmh = 0.0)))
        assertTrue(DrivingPolicy.isDriving(VehicleSnapshot(gear = Gear.NEUTRAL, speedKmh = 20.0)))
        assertTrue(DrivingPolicy.shouldSuggestRest(115))
        assertFalse(DrivingPolicy.shouldSuggestRest(60))
    }
}
