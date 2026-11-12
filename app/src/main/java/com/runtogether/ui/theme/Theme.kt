package com.runtogether.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak

/** 기획서·화면 시안과 같은 주황·차콜 팔레트 */
object RtColors {
    val Bg = Color(0xFFF5F0EA)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1C1917)
    val Ink2 = Color(0xFF292524)
    val Sub = Color(0xFF57534E)
    val Muted = Color(0xFF6B635C)
    val Faint = Color(0xFF9C928A)
    val Line = Color(0xFFE2D9CF)
    val Line2 = Color(0xFFD7CEC4)
    val Divider = Color(0xFFEDE6DE)
    val Panel = Color(0xFFF8F4EF)
    val Pending = Color(0xFFE6DED5)

    val Accent = Color(0xFFC2410C)
    val AccentSoft = Color(0x1FC2410C)
    val AccentRing = Color(0x47C2410C)

    /** '졸려요' = 밤·달 */
    val Night = Color(0xFF3730A3)
    val NightSoft = Color(0xFFEEF0FF)
    val NightLine = Color(0xFFC7CAF6)

    /** 연료 부족·간격 벌어짐 같은 주의 */
    val Warn = Color(0xFF7A3B06)
    val WarnSub = Color(0xFF6B3A0C)
    val WarnSoft = Color(0xFFFCF3E6)
    val WarnIcon = Color(0xFF9A4A07)
    val WarnLine = Color(0xFFB45309)

    val MapLand = Color(0xFFECE5DC)
    val MapPark = Color(0xFFDEE3D1)
    val MapRoadEdge = Color(0xFFDDD4CA)
    val MapHighwayEdge = Color(0xFFD4CABE)
    val Scrim = Color(0x571C1917)
    val CaptionBg = Color(0xF21C1917)
}

/**
 * 한국어 문장은 글자가 아니라 어절(띄어쓰기) 단위로 줄을 바꾼다.
 * ("골/랐어요", "1/분"처럼 단어 중간이 잘리지 않게. Android 13 이상에서 적용)
 */
val KoreanTextStyle = TextStyle(
    color = RtColors.Ink,
    localeList = LocaleList("ko-KR"),
    lineBreak = LineBreak(
        strategy = LineBreak.Strategy.HighQuality,
        strictness = LineBreak.Strictness.Strict,
        wordBreak = LineBreak.WordBreak.Phrase,
    ),
)

@Composable
fun RunTogetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = RtColors.Accent,
            onPrimary = Color.White,
            background = RtColors.Bg,
            onBackground = RtColors.Ink,
            surface = RtColors.Card,
            onSurface = RtColors.Ink,
        ),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides KoreanTextStyle,
            LocalContentColor provides RtColors.Ink,
            content = content,
        )
    }
}
