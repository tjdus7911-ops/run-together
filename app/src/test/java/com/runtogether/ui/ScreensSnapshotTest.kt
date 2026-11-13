package com.runtogether.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.runtogether.state.FeedItem
import com.runtogether.state.IncomingAlert
import com.runtogether.state.VoiceShare
import com.runtogether.state.acceptRest
import com.runtogether.ui.screens.MyMessagePanel
import com.runtogether.ui.theme.RtColors
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

    private fun shot(name: String, state: DriveUiState, caption: String? = null, speakerId: String? = null) {
        paparazzi.snapshot(name) {
            RunTogetherTheme {
                AppScreen(state, caption, AppActions(), speakerId)
            }
        }
    }

    @Test
    fun checkin() = shot("1_checkin", DemoScenario.step(DemoScenario.STEP_CHECKIN))

    @Test
    fun driving() = shot("2_driving", DemoScenario.step(DemoScenario.STEP_DRIVING))

    @Test
    fun myMessages() {
        val base = DemoScenario.step(DemoScenario.STEP_DRIVING)
        val first = base.myMessages.first()
        shot(
            "2b_my_messages",
            base.copy(messagePanelOpen = true, sentMessageId = first.id, coolingMessageIds = setOf(first.id)),
            caption = Announcements.customSent(first.text),
        )
    }

    /** 지도 자리가 좁거나 낮을 때 내 메시지 패널: 3열 간단 목록 · 2열 들어가는 만큼만 · 2열 큰 버튼 */
    @Test
    fun myMessagesCompact() {
        val base = DemoScenario.step(DemoScenario.STEP_DRIVING)
        val first = base.myMessages.first()
        val state = base.copy(messagePanelOpen = true, sentMessageId = first.id, coolingMessageIds = setOf(first.id))
        paparazzi.snapshot("2e_my_messages_compact") {
            RunTogetherTheme {
                Column(
                    Modifier.fillMaxSize().background(RtColors.Bg).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Box(Modifier.size(1100.dp, 300.dp)) {
                        MyMessagePanel(state, AppActions(), Modifier.align(Alignment.BottomCenter))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Box(Modifier.size(640.dp, 330.dp)) {
                            MyMessagePanel(state, AppActions(), Modifier.align(Alignment.BottomCenter))
                        }
                        Box(Modifier.size(640.dp, 500.dp)) {
                            MyMessagePanel(state, AppActions(), Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun messageReceived() {
        val base = DemoScenario.step(DemoScenario.STEP_DRIVING)
        val text = DemoScenario.SUA_MESSAGE
        shot(
            "2c_message_received",
            base.copy(feed = listOf(FeedItem(DemoScenario.sua.id, "방금", "내 메시지", text)) + base.feed),
            caption = Announcements.customMessage(DemoScenario.sua.name, text),
            speakerId = DemoScenario.sua.id,
        )
    }

    @Test
    fun voiceShare() {
        val base = DemoScenario.step(DemoScenario.STEP_DRIVING)
        shot("2d_voice_share", base.copy(voiceShare = VoiceShare(VoiceShare.Stage.CONFIRM, DemoScenario.DEMO_SPOKEN)))
    }

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

        // 내가 '좋아요' → 과반 → 앱과 같은 상태 전이로 문막을 코스에 넣는다
        val accepted = alert.acceptRest(RestVoteEngine.cast(vote, DemoScenario.me.id, Vote.YES, 2L))
        shot("5_share_result", accepted)
    }
}
