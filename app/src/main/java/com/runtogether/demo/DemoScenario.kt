package com.runtogether.demo

import com.runtogether.domain.Gear
import com.runtogether.domain.JoinMode
import com.runtogether.domain.Josa
import com.runtogether.domain.MeetingPointEngine
import com.runtogether.domain.Member
import com.runtogether.domain.MemberState
import com.runtogether.domain.MessageIcon
import com.runtogether.domain.QuickMessage
import com.runtogether.domain.Readiness
import com.runtogether.domain.RecommendRequest
import com.runtogether.domain.StopCandidate
import com.runtogether.domain.VehicleSnapshot
import com.runtogether.domain.Vote
import com.runtogether.state.DriveUiState
import com.runtogether.state.FeedItem
import com.runtogether.state.Notice
import com.runtogether.state.Phase
import com.runtogether.state.PlannedStop
import com.runtogether.state.RegroupInfo

/**
 * 기획서 '시나리오 A — 동호회 6대, 서울 잠실 → 강릉 안목해변 정기 드라이브'를 그대로 재현하는 시연 데이터.
 * 실제 서비스에서는 동기화 서버가 다른 차의 상태를 보내 주고, 내 차 상태는 차량 데이터로 채운다.
 */
object DemoScenario {
    const val GROUP_NAME = "강릉 바다 드라이브"
    const val REST_MINUTES = 15

    const val STEP_CHECKIN = 0
    const val STEP_DRIVING = 1
    const val STEP_REGROUP = 2
    const val STEP_SLEEPY = 3
    const val LAST_STEP = STEP_SLEEPY

    // 회원 색: 흰 바탕과 밝은 지도에서 잘 보이고 흰 이니셜이 읽히는 짙은 색 (피그마 member/1~6 과 같은 값, 나 = 포인트 파랑)
    val me = Member("me", "나", 0xFF2563EB, isLeader = true)
    val minji = Member("minji", "민지", 0xFFC2410C)
    val sua = Member("sua", "수아", 0xFF0E7490)
    val taehyun = Member("taehyun", "태현", 0xFF7C3AED, isEv = true)
    val jiwon = Member("jiwon", "지원", 0xFFBE185D)
    val haneul = Member("haneul", "하늘", 0xFF475569, joinMode = JoinMode.PHONE)

    /** 휴대폰 앱에서 만들어 계정에 저장된 ‘내 메시지’ (시연 값, 최대 6개) */
    val myMessages = listOf(
        QuickMessage("m1", "카페 들렀다 가요", MessageIcon.COFFEE, 1),
        QuickMessage("m2", "사진 찍고 갈게요", MessageIcon.PHOTO, 2),
        QuickMessage("m3", "조금 늦을 것 같아요", MessageIcon.LATE, 3),
        QuickMessage("m4", "천천히 따라갈게요", MessageIcon.SLOW, 4),
        QuickMessage("m5", "점심은 휴게소에서 먹어요", MessageIcon.FOOD, 5),
        QuickMessage("m6", "도착하면 주차장에서 봐요", MessageIcon.PARKING, 6),
    )

    /** 주행 중(08:00) 수아가 보내는 내 메시지 */
    const val SUA_MESSAGE = "주유하고 바로 따라갈게요"

    /** 음성 인식이 없는 기기에서 ‘말로 메시지 보내기’를 보여 줄 예시 문장 */
    const val DEMO_SPOKEN = "도착하면 커피는 제가 살게요"

    // 좌표는 시연용 대략값입니다.
    val jamsil = StopCandidate("jamsil", "잠실종합운동장 주차장", 0.0, lat = 37.5152, lng = 127.0729)
    val deokpyeong = StopCandidate("deokpyeong", "덕평자연휴게소", 60.0, hasFuel = true, hasCharger = true, hasFood = true, lat = 37.2414, lng = 127.3713)
    val yeoju = StopCandidate("yeoju", "여주휴게소", 78.0, hasFuel = true, hasCharger = true, hasFood = true, lat = 37.3135, lng = 127.5985)
    val munmak = StopCandidate("munmak", "문막휴게소", 112.0, hasFuel = true, hasFood = true, lat = 37.3170, lng = 127.8288)
    val hoengseong = StopCandidate("hoengseong", "횡성휴게소", 145.0, hasFuel = true, hasCharger = true, hasFood = true, lat = 37.4890, lng = 128.0273)
    val pyeongchang = StopCandidate("pyeongchang", "평창휴게소", 190.0, hasFuel = true, hasFood = true, lat = 37.5616, lng = 128.4290)
    val anmok = StopCandidate("anmok", "강릉 안목해변", 235.0, lat = 37.7717, lng = 128.9474)

