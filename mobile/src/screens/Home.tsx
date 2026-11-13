import { useState } from 'react'
import { COPY_SOURCES, COURSE, GROUP, PAST, SUGGESTED_STOPS, UPCOMING, stepOf } from '../domain/scenario'
import { copyText, hasWakeLock, shareLink } from '../platform/browser'
import { back, go } from '../router'
import { finishJoin, leaveGroup, setProfile, setSettings, toast, useApp, useMembers } from '../store'
import { Icon } from '../ui/Icon'
import { Avatar, AvatarRow, Bottom, Btn, Chip, Segmented, Sheet, StepBar, Toggle, TopBar } from '../ui/kit'
import { useLocationLine } from './Trip'

// ---------------- M-04 홈 · 내 모임 ----------------

export function HomeScreen() {
  const p = useApp((s) => s.profile)
  const trip = useApp((s) => s.trip)
  const history = useApp((s) => s.history)
  const messages = useApp((s) => s.messages)
  const all = useMembers()
  const me = all[all.length - 1]
  const step = stepOf(trip.step)
  const status = trip.step === 'lobby' ? '출발 전 · 대기실' : step.moving ? '주행 중' : trip.step === 'regroup1' ? '집결지에서 쉬는 중' : '도착'
  const nextLine =
    trip.step === 'lobby'
      ? '07:30 잠실 출발 · 첫 집결지 덕평자연휴게소'
      : step.next
        ? `다음 집결지 ${step.next.name} · ${trip.munmak ? '09:00' : step.next.eta} 도착`
        : trip.step === 'regroup1'
          ? '덕평자연휴게소에서 쉬는 중 · 08:35 출발'
          : '강릉 안목해변 도착'
  return (
    <div className="screen">
      <div className="body" style={{ paddingTop: 12 }}>
        <div className="spread">
          <span className="brand">
            <span className="logo-mark">
              <Icon name="route" size={17} stroke={2.2} />
            </span>
            같이 달려
          </span>
          <button className="avatar-btn" aria-label="설정" onClick={() => go('/settings')}>
            <Avatar m={me} size={32} />
          </button>
        </div>
        <h1 className="h1 mt24">
          {p.name}님, 오늘도
          <br />
          함께 달려요
        </h1>
        {trip.active && (
          <div className="trip-hero mt20">
            <span className="live">
              <span className="dot-live" />
              진행 중 · {status}
            </span>
            <h2 className="h2 mt8">{trip.groupName}</h2>
            <p className="hero-line mt4">{nextLine}</p>
            <div className="spread mt16">
              <AvatarRow size={26} />
              <button className="hero-btn" onClick={() => go(trip.step === 'lobby' ? '/lobby' : '/trip')}>
                {trip.step === 'lobby' ? '대기실' : '모임 지도'} <Icon name="chevron" size={16} />
              </button>
            </div>
          </div>
        )}
        <button className="list-row lined mt20" onClick={() => go('/messages')}>
          <span className="row-icon">
            <Icon name="edit" size={20} />
          </span>
          <span className="grow">
            <b className="name">내 메시지</b>
            <p className="tiny">차량 화면에서 한 번에 보내는 문장 · {messages.length}/6</p>
          </span>
          <Icon name="chevron" size={18} className="chev" />
        </button>
        <h2 className="section-title">예정된 모임</h2>
        <div className="list lined-top">
          {UPCOMING.map((u) => (
            <button className="list-row" key={u.name} onClick={() => toast('예정된 모임은 시연 화면이에요')}>
              <span className="date-col">
                {u.month}
                <small>{u.weekday}</small>
              </span>
              <span className="grow">
                <b className="name">{u.name}</b>
                <p className="tiny">{u.detail}</p>
              </span>
              <Icon name="chevron" size={18} className="chev" />
            </button>
          ))}
        </div>
        <h2 className="section-title">지난 모임</h2>
        <div className="list lined-top">
          {history.map((h, i) => (
            <div className="list-row" key={`h${i}`}>
              <span className="date-col">
                {h.date}
                <small>일</small>
              </span>
              <div className="grow">
                <b className="name">{h.name}</b>
                <p className="tiny">
                  함께 달린 {h.km}km · 같이 쉰 {h.rests}번 · {h.cars}대
                </p>
              </div>
            </div>
          ))}
          {PAST.map((h) => (
            <div className="list-row" key={h.name}>
              <span className="date-col">
                {h.month}
                <small>{h.weekday}</small>
              </span>
              <div className="grow">
                <b className="name">{h.name}</b>
                <p className="tiny">{h.detail}</p>
              </div>
            </div>
          ))}
          <div className="list-row muted-row">
            <span className="date-col">
              <Icon name="library" size={20} />
            </span>
            <div className="grow">
              <b className="name">코스 라이브러리</b>
              <p className="tiny">다녀온 코스 저장 · 계절 추천 코스</p>
            </div>
            <span className="badge-later">출시 후</span>
          </div>
        </div>
      </div>
      <Bottom>
        <Btn icon="plus" onClick={() => go('/create/1')}>
          모임 만들기
        </Btn>
      </Bottom>
    </div>
  )
}

