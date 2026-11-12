package com.runtogether.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NaviGuidanceTest {

    @Test
    fun turnTypesFromNaviHelperAreMappedByName() {
        assertNull(TurnKinds.of(null))
        assertNull(TurnKinds.of("TURN_UNKNOWN"))
        assertEquals(TurnKind.RIGHT, TurnKinds.of("TURN_SHARP_R"))
        assertEquals(TurnKind.KEEP_LEFT, TurnKinds.of("TURN_KEEP_L"))
        assertEquals(TurnKind.IC_ENTER, TurnKinds.of("TURN_ENTER_IC_C"))
        assertEquals(TurnKind.IC_EXIT, TurnKinds.of("TURN_EXIT_IC_R"))
        assertEquals(TurnKind.SERVICE_AREA, TurnKinds.of("TURN_ENTER_SERVICEAREA"))
        assertEquals(TurnKind.SERVICE_AREA, TurnKinds.of("TURN_RESTAREA"))
        assertEquals(TurnKind.ROUNDABOUT, TurnKinds.of("TURN_ENTER_ROUNDABOUT_03"))
        assertEquals(TurnKind.STRAIGHT, TurnKinds.of("TURN_AVOID_UNDER_L"))
        assertEquals(TurnKind.OTHER, TurnKinds.of("TURN_SOMETHING_NEW"))
    }

    @Test
    fun trafficLevels() {
        assertEquals(TrafficLevel.SMOOTH, TurnKinds.trafficOf("FAST"))
        assertEquals(TrafficLevel.SLOW, TurnKinds.trafficOf("MODERATE"))
        assertEquals(TrafficLevel.JAM, TurnKinds.trafficOf("HEAVY"))
        assertEquals(TrafficLevel.UNKNOWN, TurnKinds.trafficOf(null))
    }

    @Test
    fun distancesReadLikeANavigation() {
        assertEquals("18 km", formatDistanceM(18_000.0))
        assertEquals("193 km", formatDistanceM(193_400.0))
        assertEquals("1.2 km", formatDistanceM(1_240.0))
        assertEquals("2 km", formatDistanceM(1_980.0))
        assertEquals("850 m", formatDistanceM(840.0))
        assertEquals("60 m", formatDistanceM(60.0))
    }

    @Test
    fun energyKindNeverShowsBothFuelAndBattery() {
        val ice = Member("a", "가", 0L)
        val ev = Member("b", "나", 0L, isEv = true)
        assertEquals(EnergyKind.FUEL, MemberState(ice, VehicleSnapshot(energyPercent = 40)).energyKind)
        assertEquals(EnergyKind.BATTERY, MemberState(ev, VehicleSnapshot(energyPercent = 40)).energyKind)
        // 차량이 알려 준 종류가 회원 정보보다 먼저
        assertEquals(EnergyKind.BATTERY, MemberState(ice, VehicleSnapshot(energyPercent = 40, energyKind = EnergyKind.BATTERY)).energyKind)
        // 값이 없으면 아무것도 보여 주지 않는다 (휴대폰 참여자 등)
        assertNull(MemberState(ice, VehicleSnapshot()).energyKind)
    }
}
