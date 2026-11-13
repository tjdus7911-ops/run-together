package com.runtogether.domain

/** ‘내 메시지’ 아이콘. 이모지는 차량마다 글꼴이 달라 보이는 모양이 바뀌므로 아이콘으로 통일한다. */
enum class MessageIcon { COFFEE, FOOD, FUEL, CHARGE, TOILET, PHOTO, SLOW, LATE, OK, PARKING, MUSIC, SUN }

/**
 * 사용자가 휴대폰에서 만든 상황 공유 메시지 (최대 6개).
 * 계정에 저장되고 차량 앱으로 동기화된다. 차량에서는 고르기만 하고 고치지 않는다.
 */
data class QuickMessage(
    val id: String,
    val text: String,
    val icon: MessageIcon,
    /** 차량 화면의 버튼 순서 (작을수록 앞) */
    val order: Int,
)

/** 문장 검사에서 나온 문제. [message] 는 휴대폰 편집 화면에 그대로 보여 준다. */
enum class MessageProblem(val message: String) {
    EMPTY("내용을 입력해 주세요"),
    TOO_SHORT("두 글자 이상 입력해 주세요"),
    TOO_LONG("20자까지 쓸 수 있어요"),
    TOO_LONG_SPOKEN("조금 더 짧게 말해 주세요"),
    UNSUPPORTED_CHARACTER("글자·숫자와 기본 문장부호만 쓸 수 있어요 (아이콘은 따로 골라요)"),
    CONTACT_INFO("전화번호나 링크는 보낼 수 없어요"),
    UNSAFE("재촉하거나 위험한 운전을 권하는 말은 보낼 수 없어요"),
    DUPLICATE("이미 같은 메시지가 있어요"),
    SAME_AS_DEFAULT("기본 상황 공유 버튼과 같은 말이에요"),
    LIMIT_REACHED("메시지는 6개까지 만들 수 있어요"),
}

data class MessageCheck(val text: String, val problems: List<MessageProblem>) {
    val ok: Boolean get() = problems.isEmpty()
}

/**
 * 내 메시지 · 말로 보낸 메시지 문장 검사. 휴대폰 편집 화면과 차량 앱이 같은 규칙을 쓴다.
 * 위험 표현 목록은 정책 검토용 초안이다.
 */
object QuickMessagePolicy {
    const val MAX_MESSAGES = 6
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 20
    const val MAX_SPOKEN_LENGTH = 40

    /** 한글·영문·숫자·띄어쓰기와 기본 문장부호 */
    private val allowed = Regex("^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 .,!?~·()'\"%/:-]+$")
    private val phone = Regex("\\d{2,4}[-. ]?\\d{3,4}[-. ]?\\d{4}")
    private val link = Regex("(https?://|www\\.|\\.com|\\.kr|\\.net)", RegexOption.IGNORE_CASE)

    /** 띄어쓰기를 빼고 찾는다 ("신호 무시" = "신호무시") */
    private val unsafeWords = listOf(
        "밟아", "과속", "추월", "신호무시", "칼치기", "빨리따라", "빨리와", "빨리오세요", "더빨리", "음주",
        "씨발", "시발", "ㅅㅂ", "개새", "병신", "ㅂㅅ", "좆",
    )

