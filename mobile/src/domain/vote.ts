/**
 * 쉬어가기 투표 (차량 앱 RestVoteEngine 과 같은 규칙).
 * 제안한 사람은 ‘좋아요’로 시작하고, 과반이 ‘좋아요’면 쉴 곳을 모두의 경로에 넣는다.
 * 1분 뒤 마감되고, 남은 표로 과반이 불가능해지면 바로 끝낸다.
 */

export type VoteChoice = 'YES' | 'NO'
export type VoteOutcome = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export const VOTE_WINDOW_MS = 60_000

export interface RestVote {
  proposerId: string
  stopName: string
  restMinutes: number
  memberIds: string[]
  votes: Record<string, VoteChoice>
  deadline: number
}

export const yesCount = (v: RestVote) => Object.values(v.votes).filter((x) => x === 'YES').length
export const noCount = (v: RestVote) => Object.values(v.votes).filter((x) => x === 'NO').length
/** 과반: 6명이면 4명 */
export const majority = (v: RestVote) => Math.floor(v.memberIds.length / 2) + 1

export function startVote(proposerId: string, stopName: string, restMinutes: number, memberIds: string[], now: number): RestVote {
  return { proposerId, stopName, restMinutes, memberIds, votes: { [proposerId]: 'YES' }, deadline: now + VOTE_WINDOW_MS }
}

export function outcome(v: RestVote, now: number): VoteOutcome {
  if (yesCount(v) >= majority(v)) return 'ACCEPTED'
  const remaining = v.memberIds.length - Object.keys(v.votes).length
  if (yesCount(v) + remaining < majority(v)) return 'REJECTED'
  if (now >= v.deadline) return 'REJECTED'
  return 'PENDING'
}

export function castVote(v: RestVote, memberId: string, choice: VoteChoice, now: number): RestVote {
  if (!v.memberIds.includes(memberId)) return v
  if (outcome(v, now) !== 'PENDING') return v
  return { ...v, votes: { ...v.votes, [memberId]: choice } }
}

/** 말로 한 답을 좋아요(true) / 괜찮아요(false)로 읽는다. 모르겠으면 null (차량 앱 VoiceAnswerParser) */
export function parseVoteAnswer(text: string | null | undefined): boolean | null {
  const t = (text ?? '').replace(/[^\p{L}\p{N}]/gu, '')
  if (!t) return null
  if (['괜찮', '아니', '싫어', '그냥가', '안쉬'].some((w) => t.includes(w))) return false
  if (['좋아', '좋습니다', '그래', '쉬어가', '쉬자', '콜'].some((w) => t.includes(w)) || t === '네' || t === '응') return true
  return null
}

/** ‘말로 하기’로 고르는 상황: 천천히 갈게요 · 도움이 필요해요 */
export function parseSpokenSignal(text: string | null | undefined): 'SLOW' | 'HELP' | null {
  const t = (text ?? '').replace(/[^\p{L}\p{N}]/gu, '')
  if (!t) return null
  if (['도움', '도와', '살려', '사고', '고장', '문제'].some((w) => t.includes(w))) return 'HELP'
  if (['천천히', '늦게', '먼저가', '따라갈'].some((w) => t.includes(w))) return 'SLOW'
  return null
}
