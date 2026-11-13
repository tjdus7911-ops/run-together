import { useEffect, type ReactNode } from 'react'
import { stepOf } from './domain/scenario'
import { requestWakeLock, unlockSpeech, watchLocation } from './platform/browser'
import { go, useRoute } from './router'
import { EditMessageScreen, MessagesScreen } from './screens/Messages'
import { CreateCourse, CreateInfo, CreateInvite, HomeScreen, SettingsScreen } from './screens/Home'
import { CodeScreen, LobbyScreen, SetupConsent, SetupDenied, SetupPreview, SetupRole, SetupVehicle, StartScreen } from './screens/Join'
import { TripScreen } from './screens/Trip'
import { DemoTab } from './screens/Demo'
import { connectNavigation, enterStep, getState, setState, useApp } from './store'
import { CaptionBar, ToastBar } from './ui/kit'

const OPEN_ROUTES = new Set(['start', 'join', 'code', 'setup', 'create', 'messages'])

export function App() {
  const route = useRoute()
  const joined = useApp((s) => s.profile.joined)
  const parts = route.split('/').filter(Boolean)
  const head = parts[0] ?? ''

  useEffect(() => connectNavigation((path) => go(path)), [])
  // 새로고침해도 여행 중이던 단계의 시연 진행(남은 거리 · 다른 차 응답)을 다시 이어 간다
  useEffect(() => {
    const t = getState().trip
    if (t.active && t.step !== 'lobby' && t.step !== 'ended') enterStep(t.step, { byHand: true })
  }, [])
  useEffect(() => {
    const f = () => unlockSpeech()
    window.addEventListener('pointerdown', f, { once: true })
    return () => window.removeEventListener('pointerdown', f)
  }, [])
  useEffect(() => {
    if (!head) go(joined ? '/home' : '/start', true)
    else if (!joined && !OPEN_ROUTES.has(head)) go('/start', true)
  }, [head, joined])

  let screen: ReactNode = null
  switch (head) {
    case 'start':
      screen = <StartScreen />
      break
    case 'join':
      screen = <StartScreen code={parts[1]} />
      break
    case 'code':
      screen = <CodeScreen />
      break
    case 'setup':
      screen = { '2': <SetupConsent />, '3': <SetupRole />, '4': <SetupVehicle />, denied: <SetupDenied /> }[parts[1] ?? ''] ?? <SetupPreview />
      break
    case 'lobby':
      screen = <LobbyScreen />
      break
    case 'trip':
      screen = <TripScreen />
      break
    case 'home':
      screen = <HomeScreen />
      break
    case 'create':
      screen = { '2': <CreateCourse />, '3': <CreateInvite /> }[parts[1] ?? ''] ?? <CreateInfo />
      break
    case 'settings':
      screen = <SettingsScreen />
      break
    case 'messages':
      screen = parts[1] === 'new' ? <EditMessageScreen key="new" /> : parts[1] === 'edit' ? <EditMessageScreen key={parts[2]} id={parts[2]} /> : <MessagesScreen />
      break
  }

  return (
    <div className="app">
      <LocationController />
      <WakeLockController onTrip={head === 'trip'} />
      <OnlineController />
      {screen}
      <CaptionBar />
      <ToastBar />
      <DemoTab />
    </div>
  )
}

/** 출발 안내부터 여행이 끝날 때까지만 위치를 본다 (동의 · 공유 켬일 때) */
function LocationController() {
  const active = useApp(
    (s) => s.trip.active && s.trip.step !== 'lobby' && s.trip.step !== 'ended' && s.profile.location === 'share' && s.settings.sharing,
  )
  useEffect(() => {
    if (!active) {
      setState((s) => (s.geo.status === 'idle' && !s.geo.fix ? s : { ...s, geo: { status: 'idle', fix: null } }))
      return
    }
    return watchLocation(
      (fix) => setState((s) => ({ ...s, geo: { status: 'on', fix } })),
      (status) => setState((s) => (s.geo.status === status ? s : { ...s, geo: { ...s.geo, status } })),
    )
  }, [active])
  return null
}

/** 운전자 모드로 움직이는 동안 화면을 켜 둔다 (화면이 꺼지면 웹앱 위치도 멈추므로) */
function WakeLockController({ onTrip }: { onTrip: boolean }) {
  const active = useApp((s) => onTrip && s.trip.active && stepOf(s.trip.step).moving && s.profile.role === 'driver' && s.settings.keepAwake)
  useEffect(() => {
    if (!active) return
    let lock: Awaited<ReturnType<typeof requestWakeLock>> = null
    let cancelled = false
    const acquire = async () => {
      if (document.visibilityState !== 'visible') return
      const l = await requestWakeLock()
      if (cancelled) l?.release().catch(() => {})
      else lock = l
    }
    const onVisible = () => document.visibilityState === 'visible' && acquire()
    acquire()
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      cancelled = true
      document.removeEventListener('visibilitychange', onVisible)
      lock?.release().catch(() => {})
    }
  }, [active])
  return null
}

function OnlineController() {
  useEffect(() => {
    const on = () => setState((s) => ({ ...s, online: true }))
    const off = () => setState((s) => ({ ...s, online: false }))
    window.addEventListener('online', on)
    window.addEventListener('offline', off)
    return () => {
      window.removeEventListener('online', on)
      window.removeEventListener('offline', off)
    }
  }, [])
  return null
}
