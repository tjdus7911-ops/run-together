package com.runtogether.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.domain.EnergyKind
import com.runtogether.domain.JoinMode
import com.runtogether.domain.MemberState
import com.runtogether.domain.Readiness
import com.runtogether.state.DriveUiState
import com.runtogether.state.PlannedStop
import com.runtogether.state.formatClock
import com.runtogether.state.formatDuration
import com.runtogether.state.formatKm
import com.runtogether.ui.components.MemberAvatar
import com.runtogether.ui.components.NoticeRow
import com.runtogether.ui.components.OutlineTag
import com.runtogether.ui.components.PrimaryButton
import com.runtogether.ui.components.RtCard
import com.runtogether.ui.components.SectionLabel
import com.runtogether.ui.components.Tag
import com.runtogether.ui.components.VerticalSectionDivider
import com.runtogether.ui.theme.RtColors
import com.runtogether.ui.theme.RtType

/** ① 출발 전 — 오늘 코스 확인 · 시동 체크인 · 출발 안내 */
@Composable
fun CheckinScreen(state: DriveUiState, onDepart: () -> Unit) {
    // 두 구역은 상자 없이, 사이에 가는 세로선 하나
    Row(Modifier.fillMaxSize().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        RtCard(Modifier.weight(1f).fillMaxHeight().padding(bottom = 4.dp)) {
            val first = state.course.first()
            val last = state.course.last()
            val totalKm = last.stop.kmFromStart - first.stop.kmFromStart
            val restCount = state.course.count { it.restMinutes > 0 }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("오늘 코스")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "총 ${formatKm(totalKm)}km · 약 ${formatDuration(last.plannedClock - first.plannedClock)}",
                        style = RtType.Title,
                    )
                }
                Text("휴식 ${restCount}번 포함", style = RtType.Meta, color = RtColors.Sub)
            }
            Spacer(Modifier.height(22.dp))
            state.course.forEachIndexed { i, stop ->
                CourseRow(stop, i, state.course)
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HorizontalDivider(color = RtColors.Divider)
                state.notices.forEach { n ->
                    NoticeRow(if (n.warning) Icons.Outlined.LocalGasStation else Icons.Outlined.Check, n.text, n.warning)
                }
            }
        }

        VerticalSectionDivider()
        RtCard(Modifier.width(600.dp).fillMaxHeight().padding(bottom = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("출발 준비")
                    Text(
                        buildAnnotatedString {
                            withStyle(RtType.NumL.toSpanStyle()) { append("${state.readyCount}") }
                            withStyle(RtType.Title.toSpanStyle().copy(color = RtColors.Muted)) { append("/${state.members.size}") }
                            withStyle(RtType.Headline.toSpanStyle()) { append("  준비 완료") }
                        },
                    )
                }
                Text("시동을 켜면 자동으로 체크", style = RtType.Meta, color = RtColors.Sub, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            state.members.forEachIndexed { i, m ->
                ReadinessRow(m, isMe = m.member.id == state.meId)
                if (i < state.members.lastIndex) HorizontalDivider(color = RtColors.Divider)
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton("출발 안내 보내기", Icons.AutoMirrored.Outlined.VolumeUp, onDepart, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(
                "안내를 보내면 모임 위치 공유가 시작되고, 모임이 끝나면 자동 삭제돼요",
                style = RtType.Caption,
                color = RtColors.Sub,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CourseRow(item: PlannedStop, index: Int, course: List<PlannedStop>) {
    val isFirst = index == 0
    val isLast = index == course.lastIndex
    val segmentKm = if (isFirst) 0.0 else item.stop.kmFromStart - course[index - 1].stop.kmFromStart
    val caption = when {
        isFirst -> "${item.label} · ${formatClock(item.plannedClock)}"
        isLast -> "${item.label} · 약 ${formatKm(segmentKm)}km · ${formatClock(item.plannedClock)}"
        else -> "${item.label} · 약 ${formatKm(segmentKm)}km · ${formatClock(item.plannedClock)} 도착"
    }
    Row(Modifier.fillMaxWidth().height(if (isLast) 72.dp else 92.dp)) {
        // 타임라인 표시
        Box(Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 34.dp, bottom = 2.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(RtColors.Line),
                )
            }
            when {
                isFirst -> Box(
                    Modifier.size(30.dp).clip(CircleShape).background(RtColors.LiveRing).padding(6.dp).clip(CircleShape).background(RtColors.Accent),
                )
                isLast -> Box(
                    Modifier.size(30.dp).padding(5.dp).clip(RoundedCornerShape(5.dp)).background(RtColors.Ink),
                )
                else -> Box(Modifier.size(30.dp).padding(3.dp).clip(CircleShape).background(RtColors.Bg).border(3.dp, RtColors.Accent, CircleShape))
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(caption, style = RtType.Meta, color = RtColors.Sub)
            Text(item.stop.name, style = RtType.Headline)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            when {
                isFirst -> Tag("지금 여기", RtColors.AccentSoft, RtColors.AccentText)
                isLast -> Unit
                index == 1 -> {
                    // 첫 집결지는 연료가 가장 적은 차 때문에 고른 곳이라 주유·충전 여부를 보여 준다
                    if (item.stop.hasFuel) OutlineTag("주유")
                    if (item.stop.hasCharger) OutlineTag("충전")
                }
                item.restMinutes > 0 -> OutlineTag("휴식 ${item.restMinutes}분")
                else -> Unit
            }
        }
    }
}

@Composable
private fun ReadinessRow(m: MemberState, isMe: Boolean) {
    Row(Modifier.fillMaxWidth().height(68.dp), verticalAlignment = Alignment.CenterVertically) {
        MemberAvatar(m.member, 44.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(m.member.name, style = RtType.BodyStrong)
            val sub = buildList {
                if (m.member.isLeader) add("모임장")
                add(if (m.member.joinMode == JoinMode.PHONE) "폰 참여" else "차량 연동")
                if (m.member.isEv) add("전기차")
            }.joinToString(" · ")
            Text(sub, style = RtType.Caption, color = RtColors.Muted)
        }
        val pct = m.vehicle.energyPercent
        if (pct != null && m.energyKind == EnergyKind.BATTERY) {
            Tag("배터리 $pct%", if (pct <= 20) RtColors.WarnSoft else RtColors.Panel, if (pct <= 20) RtColors.Warn else RtColors.Ink2, icon = Icons.Outlined.Bolt)
            Spacer(Modifier.width(12.dp))
        } else if (pct != null && pct <= 20) {
            Tag("연료 $pct%", RtColors.WarnSoft, RtColors.Warn, icon = Icons.Outlined.LocalGasStation)
            Spacer(Modifier.width(12.dp))
        }
        when (m.readiness) {
            // 준비된 차는 조용하게(회색 체크), 아직인 차를 굵게 — 눈은 빠진 사람에게 간다
            Readiness.READY -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = RtColors.Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (m.member.joinMode == JoinMode.PHONE) "준비 완료" else "시동 켜짐", style = RtType.Meta, color = RtColors.Sub)
            }
            Readiness.COMING -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = RtColors.Ink2, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("오는 중 · 약 ${m.etaMinutes ?: 1}분", style = RtType.Label, color = RtColors.Ink)
            }
            Readiness.WAITING -> Text(
                if (isMe) "시동을 켜 주세요" else if (m.member.joinMode == JoinMode.PHONE) "준비 버튼 대기" else "시동 대기",
                style = RtType.Label,
                color = if (isMe) RtColors.AccentText else RtColors.Ink,
            )
        }
    }
}
