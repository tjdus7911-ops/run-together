package com.runtogether.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class RecommendRequest(
    val members: List<MemberState>,
    val candidates: List<StopCandidate>,
    /** 마지막 휴식 이후 연속 운전 시간 (분) */
    val continuousDrivingMinutes: Int,
    val averageSpeedKmh: Double = 80.0,
    /** 누군가 '졸려요'를 눌렀는지 */
    val sleepy: Boolean = false,
    /** 누군가 주유·충전이 필요한지 */
    val needsEnergy: Boolean = false,
)

data class Recommendation(
    val stop: StopCandidate,
    val score: Double,
    /** 선두 차 기준 남은 거리 (km) */
    val distanceKm: Double,
    /** 선두 차 기준 도착까지 (분) */
    val etaMinutes: Int,
    val reasons: List<String>,
)

/**
 * 집결지·휴식 추천 엔진.
 *
 * 다음 후보 휴게소·졸음쉼터마다 다섯 가지로 점수를 매겨 가장 높은 곳을 추천한다.
 * 1. 도달 가능성: 모임에서 연료·배터리가 가장 적은 차도 안전 여유(20%)를 두고 갈 수 있는가
 * 2. 휴식 시점: 연속 운전 2시간을 넘기 전에 도착하는가
 * 3. 졸림 신호: '졸려요'가 들어오면 거리 가중치를 높여 가장 가까운 쉴 곳을 우선
 * 4. 편의시설: 주유·충전·화장실·식당
 * 5. 기다림 최소화: 차들의 도착 예정 시각 차이가 가장 작은 곳
 */
object MeetingPointEngine {
    const val RANGE_SAFETY_MARGIN = 1.2
    const val REST_LIMIT_MINUTES = 120
    /** 다른 길로 가는 차는 거리를 조금 더 길게 본다 */
    const val OFF_ROUTE_DETOUR = 1.15

    fun recommend(request: RecommendRequest): List<Recommendation> {
        if (request.members.isEmpty()) return emptyList()
        val lead = request.members.maxOf { it.progressKm }
        return request.candidates
            .filter { it.kmFromStart > lead + 0.5 }
            .mapNotNull { score(it, request, lead) }
            .sortedByDescending { it.score }
    }

    fun best(request: RecommendRequest): Recommendation? = recommend(request).firstOrNull()

    private fun score(stop: StopCandidate, req: RecommendRequest, lead: Double): Recommendation? {
        val reasons = mutableListOf<String>()
        var score = 0.0

        // 1) 도달 가능성
        var tightestMargin = Double.MAX_VALUE
        for (m in req.members) {
            val range = m.vehicle.rangeKm ?: continue
            val needed = distanceFor(m, stop) * RANGE_SAFETY_MARGIN
            if (range < needed) return null
            tightestMargin = min(tightestMargin, (range - needed) / range)
        }
        if (tightestMargin != Double.MAX_VALUE) {
            score += tightestMargin * 20.0
            reasons += "모든 차가 도달 가능"
        }

        // 2) 휴식 시점
        val distance = stop.kmFromStart - lead
        val eta = distance / req.averageSpeedKmh * 60.0
        val minutesLeft = REST_LIMIT_MINUTES - req.continuousDrivingMinutes
        if (eta <= minutesLeft) {
            score += 30.0
            reasons += "연속 운전 2시간 전 도착"
        } else {
            score -= (eta - minutesLeft) * 0.8
        }

        // 3) 졸림 신호
        if (req.sleepy) {
            score += 60.0 * (1.0 - min(distance, 40.0) / 40.0)
            if (distance <= 10.0) reasons += "가장 가까운 쉴 곳"
        } else if (distance < 3.0) {
            score -= 10.0 // 너무 가까우면 금방 다시 서야 한다
        }

        // 4) 편의시설
        if (req.needsEnergy) {
            if (stop.hasFuel || stop.hasCharger) {
                score += 20.0
                reasons += "주유·충전 가능"
            } else {
                score -= 25.0
            }
        }
        if (stop.hasToilet) score += 5.0
        if (stop.hasFood) score += 5.0

        // 5) 기다림 최소화
        val etas = req.members.map { distanceFor(it, stop) / req.averageSpeedKmh * 60.0 }
        val spread = (etas.maxOrNull() ?: 0.0) - (etas.minOrNull() ?: 0.0)
        score -= spread * 0.5

        return Recommendation(
            stop = stop,
            score = score,
            distanceKm = distance,
            etaMinutes = max(1, eta.roundToInt()),
            reasons = reasons,
        )
    }

    private fun distanceFor(m: MemberState, stop: StopCandidate): Double {
        val base = (stop.kmFromStart - m.progressKm).coerceAtLeast(0.0)
        return if (m.onPlannedRoute) base else base * OFF_ROUTE_DETOUR
    }
}
