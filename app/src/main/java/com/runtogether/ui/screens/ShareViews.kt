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
import com.runtogether.ui.components.PrimaryButton
import com.runtogether.ui.components.RtCard
import com.runtogether.ui.components.SectionLabel
import com.runtogether.ui.components.Tag
import com.runtogether.ui.components.floatSurface
import com.runtogether.ui.theme.RtColors
import com.runtogether.ui.theme.RtDimens
import com.runtogether.ui.theme.RtType

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
            .floatSurface(RoundedCornerShape(RtDimens.CardRadius), elevation = 16.dp)
            .padding(start = 40.dp, end = 40.dp, top = 34.dp, bottom = 28.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(20.dp),
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
                Text(headline(from.name, alert.signal), style = RtType.Display)
            }
        }

        // 읽어 줬다는 표시는 면 없이 한 줄
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = RtColors.Sub, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("모든 차에 음성으로 알렸어요", style = RtType.Meta, color = RtColors.Sub)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(24.dp))
        }

        val rec = alert.recommendation
        if (rec != null) {
            // 묻는 말은 상자 대신 위쪽 가는 선 아래 큰 글자로
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(color = RtColors.Divider)
                Spacer(Modifier.height(20.dp))
                Column {
                    Text("${rec.stop.name}에서 ${alert.restMinutes}분 쉬었다 갈까요?", style = RtType.Title)
                    val facilities = buildList {
                        if (rec.stop.hasFuel) add("주유소")
                        if (rec.stop.hasCharger) add("충전소")
                        if (rec.stop.hasFood) add("카페")
                    }.joinToString("·")
                    val reason = if ("가장 가까운 쉴 곳" in rec.reasons) "가장 가까운 쉴 곳" else "추천 쉴 곳"
                    Text(
                        "$reason · ${formatKm(rec.distanceKm)}km · 약 ${rec.etaMinutes}분" + if (facilities.isNotEmpty()) " · $facilities" else "",
                        style = RtType.Meta,
                        color = RtColors.Sub,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (myVote == Vote.NO) {
                // 이미 ‘괜찮아요’로 답했으면 눌러도 되지만 눈에 띄지 않게
                Button(
                    onClick = { onVote(true) },
                    modifier = Modifier.weight(1f).height(76.dp),
                    shape = RoundedCornerShape(RtDimens.ButtonRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = RtColors.Panel, contentColor = RtColors.Ink2),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("좋아요", style = RtType.Headline, color = RtColors.Ink2)
                }
            } else {
                PrimaryButton("좋아요", Icons.Outlined.Check, { onVote(true) }, Modifier.weight(1f), textSize = 24.sp)
            }
            OutlinedButton(
                onClick = { onVote(false) },
                modifier = Modifier.weight(1f).height(76.dp),
                shape = RoundedCornerShape(RtDimens.ButtonRadius),
                border = BorderStroke(1.dp, RtColors.Line2),
            ) {
                Text("괜찮아요", style = RtType.Headline, color = RtColors.Ink2)
            }
        }
        Text(
            "말로 “좋아요”라고 해도 돼요 · 10초 뒤 자동으로 닫혀요",
            style = RtType.Caption,
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
    RtCard(modifier, padding = PaddingValues(top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("쉬어가기 투표")
            Spacer(Modifier.weight(1f))
            if (accepted) Tag("과반 동의", RtColors.AccentSoft, RtColors.AccentText, icon = Icons.Outlined.Check)
            else if (state.voteResult == null) Tag("1분 뒤 마감", RtColors.Panel, RtColors.Sub, icon = Icons.Outlined.Schedule)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${vote.yesCount}/${vote.memberIds.size}", style = RtType.NumL)
            Spacer(Modifier.width(12.dp))
            Text("명이 좋아요", style = RtType.Headline, color = RtColors.Ink2, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(14.dp))
        // 막대: 좋아요 → 괜찮아요 → 아직
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val ordered = List(vote.yesCount) { RtColors.Accent } +
                List(vote.noCount) { RtColors.Faint } +
                List(vote.memberIds.size - vote.yesCount - vote.noCount) { RtColors.Pending }
            ordered.forEach { c ->
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c))
            }
        }
        Spacer(Modifier.height(20.dp))
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
                                    Text(member.name, style = RtType.CaptionStrong)
                                    val (text, color) = when {
                                        id == vote.proposerId -> "제안 · ${state.voteSignal?.label ?: "쉬어 가요"}" to RtColors.Night
                                        vote.voteOf(id) == Vote.YES -> "좋아요" to RtColors.AccentText
                                        vote.voteOf(id) == Vote.NO -> "괜찮아요" to RtColors.Sub
                                        else -> "응답 기다리는 중" to RtColors.Muted
                                    }
                                    Text(text, style = RtType.CaptionStrong, color = color)
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
        Spacer(Modifier.height(20.dp))
        // 어디서 얼마나 쉬자는 건지 (면 없이, 위쪽 가는 선)
        HorizontalDivider(color = RtColors.Divider)
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.LocalCafe, contentDescription = null, tint = RtColors.Sub, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("${vote.stop.name}에서 ${vote.restMinutes}분 쉬기", style = RtType.BodyStrong)
                val facilities = buildList {
                    if (vote.stop.hasFuel) add("주유소")
                    if (vote.stop.hasCharger) add("충전소")
                    if (vote.stop.hasFood) add("카페")
                }.joinToString("·")
                Text(
                    "${formatKm(state.distanceTo(vote.stop))}km · 약 ${state.minutesTo(vote.stop)}분" + if (facilities.isNotEmpty()) " · $facilities" else "",
                    style = RtType.Caption,
                    color = RtColors.Sub,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = RtColors.Divider)
        Spacer(Modifier.height(14.dp))
        Text(
            "과반이 ‘좋아요’면 쉴 곳이 자동으로 모두의 경로에 들어가요. 투표는 1분 뒤 마감되고, 운전 중엔 말로만 답해도 돼요.",
            style = RtType.Caption,
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
            .clip(RoundedCornerShape(RtDimens.InnerRadius))
            .background(if (result.accepted) RtColors.AccentSoft else RtColors.Card)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (result.accepted) RtColors.Accent else RtColors.Bg)
                .border(1.5.dp, if (result.accepted) RtColors.Accent else RtColors.Line2, CircleShape),
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
            Text(result.title, style = RtType.BodyStrong)
            Text(result.detail, style = RtType.Caption, color = RtColors.Sub)
        }
    }
}
