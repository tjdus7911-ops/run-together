package com.runtogether.domain

/** 한국어 조사: 받침 유무에 따라 고른다. */
object Josa {
    private fun lastHangul(word: String): Char? = word.lastOrNull { it in '가'..'힣' }

    fun hasBatchim(word: String): Boolean {
        val c = lastHangul(word) ?: return false
        return (c.code - 0xAC00) % 28 != 0
    }

    private fun hasRieul(word: String): Boolean {
        val c = lastHangul(word) ?: return false
        return (c.code - 0xAC00) % 28 == 8
    }

    /** 을/를 */
    fun eulReul(word: String): String = if (hasBatchim(word)) "을" else "를"

    /** 으로/로 (ㄹ 받침은 '로') */
    fun euroRo(word: String): String = if (hasBatchim(word) && !hasRieul(word)) "으로" else "로"

    /** 이에요/예요 */
    fun ieyo(word: String): String = if (hasBatchim(word)) "이에요" else "예요"
}

/** 다른 차에서 읽어 주는 음성 문장 */
object Announcements {
    fun subject(name: String?, anonymous: Boolean): String =
        if (anonymous || name.isNullOrBlank()) "한 분이" else "${name}님이"

    fun forSignal(
        signal: StatusSignal,
        name: String?,
        anonymous: Boolean = false,
        stopName: String? = null,
        restMinutes: Int = 15,
    ): String {
        val who = subject(name, anonymous)
        return when (signal) {
            StatusSignal.SLEEPY ->
                if (stopName != null) "$who 졸리대요. ${stopName}에서 ${restMinutes}분 쉬었다 갈까요?"
                else "$who 졸리대요. 가까운 쉼터에서 쉬어 갈까요?"
            StatusSignal.REST -> "$who 쉬었다 가자고 해요. 좋으면 ‘좋아요’라고 말해 주세요."
            StatusSignal.TOILET -> "$who 화장실이 급하대요. 다음 휴게소에 들를게요."
            StatusSignal.ENERGY -> "$who 주유가 필요해요."
            StatusSignal.SLOW -> "$who 천천히 오신대요. 다음 집결지에서 만나요."
            StatusSignal.HELP ->
                if (anonymous || name.isNullOrBlank()) "모임 차 한 대에 문제가 생겼어요. 안전한 곳에 서 있어요."
                else "${name}님 차에 문제가 생겼어요. 안전한 곳에 서 있어요."
        }
    }

    fun sentByMe(signal: StatusSignal): String =
        "모두에게 ‘${signal.label}’${Josa.eulReul(signal.label)} 알렸어요."

    fun voteAccepted(stopName: String, restMinutes: Int): String =
        "과반이 좋다고 해서 ${stopName}${Josa.eulReul(stopName)} 모두의 경로에 넣었어요. ${restMinutes}분 쉬고 출발해요."

    fun voteRejected(nextStopName: String): String =
        "이번에는 쉬지 않고 ${nextStopName}에서 만나요."

    fun departure(nextStopName: String): String =
        "곧 출발해요. 다음 집결지는 ${nextStopName}${Josa.ieyo(nextStopName)}. 따라오지 않아도 돼요, 거기서 만나요."

    // ---------------- 내 메시지 · 음성 메시지 ----------------

    /** 받는 차에서 읽는 문장: "민지님이 메시지를 보냈어요. 카페 들렀다 가요." */
    fun customMessage(name: String?, text: String, anonymous: Boolean = false): String =
        "${subject(name, anonymous)} 메시지를 보냈어요. ${spoken(text)}"

    /** 받는 차에서 읽는 문장: "민지님이 음성 메시지를 보냈어요. 10분 뒤 도착해요." */
    fun voiceMessage(name: String?, text: String, anonymous: Boolean = false): String =
        "${subject(name, anonymous)} 음성 메시지를 보냈어요. ${spoken(text)}"

    /** 보낸 차에서 확인: "모두에게 ‘카페 들렀다 가요’를 보냈어요." */
    fun customSent(text: String): String =
        "모두에게 ‘${text}’${Josa.eulReul(text)} 보냈어요."

    /** 읽을 때 소리 내기 어색한 기호를 정리한다 (물결표, 가운뎃점, 겹친 문장부호) */
    fun spoken(text: String): String {
        val t = text.trim()
            .replace("~", "")
            .replace("·", ", ")
            .replace(Regex("([!?.])\\1+"), "$1")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (t.isEmpty() || t.last() in ".!?") t else "$t."
    }
}
