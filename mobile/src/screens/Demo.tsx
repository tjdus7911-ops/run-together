import { useState } from 'react'
import { STEPS } from '../domain/scenario'
import { go } from '../router'
import { jumpTo, resetDemo, setProfile, setTrip, useApp } from '../store'
import { Icon } from '../ui/Icon'
import { Btn, Segmented, Sheet, Toggle } from '../ui/kit'

/**
 * 시연 패널 (차량 앱의 ‘이전 / 다음’과 같은 역할).
 * 동기화 서버 없이 시나리오 A 를 따라가므로, 심사·발표 때 원하는 장면으로 바로 넘길 수 있게 한다.
 */
export function DemoTab() {
  const [open, setOpen] = useState(false)
  const trip = useApp((s) => s.trip)
  const p = useApp((s) => s.profile)
  return (
    <>
      <button className="demo-tab" onClick={() => setOpen(true)} aria-label="시연 패널 열기">
        시연
      </button>
      <Sheet open={open} onClose={() => setOpen(false)} label="시연 패널">
        <div className="spread">
          <h2 className="h2">시연 단계</h2>
          <span className="chip chip-neutral">서버 없는 시연판</span>
        </div>
        <p className="small mt4">시나리오 A(6대 · 잠실 → 강릉)를 따라가요. 다른 차의 움직임과 응답은 시연 데이터예요.</p>
        <div className="mt12">
          {STEPS.map((s) => {
            const on = p.joined && trip.active && trip.step === s.id
            return (
              <button
                key={s.id}
                className={`demo-step${on ? ' on' : ''}`}
                onClick={() => {
                  jumpTo(s.id)
                  setOpen(false)
                  go(s.id === 'lobby' ? '/lobby' : '/trip')
                }}
              >
                <b>{s.clock}</b>
                <span className="grow" style={{ fontSize: 14, fontWeight: 600 }}>
                  {s.title}
                </span>
                {on ? <Icon name="checkCircle" size={18} style={{ color: 'var(--accent)' }} /> : <Icon name="chevron" size={16} />}
              </button>
            )
          })}
        </div>
        <p className="label mt16">이 휴대폰은</p>
        <Segmented
          value={p.role}
          onChange={(role) => {
            setProfile({ role })
            setTrip((t) => ({ ...t, unlocked: false }))
          }}
          options={[
            { value: 'driver', label: '운전해요', sub: 'M-06 모임 내비' },
            { value: 'passenger', label: '같이 타고 가요', sub: 'M-07 모임 지도' },
          ]}
        />
        <div className="setting-row mt8">
          <span className="setting-icon">
            <Icon name="wifiOff" size={18} />
          </span>
          <div className="grow">
            <b style={{ fontSize: 14 }}>연결 끊김 보기</b>
            <p className="tiny">M-07 오래된 위치 · 다시 연결하는 중</p>
          </div>
          <Toggle label="연결 끊김 보기" checked={trip.demoOffline} onChange={(v) => setTrip((t) => ({ ...t, demoOffline: v }))} />
        </div>
        <div className="demo-links mt12">
          <Btn kind="secondary" small icon="route" onClick={() => (setOpen(false), go('/start'))}>
            초대 화면
          </Btn>
          <Btn kind="secondary" small icon="edit" onClick={() => (setOpen(false), go('/messages'))}>
            내 메시지
          </Btn>
          <Btn kind="secondary" small icon="plus" onClick={() => (setOpen(false), go('/create/1'))}>
            모임 만들기
          </Btn>
        </div>
        <Btn
          kind="danger"
          icon="refresh"
          className="mt16"
          onClick={() => {
            resetDemo()
            setOpen(false)
            go('/start', true)
          }}
        >
          처음부터 다시 (이 기기의 시연 데이터 지우기)
        </Btn>
      </Sheet>
    </>
  )
}
