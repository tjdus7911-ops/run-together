import { useMemo, useSyncExternalStore } from 'react'
import { Announce, SIGNAL_LABEL, type StatusSignal } from './domain/korean'
import { DEFAULT_MESSAGES, SendGuard, guardText, type QuickMessage } from './domain/messages'
import { COURSE, GROUP, LOBBY_READY, ME, MUNMAK, OTHERS, SIMULATED_VOTES, SUA_MESSAGE, members, stepOf, type StepId } from './domain/scenario'
import { VOTE_WINDOW_MS, castVote, outcome, parseSpokenSignal, parseVoteAnswer, startVote, type RestVote, type VoteChoice } from './domain/vote'
import * as B from './platform/browser'

/**
 * 앱 상태와 시연 진행.
 * 동기화 서버가 없는 시연판이라, 다른 차의 응답·메시지는 시나리오 A 순서대로 흉내 낸다.
 * 내가 하는 일(준비 완료 · 상황 공유 · 내 메시지 · 투표 · 위치 · 소리 내어 읽기)은 실제 브라우저 기능으로 동작한다.
 */

export type Role = 'driver' | 'passenger'
export type Fuel = 'plenty' | 'normal' | 'low'

export interface Profile {
  name: string
  /** 참여 설정을 마쳤는지 */
  joined: boolean
  location: 'share' | 'viewOnly'
  role: Role
  fuel: Fuel
  ev: boolean
  model: string
}

export interface Settings {
  anonymous: boolean
  readAloud: boolean
  vibrate: boolean
  keepAwake: boolean
  sharing: boolean
  lastNavi: B.NaviApp
}

export interface FeedItem {
  id: string
  memberId: string
  kind: string
  text: string
}

export type Sheet = null | 'status' | 'vote' | 'changed' | 'navi' | 'demo' | 'leave'

export interface Trip {
  /** 이 모임에 참여 중인지 (여행을 마치거나 나가면 false) */
  active: boolean
  step: StepId
  groupName: string
  ready: string[]
  meReady: boolean
  departing: boolean
  arrived: string[]
  kmLeft: number
  vote: RestVote | null
  voteResult: 'ACCEPTED' | 'REJECTED' | null
  munmak: boolean
  feed: FeedItem[]
  sheet: Sheet
  listening: null | 'vote' | 'signal'
  /** 운전자 모드에서 동승자가 대신 쓰는 중 */
  unlocked: boolean
  demoOffline: boolean
}

export interface SavedTrip {
  name: string
  date: string
  km: number
  rests: number
  cars: number
}

export interface AppState {
  profile: Profile
  settings: Settings
  messages: QuickMessage[]
  history: SavedTrip[]
  trip: Trip
  caption: { text: string; speakerId?: string } | null
  toast: { text: string; id: number } | null
  geo: { status: B.GeoStatus; fix: B.GeoFix | null }
  online: boolean
}

const K = { profile: 'rt.profile', settings: 'rt.settings', messages: 'rt.messages', history: 'rt.history', trip: 'rt.trip' }

const defaultProfile: Profile = { name: '하늘', joined: false, location: 'share', role: 'driver', fuel: 'normal', ev: false, model: '' }
const defaultSettings: Settings = { anonymous: false, readAloud: true, vibrate: true, keepAwake: true, sharing: true, lastNavi: 'kakao' }

function blankTrip(prev?: Partial<Trip>): Trip {
  return {
    active: prev?.active ?? false,
    step: 'lobby',
    groupName: prev?.groupName ?? GROUP.name,
    ready: LOBBY_READY,
    meReady: false,
    departing: false,
    arrived: [],
    kmLeft: 0,
    vote: null,
    voteResult: null,
    munmak: false,
    feed: [],
    sheet: null,
    listening: null,
    unlocked: false,
    demoOffline: prev?.demoOffline ?? false,
  }
}

let feedSeq = 0
const item = (memberId: string, kind: string, text: string): FeedItem => ({ id: `f${++feedSeq}`, memberId, kind, text })

