package com.runtogether.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GapCalculatorTest {

    private fun m(id: String, km: Double, onRoute: Boolean = true) =
        MemberState(Member(id, id, 0xFF000000), progressKm = km, onPlannedRoute = onRoute)

    @Test
    fun spreadAndLargestGapFollowTheScenario() {
        val members = listOf(
            m("me", 42.0), m("minji", 41.5), m("sua", 40.9),
            m("taehyun", 40.0, onRoute = false), m("jiwon", 39.4), m("haneul", 38.6),
        )
        val gap = GapCalculator.compute(members)
        assertEquals(3.4, gap.spreadKm, 1e-9)
        assertEquals("me", gap.leadId)
        assertEquals("haneul", gap.tailId)
        assertEquals(1.5, gap.largestGapKm, 1e-9)
        assertEquals("jiwon", gap.largestGapBehindId)
        assertEquals("sua", gap.largestGapAheadId)
        assertEquals(listOf("taehyun"), gap.offRouteIds)
    }

    @Test
    fun everyoneOffRouteGivesNoSpread() {
        val gap = GapCalculator.compute(listOf(m("a", 10.0, onRoute = false)))
        assertEquals(0.0, gap.spreadKm, 0.0)
        assertNull(gap.leadId)
    }
}
