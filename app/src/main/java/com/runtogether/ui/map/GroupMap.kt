package com.runtogether.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.runtogether.domain.CongestionSpan
import com.runtogether.domain.GroupGap
import com.runtogether.domain.JoinMode
import com.runtogether.domain.Member
import com.runtogether.domain.NaviGuidance
import com.runtogether.state.DriveUiState

/** 위치를 어디서 받았는지 (지도에서 시연 값과 실제 값을 나눠 보이게) */
enum class PositionSource {
    /** 시연 시나리오 값 */
    DEMO,

    /** 차량 내비(Pleos NaviHelper 현재 위치) */
    CAR_NAVI,

    /** 휴대폰 참여자의 GPS (동기화 서버로 받음) */
    PHONE_GPS,
}

/** 지도에 올릴 차 한 대. 시연 지도는 [progressKm] 로, 실제 지도 SDK 는 좌표로 그린다. */
data class MapVehicle(
    val member: Member,
    val progressKm: Double,
    val onPlannedRoute: Boolean,
    val isMe: Boolean,
    val isLead: Boolean,
    val source: PositionSource,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val headingDeg: Double? = null,
)

/** 코스 지점 · 휴게소 */
data class MapStop(
    val id: String,
    val name: String,
    /** "덕평자연휴게소" → "덕평" */
    val shortName: String,
    val km: Double,
    val label: String,
    val isNext: Boolean,
    val isCourse: Boolean,
    val latitude: Double?,
    val longitude: Double?,
)

/** 지도 그리기에 필요한 값 (지도 SDK 와 상관없는 모양) */
data class GroupMapModel(
    val vehicles: List<MapVehicle>,
    val stops: List<MapStop>,
    /** 경로 위 막히는 구간 (내 위치 기준 앞쪽 거리, 내비 값) */
    val congestion: List<CongestionSpan>,
    val largestGapKm: Double,
    val largestGapBehindId: String?,
    val largestGapAheadId: String?,
    /** 다음 집결지까지 (km) */
    val nextStopDistanceKm: Double?,
) {
    val me: MapVehicle? get() = vehicles.firstOrNull { it.isMe }
}

/** 화면에서 바꾸는 지도 보기: 자동 맞춤에서 몇 단계 확대(+)/축소(-)했는지, 내 차를 따라가는지 */
data class MapCamera(val zoomStep: Int = 0, val followMe: Boolean = false) {
    companion object {
        const val MIN_STEP = -3
        const val MAX_STEP = 2
    }
}

/**
 * 지도 그리기 창구.
 * Pleos NaviHelper 는 길 안내 값만 주고 지도를 그려 주지 않는다. 그래서 지도는 이 인터페이스 뒤에 두고,
 * 지금은 시연용 그림 지도([MockNavMapRenderer])를 쓴다. 실제 지도 SDK(카카오·네이버·TMAP 등)를 쓸 때는
 * 이 인터페이스를 구현해 [LocalGroupMapRenderer] 로 바꿔 끼우면 주행 화면은 그대로다.
 */
interface GroupMapRenderer {
    /** true 면 화면에 ‘시연용 지도’라고 표시한다 */
    val isDemo: Boolean

    @Composable
    fun Render(model: GroupMapModel, camera: MapCamera, modifier: Modifier)
}

val LocalGroupMapRenderer = staticCompositionLocalOf<GroupMapRenderer> { MockNavMapRenderer }

/** 화면 상태 → 지도 값 */
fun DriveUiState.toMapModel(gap: GroupGap, guidance: NaviGuidance): GroupMapModel {
    val leadId = gap.leadId
    val vehicles = members.map { m ->
        val isMe = m.member.id == meId
        val fromNavi = isMe && guidance.live && guidance.latitude != null && guidance.longitude != null
        MapVehicle(
            member = m.member,
            progressKm = m.progressKm,
            onPlannedRoute = m.onPlannedRoute,
            isMe = isMe,
            isLead = m.member.id == leadId,
            source = when {
                fromNavi -> PositionSource.CAR_NAVI
                groupLive && m.member.joinMode == JoinMode.PHONE -> PositionSource.PHONE_GPS
                else -> PositionSource.DEMO
            },
            latitude = if (fromNavi) guidance.latitude else null,
            longitude = if (fromNavi) guidance.longitude else null,
            headingDeg = if (fromNavi) guidance.headingDeg else null,
        )
    }
    val next = nextStop
    val courseStops = course.mapIndexed { i, p ->
        MapStop(
            id = p.stop.id,
            name = p.stop.name,
            shortName = p.stop.shortName,
            km = p.stop.kmFromStart,
            label = p.label,
            isNext = i == nextStopIndex,
            isCourse = true,
            latitude = p.stop.lat,
            longitude = p.stop.lng,
        )
    }
    val restAreas = candidates
        .filter { c -> course.none { it.stop.id == c.id } }
        .map {
            MapStop(
                id = it.id,
                name = it.name,
                shortName = it.shortName,
                km = it.kmFromStart,
                label = "휴게소",
                isNext = false,
                isCourse = false,
                latitude = it.lat,
                longitude = it.lng,
            )
        }
    return GroupMapModel(
        vehicles = vehicles,
        stops = courseStops + restAreas,
        congestion = guidance.congestion,
        largestGapKm = gap.largestGapKm,
        largestGapBehindId = gap.largestGapBehindId,
        largestGapAheadId = gap.largestGapAheadId,
        nextStopDistanceKm = next?.let { distanceTo(it.stop) },
    )
}
