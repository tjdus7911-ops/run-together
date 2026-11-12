package com.runtogether.domain

/**
 * 운전 중 안전 규칙.
 * - 주행 중 조작은 큰 버튼 한 번으로 끝낸다 (NHTSA 운전자 주의 분산 가이드라인: 한 번에 시선 2초, 한 작업에 총 12초 이내)
 * - 받는 쪽은 음성으로 듣고 말로 답할 수 있으며, 알림 카드는 10초 뒤 자동으로 닫힌다
 * - 글자 입력은 정차 중에만 연다
 */
object DrivingPolicy {
    const val ALERT_AUTO_DISMISS_MS = 10_000L
    /** 연속 운전 2시간이 되기 전에 먼저 '쉬었다 갈까요?'를 제안 */
    const val SUGGEST_REST_AFTER_MINUTES = 110
    private const val MOVING_SPEED_KMH = 3.0

    fun isDriving(v: VehicleSnapshot): Boolean =
        v.gear == Gear.DRIVE || v.gear == Gear.REVERSE || v.speedKmh > MOVING_SPEED_KMH

    fun shouldSuggestRest(continuousDrivingMinutes: Int): Boolean =
        continuousDrivingMinutes >= SUGGEST_REST_AFTER_MINUTES
}
