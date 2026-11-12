package com.runtogether.data

import com.runtogether.domain.StopCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 모임 코스를 차량 내비에 넣는 창구.
 * Pleos Connect 의 NaviHelper SDK(경로·경유지 설정)로 연결하는 구현을 이 인터페이스 뒤에 둔다.
 */
interface NavigationGateway {
    /** 경유지(집결지)와 목적지를 순서대로 넣는다. 출발지는 뺀다. */
    fun setCourse(stops: List<StopCandidate>)

    /** 쉬어가기 투표가 통과되면 다음 경유지로 넣는다. */
    fun addWaypoint(stop: StopCandidate)
}

/** 시연용: 실제 내비 대신 기록만 남긴다. */
class DemoNavigationGateway : NavigationGateway {
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    override fun setCourse(stops: List<StopCandidate>) {
        _log.value = _log.value + ("경로 설정: " + stops.joinToString(" → ") { it.name })
    }

    override fun addWaypoint(stop: StopCandidate) {
        _log.value = _log.value + "경유지 추가: ${stop.name}"
    }
}