    fun normalize(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    /** 휴대폰에서 만들거나 고칠 때 */
    fun validate(text: String, existing: List<QuickMessage> = emptyList(), editingId: String? = null): MessageCheck {
        val t = normalize(text)
        val problems = mutableListOf<MessageProblem>()
        if (editingId == null && existing.size >= MAX_MESSAGES) problems += MessageProblem.LIMIT_REACHED
        when {
            t.isEmpty() -> problems += MessageProblem.EMPTY
            t.length < MIN_LENGTH -> problems += MessageProblem.TOO_SHORT
            t.length > MAX_LENGTH -> problems += MessageProblem.TOO_LONG
        }
        if (t.isNotEmpty() && !allowed.matches(t)) problems += MessageProblem.UNSUPPORTED_CHARACTER
        problems += contentProblems(t)
        if (existing.any { it.id != editingId && normalize(it.text) == t }) problems += MessageProblem.DUPLICATE
        if (StatusSignal.entries.any { normalize(it.label) == t }) problems += MessageProblem.SAME_AS_DEFAULT
        return MessageCheck(t, problems.distinct())
    }

    /** ‘말로 메시지 보내기’로 받아 적은 문장 (글자 종류는 따지지 않는다) */
    fun validateSpoken(text: String?): MessageCheck {
        val t = normalize(text.orEmpty())
        val problems = mutableListOf<MessageProblem>()
        when {
            t.isEmpty() -> problems += MessageProblem.EMPTY
            t.length < MIN_LENGTH -> problems += MessageProblem.TOO_SHORT
            t.length > MAX_SPOKEN_LENGTH -> problems += MessageProblem.TOO_LONG_SPOKEN
        }
        problems += contentProblems(t)
        return MessageCheck(t, problems.distinct())
    }

    private fun contentProblems(t: String): List<MessageProblem> = buildList {
        if (phone.containsMatchIn(t) || link.containsMatchIn(t)) add(MessageProblem.CONTACT_INFO)
        val compact = t.replace(" ", "")
        if (unsafeWords.any { it in compact }) add(MessageProblem.UNSAFE)
    }

    /** 순서대로 최대 6개만 (동기화로 더 많이 와도 차량에는 6개까지) */
    fun forCar(messages: List<QuickMessage>): List<QuickMessage> =
        messages.sortedBy { it.order }.take(MAX_MESSAGES)
}

/**
 * 보내는 쪽에서 과도한 전송을 막는다. 기본 상황 공유 · 내 메시지 · 말로 보낸 메시지가 같은 규칙을 쓴다.
 * - 두 번 눌림: 2초 안에 또 누르면 무시
 * - 같은 메시지: 30초 뒤에 다시
 * - 전체: 1분에 4번까지
 */
class SendGuard(
    private val tapLockMs: Long = 2_000L,
    private val sameMessageMs: Long = 30_000L,
    private val windowMs: Long = 60_000L,
    private val maxPerWindow: Int = 4,
) {
    enum class Reason { DOUBLE_TAP, SAME_MESSAGE, TOO_MANY }

    sealed interface Decision {
        data object Allowed : Decision
        data class Blocked(val reason: Reason, val waitMs: Long) : Decision
    }

    private val sent = ArrayDeque<Pair<String, Long>>()

    fun check(key: String, nowMs: Long): Decision {
        while (sent.isNotEmpty() && nowMs - sent.first().second >= maxOf(windowMs, sameMessageMs)) sent.removeFirst()
        val last = sent.lastOrNull()
        if (last != null && nowMs - last.second < tapLockMs) {
            return Decision.Blocked(Reason.DOUBLE_TAP, tapLockMs - (nowMs - last.second))
        }
        val same = sent.lastOrNull { it.first == key }
        if (same != null && nowMs - same.second < sameMessageMs) {
            return Decision.Blocked(Reason.SAME_MESSAGE, sameMessageMs - (nowMs - same.second))
        }
        val recent = sent.filter { nowMs - it.second < windowMs }
        if (recent.size >= maxPerWindow) {
            return Decision.Blocked(Reason.TOO_MANY, windowMs - (nowMs - recent.first().second))
        }
        return Decision.Allowed
    }

    fun record(key: String, nowMs: Long) {
        sent.addLast(key to nowMs)
    }

    /** 보낼 수 있으면 기록까지 한다. */
    fun tryAcquire(key: String, nowMs: Long): Decision = check(key, nowMs).also {
        if (it == Decision.Allowed) record(key, nowMs)
    }

    /** 지금 다시 보낼 수 없는 키 (같은 메시지 30초) — 화면에서 버튼을 흐리게 */
    fun coolingKeys(nowMs: Long): Set<String> =
        sent.filter { nowMs - it.second < sameMessageMs }.map { it.first }.toSet()
}

/** 받는 쪽: 같은 사람이 같은 말을 짧은 시간에 또 보내면 다시 읽지 않는다 (소식에는 남긴다). */
class RepeatFilter(private val windowMs: Long = 60_000L) {
    private val heard = mutableMapOf<String, Long>()

    fun shouldAnnounce(fromId: String, text: String, nowMs: Long): Boolean {
        heard.entries.removeAll { nowMs - it.value >= windowMs }
        val key = fromId + "\u0000" + QuickMessagePolicy.normalize(text)
        val before = heard[key]
        heard[key] = nowMs
        return before == null
    }
}
