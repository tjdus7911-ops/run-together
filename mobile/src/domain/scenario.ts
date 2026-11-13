/**
 * 기획서 시나리오 A — 6대가 잠실에서 강릉 안목해변으로 가는 모임 드라이브 (시연 데이터).
 * 휴대폰 앱의 ‘나’는 Pleos 차량이 아닌 차로 휴대폰 링크로 참여한 하늘님이다.
 * 동기화 서버가 없는 시연판이라 다른 차의 움직임·응답은 아래 값으로 흉내 낸다 (차량 앱 DemoScenario 와 같은 이야기).
 */

export interface Member {
  id: string
  name: string
  color: string
  leader?: boolean
  ev?: boolean
  phone?: boolean
}

export const ME = 'me'

/** 회원 색: 흰 바탕과 밝은 지도에서 잘 보이고 흰 이니셜이 읽히는 짙은 색 (피그마 member/1~6, 차량 앱과 같은 값) */
export const OTHERS: Member[] = [
  { id: 'junho', name: '준호', color: '#2563EB', leader: true },
  { id: 'minji', name: '민지', color: '#C2410C' },
  { id: 'sua', name: '수아', color: '#0E7490' },
  { id: 'taehyun', name: '태현', color: '#7C3AED', ev: true },
  { id: 'jiwon', name: '지원', color: '#BE185D' },
]
export const ME_COLOR = '#475569'

export function members(myName: string): Member[] {
  return [...OTHERS, { id: ME, name: myName || '나', color: ME_COLOR, phone: true }]
}

export const GROUP = {
  name: '강릉 바다 드라이브',
  date: '11월 15일 일요일',
  shortDate: '11월 15일(일)',
  depart: '07:30',
  arrive: '10:40',
  totalKm: 235,
  totalText: '235km · 약 3시간 10분',
  code: '482719',
  leaderId: 'junho',
}

export interface CourseStop {
  id: string
  name: string
  short: string
  label: string
  time: string
  rest?: number
  note?: string
  lat: number
  lng: number
}

// 좌표는 시연용 대략값
export const COURSE: CourseStop[] = [
  { id: 'jamsil', name: '잠실종합운동장 주차장', short: '잠실', label: '출발', time: '07:30', lat: 37.5152, lng: 127.0729 },
  { id: 'deokpyeong', name: '덕평자연휴게소', short: '덕평', label: '집결 1', time: '08:12', rest: 20, note: '주유·충전', lat: 37.2414, lng: 127.3713 },
  { id: 'hoengseong', name: '횡성휴게소', short: '횡성', label: '집결 2', time: '09:35', rest: 15, lat: 37.489, lng: 128.0273 },
  { id: 'anmok', name: '강릉 안목해변', short: '강릉', label: '도착', time: '10:40', lat: 37.7717, lng: 128.9474 },
]

/** 투표로 들어오는 쉴 곳 */
export const MUNMAK = { id: 'munmak', name: '문막휴게소', short: '문막', lat: 37.317, lng: 127.8288, km: 6, minutes: 5, amenities: '주유소·카페', rest: 15 }

/** 모임 만들기에서 경로 위에 추천하는 쉴 곳 (공공데이터 자리) */
export const SUGGESTED_STOPS = [
  { id: 'yeoju', name: '여주휴게소', km: 78, eta: '08:28', tags: ['주유', '충전', '식당'] },
  { id: 'munmak', name: '문막휴게소', km: 112, eta: '08:54', tags: ['주유', '식당'] },
]

export type StepId = 'lobby' | 'drive1' | 'regroup1' | 'drive2' | 'alert' | 'changed' | 'ended'

export interface NextStop {
  name: string
  short: string
  km: number
  minutes: number
  eta: string
  lat: number
  lng: number
}

export interface Step {
  id: StepId
  clock: string
  title: string
  /** 차가 움직이는 단계 (운전자 모드 잠금 · 내 메시지 편집 잠금) */
  moving: boolean
  next?: NextStop
  /** 모임 간격 (선두~후미) */
  gapKm?: number
}

const deokpyeong = COURSE[1]
const hoengseong = COURSE[2]

export const STEPS: Step[] = [
  { id: 'lobby', clock: '07:25', title: '대기실 · 출발 전', moving: false },
  {
    id: 'drive1', clock: '08:01', title: '주행 중 · 덕평 가는 길', moving: true, gapKm: 3.4,
    next: { name: deokpyeong.name, short: deokpyeong.short, km: 21, minutes: 16, eta: '08:16', lat: deokpyeong.lat, lng: deokpyeong.lng },
  },
  { id: 'regroup1', clock: '08:15', title: '덕평 집결지 도착', moving: false },
  {
    id: 'drive2', clock: '08:41', title: '다시 출발 · 횡성 가는 길', moving: true, gapKm: 2.8,
    next: { name: hoengseong.name, short: hoengseong.short, km: 72, minutes: 58, eta: '09:39', lat: hoengseong.lat, lng: hoengseong.lng },
  },
  {
    id: 'alert', clock: '08:55', title: '민지 ‘졸려요’ → 쉬어가기 투표', moving: true, gapKm: 3.1,
    next: { name: hoengseong.name, short: hoengseong.short, km: 54, minutes: 44, eta: '09:39', lat: hoengseong.lat, lng: hoengseong.lng },
  },
  {
    id: 'changed', clock: '08:56', title: '다음 집결지 변경 → 문막', moving: true, gapKm: 3.1,
    next: { name: MUNMAK.name, short: MUNMAK.short, km: MUNMAK.km, minutes: MUNMAK.minutes, eta: '09:00', lat: MUNMAK.lat, lng: MUNMAK.lng },
  },
  { id: 'ended', clock: '10:49', title: '강릉 도착 · 여행 종료', moving: false },
]

export const stepOf = (id: StepId) => STEPS.find((s) => s.id === id)!
export const stepIndex = (id: StepId) => STEPS.findIndex((s) => s.id === id)

/** 대기실(07:25): 시동을 켜면 차량은 저절로 준비 완료, 휴대폰은 버튼으로 */
export const LOBBY_READY = ['junho', 'minji', 'sua', 'taehyun']

/** 주행 중 수아가 차량 화면 ‘내 메시지’로 보내는 문장 (차량 앱 SUA_MESSAGE) */
export const SUA_MESSAGE = '주유하고 바로 따라갈게요'

/** 시연에서 다른 회원이 투표하는 방식 (민지가 제안 · 수아·지원 좋아요 · 태현 괜찮아요 · 준호는 나중에 좋아요) */
export const SIMULATED_VOTES: { id: string; choice: 'YES' | 'NO'; afterMs: number }[] = [
  { id: 'sua', choice: 'YES', afterMs: 1_200 },
  { id: 'jiwon', choice: 'YES', afterMs: 1_900 },
  { id: 'taehyun', choice: 'NO', afterMs: 2_600 },
]

/** 홈 화면의 예정·지난 모임 (시연 값) */
export const UPCOMING = [{ name: '양평 두물머리 브런치', month: '11/22', weekday: '일', detail: '09:00 출발 · 4대 · 모임장 민지' }]
export const PAST = [{ name: '가평 단풍 드라이브', month: '10/25', weekday: '일', detail: '함께 달린 182km · 같이 쉰 2번' }]
export const COPY_SOURCES = [
  { name: '가평 단풍 드라이브', detail: '10/25 · 코스 3곳 · 4대' },
  { name: '춘천 닭갈비 투어', detail: '9/13 · 코스 2곳 · 5대' },
]
