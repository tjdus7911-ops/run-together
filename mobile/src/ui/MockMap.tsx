import { useMemo } from 'react'
import type { Member } from '../domain/scenario'

/**
 * 시연용 그림 지도 (차량 앱 MockNavMap 과 같은 역할). 실제 지도 SDK 는 키·약관이 필요해 시연판에는 넣지 않았다.
 * 모임 차 위치는 시나리오 값이고, 화면 왼쪽 아래에 ‘시연용 지도’를 표시한다.
 */

const W = 390
const H = 844

// 경로 (아래 왼쪽 → 위 오른쪽). 화면 위 머리 카드와 아래 시트 사이(대략 y 140~470)에 차들이 보이게
const ROUTE: [number, number][] = [
  [40, 900], [60, 700], [78, 520], [92, 400], [110, 340], [140, 300], [180, 280], [215, 262],
  [240, 240], [262, 212], [290, 190], [318, 170], [345, 150], [372, 132], [420, 110],
]

function cumulative(pts: [number, number][]) {
  const acc = [0]
  for (let i = 1; i < pts.length; i++) acc.push(acc[i - 1] + Math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]))
  return acc
}
const ACC = cumulative(ROUTE)
const TOTAL = ACC[ACC.length - 1]

function pointAt(f: number): { x: number; y: number } {
  const d = Math.max(0, Math.min(1, f)) * TOTAL
  let i = 1
  while (i < ACC.length - 1 && ACC[i] < d) i++
  const t = (d - ACC[i - 1]) / (ACC[i] - ACC[i - 1] || 1)
  return { x: ROUTE[i - 1][0] + (ROUTE[i][0] - ROUTE[i - 1][0]) * t, y: ROUTE[i - 1][1] + (ROUTE[i][1] - ROUTE[i - 1][1]) * t }
}

function pathUpTo(f0: number, f1: number) {
  const pts: string[] = []
  const a = pointAt(f0)
  pts.push(`${a.x.toFixed(1)},${a.y.toFixed(1)}`)
  const d0 = f0 * TOTAL
  const d1 = f1 * TOTAL
  for (let i = 0; i < ROUTE.length; i++) if (ACC[i] > d0 && ACC[i] < d1) pts.push(`${ROUTE[i][0]},${ROUTE[i][1]}`)
  const b = pointAt(f1)
  pts.push(`${b.x.toFixed(1)},${b.y.toFixed(1)}`)
  return `M${pts.join('L')}`
}

export interface MapMarker {
  member: Member
  /** 경로 위 위치 0~1. offRoute 면 옆길 */
  f: number
  offRoute?: boolean
  tag?: string
  tagTone?: 'accent' | 'warn'
  stale?: boolean
  me?: boolean
}

