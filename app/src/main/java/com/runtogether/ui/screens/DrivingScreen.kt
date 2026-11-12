package com.runtogether.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtogether.domain.EnergyKind
import com.runtogether.domain.GroupGap
import com.runtogether.domain.JoinMode
import com.runtogether.domain.Josa
import com.runtogether.domain.MessageIcon
import com.runtogether.domain.NaviGuidance
import com.runtogether.domain.QuickMessage
import com.runtogether.domain.QuickMessagePolicy
import com.runtogether.domain.TrafficLevel
import com.runtogether.domain.TurnKind
import com.runtogether.domain.formatDistanceM
import com.runtogether.state.DriveUiState
import com.runtogether.state.FeedItem
import com.runtogether.state.PlannedStop
import com.runtogether.state.VoiceShare
import com.runtogether.state.formatClock
import com.runtogether.state.formatDuration
import com.runtogether.state.formatKm
import com.runtogether.state.nextStopEta
import com.runtogether.state.shownGuidance
import com.runtogether.ui.AppActions
import com.runtogether.ui.components.CaptionBar
import com.runtogether.ui.components.CarGlyph
import com.runtogether.ui.components.MemberAvatar
import com.runtogether.ui.components.PrimaryButton
import com.runtogether.ui.components.ReadAloudMark
import com.runtogether.ui.components.RoundIconButton
import com.runtogether.ui.components.RtCard
import com.runtogether.ui.components.SecondaryButton
import com.runtogether.ui.components.SourceBadge
import com.runtogether.ui.components.StatusButtons
import com.runtogether.ui.map.LocalGroupMapRenderer
import com.runtogether.ui.map.MapCamera
import com.runtogether.ui.map.toMapModel
import com.runtogether.ui.theme.RtColors
import com.runtogether.ui.theme.RtDimens
import com.runtogether.ui.theme.RtType

/**
 * ② 주행 중 — 왼쪽 지도(다음 안내 · 모임 차 위치 · 원터치 상황 공유), 오른쪽 모임 현황 · 내 차량 상태 · 모임 소식,
 * 아래 다음 집결지 · 코스 진행 · 내 메시지 · 음성으로 상황 공유. (④ 상황 공유 알림·투표가 이 위에 뜬다)
 */