// ---------------- M-05 모임 만들기 ----------------

interface DraftStop {
  id: string
  name: string
  rest: number
}

let draftName = GROUP.name
let draftDate = '2026-11-15'
let draftTime = '07:30'
let draftStops: DraftStop[] = [
  { id: 'deokpyeong', name: '덕평자연휴게소', rest: 20 },
  { id: 'hoengseong', name: '횡성휴게소', rest: 15 },
]

function CreateFrame({ index, label, children }: { index: number; label: string; children: React.ReactNode }) {
  return (
    <div className="screen">
      <TopBar title="모임 만들기" onBack={() => back(index > 1 ? `/create/${index - 1}` : '/home')} />
      <StepBar index={index} total={3} label={label} />
      {children}
    </div>
  )
}

export function CreateInfo() {
  const [name, setName] = useState(draftName)
  const [date, setDate] = useState(draftDate)
  const [time, setTime] = useState(draftTime)
  const ok = name.trim().length >= 2 && name.trim().length <= 20
  const weekday = (() => {
    const d = new Date(`${date}T00:00:00`)
    return isNaN(d.getTime()) ? '' : `${d.getMonth() + 1}월 ${d.getDate()}일 (${'일월화수목금토'[d.getDay()]})`
  })()
  return (
    <CreateFrame index={1} label="모임 정보">
      <div className="body">
        <h1 className="h1 mt8">어떤 모임인가요?</h1>
        <label className="label mt24" htmlFor="gname">
          모임 이름
        </label>
        <input id="gname" className="input" value={name} maxLength={20} onChange={(e) => setName(e.target.value)} />
        <p className="input-help">20자까지 · 초대받은 사람이 가장 먼저 봐요</p>
        <div className="row mt20" style={{ gap: 8, alignItems: 'flex-start' }}>
          <div className="grow">
            <label className="label" htmlFor="gdate">
              날짜
            </label>
            <input id="gdate" type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} />
            <p className="input-help">{weekday}</p>
          </div>
          <div style={{ width: 132 }}>
            <label className="label" htmlFor="gtime">
              출발 시각
            </label>
            <input id="gtime" type="time" className="input" value={time} onChange={(e) => setTime(e.target.value)} />
          </div>
        </div>
        <section className="group mt32">
          <h2 className="h3">지난 모임을 복사해서 만들기</h2>
          <p className="tiny mt4">코스·집결지·휴식 시간을 그대로 가져와요</p>
          <div className="list mt8">
            {COPY_SOURCES.map((c) => (
              <div className="list-row" key={c.name}>
                <div className="grow">
                  <b className="name">{c.name}</b>
                  <p className="tiny">{c.detail}</p>
                </div>
                <Btn kind="secondary" small icon="copy" onClick={() => toast(`‘${c.name}’ 코스를 가져왔어요 (시연)`)}>
                  복사
                </Btn>
              </div>
            ))}
          </div>
        </section>
      </div>
      <Bottom>
        <Btn
          disabled={!ok}
          onClick={() => {
            draftName = name.trim()
            draftDate = date
            draftTime = time
            go('/create/2')
          }}
        >
          다음 · 코스
        </Btn>
      </Bottom>
    </CreateFrame>
  )
}

