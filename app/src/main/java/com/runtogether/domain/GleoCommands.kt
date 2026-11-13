package com.runtogether.domain

/**
 * Gleo AI(Pleos Connect 차량 음성 비서)가 부르는 같이 달려 기능. 이름은 assets/schema.json 과 같다.
 *
 * 운전자가 Gleo AI 에게 “같이 달려에서 졸리다고 알려 줘”처럼 말하면 Gleo 가 함수를 고르고
 * Intent(action = 함수 이름, extra "ACTION_PARAM" = 매개변수 JSON)를 앱에 보낸다 (component = "service" → sendBroadcast).
 * 앱은 처리한 결과를 Gleo 로 돌려주고([GleoResponse]), Gleo 가 그 결과로 운전자에게 답한다.
 */
sealed interface GleoCommand {
    val functionName: String

    /** 상황 공유 (졸려요 · 쉬었다 갈까요? · 화장실 급해요 · 주유·충전) */
    data class ShareStatus(val signal: StatusSignal) : GleoCommand {
        override val functionName: String get() = SHARE_DRIVING_STATUS
    }

    /** 모임에 짧은 글 보내기 (저장해 둔 내 메시지와 같은 문장이면 그 메시지로) */
    data class SendMessage(val text: String) : GleoCommand {
        override val functionName: String get() = SEND_GROUP_MESSAGE
    }

    /** 진행 중인 쉬어가기 투표에 답하기 */
    data class AnswerVote(val yes: Boolean) : GleoCommand {
        override val functionName: String get() = ANSWER_REST_VOTE
    }

    companion object {
        const val SHARE_DRIVING_STATUS = "SHARE_DRIVING_STATUS"
        const val SEND_GROUP_MESSAGE = "SEND_GROUP_MESSAGE"
        const val ANSWER_REST_VOTE = "ANSWER_REST_VOTE"

        /** schema.json 의 actionCalls (Gleo 규칙상 최대 3개) */
        val FUNCTIONS = listOf(SHARE_DRIVING_STATUS, SEND_GROUP_MESSAGE, ANSWER_REST_VOTE)

        /** [params] 는 ACTION_PARAM 을 풀어 놓은 값 (또는 extra 하나씩 들어온 값). 매개변수가 비었거나 모르는 값이면 null */
        fun parse(action: String?, params: Map<String, String?>): GleoCommand? = when (action) {
            SHARE_DRIVING_STATUS -> statusOf(params["status"])?.let { ShareStatus(it) }
            SEND_GROUP_MESSAGE -> params["message"]?.trim()?.takeIf { it.isNotEmpty() }?.let { SendMessage(it) }
            ANSWER_REST_VOTE -> answerOf(params["answer"])?.let { AnswerVote(it) }
            else -> null
        }

        fun statusOf(value: String?): StatusSignal? = when (value?.trim()?.uppercase()) {
            "SLEEPY" -> StatusSignal.SLEEPY
            "REST" -> StatusSignal.REST
            "TOILET" -> StatusSignal.TOILET
            "FUEL", "CHARGE", "ENERGY" -> StatusSignal.ENERGY
            else -> null
        }

        fun answerOf(value: String?): Boolean? = when (value?.trim()?.uppercase()) {
            "YES", "TRUE" -> true
            "NO", "FALSE" -> false
            else -> null
        }

        /** 함수는 알지만 매개변수가 비었거나 모르는 값일 때: Gleo 가 운전자에게 되묻게 한다 (status = asking) */
        fun askingFor(action: String?): GleoResult = when (action) {
            SHARE_DRIVING_STATUS -> GleoResult.asking("Which situation should be shared with the group: sleepy, rest, restroom, or fuel?")
            SEND_GROUP_MESSAGE -> GleoResult.asking("What message should be sent to the group?")
            ANSWER_REST_VOTE -> GleoResult.asking("Should the group stop at the proposed rest area, yes or no?")
            else -> GleoResult.fail("This request is not supported by Run Together.")
        }
    }
}

/**
 * 앱이 처리한 결과 (Gleo Callback).
 * - [message]: Gleo 가 답을 만들 때 쓰는 한 문장. Pleos 가이드대로 영어 한 문장으로 쓴다 (장소 이름 등은 한국어 그대로)
 * - [screenText]: 차량 화면에 잠깐 보여 줄 한국어 문장
 * - [data]: Gleo 가 답을 만들 때 참고할 값 (JSON 배열 문자열로 보낸다)
 */
data class GleoResult(
    val status: Status,
    val message: String,
    val screenText: String? = null,
    val data: Map<String, String> = emptyMap(),
) {
    enum class Status(val value: String) { SUCCESS("success"), ASKING("asking"), FAIL("fail") }

    companion object {
        fun success(message: String, screenText: String? = null, data: Map<String, String> = emptyMap()) =
            GleoResult(Status.SUCCESS, message, screenText, data)

        fun fail(message: String, screenText: String? = null) = GleoResult(Status.FAIL, message, screenText)

        fun asking(message: String) = GleoResult(Status.ASKING, message)
    }
}

