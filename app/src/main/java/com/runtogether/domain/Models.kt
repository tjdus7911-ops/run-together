package com.runtogether.domain

/** 모임에 참여하는 방식: 플레오스 차량 앱 또는 설치 없는 휴대폰 웹 링크 */
enum class JoinMode { CAR, PHONE }

data class Member(
    val id: String,
    val name: String,
    /** 화면에서 이 사람을 나타내는 색 (ARGB) */
    val color: Long,
    val isLeader: Boolean = false,
    val joinMode: JoinMode = JoinMode.CAR,
    val isEv: Boolean = false,
)

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/** 차량 데이터 한 시점 값. 휴대폰 참여자는 대부분 비어 있다. */
data class VehicleSnapshot(
    val ignitionOn: Boolean = false,
    val gear: Gear = Gear.PARK,
    val speedKmh: Double = 0.0,
    /** 연료 또는 배터리 잔량 (%) */
    val energyPercent: Int? = null,
    /** 남은 주행 가능 거리 (km) */
    val rangeKm: Double? = null,
    /** 이번 주행의 연속 운전 시간 (분), 차량이 알려 줄 때만 */
    val tripDrivingMinutes: Int? = null,
)

enum class Readiness { READY, COMING, WAITING }

data class MemberState(
    val member: Member,
    val vehicle: VehicleSnapshot = VehicleSnapshot(),
    /** 출발지부터 모임 코스를 따라 간 거리 (km) */
    val progressKm: Double = 0.0,
    /** false 면 다른 길로 가는 중 (다음 집결지에서 합류) */
    val onPlannedRoute: Boolean = true,
    val readiness: Readiness = Readiness.WAITING,
    val arrived: Boolean = false,
    /** 다음 지점까지 남은 시간 (분), 알 때만 */
    val etaMinutes: Int? = null,
)

/** 집결지·쉴 곳 후보 (휴게소, 졸음쉼터 등). 좌표는 내비 경유지 설정에 쓴다. */
data class StopCandidate(
    val id: String,
    val name: String,
    val kmFromStart: Double,
    val hasFuel: Boolean = false,
    val hasCharger: Boolean = false,
    val hasToilet: Boolean = true,
    val hasFood: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
) {
    /** "덕평자연휴게소" → "덕평" */
    val shortName: String
        get() = name.removeSuffix("자연휴게소").removeSuffix("휴게소").removeSuffix("졸음쉼터").ifBlank { name }
}

/** 원터치 상황 공유 버튼 */
enum class StatusSignal(val label: String) {
    SLEEPY("졸려요"),
    REST("쉬었다 갈까요?"),
    TOILET("화장실 급해요"),
    ENERGY("주유·충전"),
    SLOW("천천히 갈게요"),
    HELP("도움이 필요해요"),
}