export function CreateCourse() {
  const [stops, setStops] = useState<DraftStop[]>(draftStops)
  const update = (list: DraftStop[]) => {
    draftStops = list
    setStops(list)
  }
  const restCount = stops.length
  // 쉬는 시간을 뺀 운전 시간 2시간 35분 + 집결지 휴식
  const totalMin = 155 + stops.reduce((sum, x) => sum + x.rest, 0)
  const totalText = `약 ${Math.floor(totalMin / 60)}시간${totalMin % 60 ? ` ${totalMin % 60}분` : ''}`
  return (
    <CreateFrame index={2} label="코스">
      <div className="body">
        <h1 className="h1 mt8">코스를 만들어 주세요</h1>
        <p className="lead">출발지와 도착지만 있어도 돼요. 쉴 곳은 지금 넣거나, 여행 중에 ‘쉬어가기 투표’로 정해도 돼요.</p>
        <div className="mt24 lined">
          <div className="course-edit">
            <div className="ce-row">
              <span className="ce-dot start">
                <Icon name="pin" size={16} />
              </span>
              <div className="grow">
                <span className="tiny">출발</span>
                <b className="name">{COURSE[0].name}</b>
              </div>
            </div>
            {stops.length === 0 && (
              <div className="ce-row">
                <span className="ce-dot">
                  <Icon name="route" size={16} />
                </span>
                <p className="tiny grow">집결지 없이 출발지에서 도착지로 바로 가요</p>
              </div>
            )}
            {stops.map((s, i) => (
              <div className="ce-row" key={s.id}>
                <span className="ce-dot">
                  <Icon name="grip" size={16} />
                </span>
                <div className="grow">
                  <span className="tiny">집결 {i + 1}</span>
                  <b className="name">{s.name}</b>
                </div>
                <span className="stepper">
                  <button aria-label="휴식 줄이기" onClick={() => update(stops.map((x) => (x.id === s.id ? { ...x, rest: Math.max(5, x.rest - 5) } : x)))}>
                    <Icon name="minus" size={16} />
                  </button>
                  <span>휴식 {s.rest}분</span>
                  <button aria-label="휴식 늘리기" onClick={() => update(stops.map((x) => (x.id === s.id ? { ...x, rest: Math.min(60, x.rest + 5) } : x)))}>
                    <Icon name="plus" size={16} />
                  </button>
                </span>
                <button className="icon-btn small" aria-label={`${s.name} 빼기`} onClick={() => update(stops.filter((x) => x.id !== s.id))}>
                  <Icon name="close" size={18} />
                </button>
              </div>
            ))}
            <div className="ce-row">
              <span className="ce-dot end">
                <Icon name="flag" size={15} />
              </span>
              <div className="grow">
                <span className="tiny">도착</span>
                <b className="name">{COURSE[3].name}</b>
              </div>
            </div>
          </div>
        </div>
        <button className="add-stop mt12" onClick={() => toast('휴게소·졸음쉼터 검색은 지도 API 연결 후 열려요 · 아래 추천에서 골라 주세요')}>
          <Icon name="plus" size={16} /> 휴게소 · 졸음쉼터 추가
        </button>
        <div className="section-head mt32">
          <h2 className="h3">경로 위 추천 쉴 곳</h2>
          <span className="tiny">공공데이터</span>
        </div>
        <div className="list lined-top mt8">
          {SUGGESTED_STOPS.map((s) => {
            const added = stops.some((x) => x.id === s.id)
            return (
              <div className="list-row" key={s.id}>
                <div className="grow">
                  <b className="name">{s.name}</b>
                  <p className="meta">
                    <span className="num">
                      {s.km}km · {s.eta} 도착
                    </span>
                    {s.tags.map((t) => (
                      <span className="mi" key={t}>
                        <span className="sep">·</span>
                        <Icon name={t === '충전' ? 'bolt' : t === '식당' ? 'food' : 'fuel'} size={13} />
                        {t}
                      </span>
                    ))}
                  </p>
                </div>
                <button
                  className={`plus-btn${added ? ' on' : ''}`}
                  aria-label={added ? `${s.name} 추가됨` : `${s.name} 추가`}
                  onClick={() => {
                    if (added) return
                    const list = [...stops, { id: s.id, name: s.name, rest: 15 }]
                    const order = ['deokpyeong', 'yeoju', 'munmak', 'hoengseong']
                    update(list.sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id)))
                  }}
                >
                  <Icon name={added ? 'check' : 'plus'} size={16} stroke={2.2} />
                </button>
              </div>
            )
          })}
        </div>
        <p className="summary-line mt24">
          총 235km · {totalText} · {restCount > 0 ? `휴식 ${restCount}번` : '휴식 없이'}
        </p>
      </div>
      <Bottom>
        <Btn onClick={() => go('/create/3')}>다음 · 초대하기</Btn>
      </Bottom>
    </CreateFrame>
  )
}

