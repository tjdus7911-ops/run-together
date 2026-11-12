package com.runtogether.data.pleos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceAnswerParserTest {

    @Test
    fun yesAnswers() {
        listOf("좋아요", "좋아요.", "네", "응", "그래 쉬어가자", "좋습니다").forEach {
            assertEquals(it, true, VoiceAnswerParser.parse(it))
        }
    }

    @Test
    fun noAnswers() {
        listOf("괜찮아요", "아니요", "그냥 가요", "안 쉬어도 돼").forEach {
            assertEquals(it, false, VoiceAnswerParser.parse(it))
        }
    }

    @Test
    fun unclearSpeechIsIgnored() {
        assertNull(VoiceAnswerParser.parse(null))
        assertNull(VoiceAnswerParser.parse(""))
        assertNull(VoiceAnswerParser.parse("문막휴게소가 어디예요"))
    }
}
