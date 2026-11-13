import { Fragment, useEffect, useRef, useState } from 'react'
import { COURSE, GROUP, ME, OTHERS } from '../domain/scenario'
import { askLocationOnce, readClipboard, unlockSpeech } from '../platform/browser'
import { back, go } from '../router'
import { finishJoin, markReady, setProfile, toast, useApp, useMembers, type Fuel } from '../store'
import { Icon } from '../ui/Icon'
import { Avatar, Bottom, Btn, Card, Chip, Segmented, StepBar, Toggle, TopBar } from '../ui/kit'

const NAME_OK = /^[가-힣A-Za-z0-9 ]{2,10}$/

/** 초대 링크 미리보기 그림 (시연 코스) */
function InviteMap() {
  return (
    <svg viewBox="0 0 350 132" className="invite-map" role="img" aria-label="잠실에서 강릉 안목해변까지 코스">
      <rect width="350" height="132" rx="16" fill="#EEF1F4" />
      <path d="M0 98 C 60 86, 120 120, 200 104 S 320 84, 350 96 L 350 132 L 0 132 Z" fill="#D7E6F4" />
      <rect x="236" y="22" width="70" height="38" rx="10" fill="#DCEDD8" />
      <path d="M40 104 C 80 92, 100 70, 150 72 S 210 56, 240 50 S 290 30, 322 22" stroke="#FFFFFF" strokeWidth="9" fill="none" strokeLinecap="round" />
      <path d="M40 104 C 80 92, 100 70, 150 72 S 210 56, 240 50 S 290 30, 322 22" stroke="#2563EB" strokeWidth="4" fill="none" strokeLinecap="round" />
      {[
        [150, 72, '덕평'],
        [240, 50, '횡성'],
      ].map(([x, y, t]) => (
        <g key={t as string} transform={`translate(${x},${y})`}>
          <circle r="6" fill="#fff" stroke="#2563EB" strokeWidth="3" />
          <rect x="-18" y="-30" width="36" height="18" rx="6" fill="#fff" stroke="#D6DBE2" />
          <text y="-17" textAnchor="middle" fontSize="10" fontWeight="600" fill="#2A2F37">
            {t}
          </text>
        </g>
      ))}
      <g transform="translate(322,22)">
        <rect x="-11" y="-11" width="22" height="22" rx="6" fill="#15181E" />
        <path d="M-4 6V-6M-4 -5.5h7l-1.4 2.5 1.4 2.5h-7" stroke="#fff" strokeWidth="1.6" fill="none" />
        <rect x="-74" y="-9" width="56" height="18" rx="6" fill="#fff" stroke="#D6DBE2" />
        <text x="-46" y="4" textAnchor="middle" fontSize="10" fontWeight="600" fill="#2A2F37">
          안목해변
        </text>
      </g>
      <g transform="translate(40,104)">
        <circle r="6" fill="#2563EB" stroke="#fff" strokeWidth="2.5" />
        <rect x="-4" y="-34" width="68" height="18" rx="6" fill="#fff" stroke="#D6DBE2" />
        <text x="30" y="-21" textAnchor="middle" fontSize="10" fontWeight="600" fill="#2A2F37">
          잠실 07:30
        </text>
      </g>
    </svg>
  )
}

