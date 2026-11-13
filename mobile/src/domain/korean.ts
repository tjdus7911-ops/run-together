/**
 * 한국어 조사와 안내 문장. 차량 앱(domain/Announcements.kt)과 같은 문장을 쓴다.
 * 보내는 쪽은 글자만 보내고, 받는 쪽이 이 문장으로 바꿔 소리 내어 읽는다.
 */

export type StatusSignal = 'SLEEPY' | 'REST' | 'TOILET' | 'ENERGY' | 'SLOW' | 'HELP'

export const SIGNAL_LABEL: Record<StatusSignal, string> = {
  SLEEPY: '졸려요',
  REST: '쉬었다 갈까요?',
  TOILET: '화장실 급해요',
  ENERGY: '주유·충전',
  SLOW: '천천히 갈게요',
  HELP: '도움이 필요해요',
}

function lastHangul(word: string): number | null {
  for (let i = word.length - 1; i >= 0; i--) {
    const c = word.charCodeAt(i)
    if (c >= 0xac00 && c <= 0xd7a3) return c
  }
  return null
}

export function hasBatchim(word: string): boolean {
  const c = lastHangul(word)
  return c !== null && (c - 0xac00) % 28 !== 0
}

function hasRieul(word: string): boolean {
  const c = lastHangul(word)
  return c !== null && (c - 0xac00) % 28 === 8
}

/** 을/를 */
export const eulReul = (word: string) => (hasBatchim(word) ? '을' : '를')
/** 으로/로 (ㄹ 받침은 '로') */
export const euroRo = (word: string) => (hasBatchim(word) && !hasRieul(word) ? '으로' : '로')
/** 이에요/예요 */
export const ieyo = (word: string) => (hasBatchim(word) ? '이에요' : '예요')

export function subject(name: string | null | undefined, anonymous: boolean): string {
  return anonymous || !name?.trim() ? '한 분이' : `${name}님이`
}

/** 읽을 때 어색한 기호를 정리한다 (물결표, 가운뎃점, 겹친 문장부호) */
export function spoken(text: string): string {
  const t = text
    .trim()
    .replace(/~/g, '')
    .replace(/·/g, ', ')
    .replace(/([!?.])\1+/g, '$1')
    .replace(/\s+/g, ' ')
    .trim()
  return t === '' || '.!?'.includes(t[t.length - 1]) ? t : `${t}.`
}

export const Announce = {
  forSignal(signal: StatusSignal, name: string | null, anonymous = false, stopName: string | null = null, restMinutes = 15): string {
    const who = subject(name, anonymous)
    switch (signal) {
      case 'SLEEPY':
        return stopName ? `${who} 졸리대요. ${stopName}에서 ${restMinutes}분 쉬었다 갈까요?` : `${who} 졸리대요. 가까운 쉼터에서 쉬어 갈까요?`
      case 'REST':
        return `${who} 쉬었다 가자고 해요. 좋으면 ‘좋아요’라고 말해 주세요.`
      case 'TOILET':
        return `${who} 화장실이 급하대요. 다음 휴게소에 들를게요.`
      case 'ENERGY':
        return `${who} 주유가 필요해요.`
      case 'SLOW':
        return `${who} 천천히 오신대요. 다음 집결지에서 만나요.`
      case 'HELP':
        return anonymous || !name ? '모임 차 한 대에 문제가 생겼어요. 안전한 곳에 서 있어요.' : `${name}님 차에 문제가 생겼어요. 안전한 곳에 서 있어요.`
    }
  },
  sentByMe: (signal: StatusSignal) => `모두에게 ‘${SIGNAL_LABEL[signal]}’${eulReul(SIGNAL_LABEL[signal])} 알렸어요.`,
  voteAccepted: (stopName: string, restMinutes: number) =>
    `과반이 좋다고 해서 ${stopName}${eulReul(stopName)} 모두의 경로에 넣었어요. ${restMinutes}분 쉬고 출발해요.`,
  voteRejected: (nextStopName: string) => `이번에는 쉬지 않고 ${nextStopName}에서 만나요.`,
  departure: (nextStopName: string) =>
    `곧 출발해요. 다음 집결지는 ${nextStopName}${ieyo(nextStopName)}. 따라오지 않아도 돼요, 거기서 만나요.`,
  /** 받는 사람이 듣는 문장: "민지님이 메시지를 보냈어요. 카페 들렀다 가요." */
  customMessage: (name: string | null, text: string, anonymous = false) => `${subject(name, anonymous)} 메시지를 보냈어요. ${spoken(text)}`,
  customSent: (text: string) => `모두에게 ‘${text}’${eulReul(text)} 보냈어요.`,
}
