package com.runtogether.data.pleos

import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest
import ai.pleos.playground.navi.helper.NaviHelper
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.runtogether.data.NavigationGateway
import com.runtogether.domain.CongestionSpan
import com.runtogether.domain.NaviGuidance
import com.runtogether.domain.StopCandidate
import com.runtogether.domain.TurnKinds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

/**
 * Pleos NaviHelper SDK: 모임 코스(경유지 + 목적지)를 차량 내비에 넣고,
 * 쉬어가기 투표가 통과되면 쉴 곳을 첫 번째 경유지로 추가한다.
 * 연결 전에 들어온 요청은 모아 뒀다가 연결되면 보낸다.
 *
 * 주행 화면의 안내 카드(다음 안내 · 남은 거리 · 도착 예정)와 내 위치·교통 정보는 [guidance] 로 내보낸다.
 * NaviHelper 는 지도를 그려 주지 않으므로 지도 그림은 화면 쪽(GroupMapRenderer)이 맡는다.
 */
class PleosNaviGateway(context: Context, private val log: NavigationGateway) : NavigationGateway {

    private val main = Handler(Looper.getMainLooper())
    private val pending = ArrayDeque<(NaviHelper) -> Unit>()
    private var connected = false

    private val _guidance = MutableStateFlow<NaviGuidance?>(null)

    /** 차량 내비에서 받은 길 안내 (연결 전에는 null → 화면은 시연 안내를 쓴다) */
    val guidance: StateFlow<NaviGuidance?> = _guidance.asStateFlow()

    private val congestion = mutableListOf<CongestionSpan>()

    private fun updateGuidance(change: (NaviGuidance) -> NaviGuidance) {
        _guidance.update { change(it ?: NaviGuidance(live = true)) }
    }

    private val sink = object : PleosBridge.NaviSink {
        override fun onTurn(turnType: String?, distance: Double, description: String?) =
            updateGuidance { it.copy(turn = TurnKinds.of(turnType), turnDistanceM = distance, turnText = description) }

        override fun onDestination(name: String?, remainDistance: Int, remainTime: Int) = updateGuidance {
            it.copy(
                destinationName = name,
                destinationRemainM = remainDistance,
                destinationRemainSec = remainTime,
                destinationEtaClock = nowClock() + remainTime / 60,
            )
        }

        override fun onNextWaypoint(name: String?, remainDistance: Int, remainTime: Int) =
            updateGuidance { it.copy(waypointName = name, waypointRemainM = remainDistance, waypointRemainSec = remainTime) }

        override fun onNoWaypoint() =
            updateGuidance { it.copy(waypointName = null, waypointRemainM = null, waypointRemainSec = null) }

        override fun onLocation(latitude: Double, longitude: Double, heading: Double) =
            updateGuidance { it.copy(latitude = latitude, longitude = longitude, headingDeg = heading) }

        override fun onCongestionStart() {
            synchronized(congestion) { congestion.clear() }
        }

        override fun onCongestion(offset: Double, distance: Double, type: String?, roadName: String?) {
            synchronized(congestion) { congestion += CongestionSpan(offset, distance, TurnKinds.trafficOf(type), roadName) }
        }

        override fun onCongestionEnd() {
            val spans = synchronized(congestion) { congestion.toList() }
            updateGuidance { it.copy(congestion = spans) }
        }

        override fun onSpeedLimit(kmh: Int) = updateGuidance { it.copy(speedLimitKmh = kmh.takeIf { v -> v > 0 }) }
    }

    /** 안내 값은 요청하면 리스너로 온다: 2초마다 다음 안내·위치, 6초마다 남은 거리, 16초마다 교통 */
    private var tick = 0
    private val poll = object : Runnable {
        override fun run() {
            val helper = navi ?: return
            safely {
                helper.getTBTInfo()
                helper.getCurrentLocationInfo()
                if (tick % 3 == 0) {
                    helper.getDestinationInfo()
                    helper.getWaypointInfo()
                }
                if (tick % 8 == 0) helper.getRouteStateInfo()
            }
            tick++
            main.postDelayed(this, POLL_MS)
        }
    }

    private val navi: NaviHelper? = try {
        NaviHelper(context.applicationContext)
    } catch (t: Throwable) {
        Log.w(TAG, "NaviHelper unavailable", t)
        null
    }

    init {
        try {
            navi?.initialize(
                {
                    main.post {
                        connected = true
                        val helper = navi ?: return@post
                        while (pending.isNotEmpty()) safely { pending.removeFirst().invoke(helper) }
                        safely { helper.addListener(PleosBridge.naviListener(sink)) }
                        if (_guidance.value == null) _guidance.value = NaviGuidance(live = true)
                        main.removeCallbacks(poll)
                        main.post(poll)
                    }
                },
                { error -> Log.w(TAG, "NaviHelper connection failed", error) },
            )
        } catch (t: Throwable) {
            Log.w(TAG, "NaviHelper initialize failed", t)
        }
    }

    override fun setCourse(stops: List<StopCandidate>) {
        log.setCourse(stops)
        val located = stops.filter { it.lat != null && it.lng != null }
        val destination = located.lastOrNull() ?: return
        // NaviHelper 경유지는 두 곳까지 (FIRST, SECOND)
        val waypoints = located.dropLast(1).take(2).mapIndexed { i, stop ->
            waypointOf(stop, if (i == 0) WaypointIndex.FIRST else WaypointIndex.SECOND)
        }
        whenConnected { it.requestRouteWithWaypoints(RouteWithWaypointsRequest(routeOf(destination), waypoints)) }
    }

    override fun addWaypoint(stop: StopCandidate) {
        log.addWaypoint(stop)
        if (stop.lat == null || stop.lng == null) return
        whenConnected { it.addWaypoint(waypointOf(stop, WaypointIndex.FIRST)) }
    }

    fun release() {
        main.removeCallbacks(poll)
        try {
            navi?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "release failed", t)
        }
    }

    private fun whenConnected(action: (NaviHelper) -> Unit) {
        val helper = navi ?: return
        main.post {
            if (connected) safely { action(helper) } else pending.addLast(action)
        }
    }

    private fun routeOf(stop: StopCandidate) = RouteInfo(
        stop.lat ?: 0.0,
        stop.lng ?: 0.0,
        null,
        null,
        stop.name,
        "",
        "",
        "",
        RouteOption.RECOMMENDED,
        "",
        false,
    )

    private fun waypointOf(stop: StopCandidate, index: WaypointIndex) = RequestWaypointInfo(
        stop.lat ?: 0.0,
        stop.lng ?: 0.0,
        stop.name,
        index,
        "",
        "",
        "",
        "",
    )

    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(TAG, "NaviHelper call failed", t)
        }
    }

    private fun nowClock(): Int {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }

    private companion object {
        const val TAG = "PleosNavi"
        const val POLL_MS = 2_000L
    }
}