function acceptedVote(ids: string[]): RestVote {
  let v = startVote('minji', MUNMAK.name, MUNMAK.rest, ids, Date.now())
  for (const [id, c] of [['sua', 'YES'], ['jiwon', 'YES'], ['taehyun', 'NO'], [ME, 'YES']] as const) v = castVote(v, id, c, Date.now())
  return v
}

/** 단계에 들어갈 때의 화면 값 */
function stateForStep(step: StepId, prev: Trip, myName: string): Trip {
  const base: Trip = { ...blankTrip(prev), active: prev.active, step }
  const everyone = members(myName).map((m) => m.id)
  switch (step) {
    case 'lobby':
      return base
    case 'drive1':
      return { ...base, ready: everyone, meReady: true, kmLeft: 21, feed: [item('junho', '출발 안내', '덕평자연휴게소에서 만나요')] }
    case 'regroup1':
      return { ...base, ready: everyone, meReady: true, arrived: everyone.filter((id) => id !== 'jiwon'), feed: [item('sua', '내 메시지', SUA_MESSAGE)] }
    case 'drive2':
      return { ...base, ready: everyone, meReady: true, kmLeft: 72, feed: [item('junho', '출발 안내', '횡성휴게소에서 만나요')] }
    case 'alert':
      return {
        ...base,
        ready: everyone,
        meReady: true,
        kmLeft: 54,
        vote: startVote('minji', MUNMAK.name, MUNMAK.rest, everyone, Date.now()),
        sheet: 'vote',
        feed: [item('minji', '상황 공유', SIGNAL_LABEL.SLEEPY), item('junho', '출발 안내', '횡성휴게소에서 만나요')],
      }
    case 'changed':
      return {
        ...base,
        ready: everyone,
        meReady: true,
        kmLeft: MUNMAK.km,
        munmak: true,
        vote: acceptedVote(everyone),
        voteResult: 'ACCEPTED',
        sheet: 'changed',
        feed: [item('junho', '집결지 변경', '문막휴게소에서 15분 쉬어요'), item('minji', '상황 공유', SIGNAL_LABEL.SLEEPY)],
      }
    case 'ended':
      return { ...base, ready: everyone, meReady: true, munmak: true }
  }
}

function initial(): AppState {
  const profile = B.load(K.profile, defaultProfile)
  const savedTrip = B.load<{ active: boolean; step: StepId; groupName: string }>(K.trip, { active: false, step: 'lobby', groupName: GROUP.name })
  const trip = stateForStep(savedTrip.step, { ...blankTrip(savedTrip), active: savedTrip.active }, profile.name)
  return {
    profile,
    settings: B.load(K.settings, defaultSettings),
    messages: B.loadList(K.messages, DEFAULT_MESSAGES),
    history: B.loadList<SavedTrip>(K.history, []),
    trip,
    caption: null,
    toast: null,
    geo: { status: 'idle', fix: null },
    online: typeof navigator === 'undefined' ? true : navigator.onLine,
  }
}

// ---------------- 저장소 ----------------

const guard = new SendGuard()
let timers: number[] = []


let state: AppState = initial()
let saved: AppState = state
const listeners = new Set<() => void>()

export const getState = () => state

export function setState(fn: (s: AppState) => AppState) {
  const next = fn(state)
  if (next === state) return
  state = next
  persist(next)
  listeners.forEach((l) => l())
}

function persist(s: AppState) {
  if (s.profile !== saved.profile) B.save(K.profile, s.profile)
  if (s.settings !== saved.settings) B.save(K.settings, s.settings)
  if (s.messages !== saved.messages) B.save(K.messages, s.messages)
  if (s.history !== saved.history) B.save(K.history, s.history)
  if (s.trip.step !== saved.trip.step || s.trip.active !== saved.trip.active || s.trip.groupName !== saved.trip.groupName) {
    B.save(K.trip, { active: s.trip.active, step: s.trip.step, groupName: s.trip.groupName })
  }
  saved = s
}

const subscribe = (l: () => void) => {
  listeners.add(l)
  return () => {
    listeners.delete(l)
  }
}

