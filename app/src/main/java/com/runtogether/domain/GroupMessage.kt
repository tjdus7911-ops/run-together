package com.runtogether.domain

/**
 * 모임 안에서 오가는 메시지. 기본 상황 공유 · 내 메시지 · 음성 메시지가 같은 통로(GroupChannel)로 오간다.
 * 보내는 쪽은 글자와 보낸 사람만 보내고, 음성은 받는 차가 자기 음성 엔진으로 만든다.
 */
sealed interface GroupMessage {
    val id: String
    val fromId: String
    val sentAtMs: Long

    /** 기본 상황 공유 버튼 (졸려요 · 쉬었다 갈까요? · 화장실 급해요 · 주유·충전 …) */
    data class Status(
        override val id: String,
        override val fromId: String,
        override val sentAtMs: Long,
        val signal: StatusSignal,
    ) : GroupMessage

    /** 내 메시지 (휴대폰에서 만든 문장) */
    data class Custom(
        override val id: String,
        override val fromId: String,
        override val sentAtMs: Long,
        val messageId: String,
        val text: String,
        val icon: MessageIcon,
    ) : GroupMessage

    /** 음성으로 상황 공유 (말한 것을 받아 적은 문장) */
    data class Voice(
        override val id: String,
        override val fromId: String,
        override val sentAtMs: Long,
        val text: String,
    ) : GroupMessage

    /** 보내기 제한(SendGuard)에서 쓰는 키: 같은 버튼 · 같은 문장이면 같은 키 */
    val guardKey: String
        get() = when (this) {
            is Status -> "status:${signal.name}"
            is Custom -> "custom:$messageId"
            is Voice -> "voice:${QuickMessagePolicy.normalize(text)}"
        }
}

/** 음성 안내의 우선순위 (위가 먼저). [maxWaitMs] 동안 읽지 못하면 자막·소식으로만 남긴다. */
enum class VoicePriority(val maxWaitMs: Long) {
    /** 졸려요 · 도움 요청 · 쉬어가기 제안 */
    SAFETY(30_000L),
    /** 투표 결과 · 출발 안내 · 휴식 제안 */
    GROUP(20_000L),
    /** 내 메시지 · 음성 메시지 */
    MESSAGE(20_000L),
    /** 내가 보낸 것 확인 */
    CONFIRM(6_000L),
}

data class Utterance(
    val text: String,
    val priority: VoicePriority,
    val createdAtMs: Long,
    /** 자막에 얼굴을 함께 보여 줄 보낸 사람 */
    val speakerId: String? = null,
    /** false 면 읽지 않고 자막만 (메시지 읽어 주기 끔) */
    val speak: Boolean = true,
)

/**
 * 읽을 차례 정하기 (순수 로직). 한 번에 하나씩 꺼내고, 우선순위가 높은 것 → 먼저 들어온 것 순서.
 * - 똑같은 문장이 이미 기다리고 있으면 또 넣지 않는다
 * - 메시지는 [maxMessages] 개까지만 기다린다 (넘치면 오래된 것부터 버림 — 소식에는 남아 있다)
 * - 너무 오래 기다린 것은 버린다
 */
class AnnouncementQueue(private val maxMessages: Int = 3) {
    private val items = mutableListOf<Utterance>()

    val size: Int get() = items.size

    fun offer(u: Utterance): Boolean {
        if (items.any { it.text == u.text }) return false
        items += u
        if (u.priority == VoicePriority.MESSAGE) {
            val messages = items.filter { it.priority == VoicePriority.MESSAGE }
            if (messages.size > maxMessages) items.remove(messages.minBy { it.createdAtMs })
        }
        return true
    }

    /** 다음에 읽을 것. 없으면 null. */
    fun poll(nowMs: Long): Utterance? {
        items.removeAll { nowMs - it.createdAtMs > it.priority.maxWaitMs }
        val next = items.minWithOrNull(compareBy<Utterance>({ it.priority.ordinal }, { it.createdAtMs })) ?: return null
        items.remove(next)
        return next
    }

    fun clear() = items.clear()
}
