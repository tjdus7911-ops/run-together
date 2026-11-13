import { describe, expect, it } from 'vitest'
import { Announce, eulReul, euroRo, ieyo, spoken } from './korean'
import { SendGuard, guardText, saferSuggestions, unsafeFragment, validate, validateSpoken, type QuickMessage } from './messages'
import { castVote, outcome, parseSpokenSignal, parseVoteAnswer, startVote, yesCount } from './vote'

describe('한국어 조사 · 안내 문장 (차량 앱과 같은 문장)', () => {
  it('받침에 따라 조사를 고른다', () => {
    expect(eulReul('졸려요')).toBe('를')
    expect(eulReul('문막휴게소')).toBe('를')
    expect(eulReul('횡성')).toBe('을')
    expect(euroRo('덕평')).toBe('으로')
    expect(euroRo('강릉 안목해변')).toBe('으로')
    expect(euroRo('서울')).toBe('로')
    expect(ieyo('덕평자연휴게소')).toBe('예요')
    expect(ieyo('횡성')).toBe('이에요')
  })

  it('받는 사람이 듣는 문장', () => {
    expect(Announce.customMessage('민지', '카페 들렀다 가요')).toBe('민지님이 메시지를 보냈어요. 카페 들렀다 가요.')
    expect(Announce.customMessage('민지', '사진 찍고 갈게요!!', true)).toBe('한 분이 메시지를 보냈어요. 사진 찍고 갈게요!')
    expect(Announce.customSent('카페 들렀다 가요')).toBe('모두에게 ‘카페 들렀다 가요’를 보냈어요.')
    expect(Announce.forSignal('SLEEPY', '민지', false, '문막휴게소', 15)).toBe('민지님이 졸리대요. 문막휴게소에서 15분 쉬었다 갈까요?')
    expect(Announce.sentByMe('SLEEPY')).toBe('모두에게 ‘졸려요’를 알렸어요.')
    expect(Announce.departure('덕평자연휴게소')).toBe('곧 출발해요. 다음 집결지는 덕평자연휴게소예요. 따라오지 않아도 돼요, 거기서 만나요.')
    expect(spoken('주유·충전하고 갈게요~')).toBe('주유, 충전하고 갈게요.')
  })
})

describe('내 메시지 검사 (QuickMessagePolicy 와 같은 규칙)', () => {
  const four: QuickMessage[] = [
    { id: 'm1', text: '카페 들렀다 가요', icon: 'COFFEE', order: 1 },
    { id: 'm2', text: '사진 찍고 갈게요', icon: 'PHOTO', order: 2 },
  ]

  it('2~20자 · 글자 종류 · 겹침 · 기본 메시지', () => {
    expect(validate('카페 들렀다 가요', [], null).ok).toBe(true)
    expect(validate('  카페   들렀다 가요 ', [], null).text).toBe('카페 들렀다 가요')
    expect(validate('가', [], null).problems).toContain('TOO_SHORT')
    expect(validate('가'.repeat(21), [], null).problems).toContain('TOO_LONG')
    expect(validate('커피 ☕ 마셔요', [], null).problems).toContain('UNSUPPORTED_CHARACTER')
    expect(validate('카페 들렀다 가요', four, null).problems).toContain('DUPLICATE')
    expect(validate('카페 들렀다 가요', four, 'm1').ok).toBe(true)
    expect(validate('졸려요', [], null).problems).toContain('SAME_AS_DEFAULT')
    const six = Array.from({ length: 6 }, (_, i): QuickMessage => ({ id: `x${i}`, text: `메시지 ${i}`, icon: 'OK', order: i }))
    expect(validate('새 메시지', six, null).problems).toContain('LIMIT_REACHED')
  })

  it('재촉 · 위험 운전 · 연락처는 보낼 수 없다', () => {
    expect(validate('빨리 따라와요', [], null).problems).toContain('UNSAFE')
    expect(validate('신호 무시하고 와', [], null).problems).toContain('UNSAFE')
    expect(validate('010-1234-5678', [], null).problems).toContain('CONTACT_INFO')
    expect(unsafeFragment('빨리 따라와요')).toBe('빨리 따라')
    expect(saferSuggestions('빨리 따라와요')).toEqual(['천천히 따라갈게요', '다음 집결지에서 만나요'])
    expect(saferSuggestions('카페 들렀다 가요')).toEqual([])
  })

  it('말로 받아 적은 문장은 40자까지', () => {
    expect(validateSpoken('도착하면 커피는 제가 살게요').ok).toBe(true)
    expect(validateSpoken('').problems).toContain('EMPTY')
    expect(validateSpoken('가'.repeat(41)).problems).toContain('TOO_LONG_SPOKEN')
  })
})