export function useApp<T>(select: (s: AppState) => T): T {
  return useSyncExternalStore(subscribe, () => select(state), () => select(state))
}

export function useMembers() {
  const name = useApp((s) => s.profile.name)
  return useMemo(() => members(name), [name])
}

export const memberName = (id: string) => (id === ME ? state.profile.name : OTHERS.find((m) => m.id === id)?.name ?? '')
const memberIds = () => members(state.profile.name).map((m) => m.id)

export const setTrip = (fn: (t: Trip) => Trip) => setState((s) => ({ ...s, trip: fn(s.trip) }))
export const setProfile = (patch: Partial<Profile>) => setState((s) => ({ ...s, profile: { ...s.profile, ...patch } }))
export const setSettings = (patch: Partial<Settings>) => setState((s) => ({ ...s, settings: { ...s.settings, ...patch } }))
export const setMessages = (list: QuickMessage[]) =>
  setState((s) => ({ ...s, messages: list.map((m, i) => ({ ...m, order: i + 1 })) }))
export const openSheet = (sheet: Sheet) => setTrip((t) => ({ ...t, sheet }))
export const closeSheet = () => setTrip((t) => (t.sheet ? { ...t, sheet: null } : t))

let toastSeq = 0
export function toast(text: string) {
  const id = ++toastSeq
  setState((s) => ({ ...s, toast: { text, id } }))
  setTimeout(() => setState((s) => (s.toast?.id === id ? { ...s, toast: null } : s)), 3_400)
}

// ---------------- 소리 내어 읽기 (한 번에 하나 · 안전 안내 먼저) ----------------

export type Priority = 'SAFETY' | 'GROUP' | 'MESSAGE' | 'CONFIRM'
const ORDER: Record<Priority, number> = { SAFETY: 0, GROUP: 1, MESSAGE: 2, CONFIRM: 3 }
const MAX_WAIT: Record<Priority, number> = { SAFETY: 30_000, GROUP: 20_000, MESSAGE: 20_000, CONFIRM: 6_000 }

interface Utter {
  text: string
  priority: Priority
  speakerId?: string
  speak: boolean
  at: number
  done: () => void
}

const queue: Utter[] = []
let playing: Utter | null = null

/** 자막으로 보여 주고, ‘소리 내어 읽기’가 켜져 있으면 읽는다. 다 읽으면(또는 건너뛰면) 끝난다 */
export function announce(text: string, priority: Priority, speakerId?: string): Promise<void> {
  return new Promise((resolve) => {
    if (playing?.text === text || queue.some((u) => u.text === text)) return resolve()
    queue.push({ text, priority, speakerId, speak: state.settings.readAloud, at: Date.now(), done: resolve })
    if (state.settings.vibrate && priority !== 'CONFIRM') B.vibrate(priority === 'SAFETY' ? [180, 80, 180] : 120)
    pump()
  })
}

function pump() {
  if (playing) return
  const now = Date.now()
  for (let i = queue.length - 1; i >= 0; i--) {
    if (now - queue[i].at > MAX_WAIT[queue[i].priority]) queue.splice(i, 1)[0].done()
  }
  if (!queue.length) return
  queue.sort((a, b) => ORDER[a.priority] - ORDER[b.priority] || a.at - b.at)
  const u = queue.shift()!
  playing = u
  setState((s) => ({ ...s, caption: { text: u.text, speakerId: u.speakerId } }))
  let finished = false
  let timer = 0
  const finish = () => {
    if (finished) return
    finished = true
    clearTimeout(timer)
    playing = null
    setTimeout(() => setState((s) => (s.caption?.text === u.text && !playing ? { ...s, caption: null } : s)), 900)
    u.done()
    setTimeout(pump, 300)
  }
  const readMs = 2_200 + u.text.length * 85
  const started = u.speak && B.speak(u.text, finish)
  timer = window.setTimeout(finish, started ? readMs + 9_000 : readMs)
}

export function clearAnnouncements() {
  queue.splice(0).forEach((u) => u.done())
  B.stopSpeaking()
  setState((s) => (s.caption ? { ...s, caption: null } : s))
}

