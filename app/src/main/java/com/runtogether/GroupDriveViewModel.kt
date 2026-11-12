package com.runtogether

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runtogether.data.AndroidTtsAnnouncer
import com.runtogether.data.DemoNavigationGateway
import com.runtogether.data.NavigationGateway
import com.runtogether.data.VehicleDataSource
import com.runtogether.data.VehicleDataSources
import com.runtogether.data.VoiceAnnouncer
import com.runtogether.demo.DemoScenario
import com.runtogether.domain.Announcements
import com.runtogether.domain.DrivingPolicy
import com.runtogether.domain.Josa
import com.runtogether.domain.MeetingPointEngine
import com.runtogether.domain.RecommendRequest
import com.runtogether.domain.Recommendation
import com.runtogether.domain.RestVoteEngine
import com.runtogether.domain.RestVoteState
import com.runtogether.domain.StatusSignal
import com.runtogether.domain.VehicleSnapshot
import com.runtogether.domain.Vote
import com.runtogether.domain.VoteOutcome
import com.runtogether.state.DriveUiState
import com.runtogether.state.FeedItem
import com.runtogether.state.IncomingAlert
import com.runtogether.state.Phase
import com.runtogether.state.PlannedStop
import com.runtogether.state.VoteResult
import com.runtogether.state.formatClock
import com.runtogether.state.withMyVehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 모임 드라이브 상태를 들고 있는 곳.
 * - 시연 모드: 기획서 시나리오 A의 단계를 넘기며 보여 준다 (다른 회원의 응답은 시뮬레이션)
 * - 차량(AAOS)에서는 내 차의 시동·기어·속도·연료 데이터를 받아 화면을 자동으로 넘긴다
 */
class GroupDriveViewModel(app: Application) : AndroidViewModel(app) {

    private val voice: VoiceAnnouncer = AndroidTtsAnnouncer(app)
    private val navigation: NavigationGateway = DemoNavigationGateway()
    private val vehicleSource: VehicleDataSource? = VehicleDataSources.create(app)

    private val _state = MutableStateFlow(DemoScenario.step(DemoScenario.STEP_CHECKIN))
    val state: StateFlow<DriveUiState> = _state.asStateFlow()

    /** 음성 안내와 같은 문장을 화면 자막으로 */
    val caption: StateFlow<String?> = voice.caption

    private var liveVehicle: VehicleSnapshot? = null
    private var lastDriving: Boolean? = null
    private var vehicleJob: Job? = null
    private var alertJob: Job? = null
    private val scenarioJobs = mutableListOf<Job>()

    init {
        navigation.setCourse(DemoScenario.course.drop(1).map { it.stop })
    }

    // ---------------- 시연 단계 ----------------

    fun nextStep() = enterStep(_state.value.demoStep + 1)

    fun previousStep() = enterStep(_state.value.demoStep - 1)

    private fun enterStep(step: Int) {
        val target = step.coerceIn(0, DemoScenario.LAST_STEP)
        cancelScenarioJobs()
        _state.value = DemoScenario.step(target).withMyVehicle(liveVehicle)
        if (target == DemoScenario.STEP_SLEEPY) {
            // 08:55 횡성 가는 길, 민지가 '졸려요'를 누른다
            launchScenario(4_000L) { receiveSignal(DemoScenario.minji.id, StatusSignal.SLEEPY) }
        }
    }

    private fun launchScenario(delayMs: Long, block: () -> Unit) {
        scenarioJobs += viewModelScope.launch {
            delay(delayMs)
            block()
        }
    }

    private fun cancelScenarioJobs() {
        scenarioJobs.forEach { it.cancel() }
        scenarioJobs.clear()
        alertJob?.cancel()
    }

    // ---------------- 출발 안내 ----------------

    fun sendDepartureNotice() {
        val s = _state.value
        val next = s.nextStop?.stop?.name ?: return
        voice.speak(Announcements.departure(next))
        enterStep(s.demoStep + 1)
    }

    // ---------------- 내가 누르는 상황 공유 ----------------