describe('보내기 제한 (SendGuard)', () => {
  it('두 번 눌림 · 같은 메시지 30초 · 1분에 4번', () => {
    const g = new SendGuard()
    expect(g.tryAcquire('a', 0).allowed).toBe(true)
    const tap = g.tryAcquire('b', 500)
    expect(tap.allowed).toBe(false)
    expect(guardText(tap)).toBeNull()
    expect(guardText(g.tryAcquire('a', 3_000))).toBe('방금 보낸 메시지예요 · 27초 뒤에 다시 보낼 수 있어요')
    expect(g.tryAcquire('b', 3_000).allowed).toBe(true)
    expect(g.tryAcquire('c', 6_000).allowed).toBe(true)
    expect(g.tryAcquire('d', 9_000).allowed).toBe(true)
    const many = g.tryAcquire('e', 12_000)
    expect(many.allowed).toBe(false)
    expect(guardText(many)).toBe('잠시 뒤에 다시 보내 주세요 · 1분에 4번까지 보낼 수 있어요')
    expect(g.tryAcquire('e', 61_000).allowed).toBe(true)
  })
})

describe('쉬어가기 투표 (RestVoteEngine)', () => {
  const ids = ['junho', 'minji', 'sua', 'taehyun', 'jiwon', 'me']

  it('제안자 포함 과반(6명 중 4명)이면 통과', () => {
    let v = startVote('minji', '문막휴게소', 15, ids, 0)
    v = castVote(v, 'sua', 'YES', 1)
    v = castVote(v, 'jiwon', 'YES', 2)
    v = castVote(v, 'taehyun', 'NO', 3)
    expect(yesCount(v)).toBe(3)
    expect(outcome(v, 4)).toBe('PENDING')
    v = castVote(v, 'me', 'YES', 5)
    expect(outcome(v, 6)).toBe('ACCEPTED')
    // 끝난 투표에는 더 넣지 않는다
    expect(castVote(v, 'junho', 'NO', 7)).toBe(v)
  })

  it('과반이 불가능해지면 바로 부결 · 1분 뒤 마감', () => {
    let v = startVote('minji', '문막휴게소', 15, ids, 0)
    for (const id of ['sua', 'jiwon', 'taehyun']) v = castVote(v, id, 'NO', 1)
    expect(outcome(v, 2)).toBe('REJECTED')
    const w = startVote('minji', '문막휴게소', 15, ids, 0)
    expect(outcome(w, 60_000)).toBe('REJECTED')
  })

  it('말로 한 답', () => {
    expect(parseVoteAnswer('좋아요')).toBe(true)
    expect(parseVoteAnswer('응')).toBe(true)
    expect(parseVoteAnswer('괜찮아요')).toBe(false)
    expect(parseVoteAnswer('아니 그냥 가자')).toBe(false)
    expect(parseVoteAnswer('음')).toBeNull()
    expect(parseSpokenSignal('천천히 갈게요')).toBe('SLOW')
    expect(parseSpokenSignal('도움이 필요해요')).toBe('HELP')
    expect(parseSpokenSignal('배고파요')).toBeNull()
  })
})
