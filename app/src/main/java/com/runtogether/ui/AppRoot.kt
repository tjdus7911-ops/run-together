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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
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
import com.runtogether.ui.components.RoundIconButton
import com.runtogether.ui.components.Tag
import com.runtogether.ui.components.dashedOutline
import com.runtogether.ui.components.floatSurface
import com.runtogether.ui.screens.CheckinScreen
import com.runtogether.ui.screens.DrivingScreen
import com.runtogether.ui.screens.RegroupScreen
import com.runtogether.ui.theme.RtColors
import com.runtogether.ui.theme.RtDimens
import com.runtogether.ui.theme.RtType

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
    // 내 메시지 (차량에서는 고르기만)
    val onOpenMyMessages: () -> Unit = {},
    val onCloseMyMessages: () -> Unit = {},
    val onSendMyMessage: (String) -> Unit = {},
    val onToggleReadAloud: () -> Unit = {},
    // 말로 메시지 보내기
    val onVoiceShare: () -> Unit = {},
    val onCancelVoiceShare: () -> Unit = {},
    val onSendVoiceShareNow: () -> Unit = {},
)

@Composable
fun AppRoot(viewModel: GroupDriveViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val caption by viewModel.caption.collectAsStateWithLifecycle()
    val captionSpeakerId by viewModel.captionSpeakerId.collectAsStateWithLifecycle()
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
            onOpenMyMessages = viewModel::openMyMessages,
            onCloseMyMessages = viewModel::closeMyMessages,
            onSendMyMessage = viewModel::sendMyMessage,
            onToggleReadAloud = viewModel::toggleReadAloud,
            onVoiceShare = viewModel::startVoiceShare,
            onCancelVoiceShare = viewModel::cancelVoiceShare,
            onSendVoiceShareNow = viewModel::sendVoiceShareNow,
        )
    }
    AppScreen(state, caption, actions, captionSpeakerId)
}

@Composable
fun AppScreen(state: DriveUiState, caption: String?, actions: AppActions, captionSpeakerId: String? = null) {
    val gap = remember(state.members) { GapCalculator.compute(state.members) }

    BoxWithConstraints(Modifier.fillMaxSize().background(RtColors.Bg)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val designDensity = minOf(widthPx / DESIGN_WIDTH, heightPx / DESIGN_HEIGHT)

        CompositionLocalProvider(LocalDensity provides Density(designDensity, fontScale = 1f)) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(
                        start = RtDimens.ScreenPaddingH,
                        end = RtDimens.ScreenPaddingH,
                        top = RtDimens.ScreenPaddingTop,
                        bottom = RtDimens.ScreenPaddingBottom,
                    ),
                    verticalArrangement = Arrangement.spacedBy(RtDimens.Gap),
                ) {
                    Header(state, actions)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (state.phase) {
                            Phase.PRE_DEPARTURE -> CheckinScreen(state, actions.onDepart)
                            Phase.DRIVING -> DrivingScreen(
                                state = state,
                                gap = gap,
                                caption = caption,
                                actions = actions,
                                captionSpeakerId = captionSpeakerId,
                            )
                            Phase.AT_MEETING_POINT -> RegroupScreen(state, actions.onDepart, actions.onParkedAction)
                        }
                    }
                }
                // 주행 중에는 지도 위에, 그 밖에는 화면 위쪽 가운데에 자막
                if (state.phase != Phase.DRIVING) {
                    caption?.let {
                        CaptionBar(
                            it,
                            Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
                            speaker = captionSpeakerId?.let { id -> state.member(id) },
                        )
                    }
                }
                state.toast?.let {
                    ToastBar(it, onClose = actions.onCloseToast, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(state: DriveUiState, actions: AppActions) {
    val start = state.course.first()
    val end = state.course.last()
    val subtitle = "${state.members.size}명 · ${formatClock(start.plannedClock)} 출발 · ${formatClock(end.plannedClock)} 도착"
    val demoBar: @Composable () -> Unit = { DemoBar(state, onPrevious = actions.onPrevious, onNext = actions.onNext) }
    val clock = formatClock(state.clockMin)
    when (state.phase) {
        Phase.PRE_DEPARTURE -> AppHeader(
            state.groupName,
            subtitle,
            Icons.Outlined.Schedule,
            "출발 ${(start.plannedClock - state.clockMin).coerceAtLeast(0)}분 전",
            center = demoBar,
            clock = clock,
            vehicleLive = state.vehicleLive,
        )
        Phase.DRIVING -> AppHeader(
            state.groupName,
            subtitle,
            Icons.Outlined.VerifiedUser,
            "모임 위치 공유 중 · 끝나면 자동 삭제",
            center = demoBar,
            clock = clock,
            vehicleLive = state.vehicleLive,
        )
        Phase.AT_MEETING_POINT -> AppHeader(
            state.groupName,
            subtitle,
            Icons.Outlined.LocalParking,
            "정차 중 · ${state.regroup?.stopName ?: "집결지"}",
            center = demoBar,
            clock = clock,
            vehicleLive = state.vehicleLive,
        )
    }
}

@Composable
private fun ToastBar(text: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .floatSurface(RoundedCornerShape(RtDimens.InnerRadius))
            .padding(start = 22.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = RtType.Meta, color = RtColors.Ink)
        TextButton(onClick = onClose) { Text("닫기", style = RtType.Label, color = RtColors.AccentText) }
    }
}

/**
 * 시연용 단계 이동 (PT에서 시나리오 A를 순서대로 보여 줄 때) — 맨 위 줄 가운데.
 * 제품 화면이 아니라는 뜻으로 점선 테두리만 두른다.
 */
@Composable
private fun DemoBar(state: DriveUiState, onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .dashedOutline(RtColors.Faint, RtDimens.InnerRadius)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            label = "이전 단계",
            onClick = onPrevious,
            size = 52.dp,
            background = Color.Transparent,
            enabled = state.demoStep > 0,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                "시연 ${state.demoStep + 1}/${DemoScenario.LAST_STEP + 1}",
                style = RtType.Caption,
                color = RtColors.Muted,
            )
            Text(state.stepTitle, style = RtType.CaptionStrong)
        }
        RoundIconButton(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            label = "다음 단계",
            onClick = onNext,
            size = 52.dp,
            background = Color.Transparent,
            enabled = state.demoStep < DemoScenario.LAST_STEP,
        )
        if (state.vehicleLive) {
            Tag("실차 데이터", RtColors.GoodSoft, RtColors.Good)
            Spacer(Modifier.width(8.dp))
        }
    }
}
