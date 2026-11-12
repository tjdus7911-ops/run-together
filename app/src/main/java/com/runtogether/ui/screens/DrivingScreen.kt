package com.runtogether.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.domain.GroupGap
import com.runtogether.domain.MemberState
import com.runtogether.domain.StatusSignal
import com.runtogether.state.DriveUiState
import com.runtogether.state.formatClock
import com.runtogether.state.formatKm
import com.runtogether.ui.components.CaptionBar
import com.runtogether.ui.components.MemberAvatar
import com.runtogether.ui.components.OutlineTag
import com.runtogether.ui.components.ReadAloudMark
import com.runtogether.ui.components.RtCard
import com.runtogether.ui.components.SectionLabel
import com.runtogether.ui.components.StatusButtons
import com.runtogether.ui.components.Tag
import com.runtogether.ui.theme.RtColors
import kotlin.math.roundToInt

/** ② 주행 중 — 모임 간격 보기 · 다음 집결지 · 상황 공유 버튼 (④ 상황 공유 알림·투표가 이 위에 뜬다) */
@Composable
fun DrivingScreen(
    state: DriveUiState,
    gap: GroupGap,
    caption: String?,
    onSignal: (StatusSignal) -> Unit,
    onVote: (Boolean) -> Unit,
    onDismissAlert: () -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(RtColors.MapLand),
        ) {
            GroupMap(state, gap, Modifier.fillMaxSize())
            GapCard(
                state,
                gap,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
                    .fillMaxWidth(),
            )
            val alert = state.alert
            if (alert != null) {
                Box(Modifier.fillMaxSize().background(RtColors.Scrim))
                AlertCard(state, alert, onVote, onDismissAlert, Modifier.align(Alignment.Center))
            }
            // 음성 안내 자막은 지도 위쪽 가운데 (오른쪽 투표 패널을 가리지 않게)
            caption?.let { CaptionBar(it, Modifier.align(Alignment.TopCenter).padding(top = 18.dp, start = 18.dp, end = 18.dp)) }
        }
        Column(Modifier.width(540.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.vote != null) {
                VotePanel(state, Modifier.weight(1f).fillMaxWidth())
                state.voteResult?.let { ResultCard(it) }
            } else {
                NextStopCard(state)
                FeedCard(state, Modifier.weight(1f).fillMaxWidth())
            }
            StatusButtons(onSignal)
        }
    }
}

/** 지도 위 경로 (화면 크기에 맞춘 비율 좌표) */
private fun routePath(w: Float, h: Float): Path = Path().apply {
    moveTo(-0.01f * w, 0.711f * h)
    cubicTo(0.121f * w, 0.676f * h, 0.282f * w, 0.588f * h, 0.423f * w, 0.508f * h)
    cubicTo(0.564f * w, 0.428f * h, 0.645f * w, 0.401f * h, 0.726f * w, 0.321f * h)
    cubicTo(0.807f * w, 0.241f * h, 0.847f * w, 0.174f * h, 0.907f * w, 0.127f * h)
}

private fun roadPaths(w: Float, h: Float): List<Path> = listOf(
    Path().apply { moveTo(0f, 0.251f * h); cubicTo(0.22f * w, 0.235f * h, 0.42f * w, 0.286f * h, w, 0.225f * h) },
    Path().apply { moveTo(0.355f * w, 0f); cubicTo(0.347f * w, 0.28f * h, 0.387f * w, 0.63f * h, 0.333f * w, h) },
    Path().apply { moveTo(0.835f * w, 0f); cubicTo(0.812f * w, 0.334f * h, 0.875f * w, 0.642f * h, 0.843f * w, h) },
    Path().apply { moveTo(0.433f * w, 0.501f * h); cubicTo(0.454f * w, 0.401f * h, 0.464f * w, 0.294f * h, 0.524f * w, 0.227f * h) },
)