    val course = listOf(
        PlannedStop(jamsil, clock(7, 30), label = "출발"),
        PlannedStop(deokpyeong, clock(8, 12), restMinutes = 20, label = "집결 1"),
        PlannedStop(hoengseong, clock(9, 35), restMinutes = 15, label = "집결 2"),
        PlannedStop(anmok, clock(10, 40), label = "도착"),
    )

    val candidates = listOf(deokpyeong, yeoju, munmak, hoengseong, pyeongchang)

    fun clock(h: Int, m: Int): Int = h * 60 + m

    /** 시연에서 다른 회원이 투표하는 방식 (하늘은 응답하지 않음) */
    fun simulatedVote(memberId: String): Vote? = when (memberId) {
        minji.id, sua.id, jiwon.id -> Vote.YES
        taehyun.id -> Vote.NO
        else -> null
    }

    fun step(n: Int): DriveUiState = when (n) {
        STEP_CHECKIN -> checkin()
        STEP_DRIVING -> driving()
        STEP_REGROUP -> regroup()
        else -> sleepy()
    }

    private fun base(
        step: Int,
        title: String,
        clock: Int,
        phase: Phase,
        members: List<MemberState>,
        nextStopIndex: Int,
        continuousDrivingMin: Int,
    ) = DriveUiState(
        demoStep = step,
        stepTitle = title,
        clockMin = clock,
        phase = phase,
        groupName = GROUP_NAME,
        meId = me.id,
        members = members,
        course = course,
        nextStopIndex = nextStopIndex,
        candidates = candidates,
        continuousDrivingMin = continuousDrivingMin,
        myMessages = myMessages,
    )

    private fun checkin(): DriveUiState {
        val members = listOf(
            MemberState(me, VehicleSnapshot(ignitionOn = true, energyPercent = 72, rangeKm = 520.0), readiness = Readiness.READY),
            MemberState(minji, VehicleSnapshot(ignitionOn = true, energyPercent = 55, rangeKm = 410.0), readiness = Readiness.READY),
            MemberState(sua, VehicleSnapshot(ignitionOn = true, energyPercent = 18, rangeKm = 95.0), readiness = Readiness.READY),
            MemberState(taehyun, VehicleSnapshot(ignitionOn = true, energyPercent = 64, rangeKm = 280.0), readiness = Readiness.READY),
            MemberState(jiwon, VehicleSnapshot(ignitionOn = false, energyPercent = 47, rangeKm = 380.0), readiness = Readiness.COMING, etaMinutes = 2),
            MemberState(haneul, VehicleSnapshot(), readiness = Readiness.WAITING),
        )
        // 엔진이 고른 첫 집결지와, 그 이유가 된 가장 연료가 적은 차
        val first = MeetingPointEngine.best(RecommendRequest(members, candidates, continuousDrivingMinutes = 0, needsEnergy = true))
        val lowest = members.filter { it.vehicle.rangeKm != null }.minByOrNull { it.vehicle.rangeKm ?: Double.MAX_VALUE }
        val carCount = members.count { it.member.joinMode == JoinMode.CAR }
        val phoneNames = members.filter { it.member.joinMode == JoinMode.PHONE }.joinToString(", ") { "${it.member.name}님" }
        val notices = buildList {
            add(Notice("차량 연동 ${carCount}대의 내비에 같은 코스를 넣었어요 · ${phoneNames}은 폰 링크로 참여", warning = false))
            val pct = lowest?.vehicle?.energyPercent
            if (first != null && lowest != null && pct != null && pct <= 20) {
                val stopName = first.stop.shortName
                add(Notice("${lowest.member.name}님 연료 ${pct}% — 첫 집결지를 주유소가 있는 ${stopName}${Josa.euroRo(stopName)} 잡았어요", warning = true))
            }
        }
        return base(STEP_CHECKIN, "07:25 출발 전", clock(7, 25), Phase.PRE_DEPARTURE, members, nextStopIndex = 1, continuousDrivingMin = 0)
            .copy(notices = notices)
    }

