package com.runtogether.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.R

/**
 * Pleos Connect 시스템 글꼴 Asta Sans (42dot, SIL OFL 1.1 — assets/licenses/asta_sans_OFL.txt).
 * 앱이 차량 기본 화면과 같은 글꼴을 쓰면 처음 보는 화면도 익숙하게 읽힌다 (Pleos Connect UI 디자인 가이드 ‘일관성’).
 * 화면 코드는 Medium · Bold · Black 만 쓴다. Black(900) 은 가장 굵은 800 으로 그려진다.
 */
val AstaSans = FontFamily(
    Font(R.font.asta_sans_medium, FontWeight.Medium),
    Font(R.font.asta_sans_bold, FontWeight.Bold),
    Font(R.font.asta_sans_extrabold, FontWeight.ExtraBold),
)

/**
 * 그래파이트 × 디지털 블루 디자인 토큰 (차량 인포테인먼트, 어두운 화면 — 이전 차콜보다 한 단계 밝게).
 * 바탕은 푸른 기가 살짝 도는 그래파이트, 글자는 차가운 흰색. 모든 글자는 놓이는 면 위에서 4.5:1 이상.
 * 파랑(Accent)은 로고 · 지금 고른 것 · 주요 버튼 · 내 차 위치와 주행 경로 · 집결지 진행 · 중요한 상태에만 쓴다.
 * 이름은 이전 테마와 같게 두어 화면 코드가 그대로 따라온다 (Ink = 기본 글자, Card = 카드 바탕 …).
 */
object RtColors {
    // 바탕
    val Bg = Color(0xFF30353C)
    val Card = Color(0xFF3D434B)
    /** 카드 안의 한 단계 낮은 면 (타일·입력 칸) */
    val Panel = Color(0xFF353A42)
    /** 카드 위로 올라온 면 (보조 버튼·칩) */
    val Raised = Color(0xFF4A515A)
    /** 지도 위에 뜨는 패널 */
    val Overlay = Color(0xF5343A42)

    // 글자
    val Ink = Color(0xFFF5F6F8)
    val Ink2 = Color(0xFFE4E7EB)
    val Sub = Color(0xFFBCC2CA)
    val Muted = Color(0xFFAAB1BA)
    val Faint = Color(0xFF8A919B)

    // 선
    val Line = Color(0xFF59606A)
    val Line2 = Color(0xFF677079)
    val Divider = Color(0xFF4E555E)
    /** 투표 막대의 아직 답하지 않은 칸 */
    val Pending = Color(0xFF555C66)

    // 포인트 (디지털 블루)
    val Accent = Color(0xFF2F74EE)
    /** 어두운 바탕 위 파란 글자 (작은 글자도 읽히게 조금 밝게) */
    val AccentText = Color(0xFF80B0FF)
    val AccentSoft = Color(0x2E2F74EE)
    val AccentRing = Color(0x592F74EE)
    val OnAccent = Color(0xFFFFFFFF)

    /** '졸려요' = 밤·달 */
    val Night = Color(0xFFC3A6FF)
    val NightSoft = Color(0x29C3A6FF)
    val NightLine = Color(0x5CC3A6FF)

    /** 주의: 연료 부족 · 간격 벌어짐 */
    val Warn = Color(0xFFF4C76E)
    val WarnSub = Color(0xFFDCBB7C)
    val WarnSoft = Color(0x24F4C76E)
    val WarnIcon = Color(0xFFF4C76E)
    val WarnLine = Color(0xFFE5AC3C)

    /** 위험: 도움 요청 · 차에 문제 */
    val Danger = Color(0xFFFF8F84)
    val DangerSoft = Color(0x26FF8F84)

    /** 정상 · 실시간 연결됨 */
    val Good = Color(0xFF6CCB97)
    val GoodSoft = Color(0x246CCB97)

