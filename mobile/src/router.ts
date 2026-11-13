import { useSyncExternalStore } from 'react'

/** 해시 주소(#/trip)로 화면을 바꾼다. 정적 호스팅 어디에 올려도 새로고침에 안전하다 */

const subscribe = (l: () => void) => {
  window.addEventListener('hashchange', l)
  return () => window.removeEventListener('hashchange', l)
}

export const currentPath = () => window.location.hash.replace(/^#/, '') || '/'

export function useRoute(): string {
  return useSyncExternalStore(subscribe, currentPath, () => '/')
}

let depth = 0

export function go(path: string, replace = false) {
  if (currentPath() === path) return
  if (replace) {
    history.replaceState(null, '', `#${path}`)
    window.dispatchEvent(new HashChangeEvent('hashchange'))
  } else {
    depth++
    window.location.hash = path
  }
  window.scrollTo(0, 0)
}

/** 앱 안에서 온 곳이 있으면 뒤로, 없으면 [fallback] 으로 */
export function back(fallback: string) {
  if (depth > 0) {
    depth--
    history.back()
  } else {
    go(fallback, true)
  }
}
