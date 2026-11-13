/**
 * 브라우저 기능 모음 (휴대폰 웹앱이라 설치 없이 쓰는 것들).
 * - 소리 내어 읽기: Web Speech API speechSynthesis (ko-KR)
 * - 말로 답하기: SpeechRecognition (있는 브라우저만. 없으면 버튼으로)
 * - 위치: Geolocation (정확도 · 측정 시각만 화면에 보여 준다. 시연판은 어디에도 보내지 않는다)
 * - 화면 켜 두기: Screen Wake Lock
 * - 진동 · 공유 시트 · 클립보드
 */

// ---------------- 저장 ----------------

export function load<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? ({ ...fallback, ...JSON.parse(raw) } as T) : fallback
  } catch {
    return fallback
  }
}

export function loadList<T>(key: string, fallback: T[]): T[] {
  try {
    const raw = localStorage.getItem(key)
    const v = raw ? JSON.parse(raw) : null
    return Array.isArray(v) ? (v as T[]) : fallback
  } catch {
    return fallback
  }
}

export function save(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    /* 저장 공간이 없거나 사생활 보호 모드 */
  }
}

export function removeKeys(prefix: string) {
  try {
    Object.keys(localStorage)
      .filter((k) => k.startsWith(prefix))
      .forEach((k) => localStorage.removeItem(k))
  } catch {
    /* noop */
  }
}

// ---------------- 소리 내어 읽기 ----------------

export const canSpeak = () => typeof window !== 'undefined' && 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window

function koreanVoice(): SpeechSynthesisVoice | undefined {
  try {
    const voices = window.speechSynthesis.getVoices()
    return voices.find((v) => v.lang?.toLowerCase().startsWith('ko') && v.localService) ?? voices.find((v) => v.lang?.toLowerCase().startsWith('ko'))
  } catch {
    return undefined
  }
}

/** 읽기 시작하면 true. 끝나거나 실패하면 onEnd */
export function speak(text: string, onEnd: () => void): boolean {
  if (!canSpeak()) return false
  try {
    const u = new SpeechSynthesisUtterance(text)
    u.lang = 'ko-KR'
    const v = koreanVoice()
    if (v) u.voice = v
    u.rate = 1.02
    u.onend = onEnd
    u.onerror = onEnd
    window.speechSynthesis.speak(u)
    return true
  } catch {
    return false
  }
}

export function stopSpeaking() {
  try {
    if (canSpeak()) window.speechSynthesis.cancel()
  } catch {
    /* noop */
  }
}

let speechUnlocked = false
/** iOS 사파리는 첫 소리를 사용자의 터치 안에서 내야 이후에도 읽어 준다 */
export function unlockSpeech() {
  if (speechUnlocked || !canSpeak()) return
  speechUnlocked = true
  try {
    const u = new SpeechSynthesisUtterance(' ')
    u.volume = 0
    window.speechSynthesis.speak(u)
  } catch {
    /* noop */
  }
}

// ---------------- 말로 답하기 ----------------

type RecognitionLike = {
  lang: string
  interimResults: boolean
  maxAlternatives: number
  continuous: boolean
  start: () => void
  stop: () => void
  abort: () => void
  onresult: ((e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null
  onerror: ((e: unknown) => void) | null
  onend: (() => void) | null
}

function recognitionClass(): (new () => RecognitionLike) | null {
  if (typeof window === 'undefined') return null
  const w = window as unknown as { SpeechRecognition?: new () => RecognitionLike; webkitSpeechRecognition?: new () => RecognitionLike }
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null
}

export const canListen = () => recognitionClass() !== null

let current: RecognitionLike | null = null

/** 한 번 듣는다. 알아들은 문장 후보들(없으면 null) */
export function listenOnce(timeoutMs = 7_000): Promise<string[] | null> {
  const Rec = recognitionClass()
  if (!Rec) return Promise.resolve(null)
  stopListening()
  return new Promise((resolve) => {
    let done = false
    const finish = (v: string[] | null) => {
      if (done) return
      done = true
      clearTimeout(timer)
      if (current === rec) current = null
      resolve(v)
    }
    const rec = new Rec()
    rec.lang = 'ko-KR'
    rec.interimResults = false
    rec.maxAlternatives = 3
    rec.continuous = false
    rec.onresult = (e) => {
      const first = e.results[0]
      const out: string[] = []
      if (first) for (let i = 0; i < first.length; i++) out.push(first[i].transcript)
      finish(out.length ? out : null)
    }
    rec.onerror = () => finish(null)
    rec.onend = () => finish(null)
    const timer = setTimeout(() => {
      try {
        rec.stop()
      } catch {
        /* noop */
      }
      finish(null)
    }, timeoutMs)
    current = rec
    try {
      rec.start()
    } catch {
      finish(null)
    }
  })
}

export function stopListening() {
  const r = current
  current = null
  try {
    r?.abort()
  } catch {
    /* noop */
  }
}

// ---------------- 위치 ----------------

export type GeoStatus = 'idle' | 'on' | 'denied' | 'unavailable'

export interface GeoFix {
  accuracy: number
  at: number
}

export const hasGeolocation = () => typeof navigator !== 'undefined' && 'geolocation' in navigator

/** 위치 권한을 한 번 묻는다 (참여 설정 ② ‘동의하고 계속’) */
export function askLocationOnce(): Promise<'granted' | 'denied' | 'unavailable'> {
  if (!hasGeolocation()) return Promise.resolve('unavailable')
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      () => resolve('granted'),
      (err) => resolve(err.code === err.PERMISSION_DENIED ? 'denied' : 'unavailable'),
      { enableHighAccuracy: true, timeout: 12_000, maximumAge: 30_000 },
    )
  })
}

