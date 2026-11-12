package com.runtogether.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.domain.Member
import com.runtogether.domain.StatusSignal
import com.runtogether.ui.theme.RtColors
import com.runtogether.ui.theme.RtDimens
import com.runtogether.ui.theme.RtType

/** 카드 (차콜 면, 큰 둥근 모서리) */
@Composable
fun RtCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 34.dp, vertical = 30.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(RtDimens.CardRadius))
            .background(RtColors.Card)
            .padding(padding),
        content = content,
    )
}

/** 카드 안의 작은 제목 */
@Composable
fun SectionLabel(text: String, color: Color = RtColors.Sub) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
}

/** 같이 달려 로고: 길 하나 위에 이어진 세 점 */
@Composable
fun LogoMark(modifier: Modifier = Modifier, color: Color = RtColors.Accent) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val road = Path().apply {
            moveTo(w * 0.14f, h * 0.70f)
            cubicTo(w * 0.36f, h * 0.70f, w * 0.48f, h * 0.34f, w * 0.86f, h * 0.30f)
        }
        drawPath(road, color, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.16f, h * 0.69f))
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.49f, h * 0.48f))
        drawCircle(color, radius = w * 0.125f, center = Offset(w * 0.84f, h * 0.30f))
    }
}

/** 회원 동그라미 (점선이면 아직 도착 전·다른 길) */
@Composable
fun MemberAvatar(
    member: Member,
    size: Dp = 44.dp,
    dashed: Boolean = false,
    ring: Boolean = false,
    modifier: Modifier = Modifier,
    /** 점선일 때 안쪽 바탕 */
    fill: Color = RtColors.Card,
) {
    val color = Color(member.color)
    val initial = member.name.take(1)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (dashed) {
            Canvas(Modifier.size(size)) {
                drawCircle(fill)
                drawCircle(
                    color,
                    radius = this.size.minDimension / 2 - 1.5.dp.toPx(),
                    style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 6f))),
                )
            }
            Text(initial, color = color, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold)
        } else {
            val ringModifier = if (ring) Modifier.border(3.dp, color.copy(alpha = 0.45f), CircleShape) else Modifier
            Box(
                Modifier
                    .size(size)
                    .then(ringModifier)
                    .padding(if (ring) 4.dp else 0.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(initial, color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 작은 알약 모양 표시 */
@Composable
fun Tag(
    text: String,
    background: Color,
    foreground: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = foreground, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** 테두리만 있는 알약 (점선 가능) */
@Composable
fun OutlineTag(text: String, color: Color = RtColors.Ink2, dashed: Boolean = false, modifier: Modifier = Modifier) {
    val borderColor = if (dashed) color else RtColors.Line2
    Box(
        modifier
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                    style = Stroke(
                        width = (if (dashed) 2 else 1).dp.toPx(),
                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null,
                    ),
                )
            }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** 실제 데이터인지 시연 값인지 (SDK 연동 데이터와 Mock 데이터를 화면에서도 나눈다) */
@Composable
fun SourceBadge(live: Boolean, liveText: String, demoText: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (live) RtColors.GoodSoft else RtColors.Panel)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (live) RtColors.Good else RtColors.Faint))
        Spacer(Modifier.width(6.dp))
        Text(if (live) liveText else demoText, style = RtType.Micro, color = if (live) RtColors.Good else RtColors.Sub, maxLines = 1)
    }
}

@Composable
fun HeaderPill(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(RtColors.Panel)
            .border(1.dp, RtColors.Line, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(9.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = RtColors.Ink2, maxLines = 1)
    }
}

/**
 * 맨 위 줄: 왼쪽 로고·모임, 가운데 [center] (시연 단계), 오른쪽 상태 알약 · 시각 · 연결 상태.
 * [vehicleLive] 면 차 아이콘이 초록 (실차 데이터), 아니면 회색 (시연 값).
 */
@Composable
fun AppHeader(
    groupName: String,
    subtitle: String,
    pillIcon: ImageVector,
    pillText: String,
    center: (@Composable () -> Unit)? = null,
    clock: String? = null,
    vehicleLive: Boolean = false,
) {
    Box(Modifier.fillMaxWidth().height(RtDimens.HeaderHeight)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            LogoMark(Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            Text("같이 달려", fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            Spacer(Modifier.width(18.dp))
            Box(Modifier.width(1.dp).height(30.dp).background(RtColors.Line))
            Spacer(Modifier.width(18.dp))
            Column {
                Text(groupName, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = RtType.Caption, color = RtColors.Sub, maxLines = 1)
            }
        }
        if (center != null) {
            Box(Modifier.align(Alignment.Center)) { center() }
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            HeaderPill(pillIcon, pillText)
            if (clock != null) {
                Spacer(Modifier.width(16.dp))
                Text(clock, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RtColors.Ink)
            }
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Outlined.SignalCellularAlt, contentDescription = null, tint = RtColors.Sub, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Outlined.DirectionsCar,
                contentDescription = if (vehicleLive) "차량 데이터 연결됨" else "시연 데이터",
                tint = if (vehicleLive) RtColors.Good else RtColors.Sub,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 큰 주황 버튼 (화면의 주요 동작 하나에만) */
@Composable
fun PrimaryButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(RtDimens.ButtonHeight),
        shape = RoundedCornerShape(RtDimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = RtColors.Accent, contentColor = RtColors.OnAccent),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** 보조 버튼 (어두운 면 + 테두리). [selected] 면 지금 열려 있는 것 (주황 테두리) */
@Composable
fun SecondaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val shape = RoundedCornerShape(RtDimens.ButtonRadius)
    Row(
        modifier
            .height(RtDimens.ButtonHeight)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(if (selected) RtColors.AccentSoft else RtColors.Raised)
            .border(if (selected) 2.dp else 1.dp, if (selected) RtColors.Accent else RtColors.Line2, shape)
            .clickable(enabled = enabled, onClickLabel = text, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) RtColors.AccentText else RtColors.Ink, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = RtType.Button, color = RtColors.Ink, maxLines = 1)
    }
}

/** 아이콘 하나짜리 버튼 (지도 확대·축소, 닫기). 운전 중에도 누르기 쉽게 64dp */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = RtDimens.TouchMin,
    shape: Shape = RoundedCornerShape(18.dp),
    background: Color = RtColors.Overlay,
    tint: Color = RtColors.Ink,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size * 0.44f))
    }
}

