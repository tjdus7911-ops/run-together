import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { Announce, SIGNAL_LABEL, type StatusSignal } from '../domain/korean'
import {
  MAX_LENGTH,
  MAX_MESSAGES,
  MESSAGE_ICONS,
  PROBLEM_TEXT,
  saferSuggestions,
  textLength,
  unsafeFragment,
  validate,
  type MessageIcon,
  type QuickMessage,
} from '../domain/messages'
import { stepOf } from '../domain/scenario'
import { back, go } from '../router'
import { previewSpeech, setMessages, toast, useApp } from '../store'
import { Icon, MESSAGE_ICON, SIGNAL_ICON } from '../ui/Icon'
import { Bottom, Btn, Card } from '../ui/kit'

/** 운전 중(모임 주행 중 · 운전자)에는 만들거나 고칠 수 없다 (MOB-134) */
function useEditLocked() {
  const trip = useApp((s) => s.trip)
  const role = useApp((s) => s.profile.role)
  return trip.active && stepOf(trip.step).moving && role === 'driver' && !trip.unlocked
}

// ---------------- M-13 내 메시지 · M-15 주행 중 편집 잠금 ----------------

export function MessagesScreen() {
  const messages = useApp((s) => s.messages)
  const locked = useEditLocked()
  const defaults: StatusSignal[] = ['SLEEPY', 'REST', 'TOILET', 'ENERGY']
  return (
    <div className="screen">
      <header className="topbar">
        <button className="icon-btn" onClick={() => back('/home')} aria-label="뒤로">
          <Icon name="back" size={24} />
        </button>
        <div className="topbar-title">내 메시지</div>
        <span className="icon-btn ghost" />
      </header>
      <div className="body">
        {locked ? (
          <Card className="center-card">
            <span className="hero-icon">
              <Icon name="lock" size={24} />
            </span>
            <h2 className="h2 mt12">정차한 뒤에 고칠 수 있어요</h2>
            <p className="small mt8">모임 주행 중이고 휴대폰이 움직이고 있어요. 운전 중에는 메시지를 만들거나 고칠 수 없어요.</p>
          </Card>
        ) : (
          <>
            <p className="small">차량 화면에서 한 번 누르면 모임 전체에 바로 보내요. 운전 중에는 고칠 수 없어요.</p>
            <Card className="mt16">
              <div className="spread">
                <b>기본 메시지</b>
                <span className="tiny row" style={{ gap: 4 }}>
                  <Icon name="lock" size={12} /> 고칠 수 없어요
                </span>
              </div>
              <div className="row mt12" style={{ gap: 6, flexWrap: 'wrap' }}>
                {defaults.map((s) => (
                  <span className="chip chip-neutral" key={s} style={{ height: 30, background: '#fff', border: '1px solid var(--divider)' }}>
                    <Icon name={SIGNAL_ICON[s]} size={14} /> {SIGNAL_LABEL[s]}
                  </span>
                ))}
              </div>
              <p className="tiny mt8">기본 메시지는 가까운 쉴 곳 추천과 쉬어가기 투표로 이어져요</p>
            </Card>
          </>
        )}
        <Card className="mt12">
          <div className="spread">
            <b>
              내 메시지{' '}
              <span className="accent-text">
                {messages.length}/{MAX_MESSAGES}
              </span>
            </b>
            <span className="tiny row" style={{ gap: 4 }}>
              {locked ? (
                <>
                  <Icon name="lock" size={12} /> 지금은 고칠 수 없어요
                </>
              ) : (
                '끌어서 순서 바꾸기'
              )}
            </span>
          </div>
          {messages.length === 0 ? (
            <p className="small mt12">아직 만든 메시지가 없어요. 자주 하는 말을 만들어 두면 차량 화면에서 한 번에 보낼 수 있어요.</p>
          ) : (
            <MessageList messages={messages} locked={locked} />
          )}
        </Card>
        <p className="tiny mt12 row" style={{ gap: 6 }}>
          <Icon name={locked ? 'car' : 'refresh'} size={14} />
          {locked ? '보내기는 차량 화면의 ‘내 메시지’에서 큰 버튼으로 계속할 수 있어요' : '이 기기에 저장돼요 · 차량 앱 동기화는 서버를 연결한 뒤 (시연판)'}
        </p>
      </div>
      {locked ? (
        <Bottom note="정차하면 자동으로 다시 열려요">
          <Btn kind="secondary" onClick={() => back('/home')}>
            확인
          </Btn>
        </Bottom>
      ) : (
        <Bottom note="6개까지 · 차량 화면 버튼 순서는 이 목록 순서예요">
          <Btn icon="plus" disabled={messages.length >= MAX_MESSAGES} onClick={() => go('/messages/new')}>
            {messages.length >= MAX_MESSAGES ? '6개를 다 만들었어요' : `메시지 만들기 · ${messages.length}/${MAX_MESSAGES}`}
          </Btn>
        </Bottom>
      )}
    </div>
  )
}

