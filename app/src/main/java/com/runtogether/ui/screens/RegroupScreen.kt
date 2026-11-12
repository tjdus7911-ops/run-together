package com.runtogether.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        }

        RtCard(Modifier.width(600.dp).fillMaxHeight(), padding = PaddingValues(horizontal = 32.dp, vertical = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = RtColors.Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("정차 중에만 열려요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RtColors.Accent)
            }
            Text("주행을 시작하면 자동으로 닫혀요", fontSize = 17.sp, color = RtColors.Sub)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ParkedButton("사진 올리기", Icons.Outlined.PhotoCamera, onParkedAction, Modifier.weight(1f))
                ParkedButton("모임 채팅", Icons.Outlined.ChatBubbleOutline, onParkedAction, Modifier.weight(1f))
                ParkedButton("코스 바꾸기", Icons.Outlined.Directions, onParkedAction, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = RtColors.Divider)
            Spacer(Modifier.height(20.dp))
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
            }
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

@Composable
private fun ParkedButton(label: String, icon: ImageVector, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .height(108.dp)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, RtColors.Line, shape)
            .clickable(role = Role.Button) { onClick(label) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = RtColors.Ink, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