export function CreateInvite() {
  const all = useMembers()
  const link = `${location.origin}${location.pathname}#/join/${GROUP.code}`
  return (
    <CreateFrame index={3} label="초대">
      <div className="body">
        <h1 className="h1 mt8">모임원을 초대해 주세요</h1>
        <section className="mt24">
          <p className="label">모임 코드</p>
          <div className="code-boxes" style={{ pointerEvents: 'none' }}>
            {GROUP.code.split('').map((d, i) => (
              <span key={i}>{d}</span>
            ))}
          </div>
          <div className="row mt12">
            <Btn
              kind="secondary"
              small
              icon="copy"
              onClick={async () => toast((await copyText(GROUP.code)) ? '모임 코드를 복사했어요' : '복사하지 못했어요')}
            >
              코드 복사
            </Btn>
            <span className="tiny">모임이 끝나면 링크와 코드가 만료돼요</span>
          </div>
        </section>
        <div className="lined mt24">
          <button
            className="invite-row"
            onClick={async () => {
              const r = await shareLink(draftName, `${draftName}에 초대해요 · 설치 없이 링크로 참여해요`, link)
              if (r === 'copied') toast('공유 시트가 없어 링크를 복사했어요')
            }}
          >
            <span className="row-icon accent">
              <Icon name="share" size={20} />
            </span>
            <span className="grow">
              <b className="name">링크 보내기</b>
              <p className="tiny">카카오톡·문자 등 공유 시트로 보내요</p>
            </span>
            <Icon name="chevron" size={18} className="chev" />
          </button>
          <button className="invite-row" onClick={async () => toast((await copyText(link)) ? '초대 링크를 복사했어요' : '복사하지 못했어요')}>
            <span className="row-icon">
              <Icon name="link" size={20} />
            </span>
            <span className="grow">
              <b className="name">링크 복사</b>
              <p className="tiny">…/join/{GROUP.code}</p>
            </span>
            <Icon name="chevron" size={18} className="chev" />
          </button>
          <button className="invite-row" onClick={() => toast('Pleos 차량은 차 화면에서 모임 코드로 연결해요')}>
            <span className="row-icon">
              <Icon name="car" size={20} />
            </span>
            <span className="grow">
              <b className="name">차량으로 보내기</b>
              <p className="tiny">Pleos 차량은 차 화면에서 코드로 연결해요 (C-00)</p>
            </span>
            <Icon name="chevron" size={18} className="chev" />
          </button>
        </div>
        <div className="row mt24">
          <AvatarRow ids={['junho', 'minji', 'sua']} size={30} />
          <div className="grow">
            <b className="name">벌써 3대가 참여했어요</b>
            <p className="tiny">{all.slice(0, 3).map((m) => m.name).join('·')}</p>
          </div>
          <Chip tone="accent" icon="users">
            3/6
          </Chip>
        </div>
      </div>
      <Bottom note="링크를 받은 사람은 설치 없이 바로 참여해요">
        <Btn
          onClick={() => {
            setProfile({ joined: true })
            finishJoin(draftName)
            go('/lobby', true)
          }}
        >
          대기실로 가기
        </Btn>
      </Bottom>
    </CreateFrame>
  )
}

// ---------------- M-12 설정 · 위치 공유 관리 ----------------

