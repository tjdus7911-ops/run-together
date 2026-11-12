package com.runtogether.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.runtogether.demo.DemoScenario
import com.runtogether.domain.Announcements
import com.runtogether.domain.MeetingPointEngine
import com.runtogether.domain.RecommendRequest
import com.runtogether.domain.RestVoteEngine
import com.runtogether.domain.StatusSignal
import com.runtogether.domain.Vote
import com.runtogether.state.DriveUiState
import com.runtogether.state.IncomingAlert
import com.runtogether.state.VoteResult
import com.runtogether.ui.theme.RunTogetherTheme
import org.junit.Rule
import org.junit.Test

/** 시안(1600×900)과 같은 크기로 화면을 그려 확인한다. `./gradlew recordPaparazziDebug` */
class ScreensSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_C.copy(
            screenWidth = 1600,
            screenHeight = 900,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.LANDSCAPE,
            softButtons = false,
        ),
        showSystemUi = false,
        maxPercentDifference = 1.0,
    )

    private fun shot(name: String, state: DriveUiState, caption: String? = null) {
        paparazzi.snapshot(name) {
            RunTogetherTheme {
                AppScreen(state, caption, AppActions())
            }
        }
    }

    @Test
    fun checkin() = shot("1_checkin", DemoScenario.step(DemoScenario.STEP_CHECKIN))

    @Test
    fun driving() = shot("2_driving", DemoScenario.step(DemoScenario.STEP_DRIVING))

    @Test
    fun regroup() = shot("3_regroup", DemoScenario.step(DemoScenario.STEP_REGROUP))

    @Test
    fun sleepyAlertAndVote() {
        val base = DemoScenario.step(DemoScenario.STEP_SLEEPY)
        val rec = MeetingPointEngine.best(
            RecommendRequest(base.members, base.candidates, base.continuousDrivingMin, sleepy = true),
        )!!
        var vote = RestVoteEngine.start(DemoScenario.minji.id, rec.stop, DemoScenario.REST_MINUTES, base.members.map { it.member.id }, 0L)
        vote = RestVoteEngine.cast(vote, DemoScenario.sua.id, Vote.YES, 1L)
        vote = RestVoteEngine.cast(vote, DemoScenario.jiwon.id, Vote.YES, 1L)
        vote = RestVoteEngine.cast(vote, DemoScenario.taehyun.id, Vote.NO, 1L)
        val spoken = Announcements.forSignal(StatusSignal.SLEEPY, DemoScenario.minji.name, stopName = rec.stop.name)
        val alert = base.copy(
            alert = IncomingAlert(DemoScenario.minji.id, StatusSignal.SLEEPY, rec, DemoScenario.REST_MINUTES, spoken),
            vote = vote,
            voteSignal = StatusSignal.SLEEPY,
        )
        shot("4_share_alert", alert, caption = spoken)

        val accepted = alert.copy(
            alert = null,
            vote = RestVoteEngine.cast(vote, DemoScenario.me.id, Vote.YES, 2L),
            voteResult = VoteResult(true, "문막휴게소를 모두의 내비에 추가했어요", "09:00 도착 · 15분 쉬고 출발 · 횡성 도착 09:40"),
        )
        shot("5_share_result", accepted)
    }
}
