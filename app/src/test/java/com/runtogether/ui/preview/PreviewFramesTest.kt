package com.runtogether.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.runtogether.demo.DemoScenario
import com.runtogether.domain.Announcements
import com.runtogether.domain.MeetingPointEngine
import com.runtogether.domain.RecommendRequest
import com.runtogether.domain.Recommendation
import com.runtogether.domain.RestVoteEngine
import com.runtogether.domain.StatusSignal
import com.runtogether.domain.Vote
import com.runtogether.domain.VoteOutcome
import com.runtogether.state.DriveUiState
import com.runtogether.state.FeedItem
import com.runtogether.state.IncomingAlert
import com.runtogether.state.acceptRest
import com.runtogether.state.rejectRest
import com.runtogether.ui.AppActions
import com.runtogether.ui.AppScreen
import com.runtogether.ui.theme.RunTogetherTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * 눌러 보는 미리보기 페이지에 쓰는 화면.
 * 앱 시연 모드(GroupDriveViewModel)와 같은 순서로 상태를 만들어 1600×900 원본 크기로 그린다.
 * Paparazzi 는 1000px 넘는 이미지를 줄여 저장하므로 한 화면을 왼쪽·오른쪽 800px 두 장으로 나눠 그리고 나중에 이어 붙인다.
 *
 * 실행: PREVIEW_FRAMES=1 ./gradlew :app:recordPaparazziDebug --tests "com.runtogether.ui.preview.PreviewFramesTest"
 */
class PreviewFramesTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_C.copy(
            screenWidth = TILE_W,
            screenHeight = H,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.PORTRAIT,
            softButtons = false,
        ),
        showSystemUi = false,
        maxPercentDifference = 1.0,
    )

    private val s = DemoScenario

    private fun frame(name: String, state: DriveUiState, caption: String? = null) {
        for (tile in 0 until W / TILE_W) {
            paparazzi.snapshot("${name}__$tile") {
                Box(
                    Modifier.fillMaxSize().layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(W, H))
                        layout(TILE_W, H) { placeable.place(-TILE_W * tile, 0) }
                    },
                ) {
                    RunTogetherTheme { AppScreen(state, caption, AppActions()) }
                }
            }
        }
    }

    private fun memberIds(state: DriveUiState) = state.members.map { it.member.id }

    /** GroupDriveViewModel.recommend 와 같은 추천 */
    private fun recommend(state: DriveUiState, signal: StatusSignal): Recommendation = MeetingPointEngine.best(
        RecommendRequest(
            members = state.members,
            candidates = state.candidates,
            continuousDrivingMinutes = state.continuousDrivingMin,
            sleepy = signal == StatusSignal.SLEEPY,
            needsEnergy = signal == StatusSignal.ENERGY,
        ),
    )!!

    /** GroupDriveViewModel.startVote 의 시연 응답 순서 (제안자·나 제외, 1.2초부터 0.7초 간격) */
    private fun simulatedAnswers(state: DriveUiState, proposerId: String): List<Pair<String, Vote>> =
        memberIds(state)
            .filter { it != proposerId && it != state.meId }
            .mapNotNull { id -> s.simulatedVote(id)?.let { id to it } }

    @Test
    fun frames() {
        assumeTrue("PREVIEW_FRAMES=1 일 때만 그린다", System.getenv("PREVIEW_FRAMES") == "1")

        // ① 07:25 출발 전 → '출발 안내 보내기'
        val checkin = s.step(s.STEP_CHECKIN)
        frame("checkin", checkin)

        // ② 08:00 주행 중 (출발 안내 자막 → 자막 없음)
        val drive = s.step(s.STEP_DRIVING)
        frame("drive_depart", drive, Announcements.departure(checkin.nextStop!!.stop.name))
        frame("drive", drive)

        // ② 내가 누르는 상황 공유 4가지 → 쉬어가기 투표 → 과반 통과
        val mySignals = listOf(
            "sleepy" to StatusSignal.SLEEPY,
            "rest" to StatusSignal.REST,
            "toilet" to StatusSignal.TOILET,
            "energy" to StatusSignal.ENERGY,
        )
        for ((key, signal) in mySignals) {
            val rec = recommend(drive, signal)
            var vote = RestVoteEngine.start(drive.meId, rec.stop, s.REST_MINUTES, memberIds(drive), 0L)
            val sent = drive.copy(
                feed = listOf(FeedItem(drive.meId, "방금", "상황 공유", signal.label)) + drive.feed,
                vote = vote,
                voteSignal = signal,
                voteResult = null,
            )
            frame("my_${key}_sent", sent, Announcements.sentByMe(signal))
            for ((id, answer) in simulatedAnswers(drive, drive.meId)) vote = RestVoteEngine.cast(vote, id, answer, 1L)
            check(RestVoteEngine.outcome(vote, 2L) == VoteOutcome.ACCEPTED)
            val accepted = sent.acceptRest(vote)
            frame("my_${key}_accept_spoken", accepted, Announcements.voteAccepted(rec.stop.name, vote.restMinutes))
            frame("my_${key}_accept", accepted)
        }

        // ③ 08:15 덕평 집결 ('코스 바꾸기'는 정차 중에만, 시연에서는 안내)
        val regroup = s.step(s.STEP_REGROUP)
        frame("regroup", regroup)
        frame("regroup_toast", regroup.copy(toast = "코스 바꾸기 — 정차 중에만 열리는 기능이에요 (시연)"))

        // ④ 08:55 횡성 가는 길 (출발 안내 자막 → 4초 뒤 민지 '졸려요')
        val sleepy = s.step(s.STEP_SLEEPY)
        frame("sleepy_depart", sleepy, Announcements.departure(regroup.nextStop!!.stop.name))
        frame("sleepy", sleepy)

        val minji = s.minji
        val rec = recommend(sleepy, StatusSignal.SLEEPY)
        val spoken = Announcements.forSignal(StatusSignal.SLEEPY, minji.name, stopName = rec.stop.name, restMinutes = s.REST_MINUTES)
        var vote = RestVoteEngine.start(minji.id, rec.stop, s.REST_MINUTES, memberIds(sleepy), 0L)
        var alert = sleepy.copy(
            alert = IncomingAlert(minji.id, StatusSignal.SLEEPY, rec, s.REST_MINUTES, spoken),
            feed = listOf(FeedItem(minji.id, "방금", "상황 공유", StatusSignal.SLEEPY.label)) + sleepy.feed,
            vote = vote,
            voteSignal = StatusSignal.SLEEPY,
        )
        frame("alert_1", alert, spoken)
        simulatedAnswers(sleepy, minji.id).forEachIndexed { i, (id, answer) ->
            vote = RestVoteEngine.cast(vote, id, answer, 1L)
            alert = alert.copy(vote = vote)
            frame("alert_${i + 2}", alert, spoken)
        }
        val inVote = "이미 쉬어가기 투표가 진행 중이에요"
        frame("alert_4_toast", alert.copy(toast = inVote), spoken)

        // 10초 안에 답하지 않으면 알림 카드만 닫히고 투표는 계속 (말로 답할 수 있음)
        val closed = alert.copy(alert = null)
        frame("alert_closed", closed)
        frame("alert_closed_toast", closed.copy(toast = inVote))

        // 좋아요 → 과반(4/6) → 문막휴게소를 모두의 경로에
        val yes = RestVoteEngine.cast(vote, sleepy.meId, Vote.YES, 2L)
        check(RestVoteEngine.outcome(yes, 3L) == VoteOutcome.ACCEPTED)
        val accepted = alert.acceptRest(yes)
        frame("accept_spoken", accepted, Announcements.voteAccepted(rec.stop.name, yes.restMinutes))
        frame("accept", accepted)

        // 괜찮아요 → 1분 마감까지 과반이 안 되면 원래 집결지(횡성)에서 만난다
        val no = RestVoteEngine.cast(vote, sleepy.meId, Vote.NO, 2L)
        check(RestVoteEngine.outcome(no, 3L) == VoteOutcome.PENDING)
        check(RestVoteEngine.outcome(no, RestVoteEngine.VOTE_WINDOW_MS) == VoteOutcome.REJECTED)
        val declined = alert.copy(alert = null, vote = no)
        frame("decline_wait", declined)
        frame("decline_wait_toast", declined.copy(toast = inVote))
        val rejected = declined.rejectRest()
        val rejectSpoken = Announcements.voteRejected(rejected.nextStop!!.stop.name)
        frame("reject_spoken", rejected, rejectSpoken)
        frame("reject", rejected)

        // 끝까지 답하지 않았을 때도 1분 뒤 마감
        check(RestVoteEngine.outcome(vote, RestVoteEngine.VOTE_WINDOW_MS) == VoteOutcome.REJECTED)
        val unanswered = closed.rejectRest()
        frame("noanswer_reject_spoken", unanswered, rejectSpoken)
        frame("noanswer_reject", unanswered)
    }

    private companion object {
        const val W = 1600
        const val H = 900
        const val TILE_W = 800
    }
}
