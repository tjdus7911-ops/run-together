package com.runtogether.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingPointEngineTest {

    private val deokpyeong = StopCandidate("deokpyeong", "덕평자연휴게소", 60.0, hasFuel = true, hasCharger = true, hasFood = true)
    private val yeoju = StopCandidate("yeoju", "여주휴게소", 78.0, hasFuel = true, hasCharger = true, hasFood = true)
    private val munmak = StopCandidate("munmak", "문막휴게소", 112.0, hasFuel = true, hasFood = true)
    private val hoengseong = StopCandidate("hoengseong", "횡성휴게소", 145.0, hasFuel = true, hasCharger = true, hasFood = true)
    private val stops = listOf(deokpyeong, yeoju, munmak, hoengseong)

    private fun car(id: String, km: Double, rangeKm: Double?, onRoute: Boolean = true) = MemberState(
        member = Member(id, id, 0xFF000000),
        vehicle = VehicleSnapshot(rangeKm = rangeKm),
        progressKm = km,
        onPlannedRoute = onRoute,
    )

    @Test
    fun lowFuelCarRulesOutStopsItCannotReachWithMargin() {
        val members = listOf(car("me", 0.0, 520.0), car("sua", 0.0, 95.0), car("phone", 0.0, null))
        val ranked = MeetingPointEngine.recommend(
            RecommendRequest(members, stops, continuousDrivingMinutes = 0, needsEnergy = true),
        )
        val ids = ranked.map { it.stop.id }
        assertFalse("문막(112km)은 95km 차가 20% 여유로 갈 수 없다", "munmak" in ids)
        assertFalse("hoengseong" in ids)
        assertEquals("deokpyeong", ranked.first().stop.id)
    }

    @Test
    fun sleepySignalPicksTheNearestRestArea() {
        val members = listOf(car("me", 106.0, 410.0), car("minji", 105.2, 305.0), car("jiwon", 104.1, 290.0))
        val best = MeetingPointEngine.best(
            RecommendRequest(members, stops, continuousDrivingMinutes = 20, sleepy = true),
        )
        assertNotNull(best)
        assertEquals("munmak", best!!.stop.id)
        assertEquals(6.0, best.distanceKm, 1e-9)
        assertTrue("가장 가까운 쉴 곳" in best.reasons)
    }

    @Test
    fun stopsBehindTheLeaderAreNotSuggested() {
        val members = listOf(car("me", 80.0, 400.0), car("b", 79.0, 400.0))
        val ranked = MeetingPointEngine.recommend(RecommendRequest(members, stops, continuousDrivingMinutes = 30))
        assertTrue(ranked.none { it.stop.kmFromStart <= 80.0 })
    }

    @Test
    fun restBeforeTwoHoursIsPreferred() {
        // 100분 연속 운전: 20분 안에 닿는 곳(여주, 18km)이 2시간을 넘기는 곳(횡성)보다 낫다
        val members = listOf(car("me", 60.0, 500.0), car("b", 59.0, 500.0))
        val ranked = MeetingPointEngine.recommend(RecommendRequest(members, stops, continuousDrivingMinutes = 100))
        val yeojuScore = ranked.first { it.stop.id == "yeoju" }.score
        val hoengseongScore = ranked.first { it.stop.id == "hoengseong" }.score
        assertTrue(yeojuScore > hoengseongScore)
    }

    @Test
    fun noMembersMeansNoRecommendation() {
        assertTrue(MeetingPointEngine.recommend(RecommendRequest(emptyList(), stops, 0)).isEmpty())
    }
}
