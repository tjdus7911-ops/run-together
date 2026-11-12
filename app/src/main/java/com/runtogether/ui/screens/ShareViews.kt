package com.runtogether.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.runtogether.domain.StatusSignal
import com.runtogether.domain.Vote
import com.runtogether.state.DriveUiState
import com.runtogether.state.IncomingAlert
import com.runtogether.state.VoteResult
import com.runtogether.state.formatKm
import com.runtogether.ui.components.MemberAvatar
import com.runtogether.ui.components.RtCard
import com.runtogether.ui.components.SectionLabel
import com.runtogether.ui.components.Tag
import com.runtogether.ui.theme.RtColors

/** ④ 상황 공유 알림: "민지님이 졸리대요" — 큰 버튼 하나로 답하고, 10초 뒤 자동으로 닫힌다 */
@Composable
fun AlertCard(
    state: DriveUiState,
    alert: IncomingAlert,
    onVote: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val from = state.member(alert.fromId) ?: return
    val myVote = state.vote?.voteOf(state.meId)
    Column(
        modifier
            .width(660.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(RtColors.Card)
            .border(1.dp, RtColors.Line, RoundedCornerShape(32.dp))
            .padding(start = 40.dp, end = 40.dp, top = 36.dp, bottom = 30.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MemberAvatar(from, 80.dp)
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val isSleepy = alert.signal == StatusSignal.SLEEPY
                Tag(
                    "방금 · ${alert.signal.label}",
                    if (isSleepy) RtColors.NightSoft else RtColors.AccentSoft,
                    if (isSleepy) RtColors.Night else RtColors.AccentText,
                    icon = if (isSleepy) Icons.Outlined.Bedtime else Icons.Outlined.LocalCafe,
                )
                Text(headline(from.name, alert.signal), fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 48.sp)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(RtColors.Panel)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("모든 차에 음성으로 알렸어요", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = RtColors.Ink2)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(28.dp))
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(28.dp))
        }

        val rec = alert.recommendation
        if (rec != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(RtColors.AccentSoft)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Icon(Icons.Outlined.LocalCafe, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("${rec.stop.name}에서 ${alert.restMinutes}분 쉬었다 갈까요?", fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                    val facilities = buildList {
                        if (rec.stop.hasFuel) add("주유소")
                        if (rec.stop.hasCharger) add("충전소")
                        if (rec.stop.hasFood) add("카페")
                    }.joinToString("·")
                    val reason = if ("가장 가까운 쉴 곳" in rec.reasons) "가장 가까운 쉴 곳" else "추천 쉴 곳"
                    Text(
                        "$reason · ${formatKm(rec.distanceKm)}km · 약 ${rec.etaMinutes}분" + if (facilities.isNotEmpty()) " · $facilities" else "",
                        fontSize = 18.sp,
                        color = RtColors.Ink2,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
                onClick = { onVote(true) },
                modifier = Modifier.weight(1f).height(76.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (myVote == Vote.NO) RtColors.Raised else RtColors.Accent,
                    contentColor = if (myVote == Vote.NO) RtColors.Ink2 else Color.White,
                ),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("좋아요", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onVote(false) },
                modifier = Modifier.weight(1f).height(76.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, RtColors.Line2),
            ) {
                Text("괜찮아요", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = RtColors.Ink2)
            }
        }
        Text(
            "말로 “좋아요”라고 해도 돼요 · 10초 뒤 자동으로 닫혀요",
            fontSize = 17.sp,
            color = RtColors.Sub,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun headline(name: String, signal: StatusSignal): String = when (signal) {
    StatusSignal.SLEEPY -> "${name}님이 졸리대요"
    StatusSignal.REST -> "${name}님이 쉬었다 가재요"
    StatusSignal.TOILET -> "${name}님이 화장실이 급하대요"
    StatusSignal.ENERGY -> "${name}님이 주유가 필요해요"
    StatusSignal.SLOW -> "${name}님이 천천히 오신대요"
    StatusSignal.HELP -> "${name}님 차에 문제가 생겼어요"
}

/** 쉬어가기 투표 현황 */
@Composable
fun VotePanel(state: DriveUiState, modifier: Modifier = Modifier) {
    val vote = state.vote ?: return
    val accepted = state.voteResult?.accepted == true
    RtCard(modifier, padding = PaddingValues(horizontal = 30.dp, vertical = 26.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("쉬어가기 투표")
            Spacer(Modifier.weight(1f))
            if (accepted) Tag("과반 동의", RtColors.AccentSoft, RtColors.AccentText, icon = Icons.Outlined.Check)
            else if (state.voteResult == null) Tag("1분 뒤 마감", RtColors.Panel, RtColors.Sub, icon = Icons.Outlined.Schedule)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${vote.yesCount}/${vote.memberIds.size}", fontSize = 56.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text("명이 좋아요", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = RtColors.Ink2, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(14.dp))
        // 막대: 좋아요 → 괜찮아요 → 아직
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val ordered = List(vote.yesCount) { RtColors.Accent } +
                List(vote.noCount) { RtColors.Faint } +
                List(vote.memberIds.size - vote.yesCount - vote.noCount) { RtColors.Pending }
            ordered.forEach { c ->
                Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(c))
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vote.memberIds.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { id ->
                        val member = state.member(id)
                        if (member != null) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                MemberAvatar(member, 38.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(member.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    val (text, color) = when {
                                        id == vote.proposerId -> "제안 · ${state.voteSignal?.label ?: "쉬어 가요"}" to RtColors.Night
                                        vote.voteOf(id) == Vote.YES -> "좋아요" to RtColors.AccentText
                                        vote.voteOf(id) == Vote.NO -> "괜찮아요" to RtColors.Sub
                                        else -> "응답 기다리는 중" to RtColors.Muted
                                    }
                                    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = RtColors.Divider)
        Spacer(Modifier.height(14.dp))
        Text(
            "과반이 ‘좋아요’면 쉴 곳이 자동으로 모두의 경로에 들어가요. 투표는 1분 뒤 마감되고, 운전 중엔 말로만 답해도 돼요.",
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = RtColors.Sub,
        )
    }
}

/** 투표 결과 */
@Composable
fun ResultCard(result: VoteResult) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RtColors.Card)
            .padding(horizontal = 26.dp, vertical = 22.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (result.accepted) RtColors.Accent else RtColors.Card)
                .border(2.dp, if (result.accepted) RtColors.Accent else RtColors.Line2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = if (result.accepted) Color.White else RtColors.Sub,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(result.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
            Text(result.detail, fontSize = 17.sp, color = RtColors.Sub)
        }
    }
}
