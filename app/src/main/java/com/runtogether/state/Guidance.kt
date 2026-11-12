package com.runtogether.state

import com.runtogether.demo.DemoMapData
import com.runtogether.domain.NaviGuidance
import com.runtogether.domain.TurnKind
import kotlin.math.roundToInt

/**
 * 내비가 연결되지 않은 기기(시연)에서 보여 줄 안내 값.
 * 다음 집결지까지의 거리·시간은 화면의 다른 곳과 같은 계산(평균 80km/h)이다.
 */
fun DriveUiState.demoGuidance(): NaviGuidance {
    val destination = course.last()
    val next = nextStop ?: destination
    val toNextKm = distanceTo(next.stop)
    val toDestinationKm = distanceTo(destination.stop)
    val arriving = next.stop.id == destination.stop.id
    return NaviGuidance(
        live = false,
        turn = if (arriving) TurnKind.ARRIVE else TurnKind.SERVICE_AREA,
        turnDistanceM = toNextKm * 1000.0,
        turnText = "${next.stop.name} 방향",
        destinationName = destination.stop.name,
        destinationRemainM = (toDestinationKm * 1000.0).roundToInt(),
        destinationRemainSec = minutesTo(destination.stop) * 60,
        destinationEtaClock = clockMin + minutesTo(destination.stop),
        waypointName = next.stop.name.takeIf { !arriving },
        waypointRemainM = (toNextKm * 1000.0).roundToInt().takeIf { !arriving },
        waypointRemainSec = (minutesTo(next.stop) * 60).takeIf { !arriving },
        congestion = DemoMapData.congestionAhead(me.progressKm),
    )
}

/** 화면에 보여 줄 안내: 차량 내비 값이 있으면 그것, 없으면 시연 값 */
val DriveUiState.shownGuidance: NaviGuidance
    get() = guidance ?: demoGuidance()

/** 다음 집결지까지 남은 거리·시간 */
data class StopEta(val km: Double, val minutes: Int, val arriveClock: Int, val live: Boolean)

/**
 * 다음 집결지까지. 차량 내비가 같은 곳을 경유지로 안내하고 있으면 내비 값을, 아니면 시연 계산을 쓴다.
 * (내비 값의 단위는 m · 초로 본다 — Pleos NaviHelper remainDistance / remainTime)
 */
fun DriveUiState.nextStopEta(nowClock: Int = clockMin): StopEta? {
    val next = nextStop ?: return null
    val g = guidance
    if (g != null && g.live && g.waypointRemainM != null && g.waypointRemainSec != null && g.waypointName == next.stop.name) {
        val minutes = (g.waypointRemainSec / 60.0).roundToInt()
        return StopEta(g.waypointRemainM / 1000.0, minutes, nowClock + minutes, live = true)
    }
    val minutes = minutesTo(next.stop)
    return StopEta(distanceTo(next.stop), minutes, clockMin + minutes, live = false)
}
