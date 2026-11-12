package com.runtogether.state

import com.runtogether.domain.Member
import com.runtogether.domain.MemberState
import com.runtogether.domain.NaviGuidance
import com.runtogether.domain.QuickMessage
import com.runtogether.domain.Readiness
import com.runtogether.domain.Recommendation
import com.runtogether.domain.RestVoteState
import com.runtogether.domain.StatusSignal
import com.runtogether.domain.StopCandidate
import com.runtogether.domain.VehicleSnapshot
import java.util.Locale

/** 화면은 차의 상태(시동·기어·속도)와 모임 상황을 따라 자동으로 바뀐다. */
enum class Phase { PRE_DEPARTURE, DRIVING, AT_MEETING_POINT }

/** 모임 코스의 한 지점 (출발지·집결지·목적지) */
data class PlannedStop(
    val stop: StopCandidate,
    /** 예정 시각 (자정부터 분) */
    val plannedClock: Int,
    val restMinutes: Int = 0,
    val label: String,
)

data class FeedItem(
    val memberId: String,
    val whenText: String,
    val kindText: String,
    val text: String,
    val readAloud: Boolean = true,
)

data class Notice(val text: String, val warning: Boolean)

/** 다른 차에서 온 상황 공유 */
data class IncomingAlert(
    val fromId: String,
    val signal: StatusSignal,
    val recommendation: Recommendation?,
    val restMinutes: Int,
    val spokenText: String,
)

data class VoteResult(val accepted: Boolean, val title: String, val detail: String)

/** ‘음성으로 상황 공유’ 진행 상태: 듣는 중 → 받아 적은 문장 확인(3초 뒤 자동 전송) → 보냄 / 실패 */
data class VoiceShare(
    val stage: Stage,
    val text: String? = null,
    /** 음성 인식이 없는 기기에서 예시 문장으로 보여 주는 시연인지 */
    val demo: Boolean = false,
    val problem: String? = null,
) {
    enum class Stage { LISTENING, CONFIRM, SENT, FAILED }
}

/** 집결지 도착 화면에 보여 줄 값 */
data class RegroupInfo(
    val stopLabel: String,
    val stopName: String,
    val restMinutes: Int,
    val departClock: Int,
    val refuelMemberId: String?,
    val refuelFrom: Int,
    val refuelTo: Int,
    val lateJoinMemberId: String?,
    val lateJoinMinutes: Int,
    val pendingMemberId: String?,
    val pendingEtaMinutes: Int?,
)

data class DriveUiState(
    val demoStep: Int,
    val stepTitle: String,
    /** 지금 시각 (자정부터 분) */
    val clockMin: Int,
    val phase: Phase,
    val groupName: String,
    val meId: String,
    val members: List<MemberState>,
    val course: List<PlannedStop>,
    /** course 에서 다음 지점의 위치 */
    val nextStopIndex: Int,
    val candidates: List<StopCandidate>,
    val continuousDrivingMin: Int,
    val feed: List<FeedItem> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val alert: IncomingAlert? = null,
    val vote: RestVoteState? = null,
    /** 투표를 시작하게 한 상황 공유 버튼 */
    val voteSignal: StatusSignal? = null,
    val voteResult: VoteResult? = null,
    val regroup: RegroupInfo? = null,
    /** 실제 차량 데이터(AAOS)가 들어오고 있는지 */
    val vehicleLive: Boolean = false,
    val toast: String? = null,
    /** 차량 내비(Pleos NaviHelper)에서 받은 길 안내. null 이면 화면이 시연 안내를 계산한다 ([shownGuidance]) */
    val guidance: NaviGuidance? = null,
    /** 다른 차 위치가 동기화 서버에서 실시간으로 오는지 (시연에서는 false → ‘시연 위치’로 표시) */
    val groupLive: Boolean = false,
    /** 휴대폰에서 만든 내 메시지 (차량에서는 고르기만 한다, 최대 6개) */
    val myMessages: List<QuickMessage> = emptyList(),
    /** 주행 화면에 내 메시지 고르기 패널이 떠 있는지 */
    val messagePanelOpen: Boolean = false,
    /** 방금 보낸 내 메시지 (버튼에 ‘보냈어요’ 표시) */
    val sentMessageId: String? = null,
    /** 30초 안에는 다시 보낼 수 없는 내 메시지 */
    val coolingMessageIds: Set<String> = emptySet(),
    /** 받은 내 메시지 · 음성 메시지를 읽어 줄지 (끄면 자막과 소식만) */
    val readMessagesAloud: Boolean = true,
    val voiceShare: VoiceShare? = null,
    /** 실제 차량에서 받은 내 차 값 그대로 (화면의 ‘내 차량 상태’는 실차일 때 이 값만 보여 준다) */
    val myLiveVehicle: VehicleSnapshot? = null,
) {
    val me: MemberState get() = members.first { it.member.id == meId }
    val nextStop: PlannedStop? get() = course.getOrNull(nextStopIndex)
    val readyCount: Int get() = members.count { it.readiness == Readiness.READY }
    val arrivedCount: Int get() = members.count { it.arrived }

    fun member(id: String): Member? = members.firstOrNull { it.member.id == id }?.member
    fun stateOf(id: String): MemberState? = members.firstOrNull { it.member.id == id }

    /** 내 차 기준 남은 거리 (km) */
    fun distanceTo(stop: StopCandidate): Double = (stop.kmFromStart - me.progressKm).coerceAtLeast(0.0)

    /** 평균 80km/h 기준 걸리는 시간 (분) */
    fun minutesTo(stop: StopCandidate, fromKm: Double = me.progressKm): Int =
        Math.round(((stop.kmFromStart - fromKm).coerceAtLeast(0.0)) / AVERAGE_SPEED_KMH * 60.0).toInt()

    companion object {
        const val AVERAGE_SPEED_KMH = 80.0
    }
}

fun formatClock(minutes: Int): String {
    val m = ((minutes % 1440) + 1440) % 1440
    return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)
}

/** 10km 이상은 정수, 그 아래는 소수 한 자리 (6.0 → 6) */
fun formatKm(km: Double): String {
    if (km >= 10) return Math.round(km).toString()
    val tenths = Math.round(km * 10)
    return if (tenths % 10 == 0L) (tenths / 10).toString() else String.format(Locale.ROOT, "%.1f", tenths / 10.0)
}

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}

/** 실제 차량 데이터로 '나'의 차 상태를 덮어쓴다. */
fun DriveUiState.withMyVehicle(live: VehicleSnapshot?): DriveUiState {
    if (live == null) return this
    return copy(
        vehicleLive = true,
        myLiveVehicle = live,
        continuousDrivingMin = live.tripDrivingMinutes ?: continuousDrivingMin,
        members = members.map { m ->
            if (m.member.id != meId) m else m.copy(
                vehicle = m.vehicle.copy(
                    ignitionOn = live.ignitionOn,
                    gear = live.gear,
                    speedKmh = live.speedKmh,
                    energyPercent = live.energyPercent ?: m.vehicle.energyPercent,
                    energyKind = if (live.energyPercent != null) live.energyKind else m.vehicle.energyKind,
                    rangeKm = live.rangeKm ?: m.vehicle.rangeKm,
                    tripDrivingMinutes = live.tripDrivingMinutes ?: m.vehicle.tripDrivingMinutes,
                ),
                readiness = if (phase == Phase.PRE_DEPARTURE) {
                    if (live.ignitionOn) Readiness.READY else Readiness.WAITING
                } else m.readiness,
            )
        },
    )
}