export function StartScreen({ code }: { code?: string }) {
  const name = useApp((s) => s.profile.name)
  const [value, setValue] = useState(name)
  const ok = NAME_OK.test(value.trim())
  const leader = OTHERS[0]
  useEffect(() => {
    if (code && code !== GROUP.code) toast('초대 링크의 모임을 찾지 못했어요 · 시연 모임으로 열었어요')
  }, [code])
  return (
    <div className="screen">
      <div className="body" style={{ paddingTop: 16 }}>
        <div className="spread">
          <span className="brand">
            <span className="logo-mark">
              <Icon name="route" size={17} stroke={2.2} />
            </span>
            같이 달려
          </span>
          <span className="tiny">설치 없이 바로 참여</span>
        </div>
        <div className="mt20">
          <InviteMap />
        </div>
        <div className="row mt20">
          <Avatar m={leader} size={32} />
          <div>
            <b className="name">{leader.name}님이 초대했어요</b>
            <p className="tiny">모임장 · Pleos 차량으로 참여</p>
          </div>
        </div>
        <h1 className="h1 mt16">{GROUP.name}</h1>
        <p className="info-line mt4">
          {GROUP.date} · {GROUP.depart} 잠실 출발
        </p>
        <p className="meta mt4">
          <span>6명 참여 중</span>
          <span className="sep">·</span>
          <span>집결지 2곳</span>
          <span className="sep">·</span>
          <span>약 3시간 10분</span>
        </p>
        <label className="label mt32" htmlFor="nick">
          모임에서 보일 이름
        </label>
        <input
          id="nick"
          className={`input${value && !ok ? ' bad' : ''}`}
          value={value}
          maxLength={10}
          autoComplete="nickname"
          onChange={(e) => setValue(e.target.value)}
          placeholder="예: 하늘"
        />
        <p className="input-help">2~10자 · 이 기기에 저장돼요 · 아바타 색은 겹치지 않게 정해져요</p>
      </div>
      <Bottom note="위치는 여행 중에만, 모임 사람에게만 공유돼요">
        <Btn
          disabled={!ok}
          onClick={() => {
            unlockSpeech()
            setProfile({ name: value.trim() })
            go('/setup/1')
          }}
        >
          다음
        </Btn>
        <p className="center small mt8">
          초대 링크가 없나요?{' '}
          <button className="link-btn" onClick={() => go('/code')}>
            모임 코드로 참여
          </button>
        </p>
      </Bottom>
    </div>
  )
}

export function CodeScreen() {
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const input = useRef<HTMLInputElement>(null)
  const digits = code.padEnd(6, ' ').slice(0, 6).split('')
  const left = 6 - code.length
  const submit = () => {
    if (code === GROUP.code) go('/setup/1')
    else setError('모임을 찾지 못했어요 · 코드를 다시 확인해 주세요')
  }
  return (
    <div className="screen">
      <TopBar title="모임 코드로 참여" onBack={() => back('/start')} />
      <div className="body">
        <h1 className="h1 mt8">
          모임 코드 6자리를
          <br />
          입력해 주세요
        </h1>
        <p className="lead">모임장에게 받은 코드예요. 복사한 코드는 붙여넣기 하면 돼요.</p>
        <div className="code-boxes mt32" onClick={() => input.current?.focus()}>
          {digits.slice(0, 3).map((d, i) => (
            <span key={i} className={i === code.length ? 'cur' : ''}>
              {d.trim()}
            </span>
          ))}
          <i className="code-sep">-</i>
          {digits.slice(3).map((d, i) => (
            <span key={i + 3} className={i + 3 === code.length ? 'cur' : ''}>
              {d.trim()}
            </span>
          ))}
          <input
            ref={input}
            aria-label="모임 코드 6자리"
            inputMode="numeric"
            autoComplete="one-time-code"
            autoFocus
            value={code}
            onChange={(e) => {
              setError(null)
              setCode(e.target.value.replace(/\D/g, '').slice(0, 6))
            }}
            onKeyDown={(e) => e.key === 'Enter' && code.length === 6 && submit()}
          />
        </div>
        <div className="row mt12">
          <Btn
            kind="secondary"
            small
            icon="paste"
            onClick={async () => {
              const text = await readClipboard()
              const d = (text ?? '').replace(/\D/g, '').slice(0, 6)
              if (d) setCode(d)
              else toast('붙여넣을 코드가 없어요')
            }}
          >
            붙여넣기
          </Btn>
          <span className="tiny">숫자만 넣으면 돼요 · 시연 코드 {GROUP.code}</span>
        </div>
        {error && <p className="error-text mt12">{error}</p>}
      </div>
      <Bottom>
        <Btn disabled={left > 0} onClick={submit}>
          {left > 0 ? `참여하기 · ${left}자리 남았어요` : '참여하기'}
        </Btn>
      </Bottom>
    </div>
  )
}

// ---------------- M-02 참여 설정 ----------------

function SetupFrame({ index, label, children }: { index: number; label: string; children: React.ReactNode }) {
  return (
    <div className="screen">
      <TopBar title="참여 설정" onBack={() => back(index > 1 ? `/setup/${index - 1}` : '/start')} />
      <StepBar index={index} total={4} label={label} />
      {children}
    </div>
  )
}

