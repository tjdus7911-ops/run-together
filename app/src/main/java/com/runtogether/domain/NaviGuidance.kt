package com.runtogether.domain

/** 다음 안내 방향 — 내비 TBT(TurnType) 를 화면용으로 줄인 것 */
enum class TurnKind { STRAIGHT, LEFT, RIGHT, KEEP_LEFT, KEEP_RIGHT, UTURN, IC_ENTER, IC_EXIT, SERVICE_AREA, TOLL, TUNNEL, ROUNDABOUT, ARRIVE, OTHER }

/** 교통 상황 (내비 TrafficCongestionType) */
enum class TrafficLevel(val label: String) { SMOOTH("원활"), SLOW("서행"), JAM("정체"), UNKNOWN("정보 없음") }

/** 경로 위 막히는 구간 — [offsetM] 는 지금 위치에서 앞쪽으로 떨어진 거리 */
data class CongestionSpan(
    val offsetM: Double,
    val lengthM: Double,
    val level: TrafficLevel,
    val roadName: String? = null,
)

/**
 * 화면에 보여 줄 길 안내 값.
 * - [live] = true: 차량 내비(Pleos NaviHelper)에서 받은 값
 * - [live] = false: 내비가 없는 기기에서 시연 상태로 계산한 값 (화면에 ‘시연 안내’로 표시)
 */
data class NaviGuidance(
    val live: Boolean,
    val turn: TurnKind? = null,
    val turnDistanceM: Double? = null,
    val turnText: String? = null,
    val destinationName: String? = null,
    val destinationRemainM: Int? = null,
    val destinationRemainSec: Int? = null,
    /** 도착 예정 시각 (자정부터 분) */
    val destinationEtaClock: Int? = null,
    val waypointName: String? = null,
    val waypointRemainM: Int? = null,
    val waypointRemainSec: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val headingDeg: Double? = null,
    val congestion: List<CongestionSpan> = emptyList(),
    val speedLimitKmh: Int? = null,
) {
    /** 다음 안내가 있는지 (내비가 연결됐지만 경로 안내 전이면 false) */
    val hasTurn: Boolean get() = turn != null && turnDistanceM != null
}

object TurnKinds {
    /** Pleos NaviHelper TurnType 이름 → 화면용 방향 (SDK 버전이 바뀌어도 이름으로 맞춘다) */
    fun of(name: String?): TurnKind? = when {
        name == null || name == "TURN_UNKNOWN" -> null
        name == "TURN_STRAIGHT" -> TurnKind.STRAIGHT
        name == "TURN_LEFT" || name == "TURN_SHARP_L" -> TurnKind.LEFT
        name == "TURN_RIGHT" || name == "TURN_SHARP_R" -> TurnKind.RIGHT
        name == "TURN_KEEP_L" -> TurnKind.KEEP_LEFT
        name == "TURN_KEEP_R" -> TurnKind.KEEP_RIGHT
        name == "TURN_UTURN" -> TurnKind.UTURN
        name.startsWith("TURN_ENTER_IC") -> TurnKind.IC_ENTER
        name.startsWith("TURN_EXIT_IC") -> TurnKind.IC_EXIT
        name == "TURN_ENTER_SERVICEAREA" || name == "TURN_RESTAREA" -> TurnKind.SERVICE_AREA
        name == "TURN_TOLL" -> TurnKind.TOLL
        name == "TURN_TUNNEL" -> TurnKind.TUNNEL
        name.contains("ROUNDABOUT") -> TurnKind.ROUNDABOUT
        name == "TURN_ARRIVE" -> TurnKind.ARRIVE
        // 지하차도·고가차도 진입/회피, 분기·합류, 경유지 안내는 직진 계열로 보여 준다
        name.startsWith("TURN_ENTER_UNDER") || name.startsWith("TURN_AVOID_UNDER") ||
            name.startsWith("TURN_ENTER_OVER") || name.startsWith("TURN_AVOID_OVER") ||
            name == "TURN_DEVIDE_MERGE" || name == "TURN_WAY" -> TurnKind.STRAIGHT
        else -> TurnKind.OTHER
    }

    fun trafficOf(name: String?): TrafficLevel = when (name) {
        "FAST" -> TrafficLevel.SMOOTH
        "MODERATE", "SLOW" -> TrafficLevel.SLOW
        "HEAVY" -> TrafficLevel.JAM
        else -> TrafficLevel.UNKNOWN
    }
}

/** 미터 → 화면 글자 ("18 km", "800 m", "1.2 km") */
fun formatDistanceM(meters: Double): String = when {
    meters >= 10_000 -> "${Math.round(meters / 1000.0)} km"
    meters >= 1_000 -> {
        val tenths = Math.round(meters / 100.0)
        if (tenths % 10 == 0L) "${tenths / 10} km" else "${tenths / 10}.${tenths % 10} km"
    }
    meters >= 100 -> "${(Math.round(meters / 50.0) * 50)} m"
    else -> "${Math.round(meters)} m"
}