    fun sendSignal(signal: StatusSignal) {
        val s = _state.value
        if (s.vote != null && s.voteResult == null) {
            showToast("이미 쉬어가기 투표가 진행 중이에요")
            return
        }
        voice.speak(Announcements.sentByMe(signal))
        _state.update { it.copy(feed = listOf(FeedItem(s.meId, "방금", "상황 공유", signal.label)) + it.feed) }
        when (signal) {
            StatusSignal.SLEEPY, StatusSignal.REST, StatusSignal.TOILET, StatusSignal.ENERGY -> startVote(s.meId, signal)
            StatusSignal.SLOW, StatusSignal.HELP -> Unit
        }
    }

    // ---------------- 다른 차에서 온 상황 공유 ----------------

    private fun receiveSignal(fromId: String, signal: StatusSignal) {
        val s = _state.value
        val from = s.member(fromId) ?: return
        val rec = recommend(s, signal)
        val spoken = Announcements.forSignal(signal, from.name, stopName = rec?.stop?.name, restMinutes = DemoScenario.REST_MINUTES)
        _state.update {
            it.copy(
                alert = IncomingAlert(fromId, signal, rec, DemoScenario.REST_MINUTES, spoken),
                feed = listOf(FeedItem(fromId, "방금", "상황 공유", signal.label)) + it.feed,
            )
        }
        voice.speak(spoken)
        if (rec != null) startVote(fromId, signal, rec)
        // 운전 중 알림 카드는 10초 뒤 자동으로 닫힌다 (투표는 오른쪽 패널에서 계속)
        alertJob?.cancel()
        alertJob = viewModelScope.launch {
            delay(DrivingPolicy.ALERT_AUTO_DISMISS_MS)
            _state.update { it.copy(alert = null) }
        }
    }

    private fun recommend(s: DriveUiState, signal: StatusSignal): Recommendation? = MeetingPointEngine.best(
        RecommendRequest(
            members = s.members,
            candidates = s.candidates,
            continuousDrivingMinutes = s.continuousDrivingMin,
            sleepy = signal == StatusSignal.SLEEPY,
            needsEnergy = signal == StatusSignal.ENERGY,
        ),
    )

    // ---------------- 쉬어가기 투표 ----------------

    private fun startVote(proposerId: String, signal: StatusSignal, recommendation: Recommendation? = null) {
        val s = _state.value
        val rec = recommendation ?: recommend(s, signal)
        if (rec == null) {
            showToast("가까운 쉴 곳을 찾지 못했어요")
            return
        }
        val vote = RestVoteEngine.start(
            proposerId = proposerId,
            stop = rec.stop,
            restMinutes = DemoScenario.REST_MINUTES,
            memberIds = s.members.map { it.member.id },
            nowMs = System.currentTimeMillis(),
        )
        _state.update { it.copy(vote = vote, voteSignal = signal, voteResult = null) }

        // 시연: 다른 회원들의 응답 (실서비스에서는 동기화 서버로 들어온다)
        var delayMs = 1_200L
        for (id in vote.memberIds) {
            if (id == proposerId || id == s.meId) continue
            val answer = DemoScenario.simulatedVote(id) ?: continue
            launchScenario(delayMs) { castVote(id, answer) }
            delayMs += 700L
        }
        // 1분 뒤 마감
        launchScenario(RestVoteEngine.VOTE_WINDOW_MS + 100L) { _state.value.vote?.let { resolve(it) } }
    }

    fun answerVote(yes: Boolean) {
        alertJob?.cancel()
        _state.update { it.copy(alert = null) }
        castVote(_state.value.meId, if (yes) Vote.YES else Vote.NO)
    }

    fun dismissAlert() {
        alertJob?.cancel()
        _state.update { it.copy(alert = null) }
    }

    private fun castVote(memberId: String, vote: Vote) {
        val current = _state.value.vote ?: return
        if (_state.value.voteResult != null) return
        val updated = RestVoteEngine.cast(current, memberId, vote, System.currentTimeMillis())
        _state.update { it.copy(vote = updated) }
        resolve(updated)
    }

