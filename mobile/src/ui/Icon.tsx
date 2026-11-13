import type { CSSProperties, ReactNode } from 'react'
import type { MessageIcon } from '../domain/messages'
import type { StatusSignal } from '../domain/korean'

/** 선 아이콘 (24 격자 · 2px 선). 외부 아이콘 라이브러리 없이 필요한 것만 그린다 */

function gearPath(): string {
  const teeth = 8
  const pts: string[] = []
  for (let i = 0; i < teeth * 4; i++) {
    const a = (i / (teeth * 4)) * Math.PI * 2 - Math.PI / 2
    const r = i % 4 === 0 || i % 4 === 1 ? 9.2 : 7.2
    pts.push(`${(12 + r * Math.cos(a)).toFixed(2)} ${(12 + r * Math.sin(a)).toFixed(2)}`)
  }
  return `M${pts.join('L')}Z`
}
const GEAR = gearPath()

const P: Record<string, ReactNode> = {
  back: <path d="M15 18l-6-6 6-6" />,
  chevron: <path d="M9 18l6-6-6-6" />,
  more: (
    <g fill="currentColor" stroke="none">
      <circle cx="5" cy="12" r="1.7" />
      <circle cx="12" cy="12" r="1.7" />
      <circle cx="19" cy="12" r="1.7" />
    </g>
  ),
  gear: (
    <>
      <path d={GEAR} />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  mic: (
    <>
      <rect x="9" y="3" width="6" height="11" rx="3" />
      <path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21" />
    </>
  ),
  micOff: (
    <>
      <path d="M15 9.5V6a3 3 0 0 0-5.7-1.3M9 9v2a3 3 0 0 0 4.9 2.3" />
      <path d="M5.5 11a6.5 6.5 0 0 0 10.6 5M18.5 11a6.4 6.4 0 0 1-.6 2.7M12 17.5V21M3 3l18 18" />
    </>
  ),
  volume: (
    <>
      <path d="M11 5L6.5 9H3.5v6h3l4.5 4V5z" />
      <path d="M15.5 9a4.5 4.5 0 0 1 0 6M18.5 6a8.5 8.5 0 0 1 0 12" />
    </>
  ),
  wave: <path d="M3 12h1.5M7 8v8M10.5 5v14M14 9v6M17.5 7v10M21 11v2" />,
  check: <path d="M5 12.5l4.5 4.5L19 7.5" />,
  checkCircle: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M8 12.3l2.8 2.8L16.2 9.6" />
    </>
  ),
  close: <path d="M6 6l12 12M18 6L6 18" />,
  lock: (
    <>
      <rect x="5" y="10.5" width="14" height="10" rx="2.2" />
      <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" />
    </>
  ),
  pin: (
    <>
      <path d="M12 21s-6.5-5.8-6.5-11A6.5 6.5 0 0 1 18.5 10c0 5.2-6.5 11-6.5 11z" />
      <circle cx="12" cy="10" r="2.4" />
    </>
  ),
  navigate: <path d="M4 11.5L20 4l-7.5 16-1.9-6.6L4 11.5z" />,
  coffee: (
    <>
      <path d="M4.5 9h11v5a4.5 4.5 0 0 1-4.5 4.5H9A4.5 4.5 0 0 1 4.5 14V9z" />
      <path d="M15.5 10.5h1.2a2.3 2.3 0 0 1 0 4.6h-1.4M8 3.5v2.5M11.5 3.5v2.5M4 21h12" />
    </>
  ),
  food: (
    <>
      <path d="M6.5 3v7M4.5 3v4.5a2 2 0 0 0 4 0V3M6.5 11.5V21" />
      <path d="M17.5 21V3c-2.2 1.2-3.5 3.8-3.5 7.5h3.5" />
    </>
  ),
  fuel: (
    <>
      <path d="M4.5 21V5.5a2 2 0 0 1 2-2h5.5a2 2 0 0 1 2 2V21M3 21h12.5M7 9.5h4.5" />
      <path d="M14 10h1.8a1.7 1.7 0 0 1 1.7 1.7v4.3a1.5 1.5 0 0 0 3 0V8.7L18 6" />
    </>
  ),
  bolt: <path d="M13 2.5L4.5 13.5h6.5l-1 8 8.5-11h-6.5l1-8z" />,
  toilet: (
    <>
      <circle cx="7" cy="4.3" r="1.7" />
      <circle cx="17" cy="4.3" r="1.7" />
      <path d="M7 8v13M4.8 8.2h4.4v6.3H4.8zM17 8l-2.8 7.8h5.6L17 8zM15.6 15.8V21M18.4 15.8V21" />
    </>
  ),
  photo: (
    <>
      <path d="M4 8h3l1.8-2.6h6.4L17 8h3a1 1 0 0 1 1 1v9.5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z" />
      <circle cx="12" cy="13.5" r="3.3" />
    </>
  ),
  gauge: (
    <>
      <path d="M4.2 17.5a8 8 0 1 1 15.6 0" />
      <path d="M12 16.5l-3.6-4.6" />
      <circle cx="12" cy="16.8" r="1.2" />
    </>
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="8.8" />
      <path d="M12 7.2V12l3.2 2" />
    </>
  ),
  thumb: <path d="M7.5 10.5V20H4.2v-9.5h3.3zM7.5 10.5l3.6-6.6a1.9 1.9 0 0 1 3.4 1.6L13.5 9.3h5.2a2 2 0 0 1 2 2.3l-1.2 7A2 2 0 0 1 17.5 20H7.5" />,
  parking: (
    <>
      <rect x="4" y="3.5" width="16" height="17" rx="3" />
      <path d="M10 16.5v-9h3.2a2.8 2.8 0 0 1 0 5.6H10" />
    </>
  ),
  music: (
    <>
      <path d="M9 17.5V5.5l11-2v12" />
      <circle cx="6.3" cy="17.6" r="2.7" />
      <circle cx="17.3" cy="15.6" r="2.7" />
    </>
  ),
  sun: (
    <>
      <circle cx="12" cy="12" r="3.8" />
      <path d="M12 2.5v2M12 19.5v2M4.6 4.6L6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4L6 18M18 6l1.4-1.4" />
    </>
  ),
  moon: <path d="M19.5 14.6A7.8 7.8 0 1 1 9.4 4.5a6.2 6.2 0 0 0 10.1 10.1z" />,
  share: (
    <>
      <circle cx="17.5" cy="5.5" r="2.5" />
      <circle cx="6.5" cy="12" r="2.5" />
      <circle cx="17.5" cy="18.5" r="2.5" />
      <path d="M8.7 10.8l6.6-4M8.7 13.2l6.6 4" />
    </>
  ),
  link: (
    <>
      <path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1" />
      <path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1" />
    </>
  ),
  car: (
    <>
      <path d="M4.5 16.5V12l1.8-5a1.5 1.5 0 0 1 1.4-1h8.6a1.5 1.5 0 0 1 1.4 1l1.8 5v4.5" />
      <path d="M3.5 16.5h17M4.5 12h15" />
      <circle cx="7.8" cy="16.8" r="1.7" />
      <circle cx="16.2" cy="16.8" r="1.7" />
    </>
  ),
  copy: (
    <>
      <rect x="8.5" y="8.5" width="11.5" height="11.5" rx="2.2" />
      <path d="M15.5 8.5V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2.5" />
    </>
  ),
  users: (
    <>
      <circle cx="9" cy="8" r="3.4" />
      <path d="M2.8 20a6.2 6.2 0 0 1 12.4 0" />
      <path d="M15.8 4.8a3.4 3.4 0 0 1 0 6.4M17.8 14a6.2 6.2 0 0 1 3.4 6" />
    </>
  ),
  shield: (
    <>
      <path d="M12 3l7.5 2.8v5.7c0 4.9-3.3 7.9-7.5 9.3-4.2-1.4-7.5-4.4-7.5-9.3V5.8L12 3z" />
      <path d="M8.8 11.8l2.3 2.3 4.2-4.3" />
    </>
  ),
  eyeOff: (
    <>
      <path d="M3 3l18 18" />
      <path d="M10.7 6.1A9.6 9.6 0 0 1 12 6c5 0 8.4 4.3 9.3 6-.5.9-1.5 2.3-2.9 3.6M6.5 7.8C4.6 9.1 3.3 10.9 2.7 12c.9 1.7 4.3 6 9.3 6 1.6 0 3-.4 4.2-1" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
    </>
  ),
  bell: (
    <>
      <path d="M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.8h-15L6 16.5z" />
      <path d="M10 20.5a2.1 2.1 0 0 0 4 0" />
    </>
  ),
  phone: (
    <>
      <rect x="7" y="2.5" width="10" height="19" rx="2.6" />
      <path d="M10.8 18.3h2.4" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="8.8" />
      <path d="M12 11v5.2M12 7.6v.4" />
    </>
  ),
  warn: (
    <>
      <path d="M12 3.5l9 16h-18l9-16z" />
      <path d="M12 10v4.2M12 17v.4" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  minus: <path d="M5 12h14" />,
  grip: (
    <g fill="currentColor" stroke="none">
      {[6, 12, 18].map((y) => (
        <g key={y}>
          <circle cx="9" cy={y} r="1.5" />
          <circle cx="15" cy={y} r="1.5" />
        </g>
      ))}
    </g>
  ),
  refresh: (
    <>
      <path d="M19.5 11A7.5 7.5 0 0 0 6 6.6L4.2 8.4" />
      <path d="M4 4v4.5h4.5" />
      <path d="M4.5 13A7.5 7.5 0 0 0 18 17.4l1.8-1.8" />
      <path d="M20 20v-4.5h-4.5" />
    </>
  ),
  wifiOff: (
    <>
      <path d="M3 3l18 18" />
      <path d="M8.6 16.3a5 5 0 0 1 6.8 0M5.2 12.9A10 10 0 0 1 9 10.7M15.6 10.8a10 10 0 0 1 3.2 2.1M2 9.4a14.6 14.6 0 0 1 4.1-2.6M10.4 5.6A14.6 14.6 0 0 1 22 9.4M12 19.6v.1" />
    </>
  ),
  flag: <path d="M5.5 21V4M5.5 4.5h11l-2.2 4 2.2 4h-11" />,
  edit: (
    <>
      <path d="M4 20h4L19 9l-4-4L4 16v4z" />
      <path d="M13.5 6.5l4 4" />
    </>
  ),
  calendar: (
    <>
      <rect x="3.5" y="5" width="17" height="15.5" rx="2.2" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
    </>
  ),
  route: (
    <>
      <circle cx="6" cy="18" r="2.4" />
      <circle cx="18" cy="6" r="2.4" />
      <path d="M8.4 18h6.3a3.2 3.2 0 0 0 0-6.4H9.3a3.2 3.2 0 0 1 0-6.4h6.3" />
    </>
  ),
  logout: (
    <>
      <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
      <path d="M9.5 16.5L5 12l4.5-4.5M5 12h10.5" />
    </>
  ),
  library: (
    <>
      <rect x="3.5" y="4" width="5" height="16" rx="1.2" />
      <rect x="9.5" y="4" width="5" height="16" rx="1.2" />
      <path d="M16 5.2l3-.8 3.2 14.8-3 .8z" />
    </>
  ),
  paste: (
    <>
      <rect x="5.5" y="4.5" width="13" height="16.5" rx="2.2" />
      <path d="M9 4.5V3h6v1.5M9 10h6M9 14h4" />
    </>
  ),
  location: (
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3" />
      <circle cx="12" cy="12" r="7" />
    </>
  ),
  battery: (
    <>
      <rect x="2.5" y="7" width="17" height="10" rx="2.2" />
      <path d="M21.5 10.5v3" />
    </>
  ),
  sos: (
    <>
      <circle cx="12" cy="12" r="8.8" />
      <path d="M12 7.5v5.5M12 16.2v.3" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="3.8" />
      <path d="M4.5 20.5a7.5 7.5 0 0 1 15 0" />
    </>
  ),
}

export type IconName = keyof typeof P

export function Icon({ name, size = 20, stroke = 2, style, className }: { name: IconName | string; size?: number; stroke?: number; style?: CSSProperties; className?: string }) {
  return (
    <svg
      className={className}
      style={style}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={stroke}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {P[name] ?? P.info}
    </svg>
  )
}

export const MESSAGE_ICON: Record<MessageIcon, IconName> = {
  COFFEE: 'coffee',
  FOOD: 'food',
  FUEL: 'fuel',
  CHARGE: 'bolt',
  TOILET: 'toilet',
  PHOTO: 'photo',
  SLOW: 'gauge',
  LATE: 'clock',
  OK: 'thumb',
  PARKING: 'parking',
  MUSIC: 'music',
  SUN: 'sun',
}

export const SIGNAL_ICON: Record<StatusSignal, IconName> = {
  SLEEPY: 'moon',
  REST: 'coffee',
  TOILET: 'toilet',
  ENERGY: 'fuel',
  SLOW: 'gauge',
  HELP: 'sos',
}
