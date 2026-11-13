import { SIGNAL_LABEL, type StatusSignal } from './korean'

/**
 * ‘내 메시지’ — 휴대폰에서 만들고 차량 화면에서 한 번에 보내는 문장 (최대 6개).
 * 검사 규칙은 차량 앱 QuickMessagePolicy(domain/QuickMessages.kt)와 같다. 위험 표현 목록은 정책 검토용 초안.
 */

export type MessageIcon =
  | 'COFFEE' | 'FOOD' | 'FUEL' | 'CHARGE' | 'TOILET' | 'PHOTO'
  | 'SLOW' | 'LATE' | 'OK' | 'PARKING' | 'MUSIC' | 'SUN'

export const MESSAGE_ICONS: MessageIcon[] = ['COFFEE', 'FOOD', 'FUEL', 'CHARGE', 'TOILET', 'PHOTO', 'SLOW', 'LATE', 'OK', 'PARKING', 'MUSIC', 'SUN']

export interface QuickMessage {
  id: string
  text: string
  icon: MessageIcon
  /** 차량 화면 버튼 순서 (작을수록 앞) */
  order: number
}

export type MessageProblem =
  | 'EMPTY' | 'TOO_SHORT' | 'TOO_LONG' | 'TOO_LONG_SPOKEN' | 'UNSUPPORTED_CHARACTER'
  | 'CONTACT_INFO' | 'UNSAFE' | 'DUPLICATE' | 'SAME_AS_DEFAULT' | 'LIMIT_REACHED'

export const PROBLEM_TEXT: Record<MessageProblem, string> = {
  EMPTY: '내용을 입력해 주세요',
  TOO_SHORT: '두 글자 이상 입력해 주세요',
  TOO_LONG: '20자까지 쓸 수 있어요',
  TOO_LONG_SPOKEN: '조금 더 짧게 말해 주세요',
  UNSUPPORTED_CHARACTER: '글자·숫자와 기본 문장부호만 쓸 수 있어요 (아이콘은 따로 골라요)',
  CONTACT_INFO: '전화번호나 링크는 보낼 수 없어요',
  UNSAFE: '재촉하거나 위험한 운전을 권하는 말은 보낼 수 없어요',
  DUPLICATE: '이미 같은 메시지가 있어요',
  SAME_AS_DEFAULT: '기본 상황 공유 버튼과 같은 말이에요',
  LIMIT_REACHED: '메시지는 6개까지 만들 수 있어요',
}

export const MAX_MESSAGES = 6
export const MIN_LENGTH = 2
export const MAX_LENGTH = 20
export const MAX_SPOKEN_LENGTH = 40

const ALLOWED = /^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 .,!?~·()'"%/:-]+$/
const PHONE = /\d{2,4}[-. ]?\d{3,4}[-. ]?\d{4}/
const LINK = /(https?:\/\/|www\.|\.com|\.kr|\.net)/i

/** 띄어쓰기를 빼고 찾는다 ("신호 무시" = "신호무시") */
const UNSAFE_WORDS = [
  '밟아', '과속', '추월', '신호무시', '칼치기', '빨리따라', '빨리와', '빨리오세요', '더빨리', '음주',
  '씨발', '시발', 'ㅅㅂ', '개새', '병신', 'ㅂㅅ', '좆',
]
const SWEAR_WORDS = new Set(['씨발', '시발', 'ㅅㅂ', '개새', '병신', 'ㅂㅅ', '좆'])

export const normalize = (text: string) => text.trim().replace(/\s+/g, ' ')

export interface MessageCheck {
  text: string
  problems: MessageProblem[]
  ok: boolean
}

function contentProblems(t: string): MessageProblem[] {
  const out: MessageProblem[] = []
  if (PHONE.test(t) || LINK.test(t)) out.push('CONTACT_INFO')
  const compact = t.replace(/ /g, '')
  if (UNSAFE_WORDS.some((w) => compact.includes(w))) out.push('UNSAFE')
  return out
}

const check = (text: string, problems: MessageProblem[]): MessageCheck => {
  const distinct = [...new Set(problems)]
  return { text, problems: distinct, ok: distinct.length === 0 }
}

/** 휴대폰에서 만들거나 고칠 때 */
export function validate(text: string, existing: QuickMessage[] = [], editingId: string | null = null): MessageCheck {
  const t = normalize(text)
  const problems: MessageProblem[] = []
  if (editingId === null && existing.length >= MAX_MESSAGES) problems.push('LIMIT_REACHED')
  if (t.length === 0) problems.push('EMPTY')
  else if ([...t].length < MIN_LENGTH) problems.push('TOO_SHORT')
  else if ([...t].length > MAX_LENGTH) problems.push('TOO_LONG')
  if (t.length > 0 && !ALLOWED.test(t)) problems.push('UNSUPPORTED_CHARACTER')
  problems.push(...contentProblems(t))
  if (existing.some((m) => m.id !== editingId && normalize(m.text) === t)) problems.push('DUPLICATE')
  if ((Object.keys(SIGNAL_LABEL) as StatusSignal[]).some((s) => normalize(SIGNAL_LABEL[s]) === t)) problems.push('SAME_AS_DEFAULT')
  return check(t, problems)
}