/**
 * Gleo 로 돌려줄 응답. Intent action = [ACTION], extra "result" = 상태, "packageName" = 앱 패키지,
 * "response" = {"functionName","status","message","data"} JSON (data 는 JSON 배열을 문자열로 담는다).
 * Gleo 는 7초가 넘게 걸리는 처리를 취소하므로 받자마자 바로 답한다.
 */
object GleoResponse {
    const val ACTION = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"

    fun json(functionName: String, result: GleoResult): String = buildString {
        append("{\"functionName\":").append(quote(functionName))
        append(",\"status\":").append(quote(result.status.value))
        append(",\"message\":").append(quote(result.message))
        if (result.data.isNotEmpty()) append(",\"data\":").append(quote(dataArray(result.data)))
        append('}')
    }

    /** [{"key":"value", …}] */
    fun dataArray(data: Map<String, String>): String =
        data.entries.joinToString(separator = ",", prefix = "[{", postfix = "}]") { (k, v) -> "${quote(k)}:${quote(v)}" }

    fun quote(s: String): String = buildString {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append(String.format("\\u%04x", c.code)) else append(c)
            }
        }
        append('"')
    }
}

/**
 * ACTION_PARAM 읽기. 매개변수가 평평한 JSON 객체라서 작은 읽개로 읽는다 (JVM 단위 테스트에서도 같은 코드가 돈다).
 * Pleos 예시처럼 객체를 따옴표로 한 번 더 감싸 오거나, JSON 문자열로 한 번 더 감싸 와도 읽는다.
 * 문자열·숫자·true/false 는 글자로 받고, null 과 중첩 객체·배열은 건너뛴다. 읽지 못하면 빈 맵.
 */
object GleoParams {
    fun parse(raw: String?): Map<String, String> {
        val text = unwrap(raw ?: return emptyMap()) ?: return emptyMap()
        return try {
            FlatJsonReader(text).readObject()
        } catch (e: RuntimeException) {
            emptyMap()
        }
    }

    internal fun unwrap(raw: String): String? {
        var s = raw.trim()
        repeat(3) {
            if (s.startsWith("{")) return s
            if (s.length < 2 || s.first() != '"' || s.last() != '"') return null
            val inner = s.substring(1, s.length - 1).trim()
            // "{\"status\":…}" 처럼 JSON 문자열로 감싼 경우는 이스케이프를 풀고, "{"status":…}" 는 바깥 따옴표만 벗긴다
            s = if (inner.drop(1).trimStart().startsWith("\\\"")) {
                try {
                    FlatJsonReader(s).readString()
                } catch (e: RuntimeException) {
                    return null
                }
            } else {
                inner
            }
        }
        return s.takeIf { it.startsWith("{") }
    }
}

private class FlatJsonReader(private val s: String) {
    private var i = 0

    fun readObject(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        skipWs()
        expect('{')
        skipWs()
        if (peek() == '}') return out
        while (true) {
            skipWs()
            val key = readString()
            skipWs()
            expect(':')
            skipWs()
            val value = readValue()
            if (value != null) out[key] = value
            skipWs()
            when (next()) {
                ',' -> Unit
                '}' -> return out
                else -> error("expected , or }")
            }
        }
    }

    fun readString(): String {
        expect('"')
        val sb = StringBuilder()
        while (true) {
            when (val c = next()) {
                '"' -> return sb.toString()
                '\\' -> when (val e = next()) {
                    '"', '\\', '/' -> sb.append(e)
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        sb.append(s.substring(i, i + 4).toInt(16).toChar())
                        i += 4
                    }
                    else -> error("bad escape")
                }
                else -> sb.append(c)
            }
        }
    }

    private fun readValue(): String? = when (peek()) {
        '"' -> readString()
        '{', '[' -> {
            skipNested()
            null
        }
        else -> {
            val start = i
            while (i < s.length && s[i] !in ",}] \t\r\n") i++
            val token = s.substring(start, i)
            require(token.isNotEmpty()) { "empty value" }
            token.takeUnless { it == "null" }
        }
    }

    private fun skipNested() {
        var depth = 0
        do {
            when (peek()) {
                '"' -> {
                    readString()
                    continue
                }
                '{', '[' -> depth++
                '}', ']' -> depth--
            }
            i++
        } while (depth > 0)
    }

    private fun skipWs() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun peek(): Char = s[i]

    private fun next(): Char = s[i++]

    private fun expect(c: Char) {
        val got = next()
        require(got == c) { "expected $c but was $got" }
    }
}

/** ‘말로 메시지 보내기’ 확인을 말로 받을 때: 보내(true) · 취소(false) · 모르겠음(null) */
object SendAnswerParser {
    fun parse(text: String?): Boolean? {
        val t = text?.filter { it.isLetterOrDigit() }.orEmpty()
        if (t.isEmpty()) return null
        return when {
            listOf("취소", "보내지마", "안보내", "그만", "아니", "하지마").any { it in t } -> false
            "보내" in t || t in setOf("네", "응", "예", "그래", "좋아", "좋아요", "확인") -> true
            else -> null
        }
    }
}
