package com.runtogether.domain

data class GroupGap(
    /** 같은 길로 가는 차들 중 선두와 후미 사이 거리 (km) */
    val spreadKm: Double,
    val leadId: String?,
    val tailId: String?,
    /** 이웃한 두 차 사이에서 가장 많이 벌어진 거리 (km) */
    val largestGapKm: Double,
    /** 가장 많이 벌어진 구간의 뒤쪽 차 */
    val largestGapBehindId: String?,
    /** 가장 많이 벌어진 구간의 앞쪽 차 */
    val largestGapAheadId: String?,
    /** 다른 길로 가는 차 */
    val offRouteIds: List<String>,
)

/** 모임 간격 계산. 따라잡으라는 안내 대신 간격을 보여 주기만 한다. */
object GapCalculator {
    fun compute(members: List<MemberState>): GroupGap {
        val onRoute = members.filter { it.onPlannedRoute }.sortedBy { it.progressKm }
        val offRoute = members.filter { !it.onPlannedRoute }.map { it.member.id }
        if (onRoute.isEmpty()) return GroupGap(0.0, null, null, 0.0, null, null, offRoute)

        var largest = 0.0
        var behind: String? = null
        var ahead: String? = null
        for (i in 0 until onRoute.size - 1) {
            val gap = onRoute[i + 1].progressKm - onRoute[i].progressKm
            if (gap > largest) {
                largest = gap
                behind = onRoute[i].member.id
                ahead = onRoute[i + 1].member.id
            }
        }
        return GroupGap(
            spreadKm = onRoute.last().progressKm - onRoute.first().progressKm,
            leadId = onRoute.last().member.id,
            tailId = onRoute.first().member.id,
            largestGapKm = largest,
            largestGapBehindId = behind,
            largestGapAheadId = ahead,
            offRouteIds = offRoute,
        )
    }
}
