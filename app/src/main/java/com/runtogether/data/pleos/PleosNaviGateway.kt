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
import com.runtogether.domain.StopCandidate

/**
 * Pleos NaviHelper SDK: 모임 코스(경유지 + 목적지)를 차량 내비에 넣고,
 * 쉬어가기 투표가 통과되면 쉴 곳을 첫 번째 경유지로 추가한다.
 * 연결 전에 들어온 요청은 모아 뒀다가 연결되면 보낸다.
 */
class PleosNaviGateway(context: Context, private val log: NavigationGateway) : NavigationGateway {

    private val main = Handler(Looper.getMainLooper())
    private val pending = ArrayDeque<(NaviHelper) -> Unit>()
    private var connected = false

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

    private companion object {
        const val TAG = "PleosNavi"
    }
}