/** 카드 안의 안내 줄 (주의면 노랑) */
@Composable
fun NoticeRow(icon: ImageVector, text: String, warning: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RtDimens.InnerRadius))
            .background(if (warning) RtColors.WarnSoft else RtColors.Panel)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (warning) RtColors.WarnIcon else RtColors.AccentText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (warning) RtColors.Warn else RtColors.Ink)
    }
}

/** '읽어 줌' 표시 */
@Composable
fun ReadAloudMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = RtColors.Sub, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(4.dp))
        Text("읽어 줌", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RtColors.Sub)
    }
}

/**
 * 주행 중에 쓰는 원터치 상황 공유 버튼 4개 (기본 메시지).
 * [vertical] = true 면 지도 오른쪽 가장자리에 세로로 놓는 줄.
 */
@Composable
fun StatusButtons(onSignal: (StatusSignal) -> Unit, modifier: Modifier = Modifier, vertical: Boolean = false) {
    if (vertical) {
        StatusRail(onSignal, modifier)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("상황 공유", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RtColors.Ink2)
            Text("한 번 누르면 모두에게 음성으로 알려요", fontSize = 16.sp, color = RtColors.Sub)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusButton(StatusSignal.SLEEPY, Icons.Outlined.Bedtime, night = true, onSignal = onSignal, modifier = Modifier.weight(1f).height(88.dp))
            StatusButton(StatusSignal.REST, Icons.Outlined.LocalCafe, night = false, onSignal = onSignal, modifier = Modifier.weight(1f).height(88.dp))
            StatusButton(StatusSignal.TOILET, Icons.Outlined.Wc, night = false, onSignal = onSignal, modifier = Modifier.weight(1f).height(88.dp))
            StatusButton(StatusSignal.ENERGY, Icons.Outlined.LocalGasStation, night = false, onSignal = onSignal, modifier = Modifier.weight(1f).height(88.dp))
        }
    }
}

