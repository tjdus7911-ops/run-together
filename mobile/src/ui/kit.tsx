import { useEffect, type CSSProperties, type ReactNode } from 'react'
import type { Member } from '../domain/scenario'
import { useApp, useMembers } from '../store'
import { Icon, type IconName } from './Icon'

/** 화면 공통 부품 (피그마 ‘06 디자인 — 모바일 앱’ · 흰 바탕 × 디지털 블루) */

export function TopBar({ title, onBack, right }: { title?: ReactNode; onBack?: () => void; right?: ReactNode }) {
  return (
    <header className="topbar">
      {onBack ? (
        <button className="icon-btn" onClick={onBack} aria-label="뒤로">
          <Icon name="back" size={24} />
        </button>
      ) : (
        <span className="icon-btn ghost" />
      )}
      <div className="topbar-title">{title}</div>
      <div className="topbar-right">{right ?? <span className="icon-btn ghost" />}</div>
    </header>
  )
}

/** 단계 표시: 막대 [total] 개 + "1/4 · 모임 확인" */
export function StepBar({ index, total, label }: { index: number; total: number; label: string }) {
  return (
    <div className="stepbar">
      <div className="stepbar-track">
        {Array.from({ length: total }, (_, i) => (
          <span key={i} className={i < index ? 'on' : ''} />
        ))}
      </div>
      <div className="stepbar-label">
        {index}/{total} · {label}
      </div>
    </div>
  )
}

type BtnKind = 'primary' | 'secondary' | 'soft' | 'ghost' | 'danger' | 'dark'

export function Btn({
  kind = 'primary',
  icon,
  children,
  onClick,
  disabled,
  className = '',
  small,
  style,
}: {
  kind?: BtnKind
  icon?: IconName
  children: ReactNode
  onClick?: () => void
  disabled?: boolean
  className?: string
  small?: boolean
  style?: CSSProperties
}) {
  return (
    <button type="button" className={`btn btn-${kind}${small ? ' btn-small' : ''} ${className}`} onClick={onClick} disabled={disabled} style={style}>
      {icon && <Icon name={icon} size={small ? 17 : 20} />}
      <span>{children}</span>
    </button>
  )
}

export function Card({ children, className = '', onClick, style }: { children: ReactNode; className?: string; onClick?: () => void; style?: CSSProperties }) {
  if (onClick)
    return (
      <button type="button" className={`card card-button ${className}`} onClick={onClick} style={style}>
        {children}
      </button>
    )
  return (
    <div className={`card ${className}`} style={style}>
      {children}
    </div>
  )
}

export function Avatar({ m, size = 36, dashed, phone, style }: { m: Member; size?: number; dashed?: boolean; phone?: boolean; style?: CSSProperties }) {
  const initial = [...(m.name || '나')][0]
  return (
    <span
      className={`avatar${dashed ? ' dashed' : ''}`}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.42), background: dashed ? '#fff' : m.color, color: dashed ? m.color : '#fff', borderColor: m.color, ...style }}
      aria-label={m.name}
    >
      {initial}
      {phone && (
        <span className="avatar-badge" aria-hidden="true">
          <svg viewBox="0 0 12 12" width="70%" height="70%">
            <rect x="3" y="1" width="6" height="10" rx="1.6" fill="#2A2F37" />
            <rect x="4" y="2.4" width="4" height="5.8" rx="0.5" fill="#fff" />
          </svg>
        </span>
      )}
    </span>
  )
}

export function AvatarRow({ ids, size = 26, dashedIds = [] }: { ids?: string[]; size?: number; dashedIds?: string[] }) {
  const all = useMembers()
  const list = ids ? all.filter((m) => ids.includes(m.id)) : all
  return (
    <span className="avatar-row">
      {list.map((m) => (
        <Avatar key={m.id} m={m} size={size} dashed={dashedIds.includes(m.id)} />
      ))}
    </span>
  )
}

