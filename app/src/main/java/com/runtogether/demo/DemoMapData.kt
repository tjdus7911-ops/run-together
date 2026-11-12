package com.runtogether.demo

import com.runtogether.domain.CongestionSpan
import com.runtogether.domain.TrafficLevel

/** 지도 위 한 지점 (출발지부터 코스를 따라 간 거리 km) */
data class MapPlace(val km: Double, val name: String?)

/** 길에서 떨어진 넓은 곳 (저수지·공원). [side] 는 진행 방향 기준 +1 / -1 */
data class MapArea(val km: Double, val side: Int, val lateralKm: Double, val radiusKm: Double, val name: String? = null)

/** 길 위의 교통 구간 */
data class TrafficSpan(val fromKm: Double, val toKm: Double, val level: TrafficLevel, val roadName: String? = null)

/**
 * 시연용 지도(MockNavMap)에 그릴 길·지명.
 * 시나리오 A 코스(잠실 → 경부 → 신갈JC → 영동고속도로 → 강릉)를 따라 대략적인 위치에 둔 값이라
 * 실제 지도와 다를 수 있다. 실제 지도 SDK 를 붙이면 이 데이터는 쓰지 않는다.
 */
object DemoMapData {
    const val EXPRESSWAY = "영동고속도로"
    const val EXPRESSWAY_NO = "50"
    const val NATIONAL_ROAD = "42번 국도"
    const val NATIONAL_ROAD_NO = "42"

    /** 국도는 진행 방향 기준 이쪽(+1)에 나란히 간다 */
    const val NATIONAL_SIDE = 1

    val interchanges = listOf(
        MapPlace(32.0, "마성IC"),
        MapPlace(38.2, "용인IC"),
        MapPlace(48.6, "양지IC"),
        MapPlace(62.0, "덕평IC"),
        MapPlace(65.5, "호법JC"),
        MapPlace(70.0, "이천IC"),
        MapPlace(84.0, "여주JC"),
        MapPlace(109.6, "문막IC"),
        MapPlace(118.0, "만종JC"),
        MapPlace(124.0, "원주IC"),
        MapPlace(136.0, "새말IC"),
    )

    /** 마을 이름은 국도 쪽 시가지 가운데에 */
    val towns = listOf(
        MapPlace(38.8, "용인시 처인구"),
        MapPlace(49.0, "양지면"),
        MapPlace(61.2, "이천시 마장면"),
        MapPlace(84.6, "여주시"),
        MapPlace(110.2, "원주시 문막읍"),
        MapPlace(124.4, "원주시"),
    )

    /** 고속도로를 가로지르는 물길 */
    val rivers = listOf(
        MapPlace(44.4, null),
        MapPlace(107.0, "섬강"),
        MapPlace(126.5, "원주천"),
    )

    val reservoirs = listOf(
        MapArea(41.4, side = -1, lateralKm = 1.35, radiusKm = 0.42),
        MapArea(56.0, side = -1, lateralKm = 1.6, radiusKm = 0.5),
        MapArea(101.6, side = -1, lateralKm = 1.7, radiusKm = 0.48),
    )

    /** 국도의 교통 (시연 값) */
    val nationalTraffic = listOf(
        TrafficSpan(36.0, 37.6, TrafficLevel.SMOOTH),
        TrafficSpan(37.6, 38.7, TrafficLevel.JAM),
        TrafficSpan(38.7, 39.4, TrafficLevel.SMOOTH),
        TrafficSpan(39.4, 41.3, TrafficLevel.SLOW),
        TrafficSpan(41.3, 48.0, TrafficLevel.SMOOTH),
        TrafficSpan(48.0, 49.2, TrafficLevel.SLOW),
        TrafficSpan(98.0, 105.4, TrafficLevel.SMOOTH),
        TrafficSpan(105.4, 106.9, TrafficLevel.SLOW),
        TrafficSpan(106.9, 109.0, TrafficLevel.SMOOTH),
        TrafficSpan(109.0, 110.0, TrafficLevel.JAM),
        TrafficSpan(110.0, 116.0, TrafficLevel.SMOOTH),
    )

    /** 모임 경로(고속도로) 위 막히는 구간 — 시연 내비 안내에 쓴다 */
    val routeCongestion = listOf(
        TrafficSpan(44.8, 46.0, TrafficLevel.SLOW, EXPRESSWAY),
        TrafficSpan(108.3, 109.0, TrafficLevel.SLOW, EXPRESSWAY),
    )

    /** 내 위치([meKm]) 앞쪽의 막히는 구간을 내비 값 모양(앞쪽 거리)으로 */
    fun congestionAhead(meKm: Double): List<CongestionSpan> = routeCongestion
        .filter { it.toKm > meKm }
        .map {
            val from = maxOf(it.fromKm, meKm)
            CongestionSpan(
                offsetM = (from - meKm) * 1000.0,
                lengthM = (it.toKm - from) * 1000.0,
                level = it.level,
                roadName = it.roadName,
            )
        }
}
