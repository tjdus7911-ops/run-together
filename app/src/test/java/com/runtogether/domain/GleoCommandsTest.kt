package com.runtogether.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GleoCommandsTest {

    @Test
    fun `상황 공유 · 메시지 · 투표 답을 읽는다`() {
        assertEquals(GleoCommand.ShareStatus(StatusSignal.SLEEPY), GleoCommand.parse("SHARE_DRIVING_STATUS", mapOf("status" to "SLEEPY")))
        assertEquals(GleoCommand.ShareStatus(StatusSignal.ENERGY), GleoCommand.parse("SHARE_DRIVING_STATUS", mapOf("status" to "fuel")))
        assertEquals(GleoCommand.SendMessage("카페 들렀다 가요"), GleoCommand.parse("SEND_GROUP_MESSAGE", mapOf("message" to " 카페 들렀다 가요 ")))
        assertEquals(GleoCommand.AnswerVote(true), GleoCommand.parse("ANSWER_REST_VOTE", mapOf("answer" to "YES")))
        assertEquals(GleoCommand.AnswerVote(false), GleoCommand.parse("ANSWER_REST_VOTE", mapOf("answer" to "no")))
    }

    @Test
    fun `매개변수가 없거나 모르는 값이면 되묻는다`() {
        assertNull(GleoCommand.parse("SHARE_DRIVING_STATUS", mapOf("status" to "HUNGRY")))
        assertNull(GleoCommand.parse("SEND_GROUP_MESSAGE", mapOf("message" to "  ")))
        assertNull(GleoCommand.parse("ANSWER_REST_VOTE", emptyMap()))
        assertNull(GleoCommand.parse("PLAY_MUSIC", mapOf("status" to "SLEEPY")))
        for (f in GleoCommand.FUNCTIONS) assertEquals(GleoResult.Status.ASKING, GleoCommand.askingFor(f).status)
        assertEquals(GleoResult.Status.FAIL, GleoCommand.askingFor("PLAY_MUSIC").status)
    }

    @Test
    fun `ACTION_PARAM 을 여러 모양으로 받아도 읽는다`() {
        // 평범한 JSON 객체 (Pleos 받는 쪽 예시)
        assertEquals(mapOf("status" to "SLEEPY"), GleoParams.parse("{\"status\":\"SLEEPY\"}"))
        // Pleos 보내는 쪽 예시처럼 따옴표로 한 번 더 감싼 경우
        assertEquals(
            mapOf("title" to "Meeting", "dateTime" to "2025-02-20T06:00:00Z"),
            GleoParams.parse("\"{\"title\":\"Meeting\", \"dateTime\":\"2025-02-20T06:00:00Z\"}\""),
        )
        // JSON 문자열로 감싼 경우
        assertEquals(mapOf("message" to "10분 뒤 \"도착\""), GleoParams.parse("\"{\\\"message\\\":\\\"10분 뒤 \\\\\\\"도착\\\\\\\"\\\"}\""))
        // 숫자 · 불리언 · null · 중첩 값
        assertEquals(
            mapOf("a" to "3", "b" to "true", "e" to "x"),
            GleoParams.parse("{ \"a\": 3, \"b\": true, \"c\": null, \"d\": {\"x\": [1, \"]\"]}, \"e\": \"x\" }"),
        )
        // 유니코드 이스케이프
        assertEquals(mapOf("message" to "졸려요"), GleoParams.parse("{\"message\":\"\\uc878\\ub824\\uc694\"}"))
        // 읽지 못하면 빈 맵
        assertEquals(emptyMap<String, String>(), GleoParams.parse("{\"status\":"))
        assertEquals(emptyMap<String, String>(), GleoParams.parse("SLEEPY"))
        assertEquals(emptyMap<String, String>(), GleoParams.parse(null))
    }

    @Test
    fun `응답 JSON 은 Pleos 콜백 모양 · data 는 배열 문자열`() {
        val json = GleoResponse.json(
            "SEND_GROUP_MESSAGE",
            GleoResult.success("Sent the message to the group.", data = mapOf("message" to "카페 \"들렀다\"\n가요")),
        )
        val parsed = GleoParams.parse(json)
        assertEquals("SEND_GROUP_MESSAGE", parsed["functionName"])
        assertEquals("success", parsed["status"])
        assertEquals("Sent the message to the group.", parsed["message"])
        assertEquals("[{\"message\":\"카페 \\\"들렀다\\\"\\n가요\"}]", parsed["data"])
        // data 가 없으면 넣지 않는다
        val fail = GleoResponse.json("ANSWER_REST_VOTE", GleoResult.fail("There is no rest vote in progress right now."))
        assertEquals("{\"functionName\":\"ANSWER_REST_VOTE\",\"status\":\"fail\",\"message\":\"There is no rest vote in progress right now.\"}", fail)
    }

    @Test
    fun `schema json 이 코드의 기능 이름과 맞고 3개를 넘지 않는다`() {
        val schema = listOf("src/main/assets/schema.json", "app/src/main/assets/schema.json").map(::File).first { it.exists() }.readText()
        val names = Regex("\"name\"\\s*:\\s*\"([A-Z_]+)\"").findAll(schema).map { it.groupValues[1] }.toList()
        assertEquals(GleoCommand.FUNCTIONS, names)
        assertTrue(names.size <= 3)
        assertTrue(schema.contains("\"packageName\": \"com.runtogether.app\""))
        assertEquals(3, Regex("\"component\"\\s*:\\s*\"service\"").findAll(schema).count())
        // 상황 공유 enum 값은 모두 읽을 수 있어야 한다
        for (v in listOf("SLEEPY", "REST", "TOILET", "FUEL")) assertTrue(v, GleoCommand.statusOf(v) != null)
    }

    @Test
    fun `말로 보내기 확인 답`() {
        assertEquals(true, SendAnswerParser.parse("보내"))
        assertEquals(true, SendAnswerParser.parse("응 보내 줘"))
        assertEquals(true, SendAnswerParser.parse("네"))
        assertEquals(false, SendAnswerParser.parse("취소"))
        assertEquals(false, SendAnswerParser.parse("보내지 마"))
        assertEquals(false, SendAnswerParser.parse("아니 안 보내"))
        assertNull(SendAnswerParser.parse("음"))
        assertNull(SendAnswerParser.parse(null))
    }
}
