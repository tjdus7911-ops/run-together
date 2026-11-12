package com.runtogether.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.state.DriveUiState
import com.runtogether.state.formatClock
import com.runtogether.state.formatDuration
import com.runtogether.state.formatKm
import com.runtogether.ui.components.MemberAvatar
import com.runtogether.ui.components.PrimaryButton
import com.runtogether.ui.components.RtCard
import com.runtogether.ui.components.SectionLabel
import com.runtogether.ui.theme.RtColors

/** ③ 집결지 도착 — 도착 현황 · 주유 자동 확인 · 정차 중 모드 */
@Composable
fun RegroupScreen(state: DriveUiState, onDepart: () -> Unit, onParkedAction: (String) -> Unit) {
    val info = state.regroup
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        RtCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(horizontal = 36.dp, vertical = 32.dp)) {
            SectionLabel(if (info != null) "${info.stopLabel} · ${info.stopName}" else "집결지")
            Spacer(Modifier.height(22.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 104.sp, fontWeight = FontWeight.Bold, letterSpacing = (-3).sp)) {
                        append("${state.arrivedCount}")
                    }
                    withStyle(SpanStyle(fontSize = 104.sp, fontWeight = FontWeight.Bold, color = RtColors.Muted, letterSpacing = (-3).sp)) {
                        append("/${state.members.size}")
                    }
                    withStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Black)) { append(" 도착") }
                },
                lineHeight = 108.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.members.forEach { m ->
                    MemberAvatar(m.member, 56.dp, dashed = !m.arrived)
                }
            }
            val pending = info?.pendingMemberId?.let { state.member(it) }
            if (pending != null) {
                Spacer(Modifier.height(18.dp))
                Text("${pending.name}님이 약 ${info?.pendingEtaMinutes ?: 1}분 뒤 도착해요", fontSize = 21.sp, color = RtColors.Ink2)
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = RtColors.Divider)
            Spacer(Modifier.height(24.dp))
            if (info != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatTile("휴식", "${info.restMinutes}분", "${formatClock(info.departClock)} 출발 예정", Modifier.weight(1f))
                    val refuel = info.refuelMemberId?.let { state.member(it) }
                    if (refuel != null) {
                        StatTile("${refuel.name}님 주유 완료", "${info.refuelFrom}% → ${info.refuelTo}%", "차량 연료량으로 자동 확인", Modifier.weight(1f))
                    }
                    val late = info.lateJoinMemberId?.let { state.member(it) }
                    if (late != null) {
                        StatTile("${late.name}님 합류", "+${info.lateJoinMinutes}분", "다른 길로 왔지만 함께 도착", Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            CourseStrip(state)
        }

        RtCard(Modifier.width(600.dp).fillMaxHeight(), padding = PaddingValues(horizontal = 32.dp, vertical = 28.dp)) {
            val next = state.nextStop
            if (next != null && info != null) {
                val minutes = state.minutesTo(next.stop)
                Text("다음 구간", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RtColors.Sub)
                Text(next.stop.name, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text(
                    "${formatKm(state.distanceTo(next.stop))}km · 약 ${formatDuration(minutes)} · ${formatClock(info.departClock + minutes)} 도착 예정",
                    fontSize = 20.sp,
                    color = RtColors.Sub,
                )
                Spacer(Modifier.height(22.dp))
                HorizontalDivider(color = RtColors.Divider)
                Spacer(Modifier.height(20.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = RtColors.Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("정차 중에만 열려요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RtColors.Accent)
            }
            Text("주행을 시작하면 자동으로 닫혀요", fontSize = 17.sp, color = RtColors.Sub)
            Spacer(Modifier.height(16.dp))
            ParkedWideButton(
                label = "코스 바꾸기",
                caption = "다음 집결지·경유지를 바꿔 모두의 내비에 반영",
                icon = Icons.Outlined.Directions,
                onClick = onParkedAction,
            )
            Spacer(Modifier.weight(1f))
            if (info != null) {
                val pending = info.pendingMemberId?.let { state.member(it) }
                Text(
                    "출발 예정 ${formatClock(info.departClock)}" + if (pending != null) " · ${pending.name}님이 도착하면 알려 드릴게요" else "",
                    fontSize = 16.sp,
                    color = RtColors.Sub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }
            PrimaryButton("출발 안내 보내기", Icons.AutoMirrored.Outlined.VolumeUp, onDepart, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RtColors.Panel)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RtColors.Ink2)
        Text(value, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(caption, fontSize = 16.sp, color = RtColors.Sub)
    }
}

/** 정차 중에만 누를 수 있는 넓은 버튼 */
@Composable
private fun ParkedWideButton(label: String, caption: String, icon: ImageVector, onClick: (String) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, RtColors.Line, shape)
            .clickable(role = Role.Button, onClickLabel = label) { onClick(label) }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = RtColors.Ink, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(caption, fontSize = 16.sp, color = RtColors.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = RtColors.Faint, modifier = Modifier.size(28.dp))
    }
}

private val DOT = 30.dp

/** 오늘 코스에서 지금 어디쯤인지 — 지나온 구간은 주황, 남은 구간은 회색 */
@Composable
private fun CourseStrip(state: DriveUiState) {
    val stops = state.course
    if (stops.size < 2) return
    val here = (state.nextStopIndex - 1).coerceIn(0, stops.lastIndex)
    Column(Modifier.fillMaxWidth()) {
        Text("오늘 코스", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RtColors.Sub)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().height(DOT)) {
                val step = size.width / stops.size
                val y = size.height / 2
                val inset = DOT.toPx() / 2 + 8.dp.toPx()
                for (i in 0 until stops.lastIndex) {
                    drawLine(
                        color = if (i < here) RtColors.Accent else RtColors.Line2,
                        start = Offset(step * (i + 0.5f) + inset, y),
                        end = Offset(step * (i + 1.5f) - inset, y),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                stops.forEachIndexed { i, planned ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        CourseDot(passed = i < here, current = i == here, destination = i == stops.lastIndex)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            planned.stop.shortName,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (i <= here) RtColors.Ink else RtColors.Sub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (i == here) "지금 여기" else planned.label,
                            fontSize = 15.sp,
                            fontWeight = if (i == here) FontWeight.Bold else FontWeight.Medium,
                            color = if (i == here) RtColors.Accent else RtColors.Faint,
                        )
                    }
                }
            }
        }
    }
}

/** 체크인 화면의 코스 타임라인과 같은 표시 (지나온 곳은 체크) */
@Composable
private fun CourseDot(passed: Boolean, current: Boolean, destination: Boolean) {
    when {
        current -> Box(
            Modifier.size(DOT).border(6.dp, RtColors.AccentRing, CircleShape).padding(6.dp).clip(CircleShape).background(RtColors.Accent),
        )
        passed -> Box(Modifier.size(DOT).clip(CircleShape).background(RtColors.Accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        destination -> Box(
            Modifier.size(DOT).clip(RoundedCornerShape(8.dp)).background(RtColors.Line2).padding(5.dp).clip(RoundedCornerShape(4.dp)).background(RtColors.Ink),
        )
        else -> Box(Modifier.size(DOT).clip(CircleShape).background(Color.White).border(5.dp, RtColors.Accent, CircleShape))
    }
}
