package com.runtogether.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageGridTest {

    @Test
    fun `1600x900 주행 화면은 큰 버튼 3열 2줄`() {
        // 지도 위 패널 안쪽: 가로 약 900dp, 세로 약 400dp
        val g = MessageGrid.of(6, widthDp = 900f, heightDp = 400f)
        assertEquals(MessageGrid(3, compact = false, visible = 6), g)
        assertEquals(100f, g.rowHeightDp)
    }

    @Test
    fun `가로가 좁으면 열을 줄인다`() {
        assertEquals(2, MessageGrid.of(6, widthDp = 600f, heightDp = 400f).columns)
        assertEquals(1, MessageGrid.of(6, widthDp = 300f, heightDp = 900f).columns)
        // 3열이 딱 들어가는 경계: 220×3 + 12×2 = 684
        assertEquals(3, MessageGrid.of(6, widthDp = 684f, heightDp = 400f).columns)
        assertEquals(2, MessageGrid.of(6, widthDp = 683f, heightDp = 400f).columns)
    }

    @Test
    fun `세로가 모자라면 간단 목록으로 바꾸고 터치 크기는 76dp를 지킨다`() {
        // 큰 버튼 2줄 = 212dp, 간단 목록 2줄 = 160dp
        val g = MessageGrid.of(6, widthDp = 900f, heightDp = 200f)
        assertTrue(g.compact)
        assertEquals(6, g.visible)
        assertEquals(76f, g.rowHeightDp)
        assertFalse(MessageGrid.of(6, widthDp = 900f, heightDp = 212f).compact)
    }

    @Test
    fun `간단 목록으로도 모자라면 앞에서부터 들어가는 만큼만`() {
        // 2열 3줄(76×3 + 8×2 = 244)이 필요한데 200dp 뿐: 안내 한 줄(30dp)을 빼고 2줄만 들어간다
        val g = MessageGrid.of(6, widthDp = 600f, heightDp = 200f)
        assertEquals(MessageGrid(2, compact = true, visible = 4), g)
        // 아주 좁아도 한 줄은 보여 준다
        assertEquals(2, MessageGrid.of(6, widthDp = 600f, heightDp = 10f).visible)
    }

    @Test
    fun `메시지가 적으면 큰 버튼 그대로`() {
        assertEquals(MessageGrid(3, compact = false, visible = 2), MessageGrid.of(2, widthDp = 900f, heightDp = 120f))
        assertEquals(0, MessageGrid.of(0, widthDp = 900f, heightDp = 400f).visible)
    }
}