    private fun resolve(vote: RestVoteState) {
        if (_state.value.voteResult != null) return
        when (RestVoteEngine.outcome(vote, System.currentTimeMillis())) {
            VoteOutcome.PENDING -> Unit
            VoteOutcome.ACCEPTED -> accept(vote)
            VoteOutcome.REJECTED -> reject()
        }
    }

    private fun accept(vote: RestVoteState) {
        val s = _state.value
        val stop = vote.stop
        navigation.addWaypoint(stop)
        val arrive = s.clockMin + s.minutesTo(stop)
        val depart = arrive + vote.restMinutes
        val following = s.nextStop
        val detail = buildString {
            append("${formatClock(arrive)} 도착 · ${vote.restMinutes}분 쉬고 출발")
            if (following != null && following.stop.id != stop.id && following.stop.kmFromStart > stop.kmFromStart) {
                val after = s.minutesTo(following.stop, fromKm = stop.kmFromStart)
                append(" · ${following.stop.shortName} 도착 ${formatClock(depart + after)}")
            }
        }
        // 코스에 없던 곳이면 다음 지점 앞에 끼워 넣고, 다음 집결지로 삼는다
        val inCourse = s.course.any { it.stop.id == stop.id }
        val newCourse = if (inCourse) s.course else s.course.toMutableList().apply {
            add(s.nextStopIndex.coerceIn(0, size), PlannedStop(stop, arrive, vote.restMinutes, label = "휴식"))
        }
        val newNext = newCourse.indexOfFirst { it.stop.id == stop.id }.takeIf { it >= 0 } ?: s.nextStopIndex
        _state.update {
            it.copy(
                course = newCourse,
                nextStopIndex = newNext,
                alert = null,
                voteResult = VoteResult(
                    accepted = true,
                    title = "${stop.name}${Josa.eulReul(stop.name)} 모두의 내비에 추가했어요",
                    detail = detail,
                ),
            )
        }
        voice.speak(Announcements.voteAccepted(stop.name, vote.restMinutes))
    }

    private fun reject() {
        val next = _state.value.nextStop?.stop?.name ?: "다음 집결지"
        _state.update {
            it.copy(alert = null, voteResult = VoteResult(false, "이번에는 쉬지 않고 가요", "${next}에서 만나요"))
        }
        voice.speak(Announcements.voteRejected(next))
    }

    // ---------------- 정차 중 기능 · 알림 ----------------

    fun parkedAction(label: String) = showToast("$label — 정차 중에만 열리는 기능이에요 (시연)")

    fun clearToast() {
        _state.update { it.copy(toast = null) }
    }

    private fun showToast(text: String) {
        _state.update { it.copy(toast = text) }
        viewModelScope.launch {
            delay(3_500L)
            _state.update { if (it.toast == text) it.copy(toast = null) else it }
        }
    }

    // ---------------- 차량 데이터 ----------------

    /** 차량 권한을 받은 뒤 호출. 차량(AAOS)이 아니면 아무것도 하지 않는다. */
    fun onVehiclePermissionResult() {
        val source = vehicleSource ?: return
        if (vehicleJob != null) return
        vehicleJob = viewModelScope.launch {
            source.snapshots.collect { snapshot ->
                liveVehicle = snapshot
                _state.update { it.withMyVehicle(snapshot) }
                followVehicle(snapshot)
            }
        }
    }

    /** 기어·속도가 바뀌는 순간에만 화면을 넘긴다 (설정 없는 자동화). */
    private fun followVehicle(v: VehicleSnapshot) {
        val driving = DrivingPolicy.isDriving(v)
        val previous = lastDriving
        lastDriving = driving
        if (previous == null || previous == driving) return
        when (_state.value.phase) {
            Phase.PRE_DEPARTURE -> if (driving) enterStep(DemoScenario.STEP_DRIVING)
            Phase.AT_MEETING_POINT -> if (driving) enterStep(DemoScenario.STEP_SLEEPY)
            Phase.DRIVING -> if (!driving) {
                // 5초 넘게 서 있으면 집결지 도착으로 본다
                launchScenario(5_000L) { if (lastDriving == false) enterStep(DemoScenario.STEP_REGROUP) }
            }
        }
    }

    override fun onCleared() {
        voice.shutdown()
        super.onCleared()
    }
}