/** ‘미리 듣기’ 같은 바로 읽기 (순서 없이) */
export function previewSpeech(text: string) {
  B.unlockSpeech()
  if (!B.speak(text, () => {})) toast('이 브라우저는 소리 내어 읽기를 지원하지 않아요')
}

// ---------------- 시연 진행 ----------------

function later(ms: number, fn: () => void) {
  timers.push(window.setTimeout(fn, ms))
}

function clearTimers() {
  timers.forEach(clearTimeout)
  timers = []
  B.stopListening()
}

function addFeed(memberId: string, kind: string, text: string) {
  setTrip((t) => ({ ...t, feed: [item(memberId, kind, text), ...t.feed].slice(0, 20) }))
}

/** 시연 단계에 들어간다. byHand: 시연 패널에서 직접 넘김 (앞 단계 안내는 읽지 않는다) */
export function enterStep(step: StepId, opts: { byHand?: boolean; keepVote?: boolean } = {}) {
  clearTimers()
  if (opts.byHand) clearAnnouncements()
  setTrip((t) => {
    const next = stateForStep(step, { ...t, active: true }, state.profile.name)
    return opts.keepVote && t.vote ? { ...next, vote: t.vote, voteResult: t.voteResult, feed: [...next.feed.slice(0, 1), ...t.feed].slice(0, 20) } : next
  })
  switch (step) {
    case 'drive1':
      tickDistance(1_400, 'regroup1')
      later(6_000, () => receiveMessage('sua', SUA_MESSAGE))
      break
    case 'regroup1':
      announce('덕평자연휴게소에 도착했어요. 20분 쉬고 8시 35분에 출발해요.', 'GROUP')
      later(4_500, () => {
        setTrip((t) => ({ ...t, arrived: [...new Set([...t.arrived, 'jiwon'])] }))
        announce('지원님이 도착했어요. 모두 모였어요.', 'GROUP', 'jiwon')
      })
      later(16_000, () => {
        announce(Announce.departure(COURSE[2].name), 'GROUP', 'junho').then(() => {
          if (state.trip.step === 'regroup1') enterStep('drive2')
        })
      })
      break
    case 'drive2':
      tickDistance(1_600)
      later(10_000, () => enterStep('alert'))
      break
    case 'alert':
      runAlert()
      break
    case 'changed':
      announce(Announce.voteAccepted(MUNMAK.name, MUNMAK.rest), 'GROUP')
      break
    case 'ended':
      announce('강릉 안목해변에 모두 함께 도착했어요. 위치 공유를 멈췄어요.', 'GROUP')
      break
    case 'lobby':
      break
  }
}

/** 남은 거리를 조금씩 줄인다 (시연). 0이 되면 [arriveAt] 단계로 */
function tickDistance(everyMs: number, arriveAt?: StepId) {
  const tick = () => {
    const t = state.trip
    if (!stepOf(t.step).moving) return
    if (t.kmLeft <= 1) {
      if (arriveAt) enterStep(arriveAt)
      return
    }
    setTrip((tt) => ({ ...tt, kmLeft: tt.kmLeft - 1 }))
    later(everyMs, tick)
  }
  later(everyMs, tick)
}

function receiveMessage(fromId: string, text: string) {
  addFeed(fromId, '내 메시지', text)
  announce(Announce.customMessage(memberName(fromId), text), 'MESSAGE', fromId)
}

// ---------------- 대기실 ----------------

export function markReady() {
  if (state.trip.meReady) return
  B.unlockSpeech()
  setTrip((t) => ({ ...t, meReady: true }))
  announce('준비 완료를 알렸어요. 모임장이 출발 안내를 보내면 알려 드릴게요.', 'CONFIRM')
  later(2_600, () => setTrip((t) => ({ ...t, ready: [...new Set([...t.ready, 'jiwon'])] })))
  later(4_400, () => {
    setTrip((t) => ({ ...t, departing: true }))
    announce(Announce.departure(COURSE[1].name), 'GROUP', 'junho').then(() => {
      if (state.trip.step === 'lobby' && state.trip.departing) {
        enterStep('drive1')
        onNavigate?.('/trip')
      }
    })
  })
}