function MessageList({ messages, locked }: { messages: QuickMessage[]; locked: boolean }) {
  const list = useRef<HTMLDivElement>(null)
  const [drag, setDrag] = useState<{ index: number; startY: number; dy: number; rowH: number } | null>(null)
  const target = drag ? Math.max(0, Math.min(messages.length - 1, drag.index + Math.round(drag.dy / drag.rowH))) : -1

  const move = (from: number, to: number) => {
    if (from === to) return
    const next = [...messages]
    const [m] = next.splice(from, 1)
    next.splice(to, 0, m)
    setMessages(next)
  }

  const onDown = (e: ReactPointerEvent, index: number) => {
    if (locked) return
    const row = list.current?.children[index] as HTMLElement | undefined
    const rowH = row ? row.getBoundingClientRect().height : 56
    e.currentTarget.setPointerCapture(e.pointerId)
    setDrag({ index, startY: e.clientY, dy: 0, rowH })
  }

  return (
    <div className="mt8" ref={list}>
      {messages.map((m, i) => {
        let shift = 0
        if (drag && i !== drag.index) {
          if (drag.index < target && i > drag.index && i <= target) shift = -drag.rowH
          if (drag.index > target && i >= target && i < drag.index) shift = drag.rowH
        }
        const dragging = drag?.index === i
        return (
          <div
            key={m.id}
            className={`msg-row${dragging ? ' dragging' : ''}${locked ? ' locked' : ''}`}
            style={{ transform: dragging ? `translateY(${drag!.dy}px)` : shift ? `translateY(${shift}px)` : undefined, transition: dragging ? 'none' : 'transform .15s ease' }}
          >
            {!locked && (
              <button
                className="grip"
                aria-label={`${m.text} 순서 바꾸기 (위아래 화살표)`}
                onPointerDown={(e) => onDown(e, i)}
                onPointerMove={(e) => drag && setDrag({ ...drag, dy: e.clientY - drag.startY })}
                onPointerUp={() => {
                  if (drag) move(drag.index, target)
                  setDrag(null)
                }}
                onPointerCancel={() => setDrag(null)}
                onKeyDown={(e) => {
                  if (e.key === 'ArrowUp' && i > 0) move(i, i - 1)
                  if (e.key === 'ArrowDown' && i < messages.length - 1) move(i, i + 1)
                }}
              >
                <Icon name="grip" size={18} />
              </button>
            )}
            <span className="msg-icon">
              <Icon name={MESSAGE_ICON[m.icon]} size={19} />
            </span>
            <button className="msg-body" disabled={locked} onClick={() => go(`/messages/edit/${m.id}`)} style={{ border: 0, textAlign: 'left' }}>
              <b style={{ fontSize: 15 }}>{m.text}</b>
              <p className="tiny">
                {i + 1}번째 · {textLength(m.text)}/{MAX_LENGTH}자
              </p>
            </button>
            {!locked && (
              <button
                className="icon-btn"
                aria-label={`${m.text} 지우기`}
                onClick={() => {
                  setMessages(messages.filter((x) => x.id !== m.id))
                  toast(`‘${m.text}’를 지웠어요`)
                }}
              >
                <Icon name="close" size={18} />
              </button>
            )}
          </div>
        )
      })}
    </div>
  )
}

// ---------------- M-14 메시지 만들기 · 고치기 ----------------

