package com.runtogether

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runtogether.data.AndroidNaviVoiceMonitor
import com.runtogether.data.AndroidTtsAnnouncer
import com.runtogether.data.DemoGroupChannel
import com.runtogether.data.DemoMyMessageRepository
import com.runtogether.data.DemoNavigationGateway
import com.runtogether.data.GroupChannel
import com.runtogether.data.MyMessageRepository
import com.runtogether.data.NavigationGateway
import com.runtogether.data.NaviVoiceMonitor
import com.runtogether.data.VehicleDataSource
import com.runtogether.data.VehicleDataSources
import com.runtogether.data.VoiceAnnouncer
import com.runtogether.data.VoiceQueue
import com.runtogether.data.pleos.PleosNaviGateway
import com.runtogether.data.pleos.PleosTtsAnnouncer
import com.runtogether.data.pleos.PleosVoiceAnswer
import com.runtogether.demo.DemoScenario
import com.runtogether.domain.Announcements
import com.runtogether.domain.DrivingPolicy
import com.runtogether.domain.GroupMessage
import com.runtogether.domain.MeetingPointEngine
import com.runtogether.domain.MessageIcon
import com.runtogether.domain.MessageProblem
import com.runtogether.domain.NaviGuidance
import com.runtogether.domain.QuickMessagePolicy
import com.runtogether.domain.RecommendRequest
import com.runtogether.domain.Recommendation
import com.runtogether.domain.RepeatFilter
import com.runtogether.domain.RestVoteEngine
import com.runtogether.domain.RestVoteState
import com.runtogether.domain.SendGuard
import com.runtogether.domain.StatusSignal
import com.runtogether.domain.VehicleSnapshot
import com.runtogether.domain.VoicePriority
import com.runtogether.domain.Vote
import com.runtogether.domain.VoteOutcome
import com.runtogether.state.DriveUiState
import com.runtogether.state.FeedItem
import com.runtogether.state.IncomingAlert
import com.runtogether.state.Phase
import com.runtogether.state.VoiceShare
import com.runtogether.state.acceptRest
import com.runtogether.state.formatDuration
import com.runtogether.state.rejectRest
import com.runtogether.state.withMyVehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

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

    /** 내비 길 안내 음성이 나오는 동안은 우리 안내를 기다리게 한다 */
    private val naviVoice: NaviVoiceMonitor = AndroidNaviVoiceMonitor(app)

    /** 음성 안내 순서: 한 번에 하나, 안전 안내 먼저, 내비 길 안내와 겹치지 않게 */
    private val voiceQueue = VoiceQueue(voice, naviVoice.guidancePlaying, viewModelScope)

    /** 차량에서는 Pleos NaviHelper SDK 로 경로·경유지를 넣고, 길 안내 값을 받는다 */
    private val demoNavigation = DemoNavigationGateway()
    private val pleosNavigation: PleosNaviGateway? = if (automotive) PleosNaviGateway(app, log = demoNavigation) else null
    private val navigation: NavigationGateway = pleosNavigation ?: demoNavigation

    /** 차량에서는 Pleos SpeechToText SDK 로 “좋아요 / 괜찮아요”를 말로 답하고, 음성으로 상황을 공유한다 */
    private val voiceAnswer: PleosVoiceAnswer? = if (automotive) PleosVoiceAnswer(app) else null

    private val vehicleSource: VehicleDataSource? = VehicleDataSources.create(app)

    /** 모임 메시지 통로 (기본 상황 공유 · 내 메시지 · 음성 메시지가 같은 통로). 동기화 서버 전까지는 시연용 */
    private val demoChannel = DemoGroupChannel()
    private val channel: GroupChannel = demoChannel

    /** 휴대폰에서 만든 내 메시지 (계정 동기화 자리) */
    private val myMessageRepository: MyMessageRepository = DemoMyMessageRepository(DemoScenario.myMessages)

    private val sendGuard = SendGuard()
    private val repeatFilter = RepeatFilter()

    private val _state = MutableStateFlow(DemoScenario.step(DemoScenario.STEP_CHECKIN))
    val state: StateFlow<DriveUiState> = _state.asStateFlow()

    /** 음성 안내와 같은 문장을 화면 자막으로 */
    val caption: StateFlow<String?> = voiceQueue.caption

    /** 자막에 얼굴을 함께 보여 줄 보낸 사람 (받은 내 메시지 · 음성 메시지) */
    val captionSpeakerId: StateFlow<String?> =
        voiceQueue.current.map { it?.speakerId }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var liveVehicle: VehicleSnapshot? = null
    private var liveGuidance: NaviGuidance? = null
    private var readAloud = true
    private var lastDriving: Boolean? = null
    private var restSuggested = false
    private var vehicleJob: Job? = null
    private var alertJob: Job? = null
    private var messagePanelJob: Job? = null
    private var voiceShareJob: Job? = null
    private val scenarioJobs = mutableListOf<Job>()

    init {
        navigation.setCourse(DemoScenario.course.drop(1).map { it.stop })
        viewModelScope.launch { channel.incoming.collect { receive(it) } }
        viewModelScope.launch {
            myMessageRepository.messages.collect { list -> _state.update { it.copy(myMessages = list) } }
        }
        pleosNavigation?.let { nav ->
            viewModelScope.launch {
                nav.guidance.collect { g ->
                    liveGuidance = g
                    _state.update { it.copy(guidance = g) }
                }
            }
        }
    }

    // ---------------- 시연 단계 ----------------

    fun nextStep() = enterStep(_state.value.demoStep + 1, byHand = true)

    fun previousStep() = enterStep(_state.value.demoStep - 1, byHand = true)

    private fun enterStep(step: Int, byHand: Boolean = false) {
        val target = step.coerceIn(0, DemoScenario.LAST_STEP)
        cancelScenarioJobs()
        // 손으로 단계를 넘기면 앞 단계의 안내는 읽지 않는다
        if (byHand) voiceQueue.clearPending()
        _state.value = DemoScenario.step(target).withMyVehicle(liveVehicle).withSession()
        when (target) {
            // 08:00 주행 중, 수아가 ‘내 메시지’를 보낸다
            DemoScenario.STEP_DRIVING -> launchScenario(7_000L) {
                demoChannel.simulateIncoming(
                    GroupMessage.Custom(newId(), DemoScenario.sua.id, now(), "sua-fuel", DemoScenario.SUA_MESSAGE, MessageIcon.FUEL),
                )
            }
            // 08:55 횡성 가는 길, 민지가 '졸려요'를 누른다
            DemoScenario.STEP_SLEEPY -> launchScenario(4_000L) {
                demoChannel.simulateIncoming(GroupMessage.Status(newId(), DemoScenario.minji.id, now(), StatusSignal.SLEEPY))
            }
        }
    }

    /** 단계가 바뀌어도 이어지는 값: 내 메시지 목록, 읽어 주기 설정, 차량 내비 안내 */
    private fun DriveUiState.withSession(): DriveUiState = copy(
        myMessages = myMessageRepository.messages.value,
        readMessagesAloud = readAloud,
        guidance = liveGuidance,
    )

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
        messagePanelJob?.cancel()
        voiceShareJob?.cancel()
    }

    private fun say(text: String, priority: VoicePriority) = voiceQueue.say(text, priority)

    // ---------------- 출발 안내 ----------------

    fun sendDepartureNotice() {
        val s = _state.value
        val next = s.nextStop?.stop?.name ?: return
        say(Announcements.departure(next), VoicePriority.GROUP)
        enterStep(s.demoStep + 1)
    }

    // ---------------- 내가 누르는 상황 공유 (기본 메시지) ----------------

    fun sendSignal(signal: StatusSignal) {
        val s = _state.value
        if (s.vote != null && s.voteResult == null) {
            showToast("이미 쉬어가기 투표가 진행 중이에요")
            return
        }
        val message = GroupMessage.Status(newId(), s.meId, now(), signal)
        if (!guardSend(message)) return
        channel.send(message)
        say(Announcements.sentByMe(signal), VoicePriority.CONFIRM)
        _state.update {
            it.copy(feed = listOf(FeedItem(s.meId, "방금", "상황 공유", signal.label)) + it.feed, messagePanelOpen = false)
        }
        when (signal) {
            StatusSignal.SLEEPY, StatusSignal.REST, StatusSignal.TOILET, StatusSignal.ENERGY -> startVote(s.meId, signal)
            StatusSignal.SLOW, StatusSignal.HELP -> Unit
        }
    }

    /** 보내기 제한 (두 번 눌림 · 같은 메시지 30초 · 1분에 4번). 막히면 이유를 알리고 false */
    private fun guardSend(message: GroupMessage): Boolean = when (val decision = sendGuard.tryAcquire(message.guardKey, now())) {
        SendGuard.Decision.Allowed -> true
        is SendGuard.Decision.Blocked -> {
            when (decision.reason) {
                SendGuard.Reason.DOUBLE_TAP -> Unit // 두 번 눌림은 조용히 무시
                SendGuard.Reason.SAME_MESSAGE ->
                    showToast("방금 보낸 메시지예요 · ${(decision.waitMs + 999) / 1000}초 뒤에 다시 보낼 수 있어요")
                SendGuard.Reason.TOO_MANY -> showToast("잠시 뒤에 다시 보내 주세요 · 1분에 4번까지 보낼 수 있어요")
            }
            false
        }
    }

    // ---------------- 다른 차에서 온 메시지 ----------------

    private fun receive(message: GroupMessage) {
        when (message) {
            is GroupMessage.Status -> receiveSignal(message.fromId, message.signal)
            is GroupMessage.Custom -> receiveText(message.fromId, message.text, voiceMessage = false)
            is GroupMessage.Voice -> receiveText(message.fromId, message.text, voiceMessage = true)
        }
    }

    private fun receiveSignal(fromId: String, signal: StatusSignal) {
        val s = _state.value
        val from = s.member(fromId) ?: return
        val rec = recommend(s, signal)
        val spoken = Announcements.forSignal(signal, from.name, stopName = rec?.stop?.name, restMinutes = DemoScenario.REST_MINUTES)
        // 상황 공유 알림이 먼저: 내 메시지 패널·음성 공유는 닫는다
        voiceShareJob?.cancel()
        messagePanelJob?.cancel()
        _state.update {
            it.copy(
                alert = IncomingAlert(fromId, signal, rec, DemoScenario.REST_MINUTES, spoken),
                feed = listOf(FeedItem(fromId, "방금", "상황 공유", signal.label)) + it.feed,
                messagePanelOpen = false,
                voiceShare = null,
            )
        }
        say(spoken, if (signal == StatusSignal.SLOW) VoicePriority.GROUP else VoicePriority.SAFETY)
        if (rec != null) {
            startVote(fromId, signal, rec)
            listenForVoiceAnswer(spoken)
        }
        // 운전 중 알림 카드는 10초 뒤 자동으로 닫힌다 (투표는 오른쪽 패널에서 계속)
        alertJob?.cancel()
        alertJob = viewModelScope.launch {
            delay(DrivingPolicy.ALERT_AUTO_DISMISS_MS)
            _state.update { it.copy(alert = null) }
        }
    }

    /** 받은 내 메시지 · 음성 메시지: 소식에 남기고, 읽어 주기가 켜져 있으면 읽는다 (같은 말 반복은 한 번만) */
    private fun receiveText(fromId: String, text: String, voiceMessage: Boolean) {
        val from = _state.value.member(fromId) ?: return
        val kind = if (voiceMessage) "음성 메시지" else "내 메시지"
        _state.update { it.copy(feed = listOf(FeedItem(fromId, "방금", kind, text, readAloud = readAloud)) + it.feed) }
        if (!repeatFilter.shouldAnnounce(fromId, text, now())) return
        val spoken = if (voiceMessage) Announcements.voiceMessage(from.name, text) else Announcements.customMessage(from.name, text)
        voiceQueue.say(spoken, VoicePriority.MESSAGE, speakerId = fromId, speak = readAloud)
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

    /** 알림 음성을 다 읽은 뒤에 말로 한 답을 듣는다 (자기 안내 음성을 듣지 않도록). */
    private fun listenForVoiceAnswer(spoken: String) {
        val stt = voiceAnswer ?: return
        // 시연 단계를 넘기면 함께 취소되도록 시나리오 작업으로 둔다
        scenarioJobs += viewModelScope.launch {
            // 차례가 와서 읽기 시작하고 → 끝날 때까지
            withTimeoutOrNull(30_000L) { voiceQueue.current.first { it?.text == spoken } }
            withTimeoutOrNull(15_000L) { voiceQueue.current.first { it?.text != spoken } }
            val s = _state.value
            if (s.vote != null && s.vote.voteOf(s.meId) == null && s.voteResult == null && s.voiceShare == null) {
                stt.listen { yes -> answerVote(yes) }
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
        say(Announcements.voteAccepted(vote.stop.name, vote.restMinutes), VoicePriority.GROUP)
    }

    private fun reject() {
        alertJob?.cancel()
        _state.update { it.rejectRest() }
        say(Announcements.voteRejected(_state.value.nextStop?.stop?.name ?: "다음 집결지"), VoicePriority.GROUP)
    }

    // ---------------- 내 메시지 (휴대폰에서 만든 문장, 차량에서는 고르기만) ----------------

    fun openMyMessages() {
        if (_state.value.alert != null) return
        _state.update {
            it.copy(messagePanelOpen = true, sentMessageId = null, coolingMessageIds = coolingMessageIds(), voiceShare = null)
        }
        // 고르지 않으면 10초 뒤 저절로 닫힌다 (알림 카드와 같은 규칙)
        closeMessagePanelLater(DrivingPolicy.ALERT_AUTO_DISMISS_MS)
    }

    fun closeMyMessages() {
        messagePanelJob?.cancel()
        _state.update { it.copy(messagePanelOpen = false, sentMessageId = null) }
    }

    fun sendMyMessage(id: String) {
        val s = _state.value
        val m = s.myMessages.firstOrNull { it.id == id } ?: return
        val message = GroupMessage.Custom(newId(), s.meId, now(), m.id, m.text, m.icon)
        if (!guardSend(message)) {
            _state.update { it.copy(coolingMessageIds = coolingMessageIds()) }
            return
        }
        channel.send(message)
        say(Announcements.customSent(m.text), VoicePriority.CONFIRM)
        _state.update {
            it.copy(
                feed = listOf(FeedItem(s.meId, "방금", "내 메시지", m.text, readAloud = false)) + it.feed,
                sentMessageId = m.id,
                coolingMessageIds = coolingMessageIds(),
            )
        }
        // ‘보냈어요’를 잠깐 보여 주고 닫는다
        closeMessagePanelLater(1_500L)
    }

    private fun closeMessagePanelLater(delayMs: Long) {
        messagePanelJob?.cancel()
        messagePanelJob = viewModelScope.launch {
            delay(delayMs)
            _state.update { it.copy(messagePanelOpen = false, sentMessageId = null) }
        }
    }

    private fun coolingMessageIds(): Set<String> = sendGuard.coolingKeys(now())
        .filter { it.startsWith(CUSTOM_KEY) }
        .map { it.removePrefix(CUSTOM_KEY) }
        .toSet()

    /** 받은 내 메시지 · 음성 메시지를 읽어 줄지 (끄면 자막과 소식만, 안전 안내는 계속 읽는다) */
    fun toggleReadAloud() {
        readAloud = !readAloud
        _state.update { it.copy(readMessagesAloud = readAloud) }
        showToast(if (readAloud) "받은 메시지를 읽어 드릴게요" else "받은 메시지는 자막으로만 보여 드려요 · 안전 안내는 계속 읽어요")
    }

    // ---------------- 음성으로 상황 공유 ----------------

    fun startVoiceShare() {
        val s = _state.value
        if (s.alert != null) {
            showToast("알림에 먼저 답해 주세요")
            return
        }
        val stage = s.voiceShare?.stage
        if (stage == VoiceShare.Stage.LISTENING || stage == VoiceShare.Stage.CONFIRM) return
        voiceShareJob?.cancel()
        messagePanelJob?.cancel()
        val stt = voiceAnswer?.takeIf { it.isReady }
        _state.update { it.copy(voiceShare = VoiceShare(VoiceShare.Stage.LISTENING, demo = stt == null), messagePanelOpen = false) }
        if (stt != null) {
            if (!stt.listenText { text -> onSpoken(text) }) {
                onSpoken(null)
                return
            }
            voiceShareJob = viewModelScope.launch {
                delay(VOICE_LISTEN_MS)
                if (_state.value.voiceShare?.stage == VoiceShare.Stage.LISTENING) {
                    stt.cancel()
                    onSpoken(null)
                }
            }
        } else {
            // 음성 인식이 없는 기기(시연): 예시 문장을 받아 적은 것처럼 보여 준다
            voiceShareJob = viewModelScope.launch {
                delay(2_200L)
                onSpoken(DemoScenario.DEMO_SPOKEN)
            }
        }
    }

    private fun onSpoken(text: String?) {
        val current = _state.value.voiceShare ?: return
        if (current.stage != VoiceShare.Stage.LISTENING) return
        voiceShareJob?.cancel()
        val check = QuickMessagePolicy.validateSpoken(text)
        if (!check.ok) {
            val problem = if (MessageProblem.EMPTY in check.problems || MessageProblem.TOO_SHORT in check.problems) {
                "잘 알아듣지 못했어요. 다시 한번 말해 주세요"
            } else {
                check.problems.first().message
            }
            _state.update { it.copy(voiceShare = current.copy(stage = VoiceShare.Stage.FAILED, text = check.text, problem = problem)) }
            voiceShareJob = viewModelScope.launch {
                delay(4_000L)
                clearVoiceShare(VoiceShare.Stage.FAILED)
            }
            return
        }
        _state.update { it.copy(voiceShare = current.copy(stage = VoiceShare.Stage.CONFIRM, text = check.text)) }
        // 받아 적은 문장을 보여 주고 3초 뒤 자동으로 보낸다 (그 전에 취소 가능)
        voiceShareJob = viewModelScope.launch {
            delay(VOICE_CONFIRM_MS)
            sendVoiceShare()
        }
    }

    /** 확인 화면의 ‘지금 보내기’ */
    fun sendVoiceShareNow() {
        voiceShareJob?.cancel()
        sendVoiceShare()
    }

    private fun sendVoiceShare() {
        val s = _state.value
        val share = s.voiceShare ?: return
        val text = share.text ?: return
        if (share.stage != VoiceShare.Stage.CONFIRM) return
        val message = GroupMessage.Voice(newId(), s.meId, now(), text)
        if (!guardSend(message)) {
            _state.update { it.copy(voiceShare = null) }
            return
        }
        channel.send(message)
        say(Announcements.customSent(text), VoicePriority.CONFIRM)
        _state.update {
            it.copy(
                feed = listOf(FeedItem(s.meId, "방금", "음성 메시지", text, readAloud = false)) + it.feed,
                voiceShare = share.copy(stage = VoiceShare.Stage.SENT),
            )
        }
        voiceShareJob = viewModelScope.launch {
            delay(1_500L)
            clearVoiceShare(VoiceShare.Stage.SENT)
        }
    }

    fun cancelVoiceShare() {
        voiceShareJob?.cancel()
        voiceAnswer?.cancel()
        _state.update { it.copy(voiceShare = null) }
    }

    private fun clearVoiceShare(stage: VoiceShare.Stage) {
        _state.update { if (it.voiceShare?.stage == stage) it.copy(voiceShare = null) else it }
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
        say("연속 운전이 ${formatDuration(minutes)}째예요. 가까운 곳에서 쉬었다 갈까요?", VoicePriority.GROUP)
        startVote(s.meId, StatusSignal.REST)
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun newId(): String = UUID.randomUUID().toString()

    override fun onCleared() {
        voiceQueue.shutdown()
        naviVoice.release()
        voiceAnswer?.release()
        pleosNavigation?.release()
        super.onCleared()
    }

    private companion object {
        const val CUSTOM_KEY = "custom:"
        /** 음성으로 상황 공유: 듣는 시간 */
        const val VOICE_LISTEN_MS = 8_000L
        /** 받아 적은 문장을 보여 주고 자동으로 보내기까지 */
        const val VOICE_CONFIRM_MS = 3_000L
    }
}