/** 화면 이동을 엔진에서 해야 할 때 (App 이 연결) */
let onNavigate: ((path: string) => void) | null = null
export function connectNavigation(fn: (path: string) => void) {
  onNavigate = fn
}

// ---------------- 상황 공유 · 내 메시지 ----------------

export function sendSignal(signal: StatusSignal) {
  B.unlockSpeech()
  const t = state.trip
  if (t.vote && !t.voteResult) {
    toast('이미 쉬어가기 투표가 진행 중이에요')
    return
  }
  const d = guard.tryAcquire(`status:${signal}`, Date.now())
  if (!d.allowed) {
    const msg = guardText(d)
    if (msg) toast(msg)
    return
  }
  addFeed(ME, '상황 공유', SIGNAL_LABEL[signal])
  closeSheet()
  announce(Announce.sentByMe(signal) + (state.settings.anonymous ? ' 이름 대신 ‘한 분이’로 알렸어요.' : ''), 'CONFIRM')
  if (signal === 'HELP') {
    later(1_600, () => announce('모임장 준호님에게 내 위치를 함께 보냈어요.', 'GROUP'))
    return
  }
  if (signal === 'SLOW') return
  if (t.step === 'drive2') proposeRest()
  else if (t.step === 'drive1') later(1_500, () => announce('다음 집결지 덕평자연휴게소가 가까워서 거기서 쉬어 가요.', 'GROUP'))
  else if (t.step === 'changed') later(1_500, () => announce('다음 집결지 문막휴게소에서 쉬어 가요.', 'GROUP'))
}

/** 내가 낸 쉬어가기 제안 → 가장 가까운 쉴 곳으로 투표 (시연: 다른 회원 응답) */
function proposeRest() {
  setTrip((t) => ({ ...t, vote: startVote(ME, MUNMAK.name, MUNMAK.rest, memberIds(), Date.now()), voteResult: null, sheet: 'vote' }))
  const answers: [string, VoteChoice, number][] = [['minji', 'YES', 1_200], ['sua', 'YES', 1_900], ['taehyun', 'NO', 2_600], ['jiwon', 'YES', 3_300]]
  for (const [id, c, ms] of answers) later(ms, () => cast(id, c))
  later(VOTE_WINDOW_MS + 100, resolveVote)
}

export function sendMyMessage(id: string) {
  B.unlockSpeech()
  const m = state.messages.find((x) => x.id === id)
  if (!m) return
  const d = guard.tryAcquire(`custom:${id}`, Date.now())
  if (!d.allowed) {
    const msg = guardText(d)
    if (msg) toast(msg)
    return
  }
  addFeed(ME, '내 메시지', m.text)
  closeSheet()
  announce(Announce.customSent(m.text), 'CONFIRM')
}

/** 상황 공유 시트의 ‘말로 하기’: “천천히 갈게요” · “도움이 필요해요” */
export async function listenForSignal() {
  B.unlockSpeech()
  if (!B.canListen()) {
    toast('이 브라우저는 음성 인식을 지원하지 않아요 · 아래 버튼으로 골라 주세요')
    return
  }
  setTrip((t) => ({ ...t, listening: 'signal' }))
  const alts = await B.listenOnce(6_000)
  setTrip((t) => (t.listening === 'signal' ? { ...t, listening: null } : t))
  const signal = alts?.map(parseSpokenSignal).find((x) => x !== null) ?? null
  if (signal) sendSignal(signal)
  else toast('잘 알아듣지 못했어요 · “천천히 갈게요” 또는 “도움이 필요해요”라고 말해 주세요')
}

// ---------------- 쉬어가기 투표 ----------------

function runAlert() {
  const spokenDone = announce(Announce.forSignal('SLEEPY', '민지', false, MUNMAK.name, MUNMAK.rest), 'SAFETY', 'minji')
  for (const sv of SIMULATED_VOTES) later(sv.afterMs, () => cast(sv.id, sv.choice))
  // 준호님은 운전 중이라 늦게 답한다
  later(22_000, () => cast('junho', 'YES'))
  later(VOTE_WINDOW_MS + 100, resolveVote)
  if (state.profile.role === 'driver') spokenDone.then(() => listenForVote())
}