export function EditMessageScreen({ id }: { id?: string }) {
  const messages = useApp((s) => s.messages)
  const name = useApp((s) => s.profile.name)
  const locked = useEditLocked()
  const editing = id ? messages.find((m) => m.id === id) : undefined
  const [text, setText] = useState(editing?.text ?? '')
  const [icon, setIcon] = useState<MessageIcon>(editing?.icon ?? 'COFFEE')
  useEffect(() => {
    if (locked) go('/messages', true)
  }, [locked])
  useEffect(() => {
    if (id && !editing) go('/messages', true)
  }, [id, editing])

  const check = validate(text, messages, editing ? editing.id : null)
  const typed = text.trim().length > 0
  const has = (p: Parameters<typeof check.problems.includes>[0]) => check.problems.includes(p)
  const unsafe = has('UNSAFE')
  const fragment = unsafe ? unsafeFragment(text) : null
  const suggestions = unsafe ? saferSuggestions(text) : []
  const lengthOk = !has('EMPTY') && !has('TOO_SHORT') && !has('TOO_LONG')
  const blocking = check.problems.filter((p) => p !== 'EMPTY')

  const save = () => {
    if (!check.ok) return
    if (editing) setMessages(messages.map((m) => (m.id === editing.id ? { ...m, text: check.text, icon } : m)))
    else setMessages([...messages, { id: `m${Date.now()}`, text: check.text, icon, order: messages.length + 1 }])
    toast(editing ? '메시지를 고쳤어요' : '메시지를 저장했어요')
    back('/messages')
  }

  return (
    <div className="screen">
      <header className="topbar">
        <button className="icon-btn" onClick={() => back('/messages')} aria-label="뒤로">
          <Icon name="back" size={24} />
        </button>
        <div className="topbar-title">{editing ? '메시지 고치기' : '메시지 만들기'}</div>
        <span className="icon-btn ghost" />
      </header>
      <div className="body">
        <Card>
          <div className="spread">
            <b style={{ fontSize: 14 }}>아이콘</b>
            <span className="tiny">차량 버튼에 함께 보여요</span>
          </div>
          <div className="icon-grid mt12" role="radiogroup" aria-label="아이콘">
            {MESSAGE_ICONS.map((ic) => (
              <button key={ic} role="radio" aria-checked={icon === ic} className={icon === ic ? 'on' : ''} onClick={() => setIcon(ic)} aria-label={ic}>
                <Icon name={MESSAGE_ICON[ic]} size={22} />
              </button>
            ))}
          </div>
        </Card>
        <label className="label mt20" htmlFor="msg" style={{ display: 'block' }}>
          문장
        </label>
        <input
          id="msg"
          className={`input${typed && blocking.length ? ' bad' : ''}`}
          value={text}
          maxLength={30}
          placeholder="예: 카페 들렀다 가요"
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && save()}
        />
        {typed && unsafe ? (
          <p className="mt8" style={{ color: 'var(--warn)', fontSize: 13, fontWeight: 700 }}>
            {PROBLEM_TEXT.UNSAFE}
          </p>
        ) : (
          <p className="input-help">
            {textLength(text)}/{MAX_LENGTH}자 · 글자 · 숫자 · 기본 문장부호
          </p>
        )}
        <Card className="mt12">
          <ul className="marks">
            <CheckMark state={typed ? lengthOk : null}>2~20자예요</CheckMark>
            <CheckMark state={typed ? !unsafe : null}>{unsafe ? `재촉하는 말이 있어요 · ‘${fragment}’` : '재촉하거나 위험한 운전을 권하는 말이 없어요'}</CheckMark>
            <CheckMark state={typed ? !has('CONTACT_INFO') : null}>전화번호 · 링크가 없어요</CheckMark>
            <CheckMark state={typed ? !has('DUPLICATE') && !has('SAME_AS_DEFAULT') : null}>
              {has('SAME_AS_DEFAULT') ? '기본 메시지와 같은 말이에요' : has('DUPLICATE') ? '이미 같은 메시지가 있어요' : '다른 메시지 · 기본 메시지와 겹치지 않아요'}
            </CheckMark>
            {has('UNSUPPORTED_CHARACTER') && <CheckMark state={false}>{PROBLEM_TEXT.UNSUPPORTED_CHARACTER}</CheckMark>}
            {has('LIMIT_REACHED') && <CheckMark state={false}>{PROBLEM_TEXT.LIMIT_REACHED}</CheckMark>}
          </ul>
        </Card>
        {unsafe && suggestions.length > 0 ? (
          <Card className="mt12 warn">
            <b className="row" style={{ gap: 6, fontSize: 14 }}>
              <Icon name="info" size={16} /> 이렇게 바꿔 보면 어때요?
            </b>
            <div className="row mt8" style={{ gap: 8, flexWrap: 'wrap' }}>
              {suggestions.map((s) => (
                <button key={s} className="suggest-chip" onClick={() => setText(s)}>
                  <Icon name={s.includes('천천히') ? 'gauge' : 'flag'} size={15} /> {s}
                </button>
              ))}
            </div>
          </Card>
        ) : (
          <Card className="mt12">
            <span className="tiny">받는 사람에게 이렇게 들려요</span>
            <p className="mt4" style={{ fontWeight: 800, fontSize: 16 }}>
              “{Announce.customMessage(name, check.ok ? check.text : text.trim() || '카페 들렀다 가요')}”
            </p>
            <Btn kind="secondary" small icon="volume" className="mt8" onClick={() => previewSpeech(Announce.customMessage(name, check.text || '카페 들렀다 가요'))}>
              미리 듣기
            </Btn>
          </Card>
        )}
      </div>
      <Bottom note={check.ok || !typed ? '저장하면 이 기기에 보관돼요 · 서버를 연결하면 차량 앱으로 동기화돼요' : '문제를 고치면 저장할 수 있어요'}>
        {typed && blocking.length > 0 ? (
          <Btn kind="soft" icon="lock" disabled>
            저장할 수 없어요
          </Btn>
        ) : (
          <Btn icon="check" disabled={!check.ok} onClick={save}>
            저장
          </Btn>
        )}
      </Bottom>
    </div>
  )
}

function CheckMark({ state, children }: { state: boolean | null; children: React.ReactNode }) {
  return (
    <li className={`mark ${state === null ? 'idle' : state ? 'ok' : 'bad'}`}>
      <Icon name={state === false ? 'close' : 'check'} size={15} stroke={2.6} />
      <span>{children}</span>
    </li>
  )
}