export function MockMap({ markers, flagF = 0.915, gapBetween, gapText }: { markers: MapMarker[]; flagF?: number; gapBetween?: [string, string]; gapText?: string }) {
  const me = markers.find((m) => m.me)
  const meF = me?.f ?? 0.3
  const positioned = useMemo(
    () =>
      markers.map((m) => {
        const p = pointAt(m.f)
        return { ...m, x: m.offRoute ? p.x - 70 : p.x, y: m.offRoute ? p.y - 10 : p.y }
      }),
    [markers],
  )
  const flag = pointAt(flagF)
  const gap = gapBetween ? gapBetween.map((id) => positioned.find((p) => p.member.id === id)) : null
  const offRoute = positioned.find((p) => p.offRoute)

  return (
    <svg className="mockmap" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="xMidYMin slice" role="img" aria-label="모임 지도 (시연용)">
      <rect width={W} height={H} fill="#EEF1F4" />
      {/* 땅 · 공원 · 강 */}
      <path d="M-20 560 C 60 520, 120 600, 220 560 S 360 520, 420 560 L 420 640 C 330 610, 260 660, 170 640 S 40 610, -20 650 Z" fill="#D7E6F4" />
      <rect x="250" y="380" width="120" height="90" rx="18" fill="#DCEDD8" />
      <rect x="22" y="230" width="110" height="120" rx="20" fill="#DCEDD8" />
      <rect x="150" y="150" width="90" height="70" rx="14" fill="#E5E9EE" />
      <rect x="40" y="410" width="80" height="60" rx="12" fill="#E5E9EE" />
      <rect x="280" y="40" width="90" height="70" rx="14" fill="#E5E9EE" />
      {/* 일반 도로 */}
      <g stroke="#D6DBE2" strokeWidth="13" fill="none" strokeLinecap="round">
        <path d="M-10 300 C 90 290, 200 330, 420 300" />
        <path d="M120 -10 C 140 140, 110 300, 150 520" />
        <path d="M-10 470 C 110 450, 260 500, 420 450" />
      </g>
      <g stroke="#FFFFFF" strokeWidth="9" fill="none" strokeLinecap="round">
        <path d="M-10 300 C 90 290, 200 330, 420 300" />
        <path d="M120 -10 C 140 140, 110 300, 150 520" />
        <path d="M-10 470 C 110 450, 260 500, 420 450" />
      </g>
      {/* 옆길 (다른 길로 가는 차) */}
      {offRoute && (
        <path
          d={`M${pointAt(offRoute.f - 0.08).x} ${pointAt(offRoute.f - 0.08).y} C ${offRoute.x - 30} ${offRoute.y + 40}, ${offRoute.x - 20} ${offRoute.y - 30}, ${pointAt(offRoute.f + 0.12).x} ${pointAt(offRoute.f + 0.12).y}`}
          stroke="#B9C3CF"
          strokeWidth="6"
          strokeDasharray="2 9"
          strokeLinecap="round"
          fill="none"
        />
      )}
      {/* 고속도로 바탕 */}
      <path d={pathUpTo(0, 1)} stroke="#CDD5DF" strokeWidth="16" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d={pathUpTo(0, 1)} stroke="#FFFFFF" strokeWidth="11" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      {/* 지나온 길 · 앞길 (앞길은 은은한 빛) */}
      <path d={pathUpTo(0, meF)} stroke="#AEB8C5" strokeWidth="7" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d={pathUpTo(meF, flagF)} stroke="#3D8BFF" strokeOpacity="0.14" strokeWidth="24" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d={pathUpTo(meF, flagF)} stroke="#2563EB" strokeWidth="7" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d={pathUpTo(flagF, 1)} stroke="#93B4F5" strokeWidth="7" fill="none" strokeLinecap="round" strokeLinejoin="round" />

      {/* 벌어진 간격 */}
      {gap && gap[0] && gap[1] && (
        <g>
          <path d={`M${gap[0].x + 18} ${gap[0].y} L${gap[1].x + 18} ${gap[1].y}`} stroke="#C27C0E" strokeWidth="2" strokeDasharray="3 4" />
          <g transform={`translate(${(gap[0].x + gap[1].x) / 2 + 30}, ${(gap[0].y + gap[1].y) / 2 + 22})`}>
            <rect x="0" y="-11" width={gapText ? gapText.length * 10.5 + 16 : 80} height="22" rx="6" fill="#FFFFFF" stroke="#E8C27A" />
            <text x="8" y="4.5" fontSize="11.5" fontWeight="600" fill="#9A5B00">
              {gapText}
            </text>
          </g>
        </g>
      )}

      {/* 다음 집결지 */}
      <g transform={`translate(${flag.x - 15}, ${flag.y - 15})`}>
        <rect width="30" height="30" rx="8" fill="#15181E" />
        <path d="M10.5 22V8.5M10.5 9h9l-1.8 3.2 1.8 3.2h-9" stroke="#fff" strokeWidth="2" fill="none" strokeLinejoin="round" strokeLinecap="round" />
      </g>

      {/* 모임 차 */}
      {positioned.map((p) => (
        <g key={p.member.id} transform={`translate(${p.x}, ${p.y})`} opacity={p.stale ? 0.55 : 1}>
          {p.me && <circle r="24" fill="#2563EB" fillOpacity="0.12" />}
          <circle r={p.me ? 17 : 15} fill={p.member.color} stroke="#FFFFFF" strokeWidth="3" />
          <text y="5" textAnchor="middle" fontSize={p.me ? 14 : 13} fontWeight="700" fill="#FFFFFF">
            {[...p.member.name][0]}
          </text>
          {p.me && (
            <g transform="translate(13, 11)">
              <circle r="8.5" fill="#FFFFFF" stroke="#D6DBE2" />
              <rect x="-3.3" y="-5.2" width="6.6" height="10.4" rx="1.7" fill="#2A2F37" />
              <rect x="-2.2" y="-3.8" width="4.4" height="6" rx="0.6" fill="#FFFFFF" />
            </g>
          )}
          {p.tag && (
            <g transform="translate(19, -24)">
              <rect width={p.tag.length * 11 + 14} height="20" rx="6" fill="#FFFFFF" stroke={p.tagTone === 'warn' ? '#E8C27A' : '#C6D6F7'} />
              <text x="7" y="14" fontSize="11" fontWeight="600" fill={p.tagTone === 'warn' ? '#9A5B00' : '#2563EB'}>
                {p.tag}
              </text>
            </g>
          )}
        </g>
      ))}
    </svg>
  )
}
