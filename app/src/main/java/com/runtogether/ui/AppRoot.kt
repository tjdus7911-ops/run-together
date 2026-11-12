package com.runtogether.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runtogether.GroupDriveViewModel
import com.runtogether.demo.DemoScenario
import com.runtogether.domain.GapCalculator
import com.runtogether.domain.StatusSignal
import com.runtogether.state.DriveUiState
import com.runtogether.state.Phase
import com.runtogether.state.formatClock
import com.runtogether.ui.components.AppHeader
import com.runtogether.ui.components.CaptionBar
import com.runtogether.ui.components.Tag
import com.runtogether.ui.screens.CheckinScreen
import com.runtogether.ui.screens.DrivingScreen
import com.runtogether.ui.screens.RegroupScreen
import com.runtogether.ui.theme.RtColors

/** 시안 기준 크기. 어떤 차량 화면에서도 이 비율을 유지하고, 남는 가로 폭은 지도·카드가 나눠 쓴다. */
private const val DESIGN_WIDTH = 1600f
private const val DESIGN_HEIGHT = 900f

/** 화면에서 할 수 있는 동작 묶음 (ViewModel 없이도 화면을 그릴 수 있게) */
data class AppActions(
    val onDepart: () -> Unit = {},
    val onSignal: (StatusSignal) -> Unit = {},
    val onVote: (Boolean) -> Unit = {},
    val onDismissAlert: () -> Unit = {},
    val onParkedAction: (String) -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onCloseToast: () -> Unit = {},
)

@Composable
fun AppRoot(viewModel: GroupDriveViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val caption by viewModel.caption.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        AppActions(
            onDepart = viewModel::sendDepartureNotice,
            onSignal = viewModel::sendSignal,
            onVote = viewModel::answerVote,
            onDismissAlert = viewModel::dismissAlert,
            onParkedAction = viewModel::parkedAction,
            onPrevious = viewModel::previousStep,
            onNext = viewModel::nextStep,
            onCloseToast = viewModel::clearToast,
        )
    }
    AppScreen(state, caption, actions)
}

@Composable
fun AppScreen(state: DriveUiState, caption: String?, actions: AppActions) {
    val gap = remember(state.members) { GapCalculator.compute(state.members) }

    BoxWithConstraints(Modifier.fillMaxSize().background(RtColors.Bg)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val designDensity = minOf(widthPx / DESIGN_WIDTH, heightPx / DESIGN_HEIGHT)

        CompositionLocalProvider(LocalDensity provides Density(designDensity, fontScale = 1f)) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Header(state)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (state.phase) {
                            Phase.PRE_DEPARTURE -> CheckinScreen(state, actions.onDepart)
                            Phase.DRIVING -> DrivingScreen(
                                state = state,
                                gap = gap,
                                caption = caption,
                                onSignal = actions.onSignal,
                                onVote = actions.onVote,
                                onDismissAlert = actions.onDismissAlert,
                            )
                            Phase.AT_MEETING_POINT -> RegroupScreen(state, actions.onDepart, actions.onParkedAction)
                        }
                    }
                }
                DemoBar(
                    state,
                    onPrevious = actions.onPrevious,
                    onNext = actions.onNext,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp),
                )
                // 주행 중에는 지도 위에, 그 밖에는 화면 위쪽 가운데에 자막
                if (state.phase != Phase.DRIVING) {
                    caption?.let { CaptionBar(it, Modifier.align(Alignment.TopCenter).padding(top = 116.dp)) }
                }
                state.toast?.let {
                    ToastBar(it, onClose = actions.onCloseToast, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(state: DriveUiState) {
    val start = state.course.first()
    val end = state.course.last()
    val subtitle = "${state.members.size}명 · ${formatClock(start.plannedClock)} 출발 · 최종 도착 ${formatClock(end.plannedClock)}"
    when (state.phase) {
        Phase.PRE_DEPARTURE -> AppHeader(
            state.groupName,
            subtitle,
            Icons.Outlined.Schedule,
            "출발 ${(start.plannedClock - state.clockMin).coerceAtLeast(0)}분 전 · ${formatClock(state.clockMin)}",
        )
        Phase.DRIVING -> AppHeader(state.groupName, subtitle, Icons.Outlined.VerifiedUser, "모임 위치 공유 중 · 끝나면 자동 삭제")
        Phase.AT_MEETING_POINT -> AppHeader(
            state.groupName,
            subtitle,
            Icons.Outlined.LocalParking,
            "정차 중 · ${state.regroup?.stopName ?: "집결지"}",
        )
    }
}

@Composable
private fun ToastBar(text: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(RtColors.Ink)
            .padding(start = 22.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Color.White, fontSize = 18.sp)
        TextButton(onClick = onClose) { Text("닫기", color = Color(0xFFFDBA74), fontSize = 17.sp) }
    }
}

/** 시연용 단계 이동 (PT에서 시나리오 A를 순서대로 보여 줄 때) */
@Composable
private fun DemoBar(state: DriveUiState, onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(1.dp, RtColors.Line, RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious, enabled = state.demoStep > 0) { Text("이전", fontSize = 16.sp) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                "시연 ${state.demoStep + 1}/${DemoScenario.LAST_STEP + 1}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RtColors.Muted,
            )
            Text(state.stepTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onNext, enabled = state.demoStep < DemoScenario.LAST_STEP) { Text("다음", fontSize = 16.sp) }
        if (state.vehicleLive) {
            Tag("실차 데이터", RtColors.AccentSoft, RtColors.Accent, fontSize = 13)
            Spacer(Modifier.width(6.dp))
        }
    }
}
