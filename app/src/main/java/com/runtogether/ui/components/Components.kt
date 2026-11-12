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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.domain.Member
import com.runtogether.domain.StatusSignal
import com.runtogether.ui.theme.RtColors

/** 흰 카드 (시안의 큰 둥근 패널) */
@Composable
fun RtCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 34.dp, vertical = 30.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(RtColors.Card)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, color: Color = RtColors.Accent) {
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
) {
    val color = Color(member.color)
    val initial = member.name.take(1)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (dashed) {
            Canvas(Modifier.size(size)) {
                drawCircle(Color.White)
                drawCircle(
                    color,
                    radius = this.size.minDimension / 2 - 1.5.dp.toPx(),
                    style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 6f))),
                )
            }
            Text(initial, color = color, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold)
        } else {
            val ringModifier = if (ring) Modifier.border(3.dp, color.copy(alpha = 0.28f), CircleShape) else Modifier
            Box(
                Modifier
                    .size(size)
                    .then(ringModifier)
                    .padding(if (ring) 3.dp else 0.dp)
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

@Composable
fun HeaderPill(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(1.dp, RtColors.Line, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = RtColors.Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = RtColors.Ink2)
    }
}

@Composable
fun AppHeader(groupName: String, subtitle: String, pillIcon: ImageVector, pillText: String) {
    Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoMark(Modifier.size(44.dp))
        Spacer(Modifier.width(12.dp))
        Text("같이 달려", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp)
        Spacer(Modifier.width(20.dp))
        Box(Modifier.width(1.dp).height(36.dp).background(RtColors.Line2))
        Spacer(Modifier.width(20.dp))
        Column {
            Text(groupName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 17.sp, color = RtColors.Sub)
        }
        Spacer(Modifier.weight(1f))
        HeaderPill(pillIcon, pillText)
    }
}

/** 큰 주황 버튼 */
@Composable
fun PrimaryButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RtColors.Accent, contentColor = Color.White),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

/** 카드 안의 안내 줄 (주의면 호박색) */
@Composable
fun NoticeRow(icon: ImageVector, text: String, warning: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (warning) RtColors.WarnSoft else RtColors.AccentSoft)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (warning) RtColors.WarnIcon else RtColors.Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (warning) RtColors.Warn else RtColors.Ink)
    }
}

/** '읽어 줌' 표시 */
@Composable
fun ReadAloudMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = RtColors.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text("읽어 줌", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RtColors.Accent)
    }
}

/** 주행 중에 쓰는 원터치 상황 공유 버튼 4개 */
@Composable
fun StatusButtons(onSignal: (StatusSignal) -> Unit, modifier: Modifier = Modifier) {
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
            StatusButton(StatusSignal.SLEEPY, Icons.Outlined.Bedtime, night = true, onSignal = onSignal, modifier = Modifier.weight(1f))
            StatusButton(StatusSignal.REST, Icons.Outlined.LocalCafe, night = false, onSignal = onSignal, modifier = Modifier.weight(1f))
            StatusButton(StatusSignal.TOILET, Icons.Outlined.Wc, night = false, onSignal = onSignal, modifier = Modifier.weight(1f))
            StatusButton(StatusSignal.ENERGY, Icons.Outlined.LocalGasStation, night = false, onSignal = onSignal, modifier = Modifier.weight(1f))
        }
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
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .height(88.dp)
            .clip(shape)
            .background(if (night) RtColors.NightSoft else Color.White)
            .border(1.dp, if (night) RtColors.NightLine else RtColors.Line, shape)
            .clickable(role = Role.Button, onClickLabel = signal.label) { onSignal(signal) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (night) RtColors.Night else RtColors.Accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(5.dp))
        // 한 줄에 다 들어가게 잡되, 차마다 글꼴이 달라 넘치면 말줄임 대신 어절 단위로 두 줄
        Text(
            signal.label,
            color = if (night) RtColors.Night else RtColors.Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
    }
}