function cast(id: string, choice: VoteChoice) {
  const t = state.trip
  if (!t.vote || t.voteResult || t.vote.votes[id]) return
  setTrip((tt) => (tt.vote ? { ...tt, vote: castVote(tt.vote, id, choice, Date.now()) } : tt))
  resolveVote()
}

function resolveVote() {
  const t = state.trip
  if (!t.vote || t.voteResult) return
  const o = outcome(t.vote, Date.now())
  if (o === 'PENDING') return
  B.stopListening()
  setTrip((tt) => ({ ...tt, voteResult: o, listening: null }))
  if (o === 'ACCEPTED') {
    later(1_500, () => enterStep('changed', { keepVote: true }))
  } else {
    announce(Announce.voteRejected(stepOf(t.step).next?.name ?? '다음 집결지'), 'GROUP')
    later(2_500, () => setTrip((tt) => ({ ...tt, vote: null, voteResult: null, sheet: tt.sheet === 'vote' ? null : tt.sheet })))
  }
}

export function answerVote(yes: boolean) {
  B.unlockSpeech()
  const t = state.trip
  if (!t.vote || t.voteResult || t.vote.votes[ME]) return
  B.stopListening()
  setTrip((tt) => ({ ...tt, listening: null }))
  cast(ME, yes ? 'YES' : 'NO')
  announce(yes ? '‘좋아요’로 답했어요.' : '‘괜찮아요’로 답했어요.', 'CONFIRM')
  if (!state.trip.voteResult) later(3_000, () => cast('junho', 'YES'))
}

/** 운전자 모드: 알림을 다 읽은 뒤 “좋아요 / 괜찮아요”를 듣는다 */
export async function listenForVote() {
  const t = state.trip
  if (!B.canListen() || !t.vote || t.voteResult || t.vote.votes[ME]) return
  setTrip((tt) => ({ ...tt, listening: 'vote' }))
  const alts = await B.listenOnce(7_000)
  setTrip((tt) => (tt.listening === 'vote' ? { ...tt, listening: null } : tt))
  const answer = alts?.map(parseVoteAnswer).find((a) => a !== null) ?? null
  if (answer !== null) answerVote(answer)
}

// ---------------- 참여 · 끝내기 · 시연 ----------------

/** 참여 설정을 마치고 대기실로 */
export function finishJoin(groupName?: string) {
  setProfile({ joined: true })
  clearTimers()
  setTrip((t) => ({ ...blankTrip({ ...t, groupName: groupName ?? t.groupName }), active: true }))
}

/** 여행 종료: 위치 공유는 이미 멈췄고, 기록을 남길지 고른다 */
export function finishTrip(keepRecord: boolean) {
  clearTimers()
  clearAnnouncements()
  setState((s) => ({
    ...s,
    history: keepRecord ? [{ name: s.trip.groupName, date: '11/15', km: GROUP.totalKm, rests: 3, cars: 6 }, ...s.history] : s.history,
    trip: blankTrip({ groupName: GROUP.name, demoOffline: false, active: false }),
  }))
}

export function leaveGroup() {
  clearTimers()
  clearAnnouncements()
  setState((s) => ({ ...s, trip: blankTrip({ groupName: GROUP.name, active: false }) }))
}

/** 시연 데이터를 모두 지우고 처음(초대 링크)부터 */
export function resetDemo() {
  clearTimers()
  clearAnnouncements()
  B.removeKeys('rt.')
  setState((s) => ({
    ...s,
    profile: defaultProfile,
    settings: defaultSettings,
    messages: DEFAULT_MESSAGES,
    history: [],
    trip: blankTrip(),
    caption: null,
  }))
}

/** 시연 패널: 참여 전이면 기본 참여 상태로 만들고 단계로 이동 */
export function jumpTo(step: StepId) {
  if (!state.profile.joined) setProfile({ joined: true })
  enterStep(step, { byHand: true })
}