    private fun driving(): DriveUiState {
        val members = listOf(
            MemberState(me, VehicleSnapshot(true, Gear.DRIVE, 92.0, 66, 482.0), progressKm = 42.0, readiness = Readiness.READY),
            MemberState(minji, VehicleSnapshot(true, Gear.DRIVE, 90.0, 50, 372.0), progressKm = 41.5, readiness = Readiness.READY),
            MemberState(sua, VehicleSnapshot(true, Gear.DRIVE, 88.0, 10, 54.0), progressKm = 40.9, readiness = Readiness.READY),
            MemberState(taehyun, VehicleSnapshot(true, Gear.DRIVE, 84.0, 58, 251.0), progressKm = 40.0, onPlannedRoute = false, readiness = Readiness.READY),
            MemberState(jiwon, VehicleSnapshot(true, Gear.DRIVE, 95.0, 43, 345.0), progressKm = 39.4, readiness = Readiness.READY),
            MemberState(haneul, VehicleSnapshot(speedKmh = 86.0), progressKm = 38.6, readiness = Readiness.READY),
        )
        val feed = listOf(
            FeedItem(minji.id, "방금", "말로 보낸 메시지", "덕평에서 커피 한잔하고 가요"),
            FeedItem(sua.id, "2분 전", "상황 공유", "주유·충전이 필요해요"),
        )
        return base(STEP_DRIVING, "08:00 주행 중", clock(8, 0), Phase.DRIVING, members, nextStopIndex = 1, continuousDrivingMin = 30)
            .copy(feed = feed)
    }

    private fun regroup(): DriveUiState {
        val members = listOf(
            MemberState(me, VehicleSnapshot(true, Gear.PARK, 0.0, 63, 455.0), progressKm = 60.0, arrived = true, readiness = Readiness.READY),
            MemberState(minji, VehicleSnapshot(true, Gear.PARK, 0.0, 47, 350.0), progressKm = 60.0, arrived = true, readiness = Readiness.READY),
            MemberState(sua, VehicleSnapshot(true, Gear.PARK, 0.0, 92, 485.0), progressKm = 60.0, arrived = true, readiness = Readiness.READY),
            MemberState(taehyun, VehicleSnapshot(true, Gear.PARK, 0.0, 55, 238.0), progressKm = 60.0, arrived = true, readiness = Readiness.READY),
            MemberState(haneul, VehicleSnapshot(), progressKm = 60.0, arrived = true, readiness = Readiness.READY),
            MemberState(jiwon, VehicleSnapshot(true, Gear.DRIVE, 70.0, 40, 322.0), progressKm = 57.5, arrived = false, etaMinutes = 3, readiness = Readiness.READY),
        )
        val regroup = RegroupInfo(
            stopLabel = "집결 1",
            stopName = deokpyeong.name,
            restMinutes = 20,
            departClock = clock(8, 35),
            refuelMemberId = sua.id,
            refuelFrom = 7,
            refuelTo = 92,
            lateJoinMemberId = taehyun.id,
            lateJoinMinutes = 4,
            pendingMemberId = jiwon.id,
            pendingEtaMinutes = 3,
        )
        return base(STEP_REGROUP, "08:15 덕평 집결", clock(8, 15), Phase.AT_MEETING_POINT, members, nextStopIndex = 2, continuousDrivingMin = 0)
            .copy(regroup = regroup)
    }

    private fun sleepy(): DriveUiState {
        val members = listOf(
            MemberState(me, VehicleSnapshot(true, Gear.DRIVE, 90.0, 56, 410.0), progressKm = 106.0, readiness = Readiness.READY),
            MemberState(minji, VehicleSnapshot(true, Gear.DRIVE, 88.0, 41, 305.0), progressKm = 105.2, readiness = Readiness.READY),
            MemberState(sua, VehicleSnapshot(true, Gear.DRIVE, 89.0, 86, 452.0), progressKm = 105.6, readiness = Readiness.READY),
            MemberState(taehyun, VehicleSnapshot(true, Gear.DRIVE, 87.0, 47, 205.0), progressKm = 104.8, readiness = Readiness.READY),
            MemberState(jiwon, VehicleSnapshot(true, Gear.DRIVE, 91.0, 34, 290.0), progressKm = 104.1, readiness = Readiness.READY),
            MemberState(haneul, VehicleSnapshot(speedKmh = 86.0), progressKm = 103.6, readiness = Readiness.READY),
        )
        val feed = listOf(
            FeedItem(sua.id, "20분 전", "자동 확인", "덕평에서 주유 완료 · 92%", readAloud = false),
        )
        return base(STEP_SLEEPY, "08:55 상황 공유", clock(8, 55), Phase.DRIVING, members, nextStopIndex = 2, continuousDrivingMin = 20)
            .copy(feed = feed)
    }
}