export function SetupPreview() {
  const all = useMembers()
  return (
    <SetupFrame index={1} label="모임 확인">
      <div className="body">
        <p className="kicker mt8">
          초대받은 모임 · {GROUP.shortDate} {GROUP.depart} 출발
        </p>
        <h1 className="h1 mt4">{GROUP.name}</h1>
        <section className="mt32">
          <div className="section-head">
            <h2 className="h3">오늘 코스</h2>
            <span className="tiny">{GROUP.totalText}</span>
          </div>
          <ol className="timeline mt16">
            {COURSE.map((c, i) => (
              <li key={c.id} className={i === 0 ? 'start' : i === COURSE.length - 1 ? 'end' : ''}>
                <span className="tl-time">{c.time}</span>
                <span className="tl-dot" />
                <div className="tl-body">
                  <b>{c.name}</b>
                  <p className="meta">
                    <span>{c.label}</span>
                    {c.rest && (
                      <>
                        <span className="sep">·</span>
                        <span className="mi">
                          <Icon name="clock" size={13} />
                          {c.rest}분
                        </span>
                      </>
                    )}
                    {c.note && (
                      <>
                        <span className="sep">·</span>
                        <span className="mi">
                          <Icon name="fuel" size={13} />
                          {c.note}
                        </span>
                      </>
                    )}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        </section>
        <section className="group mt24">
          <div className="section-head">
            <h2 className="h3">함께 가는 차 6대</h2>
            <span className="tiny">모임장 준호</span>
          </div>
          <div className="row mt12" style={{ gap: 6 }}>
            {all.map((m) => (
              <Avatar key={m.id} m={m} size={32} dashed={m.id === ME} />
            ))}
          </div>
          <p className="meta mt12">
            <span className="mi">
              <Icon name="car" size={14} />
              Pleos 차량 5대
            </span>
            <span className="sep">·</span>
            <span className="mi">
              <Icon name="phone" size={14} />
              휴대폰 참여 · 나
            </span>
          </p>
        </section>
      </div>
      <Bottom note="출발 안내 전까지는 위치를 보내지 않아요">
        <Btn onClick={() => go('/setup/2')}>참여하기</Btn>
      </Bottom>
    </SetupFrame>
  )
}

export function SetupConsent() {
  const [agree, setAgree] = useState(false)
  const [more, setMore] = useState(false)
  const [busy, setBusy] = useState(false)
  const rows: [string, string][] = [
    ['누구에게', '이 모임 참여자에게만. 모임 밖으로는 공개하지 않아요.'],
    ['언제부터 언제까지', '모임장의 출발 안내부터 여행이 끝날 때까지만 보내요.'],
    ['무엇을', '휴대폰 GPS 위치·정확도·측정 시각. 지도에 휴대폰 표시로 보여요.'],
    ['끝나면', '공유를 멈추고 모임 중 위치를 지워요. 원하면 요약 기록만 남겨요.'],
  ]
  return (
    <SetupFrame index={2} label="위치 공유 동의">
      <div className="body">
        <h1 className="h1 mt8">
          위치는 여행 중에만,
          <br />
          모임 사람에게만 보여요
        </h1>
        <dl className="def-list mt24">
          {rows.map(([title, text]) => (
            <Fragment key={title}>
              <dt>{title}</dt>
              <dd>{text}</dd>
            </Fragment>
          ))}
        </dl>
        <label className={`consent-box mt24${agree ? ' on' : ''}`}>
          <input type="checkbox" className="check" checked={agree} onChange={(e) => setAgree(e.target.checked)} />
          <span className="grow">[필수] 개인위치정보 수집·이용·제공에 동의해요</span>
          <button type="button" className="link-btn" onClick={(e) => (e.preventDefault(), setMore(!more))}>
            자세히
          </button>
        </label>
        {more && (
          <ul className="small mt12 detail-list">
            <li>수집 항목: 휴대폰 위치(위도·경도), 정확도, 측정 시각</li>
            <li>이용 목적: 모임 지도에 표시, 집결지·쉴 곳 추천, 도움 요청 시 모임장에게 공유</li>
            <li>보관: 여행이 끝나면 모임 중 위치를 지워요. 요약 기록은 원할 때만 남겨요.</li>
            <li>동의하지 않아도 ‘보기만’ 참여할 수 있어요.</li>
            <li>시연판 안내: 동기화 서버가 없어 위치는 이 기기 밖으로 보내지 않아요.</li>
          </ul>
        )}
      </div>
      <Bottom note="다음에 브라우저가 위치 권한을 물어봐요 · 설정에서 언제든 끌 수 있어요">
        <Btn
          disabled={!agree || busy}
          onClick={async () => {
            setBusy(true)
            const r = await askLocationOnce()
            setBusy(false)
            if (r === 'granted') {
              setProfile({ location: 'share' })
              go('/setup/3')
            } else if (r === 'denied') {
              setProfile({ location: 'viewOnly' })
              go('/setup/denied')
            } else {
              setProfile({ location: 'viewOnly' })
              toast('이 기기에서는 위치를 쓸 수 없어 보기만 참여해요')
              go('/setup/3')
            }
          }}
        >
          {busy ? '위치 권한을 묻는 중…' : '동의하고 계속'}
        </Btn>
        <Btn
          kind="ghost"
          onClick={() => {
            setProfile({ location: 'viewOnly' })
            go('/setup/3')
          }}
        >
          위치 없이 보기만 할게요
        </Btn>
      </Bottom>
    </SetupFrame>
  )
}

export function SetupDenied() {
  const name = useApp((s) => s.profile.name)
  const isIos = /iPhone|iPad|iPod/.test(navigator.userAgent)
  const [os, setOs] = useState<'android' | 'ios'>(isIos ? 'ios' : 'android')
  const steps =
    os === 'android'
      ? ['주소창 왼쪽의 사이트 정보 아이콘을 눌러요', '권한 → 위치 → ‘허용’으로 바꿔요', '이 화면으로 돌아와 ‘다시 시도’를 눌러요']
      : ['설정 앱 → Safari → 위치를 열어요', '‘묻기’ 또는 ‘허용’으로 바꿔요', '이 화면으로 돌아와 ‘다시 시도’를 눌러요']
  return (
    <div className="screen">
      <TopBar title="참여 설정" onBack={() => back('/setup/2')} />
      <div className="body">
        <h1 className="h1 mt8">위치 권한이 꺼져 있어요</h1>
        <p className="lead">
          지금은 위치 없이 ‘보기만’ 참여해요. 다른 사람 지도에는 {name}님이 ‘위치 꺼짐’으로 보여요.
        </p>
        <section className="mt32">
          <h2 className="h3">다시 켜는 방법</h2>
          <div className="mt12">
            <Segmented
              value={os}
              onChange={setOs}
              options={[
                { value: 'android', label: 'Android · Chrome' },
                { value: 'ios', label: 'iPhone · Safari' },
              ]}
            />
          </div>
          <ol className="num-steps mt16">
            {steps.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ol>
        </section>
        <p className="note mt24">보기만 참여해도 상황 공유 알림과 집결지 변경은 그대로 받아요.</p>
      </div>
      <Bottom>
        <Btn
          icon="location"
          onClick={async () => {
            const r = await askLocationOnce()
            if (r === 'granted') {
              setProfile({ location: 'share' })
              toast('위치를 켰어요 · 출발 안내부터 모임에 보여요')
              go('/setup/3')
            } else toast('아직 위치 권한이 꺼져 있어요')
          }}
        >
          다시 시도
        </Btn>
        <Btn kind="secondary" onClick={() => go('/setup/3')}>
          보기만 할게요
        </Btn>
      </Bottom>
    </div>
  )
}

export function SetupRole() {
  const role = useApp((s) => s.profile.role)
  return (
    <SetupFrame index={3} label="운전자 · 동승자">
      <div className="body">
        <h1 className="h1 mt8">지금 어떻게 가세요?</h1>
        <p className="lead">역할에 따라 여행 중 화면이 달라져요. 정차 중에 설정에서 바꿀 수 있어요.</p>
        <div className="mt24">
          <button className={`choice${role === 'driver' ? ' on' : ''}`} onClick={() => setProfile({ role: 'driver' })} aria-pressed={role === 'driver'}>
            <Icon name="car" size={22} />
            <span className="grow">
              <b>운전해요</b>
              <p className="small mt4">움직이는 동안은 누르지 않아도 되는 모임 지도가 떠요. 소식은 소리로 들려 드려요</p>
            </span>
            <span className="radio" />
          </button>
          <button className={`choice${role === 'passenger' ? ' on' : ''}`} onClick={() => setProfile({ role: 'passenger' })} aria-pressed={role === 'passenger'}>
            <Icon name="users" size={22} />
            <span className="grow">
              <b>같이 타고 가요</b>
              <p className="small mt4">상황 공유·투표·지도를 볼 수 있어요. 내 차(하늘 차)에 탄 것으로 보여요</p>
            </span>
            <span className="radio" />
          </button>
        </div>
        {role === 'driver' && (
          <div className="mt24">
            <p className="tiny">운전자 모드 미리보기</p>
            <div className="mini-screen mt8" aria-hidden="true">
              <div className="mini-card">
                <span className="lock-state">
                  <Icon name="lock" size={13} /> 움직이는 동안 잠금
                </span>
                <p className="kicker mt8">
                  <Icon name="flag" size={14} /> 덕평자연휴게소
                </p>
                <p className="mini-num">
                  <b>21km</b>
                  <span className="small">약 16분 · 08:16 도착</span>
                </p>
              </div>
            </div>
          </div>
        )}
        <p className="warn-note mt24">
          <Icon name="warn" size={16} />
          <span>운전 중 휴대폰 조작은 도로교통법 제49조로 금지돼요. 조작은 동승자나 정차한 상태에서 해 주세요.</span>
        </p>
      </div>
      <Bottom>
        <Btn onClick={() => go('/setup/4')}>다음</Btn>
      </Bottom>
    </SetupFrame>
  )
}

function FuelBars({ level }: { level: number }) {
  return (
    <span className="fuel-bars" aria-hidden="true">
      {[8, 13, 18].map((h, i) => (
        <i key={h} className={i < level ? 'on' : ''} style={{ height: h }} />
      ))}
    </span>
  )
}

export function SetupVehicle() {
  const p = useApp((s) => s.profile)
  const options: { value: Fuel; label: string; sub: string; level: number }[] = [
    { value: 'plenty', label: '넉넉', sub: '절반 이상', level: 3 },
    { value: 'normal', label: '보통', sub: '1/4 ~ 절반', level: 2 },
    { value: 'low', label: '부족', sub: '1/4 미만', level: 1 },
  ]
  return (
    <SetupFrame index={4} label="차량 · 연료">
      <div className="body">
        <h1 className="h1 mt8">{p.ev ? '배터리는 얼마나 남았나요?' : '연료는 얼마나 남았나요?'}</h1>
        <p className="lead">Pleos 차량이 아니라서 직접 골라 주세요. 모두가 쉴 곳을 정할 때 써요.</p>
        <div className="fuel-grid mt24" role="radiogroup">
          {options.map((o) => (
            <button
              key={o.value}
              role="radio"
              aria-checked={p.fuel === o.value}
              className={`fuel-card${p.fuel === o.value ? ' on' : ''}${o.value === 'low' ? ' low' : ''}`}
              onClick={() => setProfile({ fuel: o.value })}
            >
              <FuelBars level={o.level} />
              <b>{o.label}</b>
              <small>{o.sub}</small>
            </button>
          ))}
        </div>
        <p className="tiny mt8">주유했다면 집결지에서 다시 고를 수 있어요</p>
        <div className="setting-row solo mt20">
          <div className="grow">
            <b>전기차예요</b>
            <p className="tiny">충전소가 있는 곳을 먼저 추천해요</p>
          </div>
          <Toggle label="전기차예요" checked={p.ev} onChange={(ev) => setProfile({ ev })} />
        </div>
        <label className="label mt24" htmlFor="model">
          차종 (선택)
        </label>
        <input id="model" className="input" value={p.model} maxLength={12} placeholder="예: K5, 아반떼" onChange={(e) => setProfile({ model: e.target.value })} />
        <p className="input-help">
          모임원에게 ‘{p.name}
          {p.model ? ` · ${p.model}` : ' · K5'}’처럼 보여요
        </p>
      </div>
      <Bottom note="지도와 추천에는 출처가 ‘직접 입력’으로 표시돼요">
        <Btn
          onClick={() => {
            finishJoin()
            go('/lobby', true)
          }}
        >
          참여 완료
        </Btn>
      </Bottom>
    </SetupFrame>
  )
}

// ---------------- M-03 대기실 ----------------

export function LobbyScreen() {
  const trip = useApp((s) => s.trip)
  const p = useApp((s) => s.profile)
  const all = useMembers()
  useEffect(() => {
    if (trip.active && trip.step !== 'lobby') go('/trip', true)
  }, [trip.active, trip.step])
  const readyCount = trip.ready.length + (trip.meReady ? 1 : 0)
  const sub: Record<string, [string, string | null, 'warn' | 'neutral' | null]> = {
    junho: ['모임장 · 차량 연동', null, null],
    minji: ['차량 연동', null, null],
    sua: ['차량 연동', '연료 18%', 'warn'],
    taehyun: ['전기차', '배터리 64%', 'neutral'],
    jiwon: ['차량 연동', null, null],
  }
  const fuelText = p.ev ? '전기차' : `연료 ${p.fuel === 'plenty' ? '넉넉' : p.fuel === 'low' ? '부족' : '보통'}`
  return (
    <div className="screen">
      <div className="body" style={{ paddingTop: 12 }}>
        <div className="spread" style={{ alignItems: 'flex-start' }}>
          <div className="grow" style={{ paddingTop: 8 }}>
            <h1 className="h1">{trip.groupName}</h1>
            <p className="meta mt4">6명 · 07:30 출발 · 최종 도착 10:40</p>
          </div>
          <button className="icon-btn" aria-label="설정" onClick={() => go('/settings')}>
            <Icon name="gear" size={22} />
          </button>
        </div>
        <div className="row mt12" style={{ gap: 6 }}>
          <Chip tone="accent" icon="clock">
            출발 5분 전 · 07:25
          </Chip>
          <Chip icon={p.role === 'driver' ? 'lock' : 'users'}>{p.role === 'driver' ? '운전자 모드' : '동승자 모드'}</Chip>
        </div>
        {trip.departing && (
          <Card className="accent mt16">
            <p className="status-line accent">
              <Icon name="volume" size={18} /> 모임장 준호님이 출발 안내를 보냈어요
            </p>
            <p className="tiny mt4">곧 여행 화면으로 넘어가요 · 따라오지 않아도 돼요, 덕평에서 만나요</p>
          </Card>
        )}
        <section className="mt32">
          <div className="spread" style={{ alignItems: 'flex-end' }}>
            <p className="t-num">
              {readyCount}
              <small>/6</small>
              <span className="t-num-label">준비 완료</span>
            </p>
            <span className="tiny" style={{ paddingBottom: 6 }}>
              차는 시동, 휴대폰은 버튼으로
            </span>
          </div>
          <div className="list lined-top mt12">
            {all.map((m) => {
              const isMe = m.id === ME
              const ready = isMe ? trip.meReady : trip.ready.includes(m.id)
              const [line, badge, tone] = isMe ? [`휴대폰 · ${fuelText}`, null, null] : sub[m.id]
              return (
                <div className="list-row" key={m.id}>
                  <Avatar m={m} size={36} phone={isMe} />
                  <div className="grow">
                    <span className="name">{isMe ? `${m.name} (나)` : m.name}</span>
                    <p className="tiny">{line}</p>
                  </div>
                  {badge && tone && <Chip tone={tone} icon={tone === 'warn' ? 'fuel' : 'bolt'}>{badge}</Chip>}
                  {ready ? (
                    <span className="status-ok">
                      <Icon name="check" size={14} stroke={2.2} />
                      {isMe ? '준비 완료' : '시동 켜짐'}
                    </span>
                  ) : (
                    <span className={`status-wait${isMe ? ' mine' : ''}`}>{isMe ? '버튼 대기' : '오는 중 · 약 2분'}</span>
                  )}
                </div>
              )
            })}
          </div>
        </section>
        <section className="group mt8">
          <div className="section-head">
            <h2 className="h3">오늘 코스</h2>
            <span className="tiny">235km · 휴식 2번</span>
          </div>
          <div className="course-dots mt16">
            {COURSE.map((c, i) => (
              <span key={c.id} className={`dot${i === COURSE.length - 1 ? ' end' : ''}`}>
                <i />
                {c.short}
                <small>{c.time}</small>
              </span>
            ))}
          </div>
        </section>
      </div>
      <Bottom note="차에 탄 뒤 눌러 주세요 · 모임장이 출발 안내를 보내면 소리로 알려 드려요">
        {trip.meReady ? (
          <Btn kind="soft" icon="check" disabled>
            준비 완료 · 출발 안내를 기다려요
          </Btn>
        ) : (
          <Btn onClick={markReady}>출발 준비 완료</Btn>
        )}
      </Bottom>
    </div>
  )
}
