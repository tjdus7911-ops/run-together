package com.runtogether.domain

enum class Vote { YES, NO }

enum class VoteOutcome { PENDING, ACCEPTED, REJECTED }

data class RestVoteState(
    val proposerId: String,
    val stop: StopCandidate,
    val restMinutes: Int,
    val memberIds: List<String>,
    val votes: Map<String, Vote>,
    val deadlineMs: Long,
) {
    val yesCount: Int get() = votes.values.count { it == Vote.YES }
    val noCount: Int get() = votes.values.count { it == Vote.NO }
    /** 과반: 6명이면 4명 */
    val majority: Int get() = memberIds.size / 2 + 1
    fun voteOf(memberId: String): Vote? = votes[memberId]
}

/**
 * 쉬어가기 투표.
 * 제안한 사람은 '좋아요'로 시작하고, 과반이 '좋아요'면 쉴 곳을 모두의 경로에 넣는다.
 * 투표는 1분 뒤 마감되고, 남은 표로 과반이 불가능해지면 바로 끝낸다.
 */
object RestVoteEngine {
    const val VOTE_WINDOW_MS = 60_000L

    fun start(
        proposerId: String,
        stop: StopCandidate,
        restMinutes: Int,
        memberIds: List<String>,
        nowMs: Long,
    ): RestVoteState = RestVoteState(
        proposerId = proposerId,
        stop = stop,
        restMinutes = restMinutes,
        memberIds = memberIds,
        votes = mapOf(proposerId to Vote.YES),
        deadlineMs = nowMs + VOTE_WINDOW_MS,
    )

    fun cast(state: RestVoteState, memberId: String, vote: Vote, nowMs: Long): RestVoteState {
        if (memberId !in state.memberIds) return state
        if (outcome(state, nowMs) != VoteOutcome.PENDING) return state
        return state.copy(votes = state.votes + (memberId to vote))
    }

    fun outcome(state: RestVoteState, nowMs: Long): VoteOutcome {
        if (state.yesCount >= state.majority) return VoteOutcome.ACCEPTED
        val remaining = state.memberIds.size - state.votes.size
        if (state.yesCount + remaining < state.majority) return VoteOutcome.REJECTED
        if (nowMs >= state.deadlineMs) return VoteOutcome.REJECTED
        return VoteOutcome.PENDING
    }
}