/** 지도 오른쪽 가장자리의 상황 공유 줄 */
@Composable
private fun StatusRail(onSignal: (StatusSignal) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(128.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(RtColors.Overlay)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("상황 공유", style = RtType.Micro, color = RtColors.Sub, modifier = Modifier.padding(top = 4.dp))
        StatusButton(StatusSignal.SLEEPY, Icons.Outlined.Bedtime, night = true, onSignal = onSignal, modifier = Modifier.fillMaxWidth().height(100.dp))
        StatusButton(StatusSignal.REST, Icons.Outlined.LocalCafe, night = false, onSignal = onSignal, modifier = Modifier.fillMaxWidth().height(100.dp))
        StatusButton(StatusSignal.TOILET, Icons.Outlined.Wc, night = false, onSignal = onSignal, modifier = Modifier.fillMaxWidth().height(100.dp))
        StatusButton(StatusSignal.ENERGY, Icons.Outlined.LocalGasStation, night = false, onSignal = onSignal, modifier = Modifier.fillMaxWidth().height(100.dp))
    }
}

@Composable
private fun StatusButton(
    signal: StatusSignal,
    icon: ImageVector,
    night: Boolean,
    onSignal: (StatusSignal) -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (night) RtColors.NightSoft else RtColors.Card)
            .border(1.dp, if (night) RtColors.NightLine else RtColors.Line, shape)
            .clickable(role = Role.Button, onClickLabel = signal.label) { onSignal(signal) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (night) RtColors.Night else RtColors.AccentText, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        // 한 줄에 다 들어가게 잡되, 차마다 글꼴이 달라 넘치면 말줄임 대신 어절 단위로 두 줄
        Text(
            signal.label,
            color = if (night) RtColors.Night else RtColors.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 19.sp,
        )
    }
}

/**
 * 음성 안내 자막 (소리가 안 나는 환경에서도 내용이 보이게).
 * [speaker] 가 있으면 보낸 사람 얼굴을 함께 보여 준다 (받은 내 메시지 · 음성 메시지).
 */
@Composable
fun CaptionBar(text: String, modifier: Modifier = Modifier, speaker: Member? = null) {
    Row(
        modifier
            .widthIn(max = 1100.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(RtColors.CaptionBg)
            .border(1.dp, RtColors.Line, RoundedCornerShape(999.dp))
            .padding(start = if (speaker != null) 12.dp else 24.dp, end = 26.dp, top = 12.dp, bottom = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (speaker != null) {
            MemberAvatar(speaker, 36.dp)
            Spacer(Modifier.width(10.dp))
        }
        Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = RtColors.Ink, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 2, lineHeight = 25.sp)
    }
}

/** 어떤 차인지 드러나지 않는 일반 차량 옆모습 (실제 차량 사진·브랜드 형상을 쓰지 않는다) */
@Composable
fun CarGlyph(
    modifier: Modifier = Modifier,
    body: Color = RtColors.Raised,
    glass: Color = RtColors.Panel,
    wheel: Color = RtColors.Faint,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val shell = Path().apply {
            moveTo(w * 0.04f, h * 0.74f)
            lineTo(w * 0.04f, h * 0.54f)
            cubicTo(w * 0.05f, h * 0.45f, w * 0.10f, h * 0.42f, w * 0.20f, h * 0.41f)
            lineTo(w * 0.31f, h * 0.39f)
            cubicTo(w * 0.36f, h * 0.24f, w * 0.43f, h * 0.13f, w * 0.53f, h * 0.12f)
            lineTo(w * 0.70f, h * 0.12f)
            cubicTo(w * 0.77f, h * 0.13f, w * 0.83f, h * 0.24f, w * 0.87f, h * 0.37f)
            lineTo(w * 0.93f, h * 0.41f)
            cubicTo(w * 0.97f, h * 0.45f, w * 0.97f, h * 0.60f, w * 0.96f, h * 0.74f)
            close()
        }
        drawPath(shell, body)
        val window = Path().apply {
            moveTo(w * 0.36f, h * 0.38f)
            cubicTo(w * 0.40f, h * 0.27f, w * 0.45f, h * 0.19f, w * 0.54f, h * 0.18f)
            lineTo(w * 0.69f, h * 0.18f)
            cubicTo(w * 0.74f, h * 0.19f, w * 0.78f, h * 0.27f, w * 0.81f, h * 0.37f)
            close()
        }
        drawPath(window, glass)
        drawLine(body, Offset(w * 0.585f, h * 0.17f), Offset(w * 0.585f, h * 0.39f), strokeWidth = w * 0.025f)
        for (cx in listOf(0.24f, 0.77f)) {
            drawCircle(wheel, radius = h * 0.17f, center = Offset(w * cx, h * 0.75f))
            drawCircle(body, radius = h * 0.075f, center = Offset(w * cx, h * 0.75f))
        }
    }
}