@Composable
private fun GroupMap(state: DriveUiState, gap: GroupGap, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val route = remember(wPx, hPx) { routePath(wPx, hPx) }
        val roads = remember(wPx, hPx) { roadPaths(wPx, hPx) }
        val measure = remember(route) { PathMeasure().apply { setPath(route, false) } }
        val length = measure.length

        val onRoute = state.members.filter { it.onPlannedRoute }
        val tail = onRoute.minOfOrNull { it.progressKm } ?: 0.0
        val lead = onRoute.maxOfOrNull { it.progressKm } ?: 0.0
        val windowStart = tail - 1.0
        val windowEnd = maxOf(lead + 3.0, windowStart + 6.0)
        fun positionOf(km: Double): Offset {
            val f = (0.06 + 0.78 * ((km - windowStart) / (windowEnd - windowStart))).coerceIn(0.0, 0.98)
            return measure.getPosition((length * f).toFloat())
        }

        Canvas(Modifier.fillMaxSize()) {
            drawOval(RtColors.MapPark, topLeft = Offset(0.04f * size.width, 0.05f * size.height), size = Size(0.27f * size.width, 0.25f * size.height))
            drawOval(RtColors.MapPark, topLeft = Offset(0.62f * size.width, 0.52f * size.height), size = Size(0.34f * size.width, 0.21f * size.height))
            roads.forEach { drawPath(it, RtColors.MapRoadEdge, style = Stroke(16.dp.toPx(), cap = StrokeCap.Round)) }
            roads.forEach { drawPath(it, Color.White, style = Stroke(11.dp.toPx(), cap = StrokeCap.Round)) }
            drawPath(route, RtColors.MapHighwayEdge, style = Stroke(26.dp.toPx(), cap = StrokeCap.Round))
            drawPath(route, Color.White, style = Stroke(19.dp.toPx(), cap = StrokeCap.Round))
            drawPath(route, RtColors.Accent, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
        }

        // 다음 집결지 깃발
        val next = state.nextStop
        val flag = measure.getPosition(length * 0.985f)
        if (next != null) {
            MapPin(flag, 52.dp) {
                Box(
                    Modifier.fillMaxSize().clip(CircleShape).background(RtColors.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            MapLabel(Offset(flag.x - with(density) { 250.dp.toPx() }, flag.y + with(density) { 6.dp.toPx() })) {
                Row(
                    Modifier
                        .shadow(6.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(next.stop.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("${formatKm(state.distanceTo(next.stop))}km", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = RtColors.Sub)
                }
            }
        }

        // 다른 길로 가는 차 (경로 옆에 점선으로)
        state.members.filter { !it.onPlannedRoute }.forEach { m ->
            val p = positionOf(m.progressKm)
            val shifted = Offset(p.x - with(density) { 60.dp.toPx() }, p.y - with(density) { 70.dp.toPx() })
            MapPin(shifted, 44.dp) { MemberAvatar(m.member, 44.dp, dashed = true) }
            MapLabel(Offset(shifted.x + with(density) { 28.dp.toPx() }, shifted.y - with(density) { 14.dp.toPx() })) {
                Tag("다른 길", Color.White, Color(m.member.color), fontSize = 15)
            }
        }

        // 가장 많이 벌어진 구간
        if (gap.largestGapKm >= 1.0) {
            val behind = state.stateOf(gap.largestGapBehindId ?: "")
            val ahead = state.stateOf(gap.largestGapAheadId ?: "")
            if (behind != null && ahead != null) {
                val mid = positionOf((behind.progressKm + ahead.progressKm) / 2)
                MapLabel(Offset(mid.x - with(density) { 10.dp.toPx() }, mid.y + with(density) { 24.dp.toPx() })) {
                    Tag("${formatKm(gap.largestGapKm)}km 벌어짐", RtColors.WarnSoft, RtColors.Warn)
                }
            }
        }

        // 같은 길로 가는 차들 (뒤에서부터 그려서 선두가 위에 오게)
        onRoute.sortedBy { it.progressKm }.forEach { m ->
            val isMe = m.member.id == state.meId
            val p = positionOf(m.progressKm)
            val pinSize = if (isMe) 56.dp else 44.dp
            MapPin(p, pinSize) {
                Box(Modifier.fillMaxSize().shadow(8.dp, CircleShape).clip(CircleShape).background(Color.White).padding(3.dp)) {
                    MemberAvatar(m.member, pinSize - 6.dp)
                }
            }
            if (m.member.id == gap.leadId) {
                MapLabel(Offset(p.x + with(density) { 32.dp.toPx() }, p.y + with(density) { 10.dp.toPx() })) {
                    Tag("선두", Color.White, RtColors.Accent, fontSize = 15)
                }
            }
        }
    }
}

/** 좌표(px)를 중심으로 놓는 지도 표시 */
@Composable
private fun MapPin(center: Offset, size: Dp, content: @Composable () -> Unit) {
    val half = with(LocalDensity.current) { (size / 2).toPx() }
    Box(
        Modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .size(size),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 좌표(px)를 왼쪽 위로 놓는 지도 글자 */
@Composable
private fun MapLabel(topLeft: Offset, content: @Composable () -> Unit) {
    Box(Modifier.offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }) { content() }
}

@Composable
private fun GapCard(state: DriveUiState, gap: GroupGap, modifier: Modifier = Modifier) {
    Column(
        modifier
            .shadow(14.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("모임 간격", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            Text("${formatKm(gap.spreadKm)}km", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            val same = state.members.count { it.onPlannedRoute }
            val other = state.members.size - same
            Text(
                if (other > 0) "${same}대 같은 길 · ${other}대 다른 길" else "${same}대 모두 같은 길",
                fontSize = 18.sp,
                color = RtColors.Sub,
            )
            Spacer(Modifier.weight(1f))
            val offRoute = gap.offRouteIds.firstOrNull()?.let { state.member(it) }
            val next = state.nextStop
            if (offRoute != null && next != null) {
                OutlineTag(
                    "${offRoute.name} · 다른 길로 ${next.stop.shortName}에서 합류",
                    color = Color(offRoute.color),
                    dashed = true,
                )
            }
        }
        GapBar(state, gap)
    }
}

@Composable
private fun GapBar(state: DriveUiState, gap: GroupGap) {
    val onRoute = state.members.filter { it.onPlannedRoute }.sortedBy { it.progressKm }
    if (onRoute.isEmpty()) return
    val tailKm = onRoute.first().progressKm
    val span = (onRoute.last().progressKm - tailKm).coerceAtLeast(0.1)
    BoxWithConstraints(Modifier.fillMaxWidth().height(88.dp)) {
        val pad = 40.dp
        val usable = maxWidth - pad * 2
        fun xOf(km: Double): Dp = pad + usable * ((km - tailKm) / span).toFloat()

        Box(
            Modifier
                .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RtColors.AccentSoft),
        )
        if (gap.largestGapKm >= 1.0) {
            val behind = onRoute.firstOrNull { it.member.id == gap.largestGapBehindId }
            val ahead = onRoute.firstOrNull { it.member.id == gap.largestGapAheadId }
            if (behind != null && ahead != null) {
                val x1 = xOf(behind.progressKm) + 20.dp
                val x2 = xOf(ahead.progressKm) - 20.dp
                if (x2 > x1) {
                    Canvas(Modifier.offset(x = x1, y = 22.dp).width(x2 - x1).height(4.dp)) {
                        drawLine(
                            RtColors.WarnLine,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f)),
                        )
                    }
                }
                Box(Modifier.offset(x = (x1 + x2) / 2 - 60.dp, y = 44.dp)) {
                    Tag("${formatKm(gap.largestGapKm)}km 벌어짐", RtColors.WarnSoft, RtColors.Warn)
                }
            }
        }
        onRoute.forEach { m ->
            val isMe = m.member.id == state.meId
            val label = when (m.member.id) {
                gap.leadId -> "${m.member.name} · 선두"
                gap.tailId -> "${m.member.name} · 후미"
                else -> m.member.name
            }
            Column(
                Modifier.offset(x = xOf(m.progressKm) - 45.dp, y = if (isMe) 2.dp else 6.dp).width(90.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MemberAvatar(m.member, if (isMe) 48.dp else 40.dp, ring = isMe)
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 16.sp, fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal, color = RtColors.Ink2, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NextStopCard(state: DriveUiState) {
    val next = state.nextStop ?: return
    RtCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 30.dp, vertical = 28.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("다음 집결지")
            Spacer(Modifier.weight(1f))
            Tag("모두의 내비에 추가됨", RtColors.AccentSoft, RtColors.Accent, icon = Icons.Outlined.Check)
        }
        Spacer(Modifier.height(16.dp))
        Text(next.stop.name, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
        val km = state.distanceTo(next.stop)
        val minutes = state.minutesTo(next.stop)
        Text(
            "${formatKm(km)}km · 약 ${minutes}분 · ${formatClock(state.clockMin + minutes)} 도착",
            fontSize = 21.sp,
            color = RtColors.Sub,
        )
        // 연료·배터리가 가장 적은 차가 있으면 이유를 함께 보여 준다
        val low = state.members
            .filter { (it.vehicle.energyPercent ?: 100) <= 15 && it.vehicle.rangeKm != null }
            .minByOrNull { it.vehicle.rangeKm ?: Double.MAX_VALUE }
        if (low != null) {
            Spacer(Modifier.height(18.dp))
            LowEnergyBox(low, isEv = low.member.isEv)
        }
    }
}

@Composable
private fun LowEnergyBox(m: MemberState, isEv: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RtColors.WarnSoft)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Icon(Icons.Outlined.LocalGasStation, contentDescription = null, tint = RtColors.WarnIcon, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                "${m.member.name}님 ${if (isEv) "배터리" else "연료"} ${m.vehicle.energyPercent}% · 주행 가능 약 ${formatKm(m.vehicle.rangeKm ?: 0.0)}km",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = RtColors.Warn,
            )
            Text(
                "모임에서 가장 적어서 ${if (isEv) "충전소" else "주유소"}가 있는 첫 휴게소로 골랐어요",
                fontSize = 17.sp,
                color = RtColors.WarnSub,
            )
        }
    }
}

@Composable
private fun FeedCard(state: DriveUiState, modifier: Modifier = Modifier) {
    RtCard(modifier, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 30.dp, vertical = 24.dp)) {
        Text("모임 소식", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RtColors.Ink2)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            state.feed.take(3).forEach { item ->
                val member = state.member(item.memberId) ?: return@forEach
                Row(verticalAlignment = Alignment.Top) {
                    MemberAvatar(member, 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(member.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("${item.whenText} · ${item.kindText}", fontSize = 16.sp, color = RtColors.Muted)
                        }
                        Text(item.text, fontSize = 20.sp)
                    }
                    if (item.readAloud) ReadAloudMark()
                }
            }
            if (state.feed.isEmpty()) {
                Text("아직 소식이 없어요", fontSize = 18.sp, color = RtColors.Muted)
            }
        }
    }
}
