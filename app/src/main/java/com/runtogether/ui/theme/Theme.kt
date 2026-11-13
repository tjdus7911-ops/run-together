package com.runtogether.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
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
 * 조용한 계기판 (Quiet Instrument) 디자인 토큰 — 휴대폰 웹앱(mobile/src/styles.css)과 같은 값.
 * 흰 바탕 위를 상자 대신 가는 선과 여백으로 나누고, 면은 지도 위에 뜨는 층과 고른 것에만 쓴다.
 * 디지털 블루(Accent)는 지금 누를 것 · 내 차 위치와 경로 · 고른 것 · 집결지 진행에만 쓴다.
 * 그라데이션은 주요 버튼 하나에만 아주 옅게(위 [AccentHi] → 아래 [AccentLo]), 바깥 빛은 쓰지 않는다.
 * 은은한 빛([LiveRing])은 ‘지금 살아 있는 것’(듣는 중인 마이크 · 내 차)에만. 움직이는 효과는 쓰지 않는다.
 * 모든 글자는 놓이는 면 위에서 4.5:1 이상. 이전 값은 git 기록(21509c3)의 이 파일에 있다.
 * 이름은 이전 테마와 같게 두어 화면 코드가 그대로 따라온다 (Ink = 기본 글자, Card = 옅은 면 …).
 */
object RtColors {
    // 바탕 · 면
    val Bg = Color(0xFFFFFFFF)
    /** 옅은 면 (sunken): 점선 아바타 안쪽 · 작은 칸. 화면 안 구역은 면 대신 [Divider] 선으로 나눈다 */
    val Card = Color(0xFFF8F9FB)
    /** 한 단계 진한 면 (fill): 상태 칩 · 막대 바탕 · 꺼진 버튼 */
    val Panel = Color(0xFFF2F4F7)
    /** 올라온 면 (보조 버튼 · 고르는 칸): 흰색 + 테두리 */
    val Raised = Color(0xFFFFFFFF)
    /** 지도 위에 뜨는 층: 흰색 + 그림자 한 단계([FloatShadow]) */
    val Overlay = Color(0xFFFFFFFF)

    // 글자
    val Ink = Color(0xFF15181E)
    val Ink2 = Color(0xFF2A2F37)
    val Sub = Color(0xFF4B5563)
    /** 흰 바탕 4.8:1 — 보조 설명의 가장 옅은 글자 */
    val Muted = Color(0xFF6B7480)
    /** 글자에는 쓰지 않는다 (꺼진 상태 · 화살표 같은 장식) */
    val Faint = Color(0xFF98A1AD)

    // 선
    /** 테두리 (보조 버튼 · 고르는 칸) */
    val Line = Color(0xFFD6DBE2)
    /** 진한 테두리: 햇빛 아래에서도 보여야 하는 칸 (차량 화면만) */
    val Line2 = Color(0xFFC6CDD7)
    /** 구역을 나누는 가는 선 (hairline) */
    val Divider = Color(0xFFE8EBF0)
    /** 투표 막대의 아직 답하지 않은 칸 */
    val Pending = Color(0xFFDCE1E8)

    // 포인트 (디지털 블루)
    /** 흰 바탕에서 흰 글자 5.2:1 · 파란 글자로도 4.7:1 */
    val Accent = Color(0xFF2563EB)
    val AccentText = Color(0xFF2563EB)
    /** 누르는 동안 */
    val AccentPress = Color(0xFF1D4ED8)
    /** 고른 것의 바탕 (accent-subtle) */
    val AccentSoft = Color(0xFFEFF4FF)
    val AccentRing = Color(0x4D2563EB)
    /** 주요 버튼의 옅은 세로 그라데이션: 위 → 아래 (흰 글자 위쪽 4.6:1 · 아래쪽 5.2:1) */
    val AccentHi = Color(0xFF2F6EEE)
    val AccentLo = Color(0xFF2563EB)
    /** 내 차 둘레 빛 (지도) */
    val AccentGlow = Color(0xFF3D8BFF)
    /** 살아 있는 것 둘레의 옅은 고리 (듣는 중인 마이크) — glow-live */
    val LiveRing = Color(0x242563EB)
    val OnAccent = Color(0xFFFFFFFF)

    /** '졸려요' = 밤·달 */
    val Night = Color(0xFF5B21B6)
    val NightSoft = Color(0xFFF3EEFC)
    val NightLine = Color(0x475B21B6)

    /** 주의: 연료 부족 · 간격 벌어짐 */
    val Warn = Color(0xFF9A5B00)
    val WarnSub = Color(0xFF7A5A1E)
    val WarnSoft = Color(0xFFFFF6E5)
    val WarnIcon = Color(0xFFC27C0E)
    val WarnLine = Color(0xFFE5AC3C)

    /** 위험: 도움 요청 · 차에 문제 */
    val Danger = Color(0xFFC2362B)
    val DangerSoft = Color(0xFFFDEEEC)