/** 여행 중에만 위치를 본다. 끄면 바로 멈춘다 */
export function watchLocation(onFix: (fix: GeoFix) => void, onStatus: (s: GeoStatus) => void): () => void {
  if (!hasGeolocation()) {
    onStatus('unavailable')
    return () => {}
  }
  const id = navigator.geolocation.watchPosition(
    (p) => {
      onStatus('on')
      onFix({ accuracy: Math.round(p.coords.accuracy), at: p.timestamp || Date.now() })
    },
    (err) => onStatus(err.code === err.PERMISSION_DENIED ? 'denied' : 'unavailable'),
    { enableHighAccuracy: true, maximumAge: 5_000, timeout: 20_000 },
  )
  return () => navigator.geolocation.clearWatch(id)
}

// ---------------- 화면 켜 두기 ----------------

type Sentinel = { release: () => Promise<void>; addEventListener: (t: 'release', f: () => void) => void }

export async function requestWakeLock(): Promise<Sentinel | null> {
  const nav = navigator as unknown as { wakeLock?: { request: (t: 'screen') => Promise<Sentinel> } }
  if (!nav.wakeLock) return null
  try {
    return await nav.wakeLock.request('screen')
  } catch {
    return null
  }
}

export const hasWakeLock = () => typeof navigator !== 'undefined' && 'wakeLock' in navigator

// ---------------- 진동 · 공유 · 클립보드 ----------------

export function vibrate(pattern: number | number[]) {
  try {
    navigator.vibrate?.(pattern)
  } catch {
    /* noop */
  }
}

export async function shareLink(title: string, text: string, url: string): Promise<'shared' | 'copied' | 'failed'> {
  try {
    if (navigator.share) {
      await navigator.share({ title, text, url })
      return 'shared'
    }
  } catch (e) {
    if ((e as { name?: string })?.name === 'AbortError') return 'failed'
  }
  return (await copyText(url)) ? 'copied' : 'failed'
}

export async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      const ok = document.execCommand('copy')
      ta.remove()
      return ok
    } catch {
      return false
    }
  }
}

export async function readClipboard(): Promise<string | null> {
  try {
    return await navigator.clipboard.readText()
  } catch {
    return null
  }
}

// ---------------- 다른 내비로 열기 ----------------

export type NaviApp = 'kakao' | 'naver' | 'tmap'

export const NAVI_LABEL: Record<NaviApp, string> = { kakao: '카카오맵', naver: '네이버 지도', tmap: '티맵' }

/** 앱이 있으면 앱으로, 없으면 웹 지도로 (티맵은 앱만) */
export function openNavi(app: NaviApp, dest: { name: string; lat: number; lng: number }, onNoApp: (msg: string) => void) {
  const name = encodeURIComponent(dest.name)
  if (app === 'kakao') {
    window.open(`https://map.kakao.com/link/to/${name},${dest.lat},${dest.lng}`, '_blank', 'noopener')
    return
  }
  const scheme =
    app === 'naver'
      ? `nmap://route/car?dlat=${dest.lat}&dlng=${dest.lng}&dname=${name}&appname=${encodeURIComponent(location.hostname || 'run-together')}`
      : `tmap://route?goalname=${name}&goalx=${dest.lng}&goaly=${dest.lat}`
  const started = Date.now()
  const fallback = setTimeout(() => {
    if (document.visibilityState !== 'visible' || Date.now() - started > 3_000) return
    if (app === 'naver') window.open(`https://map.naver.com/p/search/${name}`, '_blank', 'noopener')
    else onNoApp('티맵 앱이 없어서 열지 못했어요')
  }, 1_400)
  document.addEventListener('visibilitychange', () => clearTimeout(fallback), { once: true })
  window.location.href = scheme
}