    // 지도 (밝은 내비 지도: 어두운 화면 가운데에서도 길이 잘 보이게)
    val MapLand = Color(0xFFEFEFEA)
    val MapPark = Color(0xFFD5E7C8)
    val MapForest = Color(0xFFC6DEB6)
    val MapWater = Color(0xFFB6D4EA)
    val MapWaterEdge = Color(0xFF9DC2DD)
    val MapBuilding = Color(0xFFDEDDD6)
    val MapBuildingEdge = Color(0xFFCDCCC4)
    val MapRoad = Color(0xFFFFFFFF)
    val MapRoadEdge = Color(0xFFD5D4CC)
    val MapHighway = Color(0xFFFFFFFF)
    val MapHighwayEdge = Color(0xFFBCB7AA)
    val MapLabel = Color(0xFF5B5F66)
    val MapLabelStrong = Color(0xFF30343A)
    val MapShield = Color(0xFF2C5DA6)
    /** 지난 길(내 차 뒤) */
    val MapRoutePassed = Color(0xFFA9C2EA)

    // 교통 (파란 경로와 섞이지 않게 초록 · 노랑 · 진홍)
    val TrafficSmooth = Color(0xFF2E9F68)
    val TrafficSlow = Color(0xFFE8B928)
    val TrafficJam = Color(0xFFCC3340)

    val Scrim = Color(0x8C141618)
    val CaptionBg = Color(0xF51F2329)
}

/**
 * 한국어 문장은 글자가 아니라 어절(띄어쓰기) 단위로 줄을 바꾼다.
 * ("골/랐어요", "1/분"처럼 단어 중간이 잘리지 않게. Android 13 이상에서 적용)
 */
val KoreanTextStyle = TextStyle(
    color = RtColors.Ink,
    fontFamily = AstaSans,
    localeList = LocaleList("ko-KR"),
    lineBreak = LineBreak(
        strategy = LineBreak.Strategy.HighQuality,
        strictness = LineBreak.Strictness.Strict,
        wordBreak = LineBreak.WordBreak.Phrase,
    ),
)

/** 글자 단계. 운전 중에 읽는 값은 크게, 보조 설명은 작게. 모두 [KoreanTextStyle] 위에 쌓는다. */
object RtType {
    /** 큰 숫자 · 다음 안내 거리 */
    val Display = KoreanTextStyle.copy(fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp, lineHeight = 46.sp)
    /** 화면 제목 · 집결지 이름 */
    val Title = KoreanTextStyle.copy(fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp, lineHeight = 34.sp)
    /** 카드 제목 */
    val Heading = KoreanTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)
    /** 본문 · 소식 */
    val Body = KoreanTextStyle.copy(fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp)
    /** 버튼 글자 */
    val Button = KoreanTextStyle.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
    /** 이름 · 작은 제목 */
    val Label = KoreanTextStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
    /** 보조 설명 */
    val Caption = KoreanTextStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    /** 지도 위 작은 글자 */
    val Micro = KoreanTextStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
}

/** 간격 · 모서리 · 크기 (1600×900 설계 기준 dp) */
object RtDimens {
    val ScreenPaddingH = 24.dp
    val ScreenPaddingTop = 16.dp
    val ScreenPaddingBottom = 20.dp
    val Gap = 16.dp
    val GapSmall = 12.dp
    val CardRadius = 24.dp
    val InnerRadius = 16.dp
    val ButtonRadius = 18.dp
    val CardPaddingH = 24.dp
    val CardPaddingV = 20.dp
    val HeaderHeight = 64.dp
    val BottomBarHeight = 112.dp
    val RightColumnWidth = 552.dp
    /** 운전 중 누르는 버튼의 최소 크기 */
    val TouchMin = 64.dp
    val ButtonHeight = 76.dp
}

@Composable
fun RunTogetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RtColors.Accent,
            onPrimary = RtColors.OnAccent,
            background = RtColors.Bg,
            onBackground = RtColors.Ink,
            surface = RtColors.Card,
            onSurface = RtColors.Ink,
            surfaceVariant = RtColors.Panel,
            onSurfaceVariant = RtColors.Sub,
            outline = RtColors.Line,
        ),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides KoreanTextStyle,
            LocalContentColor provides RtColors.Ink,
            content = content,
        )
    }
}
