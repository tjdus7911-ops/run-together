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
import com.runtogether.data.pleos.PleosNaviGateway
import com.runtogether.data.pleos.PleosTtsAnnouncer
import com.runtogether.data.pleos.PleosVoiceAnswer
import com.runtogether.demo.DemoScenario
import com.runtogether.domain.Announcements
import com.runtogether.domain.DrivingPolicy
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
import com.runtogether.state.acceptRest
import com.runtogether.state.formatDuration
import com.runtogether.state.rejectRest
import com.runtogether.state.withMyVehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 모임 드라이브 상태를 들고 있는 곳.
 * - 시연 모드: 기획서 시나리오 A의 단계를 넘기며 보여 준다 (다른 회원의 응답은 시뮬레이션)
 * - 차량(AAOS)에서는 내 차의 시동·기어·속도·연료 데이터를 받아 화면을 자동으로 넘긴다
 */
class GroupDriveViewModel(app: Application) : AndroidViewModel(app) {

    private val automotive = VehicleDataSources.isAutomotive(app)

    /** 차량에서는 Pleos TextToSpeech SDK, 실패하거나 일반 기기면 안드로이드 기본 TTS */
    private val voice: VoiceAnnouncer =
        if (automotive) PleosTtsAnnouncer(app, fallback = AndroidTtsAnnouncer(app)) else AndroidTtsAnnouncer(app)

    /** 차량에서는 Pleos NaviHelper SDK 로 경로·경유지를 넣는다 */
    private val demoNavigation = DemoNavigationGateway()
    private val pleosNavigation: PleosNaviGateway? = if (automotive) PleosNaviGateway(app, log = demoNavigation) else null
    private val navigation: NavigationGateway = pleosNavigation ?: demoNavigation

    /** 차량에서는 Pleos SpeechToText SDK 로 “좋아요 / 괜찮아요”를 말로 답한다 */
    private val voiceAnswer: PleosVoiceAnswer? = if (automotive) PleosVoiceAnswer(app) else null

    private val vehicleSource: VehicleDataSource? = VehicleDataSources.create(app)

    private val _state = MutableStateFlow(DemoScenario.step(DemoScenario.STEP_CHECKIN))
    val state: StateFlow<DriveUiState> = _state.asStateFlow()

    /** 음성 안내와 같은 문장을 화면 자막으로 */
    val caption: StateFlow<String?> = voice.caption

    private var liveVehicle: VehicleSnapshot? = null
    private var lastDriving: Boolean? = null
    private var restSuggested = false
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
        if (rec != null) {
            startVote(fromId, signal, rec)
            listenForVoiceAnswer()
        }
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

    /** 안내 음성이 끝나면 말로 한 답을 듣는다 (자기 안내 음성을 듣지 않도록). */
    private fun listenForVoiceAnswer() {
        val stt = voiceAnswer ?: return
        launchScenario(600L) {
            viewModelScope.launch {
                withTimeoutOrNull(8_000L) { caption.first { it == null } }
                if (_state.value.vote?.voteOf(_state.value.meId) == null && _state.value.voteResult == null) {
                    stt.listen { yes -> answerVote(yes) }
                }
            }
        }
    }

    fun answerVote(yes: Boolean) {
        voiceAnswer?.cancel()
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
        navigation.addWaypoint(vote.stop)
        alertJob?.cancel()
        _state.update { it.acceptRest(vote) }
        voice.speak(Announcements.voteAccepted(vote.stop.name, vote.restMinutes))
    }

    private fun reject() {
        alertJob?.cancel()
        _state.update { it.rejectRest() }
        voice.speak(Announcements.voteRejected(_state.value.nextStop?.stop?.name ?: "다음 집결지"))
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
                suggestRestIfLong(snapshot)
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

    /** 연속 운전 2시간이 되기 전에 앱이 먼저 '쉬었다 갈까요?'를 묻는다 (차량의 주행 시간 데이터). */
    private fun suggestRestIfLong(v: VehicleSnapshot) {
        val minutes = v.tripDrivingMinutes ?: return
        if (minutes < 10) restSuggested = false // 쉬고 다시 출발
        val s = _state.value
        if (restSuggested || s.phase != Phase.DRIVING || (s.vote != null && s.voteResult == null)) return
        if (!DrivingPolicy.shouldSuggestRest(minutes)) return
        restSuggested = true
        voice.speak("연속 운전이 ${formatDuration(minutes)}째예요. 가까운 곳에서 쉬었다 갈까요?")
        startVote(s.meId, StatusSignal.REST)
    }

    override fun onCleared() {
        voice.shutdown()
        voiceAnswer?.release()
        pleosNavigation?.release()
        super.onCleared()
    }
}
