package com.runtogether.ui.screens

import kotlin.math.ceil
import kotlin.math.floor

/**
 * 차량 ‘내 메시지’ 버튼을 어떻게 놓을지 정한다. (Compose 없이 계산만 해서 단위 테스트로 확인한다)
 *
 * 1. 큰 버튼(아이콘 + 두 줄 문장, 높이 100dp)을 3열로 놓는다. 1600×900 주행 화면은 항상 여기에 해당한다.
 * 2. 가로가 좁으면 열을 2열 → 1열로 줄인다. 버튼 하나가 [MIN_BUTTON_DP] 보다 좁아지지 않게.
 * 3. 세로가 모자라면 한 줄짜리 간단 목록(높이 [COMPACT_ROW_DP])으로 바꾼다. 차량 터치 최소 크기(76dp)는 지킨다.
 * 4. 간단 목록으로도 다 못 넣으면 들어가는 만큼만 앞에서부터 보여 주고(안내 한 줄 [NOTE_DP] 자리를 남긴다),
 *    주행 중에는 스크롤하지 않는다. 순서는 휴대폰에서 정한 순서라 자주 쓰는 문장이 앞에 남는다.
 */
data class MessageGrid(val columns: Int, val compact: Boolean, val visible: Int) {

    val rowHeightDp: Float get() = if (compact) COMPACT_ROW_DP else ROW_DP
    val gapDp: Float get() = if (compact) COMPACT_GAP_DP else GAP_DP

    companion object {
        const val ROW_DP = 100f
        const val GAP_DP = 12f
        const val COMPACT_ROW_DP = 76f
        const val COMPACT_GAP_DP = 8f
        const val MIN_BUTTON_DP = 220f
        const val MAX_COLUMNS = 3
        const val NOTE_DP = 30f

        fun of(count: Int, widthDp: Float, heightDp: Float): MessageGrid {
            if (count <= 0) return MessageGrid(MAX_COLUMNS, compact = false, visible = 0)
            val columns = (MAX_COLUMNS downTo 2).firstOrNull { c -> c * MIN_BUTTON_DP + (c - 1) * GAP_DP <= widthDp } ?: 1
            val rows = ceil(count / columns.toFloat()).toInt()
            if (stackHeight(rows, ROW_DP, GAP_DP) <= heightDp) return MessageGrid(columns, compact = false, visible = count)
            if (stackHeight(rows, COMPACT_ROW_DP, COMPACT_GAP_DP) <= heightDp) return MessageGrid(columns, compact = true, visible = count)
            val fitRows = floor((heightDp - NOTE_DP + COMPACT_GAP_DP) / (COMPACT_ROW_DP + COMPACT_GAP_DP)).toInt().coerceAtLeast(1)
            return MessageGrid(columns, compact = true, visible = minOf(count, fitRows * columns))
        }

        private fun stackHeight(rows: Int, row: Float, gap: Float): Float = rows * row + (rows - 1) * gap
    }
}