@Composable
fun DrivingScreen(
    state: DriveUiState,
    gap: GroupGap,
    caption: String?,
    actions: AppActions,
    captionSpeakerId: String? = null,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(RtDimens.Gap)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RtDimens.Gap)) {
            MapArea(state, gap, caption, captionSpeakerId, actions, Modifier.weight(1f).fillMaxHeight())
            Column(
                Modifier.width(RtDimens.RightColumnWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.vote != null) {
                    VotePanel(state, Modifier.weight(1f).fillMaxWidth())
                    state.voteResult?.let { ResultCard(it) }
                } else {
                    GroupStatusCard(state, gap)
                    MyCarCard(state)
                    FeedCard(state, actions.onToggleReadAloud, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
        DriveBottomBar(state, actions)
    }
}

// ---------------- 지도 ----------------

@Composable
private fun MapArea(
    state: DriveUiState,
    gap: GroupGap,
    caption: String?,
    captionSpeakerId: String?,
    actions: AppActions,
    modifier: Modifier,
) {
    var camera by remember(state.demoStep) { mutableStateOf(MapCamera()) }
    val guidance = state.shownGuidance
    val model = remember(state.members, state.course, state.nextStopIndex, state.candidates, state.groupLive, guidance, gap) {
        state.toMapModel(gap, guidance)
    }
    val renderer = LocalGroupMapRenderer.current
    Box(modifier.clip(RoundedCornerShape(RtDimens.CardRadius))) {
        renderer.Render(model, camera, Modifier.fillMaxSize())

        TurnCard(guidance, Modifier.align(Alignment.TopStart).padding(16.dp).width(372.dp))
        StatusButtons(actions.onSignal, Modifier.align(Alignment.CenterEnd).padding(end = 14.dp), vertical = true)
        MapControls(camera, onChange = { camera = it }, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))

        if (state.messagePanelOpen) {
            Box(Modifier.fillMaxSize().background(RtColors.Scrim).pointerInput(Unit) { detectTapGestures { actions.onCloseMyMessages() } })
            MyMessagePanel(state, actions, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
        state.voiceShare?.let { share ->
            Box(Modifier.fillMaxSize().background(RtColors.Scrim).pointerInput(Unit) { detectTapGestures { } })
            VoiceSharePanel(share, actions, Modifier.align(Alignment.Center))
        }
        val alert = state.alert
        if (alert != null) {
            Box(Modifier.fillMaxSize().background(RtColors.Scrim))
            AlertCard(state, alert, actions.onVote, actions.onDismissAlert, Modifier.align(Alignment.Center))
        }
        // 음성 안내 자막: 안내 카드 오른쪽 위 (투표 패널·상황 공유 줄을 가리지 않게)
        caption?.let {
            CaptionBar(
                it,
                Modifier.align(Alignment.TopStart).padding(start = 404.dp, top = 16.dp, end = 16.dp),
                speaker = captionSpeakerId?.let { id -> state.member(id) },
            )
        }
    }
}

/** 다음 안내 (내비 TBT) · 남은 거리 · 도착 예정. 내비가 없으면 ‘시연 안내’로 표시 */
@Composable
private fun TurnCard(g: NaviGuidance, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(RtColors.Overlay)
            .border(1.dp, RtColors.Line, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (!g.hasTurn) {
            Text("내비 경로 안내를 기다리는 중이에요", style = RtType.Heading)
            Spacer(Modifier.height(4.dp))
            Text("차량 내비에 모임 코스를 넣으면 다음 안내가 여기에 나와요", style = RtType.Caption, color = RtColors.Sub)
            Spacer(Modifier.height(10.dp))
            SourceBadge(live = g.live, liveText = "내비 연동", demoText = "시연 안내")
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(RtColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                TurnGlyph(g.turn ?: TurnKind.STRAIGHT, Modifier.size(42.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                val parts = formatDistanceM(g.turnDistanceM ?: 0.0).split(" ")
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)) { append(parts.first()) }
                        withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) { append(" ${parts.getOrElse(1) { "" }} 후") }
                    },
                    lineHeight = 42.sp,
                    maxLines = 1,
                )
                Text(g.turnText.orEmpty(), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = RtColors.Ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // 10km 안에 막히는 구간이 있으면 한 줄로
        val jam = g.congestion.firstOrNull { it.offsetM < 10_000 && it.level != TrafficLevel.SMOOTH && it.level != TrafficLevel.UNKNOWN }
        if (jam != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(width = 16.dp, height = 6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (jam.level == TrafficLevel.JAM) RtColors.TrafficJam else RtColors.TrafficSlow),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${formatDistanceM(jam.offsetM)} 앞 ${jam.level.label} ${formatDistanceM(jam.lengthM)}",
                    style = RtType.Caption,
                    color = RtColors.Sub,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(RtColors.Line))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("남은 거리", style = RtType.Micro, color = RtColors.Sub)
                Text(g.destinationRemainM?.let { formatDistanceM(it.toDouble()) } ?: "–", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text("예상 도착", style = RtType.Micro, color = RtColors.Sub)
                Text(g.destinationEtaClock?.let { formatClock(it) } ?: "–", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            SourceBadge(live = g.live, liveText = "내비 연동", demoText = "시연 안내")
        }
    }
}

/** 다음 안내 방향 그림 (아이콘 세트에 없는 회전 화살표를 직접 그린다) */
@Composable
private fun TurnGlyph(kind: TurnKind, modifier: Modifier) {
    val icon: ImageVector? = when (kind) {
        TurnKind.SERVICE_AREA -> Icons.Outlined.LocalCafe
        TurnKind.TOLL -> Icons.Outlined.Toll
        TurnKind.ARRIVE -> Icons.Outlined.Flag
        else -> null
    }
    if (icon != null) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = modifier)
        return
    }
    Canvas(modifier) {
        val w = size.width
        val stroke = Stroke(w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(w * x, w * y)
        when (kind) {
            TurnKind.LEFT, TurnKind.RIGHT -> {
                val s = if (kind == TurnKind.RIGHT) 1f else -1f
                val path = Path().apply {
                    moveTo(w * (0.5f - 0.14f * s), w * 0.9f)
                    lineTo(w * (0.5f - 0.14f * s), w * 0.52f)
                    cubicTo(w * (0.5f - 0.14f * s), w * 0.4f, w * (0.5f - 0.06f * s), w * 0.36f, w * (0.5f + 0.06f * s), w * 0.36f)
                    lineTo(w * (0.5f + 0.2f * s), w * 0.36f)
                }
                drawPath(path, Color.White, style = stroke)
                arrowHead(p(0.5f + 0.34f * s, 0.36f), if (s > 0) 0f else 180f, w)
            }
            TurnKind.KEEP_LEFT, TurnKind.KEEP_RIGHT, TurnKind.IC_ENTER, TurnKind.IC_EXIT -> {
                val s = if (kind == TurnKind.KEEP_LEFT) -1f else 1f
                val path = Path().apply {
                    moveTo(w * (0.5f - 0.1f * s), w * 0.9f)
                    lineTo(w * (0.5f - 0.1f * s), w * 0.6f)
                    lineTo(w * (0.5f + 0.12f * s), w * 0.34f)
                }
                drawPath(path, Color.White, style = stroke)
                arrowHead(p(0.5f + 0.2f * s, 0.24f), if (s > 0) -50f else -130f, w)
            }
            TurnKind.UTURN -> {
                val path = Path().apply {
                    moveTo(w * 0.64f, w * 0.9f)
                    lineTo(w * 0.64f, w * 0.42f)
                    cubicTo(w * 0.64f, w * 0.2f, w * 0.34f, w * 0.2f, w * 0.34f, w * 0.42f)
                    lineTo(w * 0.34f, w * 0.6f)
                }
                drawPath(path, Color.White, style = stroke)
                arrowHead(p(0.34f, 0.74f), 90f, w)
            }
            TurnKind.ROUNDABOUT -> {
                drawCircle(Color.White, radius = w * 0.17f, center = p(0.5f, 0.4f), style = stroke)
                drawLine(Color.White, p(0.5f, 0.9f), p(0.5f, 0.57f), strokeWidth = w * 0.13f, cap = StrokeCap.Round)
                arrowHead(p(0.78f, 0.3f), -30f, w)
            }
            else -> {
                drawLine(Color.White, p(0.5f, 0.9f), p(0.5f, 0.32f), strokeWidth = w * 0.13f, cap = StrokeCap.Round)
                arrowHead(p(0.5f, 0.2f), -90f, w)
            }
        }
    }
}

/** 화살촉: [tip] 에서 [deg] 방향(0 = 오른쪽, -90 = 위)을 가리키는 삼각형 */
private fun DrawScope.arrowHead(tip: Offset, deg: Float, w: Float) {
    val a = Math.toRadians(deg.toDouble())
    val len = w * 0.26f
    val half = w * 0.17f
    val bx = tip.x - (kotlin.math.cos(a) * len).toFloat()
    val by = tip.y - (kotlin.math.sin(a) * len).toFloat()
    val nx = (-kotlin.math.sin(a) * half).toFloat()
    val ny = (kotlin.math.cos(a) * half).toFloat()
    drawPath(
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(bx + nx, by + ny)
            lineTo(bx - nx, by - ny)
            close()
        },
        Color.White,
    )
}

/** 지도 확대·축소 · 내 차 따라가기 */
@Composable
private fun MapControls(camera: MapCamera, onChange: (MapCamera) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RoundIconButton(
            Icons.Outlined.MyLocation,
            label = if (camera.followMe) "모임 전체 보기" else "내 차 따라가기",
            onClick = { onChange(camera.copy(followMe = !camera.followMe, zoomStep = 0)) },
            background = if (camera.followMe) RtColors.Accent else RtColors.Overlay,
        )
        Column(Modifier.clip(RoundedCornerShape(18.dp)).background(RtColors.Overlay)) {
            RoundIconButton(
                Icons.Outlined.Add,
                label = "지도 확대",
                onClick = { onChange(camera.copy(zoomStep = (camera.zoomStep + 1).coerceAtMost(MapCamera.MAX_STEP))) },
                background = Color.Transparent,
                enabled = camera.zoomStep < MapCamera.MAX_STEP,
            )
            Box(Modifier.width(RtDimens.TouchMin).height(1.dp).background(RtColors.Line))
            RoundIconButton(
                Icons.Outlined.Remove,
                label = "지도 축소",
                onClick = { onChange(camera.copy(zoomStep = (camera.zoomStep - 1).coerceAtLeast(MapCamera.MIN_STEP))) },
                background = Color.Transparent,
                enabled = camera.zoomStep > MapCamera.MIN_STEP,
            )
        }
    }
}

// ---------------- 오른쪽 카드 ----------------

/** 모임 주행 현황: 같은 길 대수, 차 순서와 나와의 거리, 선두–후미 간격, 연료가 가장 적은 차 */
@Composable
private fun GroupStatusCard(state: DriveUiState, gap: GroupGap) {
    val me = state.me
    val sameRoute = state.members.count { it.onPlannedRoute }
    val ordered = state.members.filter { it.onPlannedRoute }.sortedByDescending { it.progressKm } +
        state.members.filter { !it.onPlannedRoute }
    RtCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 22.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("모임 주행 중", style = RtType.Heading)
            Spacer(Modifier.width(10.dp))
            Text("같은 길 $sameRoute/${state.members.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RtColors.AccentText)
            Spacer(Modifier.weight(1f))
            SourceBadge(live = state.groupLive, liveText = "실시간 연동", demoText = "시연 위치")
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ordered.forEach { m ->
                val isMe = m.member.id == state.meId
                val (sub, subColor) = when {
                    isMe && m.member.id == gap.leadId -> "나 · 선두" to RtColors.AccentText
                    isMe -> "나" to RtColors.AccentText
                    !m.onPlannedRoute -> "다른 길" to RtColors.Sub
                    m.member.id == gap.leadId -> "선두" to RtColors.AccentText
                    else -> {
                        val d = me.progressKm - m.progressKm
                        (if (d >= 0) "${formatKm(d)}km 뒤" else "${formatKm(-d)}km 앞") to RtColors.Sub
                    }
                }
                Column(Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        MemberAvatar(m.member, 50.dp, dashed = !m.onPlannedRoute, ring = isMe)
                        if (m.member.joinMode == JoinMode.PHONE) {
                            // 휴대폰으로 참여 (위치는 휴대폰 GPS)
                            Box(
                                Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(RtColors.Card)
                                    .border(1.5.dp, RtColors.Line2, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.PhoneAndroid, contentDescription = "휴대폰 참여", tint = RtColors.Ink2, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(m.member.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(sub, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = subColor, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(RtColors.Divider))
        Spacer(Modifier.height(10.dp))
        // 따라잡으라는 안내 대신 간격을 보여 주기만 한다
        val behind = gap.largestGapBehindId?.let { state.member(it) }
        val ahead = gap.largestGapAheadId?.let { state.member(it) }
        val spread = "선두–후미 ${formatKm(gap.spreadKm)}km"
        if (gap.largestGapKm >= 1.0 && behind != null && ahead != null) {
            InfoLine(Icons.Outlined.WarningAmber, "$spread · ${ahead.name}–${behind.name} 사이 ${formatKm(gap.largestGapKm)}km 벌어짐", warn = true)
        } else {
            InfoLine(null, "$spread · 간격이 고르게 유지되고 있어요", warn = false)
        }
        lowEnergyLine(state)?.let {
            Spacer(Modifier.height(6.dp))
            InfoLine(Icons.Outlined.LocalGasStation, it, warn = true)
        }
    }
}

/** 연료·배터리가 가장 적은 차가 있으면 그 이유로 고른 다음 집결지를 함께 */
private fun lowEnergyLine(state: DriveUiState): String? {
    val low = state.members
        .filter { (it.vehicle.energyPercent ?: 100) <= 15 && it.vehicle.rangeKm != null && it.energyKind != null }
        .minByOrNull { it.vehicle.rangeKm ?: Double.MAX_VALUE } ?: return null
    val ev = low.energyKind == EnergyKind.BATTERY
    val next = state.nextStop?.stop
    val reason = if (next != null && ((ev && next.hasCharger) || (!ev && next.hasFuel))) {
        " → ${if (ev) "충전소" else "주유소"} 있는 ${next.shortName}${Josa.euroRo(next.shortName)} 골랐어요"
    } else {
        ""
    }
    return "${low.member.name}님 ${if (ev) "배터리" else "연료"} ${low.vehicle.energyPercent}% · ${formatKm(low.vehicle.rangeKm ?: 0.0)}km 남음$reason"
}

@Composable
private fun InfoLine(icon: ImageVector?, text: String, warn: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (warn) RtColors.WarnIcon else RtColors.Sub, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (warn) RtColors.Warn else RtColors.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 내 차량 상태: 차가 실제로 주는 값만 보여 준다.
 * 내연기관차는 연료, 전기차는 배터리 하나만 (둘을 함께 보여 주지 않는다). 값이 없으면 칸을 빼고, 시연 값은 ‘시연 값’으로 표시.
 */
@Composable
private fun MyCarCard(state: DriveUiState) {
    val me = state.me
    val live = state.vehicleLive
    val vehicle = if (live) state.myLiveVehicle ?: me.vehicle else me.vehicle
    val kind = if (live) vehicle.energyKind else me.energyKind
    val energy = vehicle.energyPercent?.takeIf { kind != null }
    val drivingMin = if (live) vehicle.tripDrivingMinutes else state.continuousDrivingMin
    RtCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("내 차량 상태", style = RtType.Heading)
            Spacer(Modifier.weight(1f))
            SourceBadge(live = live, liveText = "실차 데이터", demoText = "시연 값")
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            var shown = 0
            if (energy != null) {
                Column(Modifier.width(112.dp)) {
                    Text(if (kind == EnergyKind.BATTERY) "배터리" else "연료", style = RtType.Caption, color = RtColors.Sub)
                    Text("$energy%", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = if (energy <= 20) RtColors.Warn else RtColors.Ink)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(92.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(RtColors.Panel)) {
                        Box(
                            Modifier.fillMaxHeight().width((92f * energy / 100f).coerceIn(4f, 92f).dp).clip(RoundedCornerShape(3.dp))
                                .background(if (energy <= 20) RtColors.Warn else RtColors.Good),
                        )
                    }
                }
                shown++
            }
            vehicle.rangeKm?.let { range ->
                if (shown > 0) MetricDivider()
                Metric("주행 가능", "${formatKm(range)}km")
                shown++
            }
            drivingMin?.let { minutes ->
                if (shown > 0) MetricDivider()
                Metric("연속 운전", formatDuration(minutes), warn = minutes >= 100)
                shown++
            }
            if (shown == 0) {
                Text("이 차는 연료·주행 정보를 보내 주지 않아요", style = RtType.Caption, color = RtColors.Sub)
            }
            Spacer(Modifier.weight(1f))
            CarGlyph(Modifier.size(width = 104.dp, height = 50.dp))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, warn: Boolean = false) {
    Column {
        Text(label, style = RtType.Caption, color = RtColors.Sub)
        Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = if (warn) RtColors.Warn else RtColors.Ink, maxLines = 1)
    }
}

@Composable
private fun MetricDivider() {
    Spacer(Modifier.width(16.dp))
    Box(Modifier.width(1.dp).height(44.dp).background(RtColors.Divider))
    Spacer(Modifier.width(16.dp))
}

/** 모임 소식: 상황 공유 · 내 메시지 · 음성 메시지. 오른쪽 위에서 받은 메시지 읽어 주기를 켜고 끈다. */
@Composable
private fun FeedCard(state: DriveUiState, onToggleReadAloud: () -> Unit, modifier: Modifier = Modifier) {
    RtCard(modifier, padding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("모임 소식", style = RtType.Heading)
            Spacer(Modifier.weight(1f))
            ReadAloudToggle(state.readMessagesAloud, onToggleReadAloud)
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val rowHeight = 56.dp
            val rowGap = 10.dp
            val fits = ((maxHeight + rowGap) / (rowHeight + rowGap)).toInt().coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
                state.feed.take(fits).forEach { item -> FeedRow(state, item, Modifier.height(rowHeight)) }
                if (state.feed.isEmpty()) {
                    Text("아직 소식이 없어요", fontSize = 18.sp, color = RtColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun FeedRow(state: DriveUiState, item: FeedItem, modifier: Modifier) {
    val member = state.member(item.memberId) ?: return
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MemberAvatar(member, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("${item.whenText} · ${item.kindText}", fontSize = 14.sp, color = RtColors.Sub, maxLines = 1)
            }
            Text(item.text, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (item.readAloud) ReadAloudMark()
    }
}

@Composable
private fun ReadAloudToggle(on: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .height(48.dp)
            .clip(shape)
            .background(RtColors.Panel)
            .border(1.dp, if (on) RtColors.Line2 else RtColors.Line, shape)
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (on) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
            contentDescription = null,
            tint = if (on) RtColors.AccentText else RtColors.Sub,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(if (on) "메시지 읽어 주기 켬" else "메시지 읽어 주기 끔", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) RtColors.Ink else RtColors.Sub)
    }
}

// ---------------- 아래 막대 ----------------

@Composable
private fun DriveBottomBar(state: DriveUiState, actions: AppActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(RtDimens.BottomBarHeight)
            .clip(RoundedCornerShape(RtDimens.CardRadius))
            .background(RtColors.Card)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NextStopBlock(state, Modifier.width(430.dp))
        Box(Modifier.width(1.dp).height(64.dp).background(RtColors.Line))
        Spacer(Modifier.width(18.dp))
        CourseStepper(state.course, state.nextStopIndex, Modifier.weight(1f))
        Spacer(Modifier.width(18.dp))
        SecondaryButton(
            "내 메시지",
            Icons.Outlined.ChatBubbleOutline,
            if (state.messagePanelOpen) actions.onCloseMyMessages else actions.onOpenMyMessages,
            Modifier.width(196.dp),
            selected = state.messagePanelOpen,
        )
        Spacer(Modifier.width(12.dp))
        val listening = state.voiceShare?.stage == VoiceShare.Stage.LISTENING
        PrimaryButton(if (listening) "듣고 있어요…" else "음성으로 상황 공유", Icons.Outlined.Mic, actions.onVoiceShare, Modifier.width(300.dp))
    }
}

@Composable
private fun NextStopBlock(state: DriveUiState, modifier: Modifier) {
    val next = state.nextStop
    val eta = state.nextStopEta()
    val isDestination = next != null && next == state.course.lastOrNull()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(RtColors.Panel),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isDestination) "목적지" else "다음 집결지", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RtColors.Sub)
                if (next != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = RtColors.AccentText, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("모두의 내비에 추가됨", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RtColors.AccentText)
                }
            }
            Text(next?.stop?.name ?: "모두 도착했어요", fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (eta != null) {
                Text(
                    "${formatKm(eta.km)}km · 약 ${formatDuration(eta.minutes)} · ${formatClock(eta.arriveClock)} 도착",
                    fontSize = 16.sp,
                    color = RtColors.Sub,
                    maxLines = 1,
                )
            }
        }
    }
}

private val STEP_DOT = 28.dp

/** 코스 진행: 지난 곳 ✓ · 다음 집결지 · 남은 곳 · 도착 */
@Composable
private fun CourseStepper(course: List<PlannedStop>, nextIndex: Int, modifier: Modifier) {
    if (course.size < 2) return
    Box(modifier) {
        Canvas(Modifier.fillMaxWidth().height(STEP_DOT)) {
            val n = course.size
            val cell = size.width / n
            val y = size.height / 2
            val inset = STEP_DOT.toPx() / 2 + 6.dp.toPx()
            for (i in 0 until n - 1) {
                val heading = i + 1 == nextIndex
                val done = i + 1 < nextIndex
                drawLine(
                    color = if (done || heading) RtColors.Accent else RtColors.Line2,
                    start = Offset(cell * (i + 0.5f) + inset, y),
                    end = Offset(cell * (i + 1.5f) - inset, y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (heading) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            course.forEachIndexed { i, stop ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    StepDot(passed = i < nextIndex, next = i == nextIndex, destination = i == course.lastIndex)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stop.stop.shortName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (i <= nextIndex) RtColors.Ink else RtColors.Sub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (i == nextIndex) "${stop.label} · 다음" else stop.label,
                        fontSize = 13.sp,
                        fontWeight = if (i == nextIndex) FontWeight.Bold else FontWeight.Medium,
                        color = if (i == nextIndex) RtColors.AccentText else RtColors.Muted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDot(passed: Boolean, next: Boolean, destination: Boolean) {
    when {
        passed -> Box(Modifier.size(STEP_DOT).clip(CircleShape).background(RtColors.Accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        next -> Box(
            Modifier.size(STEP_DOT).clip(CircleShape).background(RtColors.AccentRing).padding(5.dp).clip(CircleShape)
                .background(RtColors.Card).border(4.dp, RtColors.Accent, CircleShape),
        )
        destination -> Box(
            Modifier.size(STEP_DOT).clip(RoundedCornerShape(8.dp)).background(RtColors.Line2).padding(6.dp)
                .clip(RoundedCornerShape(3.dp)).background(RtColors.Ink2),
        )
        else -> Box(Modifier.size(STEP_DOT).clip(CircleShape).background(RtColors.Card).border(3.dp, RtColors.Line2, CircleShape))
    }
}

// ---------------- 내 메시지 ----------------

/** 내 메시지 고르기: 큰 버튼 최대 6개(3×2), 스크롤 없음. 누르면 바로 모임에 보낸다. 고치기는 휴대폰에서. */
@Composable
private fun MyMessagePanel(state: DriveUiState, actions: AppActions, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RtColors.Overlay)
            .border(1.dp, RtColors.Line, shape)
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("내 메시지", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(
                    "누르면 모임 전체에 바로 보내요 · 만들기와 고치기는 휴대폰 ‘같이 달려’ 앱에서",
                    style = RtType.Caption,
                    color = RtColors.Sub,
                    maxLines = 1,
                )
            }
            RoundIconButton(Icons.Outlined.Close, label = "닫기", onClick = actions.onCloseMyMessages, background = RtColors.Raised)
        }
        Spacer(Modifier.height(16.dp))
        val messages = QuickMessagePolicy.forCar(state.myMessages)
        if (messages.isEmpty()) {
            Text("아직 만든 메시지가 없어요", style = RtType.Heading)
            Text("휴대폰 ‘같이 달려’ 앱 → 내 메시지에서 6개까지 만들 수 있어요", style = RtType.Body, color = RtColors.Sub)
            return@Column
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            messages.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { m ->
                        MessageButton(
                            m,
                            sent = state.sentMessageId == m.id,
                            cooling = m.id in state.coolingMessageIds && state.sentMessageId != m.id,
                            onClick = { actions.onSendMyMessage(m.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MessageButton(m: QuickMessage, sent: Boolean, cooling: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .height(100.dp)
            .alpha(if (cooling) 0.45f else 1f)
            .clip(shape)
            .background(if (sent) RtColors.AccentSoft else RtColors.Card)
            .border(if (sent) 2.dp else 1.dp, if (sent) RtColors.Accent else RtColors.Line, shape)
            .clickable(enabled = !sent && !cooling, onClickLabel = m.text, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (sent) RtColors.Accent else RtColors.Raised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (sent) Icons.Outlined.Check else iconOf(m.icon), contentDescription = null, tint = if (sent) Color.White else RtColors.Ink, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(m.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            when {
                sent -> Text("보냈어요", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RtColors.AccentText)
                cooling -> Text("잠시 뒤 다시 보낼 수 있어요", fontSize = 13.sp, color = RtColors.Sub, maxLines = 1)
            }
        }
    }
}

private fun iconOf(icon: MessageIcon): ImageVector = when (icon) {
    MessageIcon.COFFEE -> Icons.Outlined.LocalCafe
    MessageIcon.FOOD -> Icons.Outlined.Restaurant
    MessageIcon.FUEL -> Icons.Outlined.LocalGasStation
    MessageIcon.CHARGE -> Icons.Outlined.EvStation
    MessageIcon.TOILET -> Icons.Outlined.Wc
    MessageIcon.PHOTO -> Icons.Outlined.PhotoCamera
    MessageIcon.SLOW -> Icons.Outlined.Speed
    MessageIcon.LATE -> Icons.Outlined.Schedule
    MessageIcon.OK -> Icons.Outlined.ThumbUp
    MessageIcon.PARKING -> Icons.Outlined.LocalParking
    MessageIcon.MUSIC -> Icons.Outlined.MusicNote
    MessageIcon.SUN -> Icons.Outlined.WbSunny
}

// ---------------- 음성으로 상황 공유 ----------------

/** 듣는 중 → 받아 적은 문장 확인(3초 뒤 자동 전송, 취소 가능) → 보냄 / 다시 말하기 */
@Composable
private fun VoiceSharePanel(share: VoiceShare, actions: AppActions, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .width(620.dp)
            .clip(shape)
            .background(RtColors.Card)
            .border(1.dp, RtColors.Line, shape)
            .padding(horizontal = 36.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (share.stage) {
            VoiceShare.Stage.LISTENING -> {
                Box(Modifier.size(88.dp).clip(CircleShape).background(RtColors.Accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text("듣고 있어요", style = RtType.Title)
                Spacer(Modifier.height(6.dp))
                Text("모임에 보낼 말을 해 주세요 · 예: “10분 뒤 도착해요”", style = RtType.Body, color = RtColors.Sub, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                SecondaryButton("취소", Icons.Outlined.Close, actions.onCancelVoiceShare, Modifier.width(220.dp))
            }
            VoiceShare.Stage.CONFIRM -> {
                Text("이렇게 보낼게요", style = RtType.Label, color = RtColors.Sub)
                Spacer(Modifier.height(10.dp))
                Text("“${share.text.orEmpty()}”", fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("3초 뒤 모두에게 보내요", style = RtType.Caption, color = RtColors.Sub)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PrimaryButton("지금 보내기", Icons.AutoMirrored.Outlined.Send, actions.onSendVoiceShareNow, Modifier.weight(1f))
                    SecondaryButton("취소", null, actions.onCancelVoiceShare, Modifier.weight(1f))
                }
            }
            VoiceShare.Stage.SENT -> {
                Box(Modifier.size(80.dp).clip(CircleShape).background(RtColors.Accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("모두에게 보냈어요", style = RtType.Title)
                Spacer(Modifier.height(6.dp))
                Text("“${share.text.orEmpty()}”", style = RtType.Body, color = RtColors.Sub, textAlign = TextAlign.Center)
            }
            VoiceShare.Stage.FAILED -> {
                Box(Modifier.size(80.dp).clip(CircleShape).background(RtColors.Raised), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MicOff, contentDescription = null, tint = RtColors.Ink, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(share.problem ?: "잘 알아듣지 못했어요", style = RtType.Heading, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PrimaryButton("다시 말하기", Icons.Outlined.Mic, actions.onVoiceShare, Modifier.weight(1f))
                    SecondaryButton("닫기", null, actions.onCancelVoiceShare, Modifier.weight(1f))
                }
            }
        }
        if (share.demo) {
            Spacer(Modifier.height(16.dp))
            Text(
                "시연 모드 · 음성 인식이 없는 기기라 예시 문장을 받아 적은 것처럼 보여 줘요",
                fontSize = 13.sp,
                color = RtColors.Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