    /** 정상 · 실시간 연결됨 */
    val Good = Color(0xFF1B7F4F)
    val GoodSoft = Color(0xFFEEF8F2)

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

    val Scrim = Color(0x52101828)
    /** 음성 자막 · 알림 띠: 흰색 + 그림자 한 단계 */
    val CaptionBg = Color(0xFFFFFFFF)
    /** 뜨는 층 그림자 색 (shadow-float) */
    val FloatShadow = Color(0xFF101828)
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

/**
 * 글자 단계 (휴대폰 웹앱과 같은 이름 · 차량은 한 단계씩 크게).
 * 숫자 3단계(88 · 56 · 40) + 글자 7단계(30 · 24 · 22 · 20 · 18 · 16). 운전 중에 읽는 글자는 18 이상,
 * 16 은 정차 중에 읽는 보조 설명에만. 굵기는 Medium(본문) · Bold(제목 · 이름) · ExtraBold(숫자) 셋만 쓴다.
 */
object RtType {
    /** 아주 큰 숫자: 집결지 도착 5/6 */
    val NumXL = KoreanTextStyle.copy(fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.8).sp, lineHeight = 92.sp)
    /** 큰 숫자: 준비 완료 4/6 · 투표 3/6 */
    val NumL = KoreanTextStyle.copy(fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.1).sp, lineHeight = 60.sp)
    /** 칸 안 숫자: 다음 안내 거리 · 휴식 시간 */
    val NumM = KoreanTextStyle.copy(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp, lineHeight = 46.sp)
    /** 알림 한 문장 (“민지님이 졸리대요”) — 운전 중 한눈에 */
    val Display = KoreanTextStyle.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, lineHeight = 48.sp)
    /** 화면 제목 · 집결지 이름 */
    val Title = KoreanTextStyle.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, lineHeight = 38.sp)
    /** 묶음 제목 · 중요한 값 */
    val Headline = KoreanTextStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp, lineHeight = 32.sp)
    /** 버튼 글자 */
    val Button = KoreanTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
    /** 본문 · 소식 */
    val Body = KoreanTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp)
    /** 본문 굵게: 이름 · 고르는 칸 글자 */
    val BodyStrong = KoreanTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
    /** 묶음 이름표 (회색) · 작은 제목 */
    val Label = KoreanTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
    /** 운전 중에도 읽는 보조 정보 */
    val Meta = KoreanTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
    /** 보조 설명 (정차 중에 읽는 것) */
    val Caption = KoreanTextStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    /** 보조 설명 굵게: 칩 · 작은 이름표 */
    val CaptionStrong = KoreanTextStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)

    /** 예전 이름 (화면 코드 호환) */
    val Heading get() = Headline
    val Micro get() = CaptionStrong
}

/** 간격 · 모서리 · 크기 (1600×900 설계 기준 dp). 모서리는 8 · 14 · 20 · 완전 원 넷만 */
object RtDimens {
    val ScreenPaddingH = 24.dp
    val ScreenPaddingTop = 16.dp
    val ScreenPaddingBottom = 20.dp
    val Gap = 16.dp
    val GapSmall = 12.dp
    /** 큰 면 (지도 · 뜨는 층) */
    val CardRadius = 20.dp
    /** 고르는 칸 · 버튼 · 작은 면 */
    val InnerRadius = 14.dp
    val ButtonRadius = 14.dp
    /** 칩 · 막대 */
    val ChipRadius = 8.dp
    val CardPaddingH = 24.dp
    val CardPaddingV = 20.dp
    val HeaderHeight = 64.dp
    val BottomBarHeight = 112.dp
    val RightColumnWidth = 552.dp
    /** 운전 중 누르는 버튼의 최소 크기 */
    val TouchMin = 64.dp
    val ButtonHeight = 76.dp
}

/** Material 부품(버튼 · 대화상자 …) 안의 글자도 Asta Sans 로 */
private val RtTypography = Typography().run {
    fun TextStyle.asta() = copy(fontFamily = AstaSans)
    copy(
        displayLarge = displayLarge.asta(), displayMedium = displayMedium.asta(), displaySmall = displaySmall.asta(),
        headlineLarge = headlineLarge.asta(), headlineMedium = headlineMedium.asta(), headlineSmall = headlineSmall.asta(),
        titleLarge = titleLarge.asta(), titleMedium = titleMedium.asta(), titleSmall = titleSmall.asta(),
        bodyLarge = bodyLarge.asta(), bodyMedium = bodyMedium.asta(), bodySmall = bodySmall.asta(),
        labelLarge = labelLarge.asta(), labelMedium = labelMedium.asta(), labelSmall = labelSmall.asta(),
    )
}

@Composable
fun RunTogetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = RtTypography,
        colorScheme = lightColorScheme(
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