export function Toggle({ checked, onChange, label, disabled }: { checked: boolean; onChange: (v: boolean) => void; label: string; disabled?: boolean }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      className={`toggle${checked ? ' on' : ''}`}
      onClick={() => onChange(!checked)}
    >
      <span />
    </button>
  )
}

export function Segmented<T extends string>({
  options,
  value,
  onChange,
  disabled,
}: {
  options: { value: T; label: string; sub?: string }[]
  value: T
  onChange: (v: T) => void
  disabled?: boolean
}) {
  return (
    <div className="segmented" role="radiogroup">
      {options.map((o) => (
        <button
          type="button"
          role="radio"
          aria-checked={o.value === value}
          key={o.value}
          disabled={disabled}
          className={o.value === value ? 'on' : ''}
          onClick={() => onChange(o.value)}
        >
          {o.value === value && <Icon name="check" size={15} stroke={2.6} />}
          <span>{o.label}</span>
          {o.sub && <small>{o.sub}</small>}
        </button>
      ))}
    </div>
  )
}

export function Chip({ children, tone = 'neutral', icon, className = '' }: { children: ReactNode; tone?: 'accent' | 'neutral' | 'warn' | 'good' | 'danger' | 'night'; icon?: IconName; className?: string }) {
  return (
    <span className={`chip chip-${tone} ${className}`}>
      {icon && <Icon name={icon} size={13} stroke={2.4} />}
      {children}
    </span>
  )
}

export function Sheet({ open, onClose, children, label, tall }: { open: boolean; onClose: () => void; children: ReactNode; label: string; tall?: boolean }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])
  if (!open) return null
  return (
    <div className="sheet-layer" role="dialog" aria-modal="true" aria-label={label}>
      <button type="button" className="scrim" aria-label="닫기" onClick={onClose} />
      <div className={`sheet${tall ? ' tall' : ''}`}>
        <span className="sheet-handle" />
        {children}
      </div>
    </div>
  )
}

/** 아래에 붙는 주요 버튼 영역 */
export function Bottom({ children, note, noteIcon = 'info' }: { children: ReactNode; note?: ReactNode; noteIcon?: IconName }) {
  return (
    <div className="bottom">
      {children}
      {note && (
        <p className="bottom-note">
          <Icon name={noteIcon} size={14} />
          <span>{note}</span>
        </p>
      )}
    </div>
  )
}

export function Progress({ value, max, tone = 'accent' }: { value: number; max: number; tone?: 'accent' | 'warn' }) {
  return (
    <div className={`progress progress-${tone}`} role="progressbar" aria-valuenow={value} aria-valuemax={max}>
      <span style={{ width: `${Math.max(0, Math.min(1, value / max)) * 100}%` }} />
    </div>
  )
}

/** 음성 안내와 같은 문장을 화면 위 자막으로 (보낸 사람 얼굴과 함께) */
export function CaptionBar() {
  const caption = useApp((s) => s.caption)
  const all = useMembers()
  if (!caption) return null
  const who = caption.speakerId ? all.find((m) => m.id === caption.speakerId) : undefined
  return (
    <div className="caption" role="status" aria-live="polite">
      {who ? <Avatar m={who} size={30} /> : <span className="caption-icon"><Icon name="volume" size={18} /></span>}
      <p>{caption.text}</p>
    </div>
  )
}

export function ToastBar() {
  const toast = useApp((s) => s.toast)
  if (!toast) return null
  return (
    <div className="toast" role="status" aria-live="polite" key={toast.id}>
      {toast.text}
    </div>
  )
}

export function Mark({ ok, children }: { ok: boolean; children: ReactNode }) {
  return (
    <li className={`mark ${ok ? 'ok' : 'bad'}`}>
      <Icon name={ok ? 'check' : 'close'} size={15} stroke={2.6} />
      <span>{children}</span>
    </li>
  )
}
