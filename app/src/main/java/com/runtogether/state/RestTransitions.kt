package com.runtogether.state

import com.runtogether.domain.Josa
import com.runtogether.domain.RestVoteState

/**
 * 쉬어가기 투표가 과반으로 통과했을 때의 상태.
 * - 코스에 없던 곳이면 거리 순서에 맞게 끼워 넣는다 (앞에 오면 그곳이 다음 집결지가 된다)
 * - 결과 카드에 도착·출발 시각과 그다음 집결지 도착 시각을 적는다
 */
fun DriveUiState.acceptRest(vote: RestVoteState): DriveUiState {
    val stop = vote.stop
    val arrive = clockMin + minutesTo(stop)
    val depart = arrive + vote.restMinutes
    val following = nextStop
    val detail = buildString {
        append("${formatClock(arrive)} 도착 · ${vote.restMinutes}분 쉬고 출발")
        if (following != null && following.stop.id != stop.id && following.stop.kmFromStart > stop.kmFromStart) {
            val after = minutesTo(following.stop, fromKm = stop.kmFromStart)
            append(" · ${following.stop.shortName} 도착 ${formatClock(depart + after)}")
        }
    }
    val newCourse = if (course.any { it.stop.id == stop.id }) {
        course
    } else {
        val from = nextStopIndex.coerceIn(0, course.size)
        val insertAt = (from until course.size).firstOrNull { course[it].stop.kmFromStart > stop.kmFromStart } ?: course.size
        course.toMutableList().apply { add(insertAt, PlannedStop(stop, arrive, vote.restMinutes, label = "휴식")) }
    }
    return copy(
        course = newCourse,
        // 지나온 지점은 그대로라 '다음 지점' 위치도 그대로 — 쉴 곳이 앞이면 그곳을 가리키게 된다
        nextStopIndex = nextStopIndex,
        alert = null,
        vote = vote,
        voteResult = VoteResult(
            accepted = true,
            title = "${stop.name}${Josa.eulReul(stop.name)} 모두의 내비에 추가했어요",
            detail = detail,
        ),
    )
}

/** 과반이 안 되면 쉬지 않고 원래 집결지에서 만난다. */
fun DriveUiState.rejectRest(): DriveUiState {
    val next = nextStop?.stop?.name ?: "다음 집결지"
    return copy(alert = null, voteResult = VoteResult(false, "이번에는 쉬지 않고 가요", "${next}에서 만나요"))
}