/** 말로 받아 적은 문장 (글자 종류는 따지지 않는다) */
export function validateSpoken(text: string | null | undefined): MessageCheck {
  const t = normalize(text ?? '')
  const problems: MessageProblem[] = []
  if (t.length === 0) problems.push('EMPTY')
  else if ([...t].length < MIN_LENGTH) problems.push('TOO_SHORT')
  else if ([...t].length > MAX_SPOKEN_LENGTH) problems.push('TOO_LONG_SPOKEN')
  problems.push(...contentProblems(t))
  return check(t, problems)
}

/** 화면에 보여 줄 걸린 말: "빨리 따라와요" → ‘빨리 따라’ (사용자가 쓴 띄어쓰기 그대로) */
export function unsafeFragment(text: string): string | null {
  const word = UNSAFE_WORDS.find((w) => text.replace(/\s/g, '').includes(w))
  if (!word) return null
  for (let start = 0; start < text.length; start++) {
    let i = start
    let k = 0
    while (i < text.length && k < word.length) {
      if (/\s/.test(text[i])) {
        if (k === 0) break
        i++
        continue
      }
      if (text[i] !== word[k]) break
      i++
      k++
    }
    if (k === word.length) return text.slice(start, i)
  }
  return word
}

/** 보낼 수 없는 말일 때 바꿔 쓸 문장 */
export function saferSuggestions(text: string): string[] {
  const word = UNSAFE_WORDS.find((w) => text.replace(/\s/g, '').includes(w))
  if (!word) return []
  if (SWEAR_WORDS.has(word)) return ['조금 늦을 것 같아요', '다음 집결지에서 만나요']
  return ['천천히 따라갈게요', '다음 집결지에서 만나요']
}

export const DEFAULT_MESSAGES: QuickMessage[] = [
  { id: 'm1', text: '카페 들렀다 가요', icon: 'COFFEE', order: 1 },
  { id: 'm2', text: '사진 찍고 갈게요', icon: 'PHOTO', order: 2 },
  { id: 'm3', text: '조금 늦을 것 같아요', icon: 'LATE', order: 3 },
  { id: 'm4', text: '천천히 따라갈게요', icon: 'SLOW', order: 4 },
]

export const textLength = (t: string) => [...normalize(t)].length

/**
 * 보내는 쪽에서 과도한 전송을 막는다 (차량 앱 SendGuard 와 같은 규칙).
 * 2초 안에 또 누르면 무시 · 같은 메시지는 30초 뒤에 다시 · 1분에 4번까지.
 */
export type GuardDecision =
  | { allowed: true }
  | { allowed: false; reason: 'DOUBLE_TAP' | 'SAME_MESSAGE' | 'TOO_MANY'; waitMs: number }

export class SendGuard {
  private sent: { key: string; at: number }[] = []
  constructor(
    private tapLockMs = 2_000,
    private sameMessageMs = 30_000,
    private windowMs = 60_000,
    private maxPerWindow = 4,
  ) {}

  check(key: string, now: number): GuardDecision {
    const keep = Math.max(this.windowMs, this.sameMessageMs)
    this.sent = this.sent.filter((s) => now - s.at < keep)
    const last = this.sent[this.sent.length - 1]
    if (last && now - last.at < this.tapLockMs) return { allowed: false, reason: 'DOUBLE_TAP', waitMs: this.tapLockMs - (now - last.at) }
    const same = [...this.sent].reverse().find((s) => s.key === key)
    if (same && now - same.at < this.sameMessageMs) return { allowed: false, reason: 'SAME_MESSAGE', waitMs: this.sameMessageMs - (now - same.at) }
    const recent = this.sent.filter((s) => now - s.at < this.windowMs)
    if (recent.length >= this.maxPerWindow) return { allowed: false, reason: 'TOO_MANY', waitMs: this.windowMs - (now - recent[0].at) }
    return { allowed: true }
  }

  tryAcquire(key: string, now: number): GuardDecision {
    const d = this.check(key, now)
    if (d.allowed) this.sent.push({ key, at: now })
    return d
  }
}

export function guardText(d: GuardDecision): string | null {
  if (d.allowed) return null
  switch (d.reason) {
    case 'DOUBLE_TAP':
      return null
    case 'SAME_MESSAGE':
      return `방금 보낸 메시지예요 · ${Math.ceil(d.waitMs / 1000)}초 뒤에 다시 보낼 수 있어요`
    case 'TOO_MANY':
      return '잠시 뒤에 다시 보내 주세요 · 1분에 4번까지 보낼 수 있어요'
  }
}
