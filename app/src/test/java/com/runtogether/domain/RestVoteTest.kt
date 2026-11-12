package com.runtogether.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RestVoteTest {

    private val stop = StopCandidate("munmak", "문막휴게소", 112.0)
    private val members = listOf("minji", "me", "sua", "taehyun", "jiwon", "haneul")

    private fun start(now: Long = 0L) = RestVoteEngine.start("minji", stop, 15, members, now)

    @Test
    fun proposerCountsAsYesAndFourOfSixIsMajority() {
        var v = start()
        assertEquals(4, v.majority)
        assertEquals(1, v.yesCount)
        v = RestVoteEngine.cast(v, "sua", Vote.YES, 1_000)
        v = RestVoteEngine.cast(v, "jiwon", Vote.YES, 2_000)
        v = RestVoteEngine.cast(v, "taehyun", Vote.NO, 2_500)
        assertEquals(VoteOutcome.PENDING, RestVoteEngine.outcome(v, 3_000))
        v = RestVoteEngine.cast(v, "me", Vote.YES, 4_000)
        assertEquals(VoteOutcome.ACCEPTED, RestVoteEngine.outcome(v, 4_000))
    }

    @Test
    fun notEnoughYesByTheDeadlineIsRejected() {
        var v = start()
        v = RestVoteEngine.cast(v, "sua", Vote.YES, 1_000)
        v = RestVoteEngine.cast(v, "jiwon", Vote.YES, 1_000)
        assertEquals(VoteOutcome.PENDING, RestVoteEngine.outcome(v, 59_999))
        assertEquals(VoteOutcome.REJECTED, RestVoteEngine.outcome(v, RestVoteEngine.VOTE_WINDOW_MS))
    }

    @Test
    fun voteEndsEarlyWhenMajorityBecomesImpossible() {
        var v = start()
        v = RestVoteEngine.cast(v, "me", Vote.NO, 1_000)
        v = RestVoteEngine.cast(v, "sua", Vote.NO, 1_000)
        v = RestVoteEngine.cast(v, "taehyun", Vote.NO, 1_000)
        // 좋아요 1 + 남은 2표 = 3 < 4
        assertEquals(VoteOutcome.REJECTED, RestVoteEngine.outcome(v, 1_000))
    }

    @Test
    fun votesFromOutsidersOrAfterTheEndAreIgnored() {
        var v = start()
        v = RestVoteEngine.cast(v, "stranger", Vote.YES, 1_000)
        assertEquals(1, v.votes.size)
        v = RestVoteEngine.cast(v, "sua", Vote.YES, RestVoteEngine.VOTE_WINDOW_MS + 1)
        assertEquals(1, v.votes.size)
    }
}