export function SettingsScreen() {
  const s = useApp((st) => st.settings)
  const p = useApp((st) => st.profile)
  const trip = useApp((st) => st.trip)
  const loc = useLocationLine()
  const [leaving, setLeaving] = useState(false)
  const moving = trip.active && stepOf(trip.step).moving
  const sharingOn = p.location === 'share' && s.sharing
  return (
    <div className="screen">
      <TopBar title="설정" onBack={() => back('/home')} />
      <div className="body">
        <section className="mt8">
          <div className="row">
            <div className="grow">
              <h1 className="h2">{sharingOn ? '모임 위치 공유 중' : '위치 공유 꺼짐'}</h1>
              <p className="tiny">참여자에게만 · 끝나면 자동 삭제</p>
            </div>
            <Toggle
              label="모임 위치 공유"
              checked={sharingOn}
              onChange={(v) => {
                if (v && p.location === 'viewOnly') {
                  go('/setup/2')
                  return
                }
                setSettings({ sharing: v })
                toast(v ? '위치 공유를 다시 켰어요' : '위치 공유를 껐어요 · 다른 사람 지도에 ‘위치 꺼짐’으로 보여요')
              }}
            />
          </div>
          <p className="loc-line mt12">
            <span className={`dot-live${loc.tone === 'on' ? '' : ` ${loc.tone}`}`} />
            {loc.text} · 출처 휴대폰
          </p>
        </section>
        <div className="lined mt24">
          <div className="setting-row">
            <span className="row-icon">
              <Icon name="eyeOff" size={20} />
            </span>
            <div className="grow">
              <b className="name">익명으로 알리기</b>
              <p className="tiny">이름 대신 ‘한 분이’로 알려요</p>
            </div>
            <Toggle label="익명으로 알리기" checked={s.anonymous} onChange={(v) => setSettings({ anonymous: v })} />
          </div>
          <div className="setting-row">
            <span className="row-icon">
              <Icon name="volume" size={20} />
            </span>
            <div className="grow">
              <b className="name">소리 내어 읽기</b>
              <p className="tiny">상황 공유·투표·집결지 변경</p>
            </div>
            <Toggle label="소리 내어 읽기" checked={s.readAloud} onChange={(v) => setSettings({ readAloud: v })} />
          </div>
          <div className="setting-row">
            <span className="row-icon">
              <Icon name="bell" size={20} />
            </span>
            <div className="grow">
              <b className="name">진동</b>
              <p className="tiny">새 소식이 오면 짧게 진동</p>
            </div>
            <Toggle label="진동" checked={s.vibrate} onChange={(v) => setSettings({ vibrate: v })} />
          </div>
          <div className="setting-row">
            <span className="row-icon">
              <Icon name="sun" size={20} />
            </span>
            <div className="grow">
              <b className="name">화면 켜 두기</b>
              <p className="tiny">{hasWakeLock() ? '운전자 모드에서 화면이 꺼지지 않게 해요' : '이 브라우저는 화면 켜 두기를 지원하지 않아요'}</p>
            </div>
            <Toggle label="화면 켜 두기" checked={s.keepAwake} onChange={(v) => setSettings({ keepAwake: v })} disabled={!hasWakeLock()} />
          </div>
        </div>
        <section className="mt32">
          <div className="section-head">
            <h2 className="h3">운전자 · 동승자</h2>
            <span className="loc-line">
              <Icon name="lock" size={12} /> 정차 중에만 바꿀 수 있어요
            </span>
          </div>
          <div className="mt12">
            <Segmented
              disabled={moving}
              value={p.role}
              onChange={(role) => setProfile({ role })}
              options={[
                { value: 'driver', label: '운전해요' },
                { value: 'passenger', label: '같이 타고 가요' },
              ]}
            />
          </div>
          {moving && <p className="tiny mt8">지금은 주행 중이라 바꿀 수 없어요 · 시연은 화면 왼쪽 ‘시연’ 탭에서 바꿀 수 있어요</p>}
        </section>
        <div className="lined mt32">
          <div className="setting-row">
            <span className="row-icon">
              <Icon name="pin" size={20} />
            </span>
            <div className="grow">
              <b className="name">백그라운드 위치</b>
              <p className="tiny">화면이 꺼져도 공유 · 하이브리드</p>
            </div>
            <span className="badge-later">출시 후</span>
          </div>
          <div className="setting-row">
            <span className="row-icon">
              <Icon name="info" size={20} />
            </span>
            <div className="grow">
              <b className="name">동의 내역 · 처리방침</b>
              <p className="tiny">동의·철회 이력 · 처리방침</p>
            </div>
            <span className="badge-later warn">출시 전 필수</span>
          </div>
        </div>
        <p className="tiny mt16">
          <b>시연판 안내</b> · 동기화 서버가 없어 위치와 메시지는 이 기기 밖으로 나가지 않아요. 다른 차의 움직임과 응답은 시나리오 A 시연 데이터예요. 말로 답하기는 브라우저의 음성 인식(Chrome 은 Google 서버)을 써요.
        </p>
        <div className="mt24">
          <Btn
            kind="secondary"
            icon="eyeOff"
            disabled={!sharingOn}
            onClick={() => {
              setSettings({ sharing: false })
              toast('위치 공유를 껐어요')
            }}
          >
            위치 공유 끄기
          </Btn>
          {trip.active && (
            <div className="row center-row mt8">
              <button className="link-btn danger" onClick={() => setLeaving(true)}>
                <Icon name="logout" size={15} /> 모임 나가기
              </button>
            </div>
          )}
        </div>
      </div>
      <Sheet open={leaving} onClose={() => setLeaving(false)} label="모임 나가기">
        <h2 className="h2">모임에서 나갈까요?</h2>
        <p className="small mt8">위치 공유를 멈추고 모임 중 위치를 지워요. 초대 링크로 다시 참여할 수 있어요.</p>
        <Btn
          kind="danger"
          className="mt16"
          onClick={() => {
            leaveGroup()
            setLeaving(false)
            toast('모임에서 나왔어요')
            go('/home', true)
          }}
        >
          나가기
        </Btn>
        <Btn kind="ghost" onClick={() => setLeaving(false)}>
          취소
        </Btn>
      </Sheet>
    </div>
  )
}
