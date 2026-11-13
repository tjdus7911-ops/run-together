import { useEffect, useMemo, useState } from 'react'
import { SIGNAL_LABEL, type StatusSignal } from '../domain/korean'
import { COURSE, GROUP, ME, MUNMAK, stepOf, type Member, type NextStop } from '../domain/scenario'
import { majority, yesCount } from '../domain/vote'
import { euroRo } from '../domain/korean'
import { NAVI_LABEL, canListen, openNavi, type NaviApp } from '../platform/browser'
import { go } from '../router'
import {
  answerVote,
  closeSheet,
  finishTrip,
  listenForSignal,
  listenForVote,
  openSheet,
  sendMyMessage,
  sendSignal,
  setProfile,
  setSettings,
  setTrip,
  toast,
  useApp,
  useMembers,
  type AppState,
} from '../store'
import { Icon, MESSAGE_ICON, SIGNAL_ICON } from '../ui/Icon'
import { Avatar, AvatarRow, Bottom, Btn, Card, Chip, Progress, Segmented, Sheet, Toggle } from '../ui/kit'
import { MockMap, type MapMarker } from '../ui/MockMap'

function useNow(everyMs: number) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), everyMs)
    return () => clearInterval(id)
  }, [everyMs])
  return now
}

/** 위치 상태 한 줄: “위치 공유 중 · 3초 전 · 정확도 12m” */
export function useLocationLine(): { text: string; tone: 'on' | 'off' | 'warn' } {
  const geo = useApp((s) => s.geo)
  const where = useApp((s) => s.profile.location)
  const sharing = useApp((s) => s.settings.sharing)
  const now = useNow(1_000)
  if (where === 'viewOnly') return { text: '위치 꺼짐 · 보기만 참여 중', tone: 'off' }
  if (!sharing) return { text: '위치 공유를 껐어요 · 다른 사람 지도에 ‘위치 꺼짐’으로 보여요', tone: 'off' }
  if (geo.status === 'denied') return { text: '위치 권한이 꺼져 있어요 · 설정에서 다시 켤 수 있어요', tone: 'warn' }
  if (geo.status === 'on' && geo.fix) {
    const sec = Math.max(1, Math.round((now - geo.fix.at) / 1000))
    return { text: `위치 공유 중 · ${sec}초 전 · 정확도 ${geo.fix.accuracy}m`, tone: sec > 60 ? 'warn' : 'on' }
  }
  return { text: '위치 공유 중 · 시연 위치 · 정확도 12m', tone: 'on' }
}

function addMinutes(clock: string, minutes: number) {
  const [h, m] = clock.split(':').map(Number)
  const t = h * 60 + m + minutes
  return `${String(Math.floor(t / 60) % 24).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`
}

const START_KM: Record<string, number> = { drive1: 21, drive2: 72, alert: 54, changed: MUNMAK.km }

/** 남은 거리 → 지금 시각 (시연 시계) */
function liveClock(s: AppState['trip']) {
  const step = stepOf(s.step)
  const start = START_KM[s.step]
  if (!start || !s.kmLeft) return step.clock
  return addMinutes(step.clock, Math.round((start - s.kmLeft) * 0.76))
}

function nextOf(trip: AppState['trip']): NextStop | undefined {
  const n = stepOf(trip.step).next
  if (!n) return undefined
  const minutes = Math.max(1, Math.round(trip.kmLeft * (n.minutes / (START_KM[trip.step] || n.km))))
  return { ...n, km: trip.kmLeft || n.km, minutes }
}

export function TripScreen() {
  const trip = useApp((s) => s.trip)
  const role = useApp((s) => s.profile.role)
  useEffect(() => {
    if (!trip.active) go('/home', true)
    else if (trip.step === 'lobby') go('/lobby', true)
  }, [trip.active, trip.step])
  if (!trip.active || trip.step === 'lobby') return null
  if (trip.step === 'regroup1') return <ArriveView />
  if (trip.step === 'ended') return <EndView />
  if (role === 'driver' && !trip.unlocked) return <DriverNav />
  return <PassengerMap />
}

// ---------------- M-06 운전자 모드 · 모임 내비 (움직이는 동안 잠금) ----------------
// 거치한 휴대폰에 지도(지리안내 영상)를 보여 주는 것은 괜찮지만, 운전 중 조작은 안 된다 (도로교통법 제49조).
// 그래서 누르지 않아도 되는 화면: 다음 집결지 · 남은 거리 · 모임 차 위치와 간격을 크게, 소식은 소리로, 답은 말로.

function DriverNav() {
  const trip = useApp((s) => s.trip)
  const keepAwake = useApp((s) => s.settings.keepAwake)
  const all = useMembers()
  const loc = useLocationLine()
  const { markers, gap, gapText } = useMarkers()
  const next = nextOf(trip)
  const vote = trip.vote
  const pending = vote && !trip.voteResult
  const proposer = vote ? all.find((m) => m.id === vote.proposerId) : undefined
  const startKm = START_KM[trip.step] ?? 21
  const myVote = vote?.votes[ME]
  const order = ['me', 'jiwon', 'sua', 'minji', 'junho']
  return (
    <div className="mapscreen">
      <div className="drive-map">
        <MockMap markers={markers} gapBetween={gap} gapText={gapText} />
      </div>
      <div className="drive-card">
        <div className="spread">
          <span className="lock-state">
            <Icon name="lock" size={14} /> 움직이는 동안 잠겨 있어요
          </span>
          <b className="clock">{liveClock(trip)}</b>
        </div>
        {next && (
          <>
            <div className="mt12">
              {trip.munmak ? (
                <Chip tone="accent" icon="flag">
                  바뀐 집결지
                </Chip>
              ) : (
                <p className="kicker">
                  <Icon name="flag" size={14} /> 다음 집결지
                </p>
              )}
            </div>
            <div className="spread mt4" style={{ alignItems: 'flex-end' }}>
              <div className="grow">
                <h1 className="h2">{next.name}</h1>
                <p className="small">
                  약 {next.minutes}분 · {trip.munmak ? '09:00' : next.eta} 도착
                </p>
              </div>
              <p className="t-num">
                {next.km}
                <small>km</small>
              </p>
            </div>
            <div className="mt12">
              <Progress value={startKm - next.km + 2} max={startKm + 2} />
            </div>
            <p className="drive-line mt12">따라오지 않아도 돼요 · {next.short}에서 만나요</p>
          </>
        )}
      </div>
      <span className="map-demo dev-mark" style={{ top: 'calc(232px + env(safe-area-inset-top))', bottom: 'auto' }}>
        시연용 지도
      </span>

      <div className="drive-bottom">
        {pending && vote ? (
          <>
            <p className="row" style={{ gap: 8 }}>
              <Chip tone="night" icon="moon">
                {SIGNAL_LABEL.SLEEPY}
              </Chip>
              <span className="tiny">{proposer?.id === ME ? '내가 제안 · 방금' : `${proposer?.name}님 차에서 · 방금`}</span>
            </p>
            <h2 className="h2 mt8">
              {proposer?.id === ME ? '쉬어가기를 제안했어요. ' : `${proposer?.name}님이 졸리대요. `}
              {vote.stopName}에서 {vote.restMinutes}분 쉬었다 갈까요?
            </h2>
            {myVote ? (
              <p className="status-line accent mt12">
                <Icon name="checkCircle" size={18} /> ‘{myVote === 'YES' ? '좋아요' : '괜찮아요'}’로 답했어요 · 결과는 소리로 알려 드려요
              </p>
            ) : (
              <button className="listen mt12" onClick={() => (canListen() ? listenForVote() : setTrip((t) => ({ ...t, unlocked: true, sheet: 'vote' })))}>
                <span className={`listen-mic${trip.listening === 'vote' ? '' : ' idle'}`}>
                  <Icon name="mic" size={22} />
                </span>
                <span className="grow">
                  <b>{trip.listening === 'vote' ? '듣고 있어요 — 말로 답해 주세요' : canListen() ? '말로 답하기' : '이 브라우저는 음성 인식이 없어요'}</b>
                  <p className="tiny mt4">“좋아요” 또는 “괜찮아요” · 안 되면 동승자가 대신 답해요</p>
                </span>
              </button>
            )}
            <div className="tally-row mt16">
              <b className="tally">
                {yesCount(vote)}/{vote.memberIds.length} 좋아요
              </b>
              <span className="tiny">과반 {majority(vote)}표면 확정 · 1분 뒤 마감</span>
            </div>
            <div className="mt8">
              <Progress value={yesCount(vote)} max={vote.memberIds.length} />
            </div>
            <button className="link-btn mt8" onClick={() => setTrip((t) => ({ ...t, unlocked: true, sheet: 'vote' }))}>
              동승자가 대신 답하기
            </button>
          </>
        ) : (
          <>
            <div className="spread">
              <p className="gap-title">
                <b>모임 간격</b>
                <b>{stepOf(trip.step).gapKm ?? 3.4}km</b>
              </p>
              <span className="tiny">5대 같은 길 · 1대 다른 길</span>
            </div>
            <div className="gapbar">
              <span className="gapbar-line" />
              {order.map((id, i) => {
                const m = all.find((x) => x.id === id)!
                return (
                  <span className="gapbar-item" key={id} style={{ left: `${[4, 26, 46, 70, 96][i]}%` }}>
                    <Avatar m={m} size={28} dashed={id === 'minji'} />
                    <span>{id === ME ? '나' : id === 'junho' ? `${m.name} · 선두` : id === 'minji' ? `${m.name} · 다른 길` : m.name}</span>
                  </span>
                )
              })}
            </div>
            <button className="listen compact mt8" onClick={listenForSignal}>
              <span className={`listen-mic${trip.listening === 'signal' ? '' : ' idle'}`}>
                <Icon name={trip.listening === 'signal' ? 'mic' : 'volume'} size={20} />
              </span>
              <span className="grow">
                <b>{trip.listening === 'signal' ? '듣고 있어요…' : '새 소식은 소리로 읽어 드려요'}</b>
                <p className="tiny">위급할 땐 말로 “도움이 필요해요”</p>
              </span>
            </button>
          </>
        )}
        <p className="loc-line mt12">
          <span className={`dot-live${loc.tone === 'on' ? '' : ` ${loc.tone}`}`} />
          {loc.text}
        </p>
        <div className="spread">
          <span className="tiny">화면 켜 두기 {keepAwake ? '켜짐' : '꺼짐'} · 길 안내 음성은 쓰던 내비로</span>
          {!pending && (
            <button className="link-btn" onClick={() => setTrip((t) => ({ ...t, unlocked: true }))}>
              동승자가 대신 쓰기
            </button>
          )}
        </div>
      </div>
      {/* 운전자 모드에서는 누르는 시트를 띄우지 않는다 (투표는 말로, 집결지 변경은 위 카드와 소리로) */}
    </div>
  )
}

// ---------------- M-07 동승자 모드 · 모임 지도 ----------------

function useMarkers(): { markers: MapMarker[]; gap: [string, string]; gapText: string } {
  const trip = useApp((s) => s.trip)
  const online = useApp((s) => s.online)
  const all = useMembers()
  return useMemo(() => {
    const start = START_KM[trip.step] ?? 21
    const p = trip.kmLeft ? ((start - trip.kmLeft) / start) * 0.06 : 0
    const stale = !online || trip.demoOffline
    const by = (id: string) => all.find((m) => m.id === id) as Member
    const markers: MapMarker[] = [
      { member: by('junho'), f: 0.8 + p, tag: '선두' },
      { member: by('taehyun'), f: 0.725 + p },
      { member: by('sua'), f: 0.7 + p },
      { member: by('minji'), f: 0.665 + p, offRoute: true, tag: '다른 길', tagTone: 'warn' },
      { member: by('jiwon'), f: 0.635 + (stale ? 0 : p), stale, tag: stale ? '2분 전' : undefined, tagTone: 'warn' },
      { member: by(ME), f: 0.6 + p, me: true },
    ]
    return { markers, gap: ['sua', 'jiwon'], gapText: '1.5km 벌어짐' }
  }, [trip.step, trip.kmLeft, trip.demoOffline, online, all])
}

function PassengerMap() {
  const trip = useApp((s) => s.trip)
  const online = useApp((s) => s.online)
  const p = useApp((s) => s.profile)
  const all = useMembers()
  const { markers, gap, gapText } = useMarkers()
  const next = nextOf(trip)
  const offline = !online || trip.demoOffline
  const pendingVote = trip.vote && !trip.voteResult
  const order = ['me', 'jiwon', 'sua', 'minji', 'junho']
  const loc = useLocationLine()
  return (
    <div className="mapscreen">
      <MockMap markers={markers} gapBetween={gap} gapText={gapText} />
      <div className="map-head">
        <div className="grow">
          <b className="name">{trip.groupName}</b>
          <p className="loc-line">
            <span className={`dot-live${loc.tone === 'on' ? '' : ` ${loc.tone}`}`} />
            {p.location === 'viewOnly' ? '보기만 참여 중 · 내 위치는 보내지 않아요' : '모임 위치 공유 중 · 끝나면 자동 삭제'}
          </p>
        </div>
        {p.role === 'driver' && trip.unlocked && (
          <Btn kind="secondary" small icon="lock" onClick={() => setTrip((t) => ({ ...t, unlocked: false, sheet: null }))}>
            잠금
          </Btn>
        )}
        <button className="icon-btn" aria-label="설정" onClick={() => go('/settings')}>
          <Icon name="gear" size={22} />
        </button>
      </div>
      {offline ? (
        <div className="offline-banner" style={{ top: 'calc(80px + env(safe-area-inset-top))' }}>
          <Icon name="wifiOff" size={18} />
          <div className="grow">
            연결이 끊겼어요 · 다시 연결하는 중…
            <p>붙으면 최신 상태를 한 번에 받아요</p>
          </div>
          <Icon name="refresh" size={18} />
        </div>
      ) : (
        next && (
          <span className="map-chip" style={{ top: 'calc(80px + env(safe-area-inset-top))' }}>
            <Icon name="flag" size={14} /> {next.name} · {next.km}km
          </span>
        )
      )}
      {pendingVote && trip.sheet !== 'vote' && (
        <button className="vote-chip" style={{ top: 'calc(124px + env(safe-area-inset-top))' }} onClick={() => openSheet('vote')}>
          <Icon name="moon" size={16} /> 쉬어가기 투표 {yesCount(trip.vote!)}/{trip.vote!.memberIds.length} · 1분 뒤 마감
          <Icon name="chevron" size={16} />
        </button>
      )}
      <span className="map-demo dev-mark" style={{ bottom: offline ? '47%' : '43%' }}>
        시연용 지도
      </span>

      <div className="map-sheet">
        <span className="sheet-handle" />
        {offline ? (
          <OfflineList />
        ) : (
          <>
            <div className="spread" style={{ alignItems: 'flex-end' }}>
              <div className="grow">
                <p className="tiny">
                  <b className="strong-text">다음 집결지</b> · {p.role === 'driver' ? '동승자가 대신 쓰는 중' : `동승자 · ${p.name} 차`}
                </p>
                <h2 className="h2 mt4">{next?.name}</h2>
                <p className="small num">
                  {next?.km}km · 약 {next?.minutes}분 · {trip.munmak ? '09:00' : next?.eta} 도착
                </p>
              </div>
              <Btn kind="secondary" small icon="navigate" onClick={() => openSheet('navi')}>
                내비로 열기
              </Btn>
            </div>
            <section className="group mt16" style={{ paddingTop: 16 }}>
              <div className="spread">
                <p className="gap-title">
                  <b>모임 간격</b>
                  <b>{stepOf(trip.step).gapKm ?? 3.4}km</b>
                </p>
                <span className="tiny">5대 같은 길 · 1대 다른 길</span>
              </div>
              <div className="gapbar">
                <span className="gapbar-line" />
                {order.map((id, i) => {
                  const m = all.find((x) => x.id === id)!
                  const left = [4, 26, 46, 70, 96][i]
                  return (
                    <span className="gapbar-item" key={id} style={{ left: `${left}%` }}>
                      <Avatar m={m} size={28} dashed={id === 'minji'} />
                      <span>{id === ME ? `${m.name} · 나` : id === 'junho' ? `${m.name} · 선두` : m.name}</span>
                    </span>
                  )
                })}
              </div>
            </section>
            <Btn icon="volume" className="mt16" onClick={() => openSheet('status')}>
              상황 공유하기
            </Btn>
            <p className="loc-line center-row mt8">
              <Icon name="phone" size={14} /> {p.name} 차 위치는 휴대폰 GPS — 지도에 휴대폰 배지로 보여요
            </p>
            {trip.feed.length > 0 && (
              <section className="mt24">
                <h3 className="section-title" style={{ marginTop: 0 }}>
                  모임 소식
                </h3>
                <div className="list lined-top">
                  {trip.feed.slice(0, 3).map((f) => {
                    const m = all.find((x) => x.id === f.memberId)
                    return (
                      <div className="list-row tight" key={f.id}>
                        {m && <Avatar m={m} size={28} />}
                        <div className="grow">
                          <span className="tiny">
                            {m?.name} · {f.kind}
                          </span>
                          <p className="name" style={{ fontSize: 14, lineHeight: '20px' }}>
                            {f.text}
                          </p>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </section>
            )}
          </>
        )}
      </div>
      <TripSheets />
    </div>
  )
}

function OfflineList() {
  const all = useMembers()
  const name = useApp((s) => s.profile.name)
  const rows: { id: string; badge?: string; tone?: 'warn' | 'neutral'; line: string }[] = [
    { id: 'jiwon', badge: '오래됨', tone: 'warn', line: '차량 연동 · 마지막 위치 2분 전 · 약 08:19 도착' },
    { id: ME, badge: '대기 1개', tone: 'neutral', line: '휴대폰 GPS · 연결되면 마지막 위치 1개를 보내요' },
    { id: 'sua', line: '차량 연동 · 이동 중 · 1.5km 앞' },
    { id: 'taehyun', badge: '다른 길', tone: 'neutral', line: '차량 연동 · 다른 길로 덕평에서 합류' },
  ]
  return (
    <>
      <div className="section-head">
        <h2 className="h3">참여자 6명</h2>
        <span className="tiny">마지막 상태로 보여요</span>
      </div>
      <div className="list lined-top mt8">
        {rows.map((r) => {
          const m = all.find((x) => x.id === r.id)!
          return (
            <div className="list-row" key={r.id}>
              <Avatar m={m} size={34} phone={r.id === ME} />
              <div className="grow">
                <span className="name">{r.id === ME ? `${name} 차 · 나` : m.name}</span>
                <p className="tiny">{r.line}</p>
              </div>
              {r.badge && <Chip tone={r.tone === 'warn' ? 'warn' : 'neutral'}>{r.badge}</Chip>}
            </div>
          )
        })}
      </div>
      <p className="tiny mt8">‘약’이 붙은 도착 예정은 마지막 속도로 추정한 값이에요</p>
    </>
  )
}

// ---------------- 시트: 상황 공유 · 쉬어가기 투표 · 집결지 변경 · 내비 ----------------

export function TripSheets() {
  const sheet = useApp((s) => s.trip.sheet)
  return (
    <>
      <Sheet open={sheet === 'status'} onClose={closeSheet} label="상황 공유">
        <StatusSheet />
      </Sheet>
      <Sheet open={sheet === 'vote'} onClose={closeSheet} label="쉬어가기 투표">
        <VoteSheet />
      </Sheet>
      <Sheet open={sheet === 'changed'} onClose={closeSheet} label="다음 집결지 변경">
        <NaviChooser changed />
      </Sheet>
      <Sheet open={sheet === 'navi'} onClose={closeSheet} label="내비로 열기">
        <NaviChooser />
      </Sheet>
    </>
  )
}

function StatusSheet() {
  const name = useApp((s) => s.profile.name)
  const anonymous = useApp((s) => s.settings.anonymous)
  const listening = useApp((s) => s.trip.listening)
  const messages = useApp((s) => s.messages)
  const signals: StatusSignal[] = ['SLEEPY', 'REST', 'TOILET', 'ENERGY']
  return (
    <>
      <div className="section-head">
        <h2 className="h2">상황 공유</h2>
        <span className="meta">
          <span className="mi">
            <Icon name="car" size={14} />
            {name} 차 · 나
          </span>
        </span>
      </div>
      <p className="small mt4">한 번 누르면 모두에게 소리로 알려요</p>
      <div className="signal-grid mt16">
        {signals.map((s) => (
          <button key={s} className={`signal-btn${s === 'SLEEPY' ? ' sleepy' : ''}`} onClick={() => sendSignal(s)}>
            <Icon name={SIGNAL_ICON[s]} size={26} />
            {SIGNAL_LABEL[s]}
          </button>
        ))}
      </div>
      <button className="voice-row mt16" onClick={listenForSignal}>
        <span className={`listen-mic${listening === 'signal' ? '' : ' idle'}`}>
          <Icon name="mic" size={20} />
        </span>
        <span className="grow">
          <b>{listening === 'signal' ? '듣고 있어요…' : '말로 하기'}</b>
          <p className="tiny">“천천히 갈게요” · “도움이 필요해요”</p>
        </span>
      </button>
      {!canListen() && (
        <div className="row mt12" style={{ gap: 8 }}>
          <Btn kind="secondary" small icon="gauge" onClick={() => sendSignal('SLOW')}>
            천천히 갈게요
          </Btn>
          <Btn kind="secondary" small icon="sos" onClick={() => sendSignal('HELP')}>
            도움이 필요해요
          </Btn>
        </div>
      )}
      {messages.length > 0 && (
        <>
          <p className="label mt20">내 메시지</p>
          <div className="msg-chips">
            {messages.map((m) => (
              <button key={m.id} onClick={() => sendMyMessage(m.id)}>
                <Icon name={MESSAGE_ICON[m.icon]} size={17} />
                {m.text}
              </button>
            ))}
          </div>
        </>
      )}
      <div className="setting-row solo mt20">
        <div className="grow">
          <b>익명으로 알리기</b>
          <p className="tiny">이름 대신 ‘한 분이’로 알려요 · 도움 요청은 모임장에게 위치 공유</p>
        </div>
        <Toggle label="익명으로 알리기" checked={anonymous} onChange={(v) => setSettings({ anonymous: v })} />
      </div>
    </>
  )
}

function VoteSheet() {
  const trip = useApp((s) => s.trip)
  const name = useApp((s) => s.profile.name)
  const all = useMembers()
  const vote = trip.vote
  const result = trip.voteResult
  // 10초 뒤 접히고 진행 칩으로 남는다
  useEffect(() => {
    if (!vote || result) return
    const id = setTimeout(() => setTrip((t) => (t.sheet === 'vote' && t.vote && !t.voteResult ? { ...t, sheet: null } : t)), 10_000)
    return () => clearTimeout(id)
  }, [vote, result])
  if (!vote) return <p className="small">진행 중인 투표가 없어요</p>
  const proposer = all.find((m) => m.id === vote.proposerId)!
  const mine = vote.votes[ME]
  const statusOf = (id: string): [string, string] => {
    const v = vote.votes[id]
    if (id === vote.proposerId) return [id === ME ? '제안 · 나' : '제안 · 졸려요', 'yes']
    if (v === 'YES') return ['좋아요', 'yes']
    if (v === 'NO') return ['괜찮아요', 'no']
    return [id === ME ? '아직' : '기다리는 중', '']
  }
  return (
    <>
      <div className="spread">
        <p className="kicker">쉬어가기 투표</p>
        <span className="loc-line">
          <Icon name="clock" size={13} /> 1분 뒤 마감
        </span>
      </div>
      <div className="row mt16">
        <Avatar m={proposer} size={40} />
        <div className="grow">
          <Chip tone="night" icon="moon">
            방금 · {SIGNAL_LABEL.SLEEPY}
          </Chip>
          <p className="h3 mt4">{proposer.id === ME ? '쉬어가기를 제안했어요' : `${proposer.name}님이 졸리대요`}</p>
        </div>
      </div>
      <h2 className="h2 mt16">
        {vote.stopName}에서 {vote.restMinutes}분 쉬었다 갈까요?
      </h2>
      <p className="meta mt4">
        <span className="mi">
          <Icon name="coffee" size={14} />
          가장 가까운 쉴 곳 · {MUNMAK.km}km · 약 {MUNMAK.minutes}분 · {MUNMAK.amenities}
        </span>
      </p>
      <div className="tally-row mt20">
        <p className="tally">
          <strong>
            {yesCount(vote)}/{vote.memberIds.length}
          </strong>
          좋아요
        </p>
        <span className="tiny">과반 {majority(vote)}표면 확정</span>
      </div>
      <div className="mt8">
        <Progress value={yesCount(vote)} max={vote.memberIds.length} />
      </div>
      <div className="voters mt20">
        {['minji', 'sua', 'jiwon', 'taehyun', 'junho', ME].map((id) => {
          const m = all.find((x) => x.id === id)!
          const [text, cls] = statusOf(id)
          return (
            <div className="voter" key={id}>
              <Avatar m={m} size={28} dashed={!vote.votes[id]} />
              <span>
                <b>{id === ME ? `${m.name} 차 · 나` : m.name}</b>
                <small className={cls}>{text}</small>
              </span>
            </div>
          )
        })}
      </div>
      {result ? (
        <Card className={`mt20 ${result === 'ACCEPTED' ? 'accent' : ''}`}>
          <p className={`status-line${result === 'ACCEPTED' ? ' accent' : ''}`}>
            <Icon name={result === 'ACCEPTED' ? 'checkCircle' : 'info'} size={18} />
            {result === 'ACCEPTED' ? `과반이 좋다고 했어요 · ${vote.stopName}에서 쉬어요` : '이번에는 쉬지 않고 가요'}
          </p>
        </Card>
      ) : mine ? (
        <p className="small row mt20 lined-top" style={{ gap: 6, paddingTop: 16 }}>
          <Icon name="check" size={16} /> ‘{mine === 'YES' ? '좋아요' : '괜찮아요'}’로 답했어요 · 다른 분들을 기다려요
        </p>
      ) : (
        <div className="row mt20" style={{ gap: 8 }}>
          <Btn onClick={() => answerVote(true)}>좋아요</Btn>
          <Btn kind="secondary" onClick={() => answerVote(false)}>
            괜찮아요
          </Btn>
        </div>
      )}
      <p className="tiny mt12 center">{name} 차를 대신해 답해요 · 10초 뒤 접히고 진행 칩으로 남아요</p>
    </>
  )
}

function NaviChooser({ changed }: { changed?: boolean }) {
  const trip = useApp((s) => s.trip)
  const last = useApp((s) => s.settings.lastNavi)
  const [app, setApp] = useState<NaviApp>(last)
  const next = trip.step === 'regroup1' ? { name: COURSE[2].name, lat: COURSE[2].lat, lng: COURSE[2].lng } : stepOf(trip.step).next
  const vote = trip.vote
  return (
    <>
      {changed && (
        <>
          <h2 className="h2">다음 집결지가 바뀌었어요</h2>
          <p className="small mt4">
            과반({vote ? `${yesCount(vote)}/${vote.memberIds.length}` : '4/6'})이 좋다고 해서 모두의 경로에 넣었어요
          </p>
          <ol className="timeline compact mt20">
            <li>
              <span className="tl-dot on" />
              <div className="tl-body">
                <div className="row" style={{ gap: 8 }}>
                  <b>{MUNMAK.name}</b>
                  <Chip tone="accent">새 집결지</Chip>
                </div>
                <p className="tiny">09:00 도착 · {MUNMAK.rest}분 쉬고 출발</p>
              </div>
            </li>
            <li>
              <span className="tl-dot off" />
              <div className="tl-body off">
                <b>{COURSE[2].name}</b>
                <p className="tiny">09:40 도착 (+5분) · 그다음 집결지</p>
              </div>
            </li>
          </ol>
        </>
      )}
      <p className={`label${changed ? ' mt24' : ''}`}>어떤 내비로 열까요?</p>
      <div className="lined-top">
        {(['kakao', 'naver', 'tmap'] as NaviApp[]).map((a) => (
          <button key={a} className={`pick-row${app === a ? ' on' : ''}`} onClick={() => setApp(a)} aria-pressed={app === a}>
            <Icon name="navigate" size={20} />
            <span className="grow">
              <b>{NAVI_LABEL[a]}</b>
              <p className="tiny">{a === last ? '지난번에 쓴 내비' : a === 'naver' ? '앱이 없으면 웹 지도로 열려요' : a === 'kakao' ? '앱이 없으면 웹 지도로 열려요' : '앱이 있어야 열려요'}</p>
            </span>
            <span className={`radio${app === a ? ' on' : ''}`} />
          </button>
        ))}
      </div>
      <Btn
        icon="navigate"
        className="mt16"
        onClick={() => {
          if (!next) return
          setSettings({ lastNavi: app })
          openNavi(app, next, toast)
          closeSheet()
        }}
      >
        {NAVI_LABEL[app]}
        {euroRo(NAVI_LABEL[app])} 열기
      </Btn>
      <p className="tiny mt8 center">운전자 대신 동승자가 열어 주세요 · 운전자 모드에서는 소리로만 알려요</p>
    </>
  )
}

// ---------------- M-10 집결지 도착 ----------------

function ArriveView() {
  const trip = useApp((s) => s.trip)
  const fuel = useApp((s) => s.profile.fuel)
  const all = useMembers()
  const [left, setLeft] = useState(14)
  useEffect(() => {
    const id = setInterval(() => setLeft((v) => Math.max(1, v - 1)), 60_000)
    return () => clearInterval(id)
  }, [])
  const everyone = trip.arrived.length >= 6
  return (
    <div className="screen">
      <div className="body" style={{ paddingTop: 12 }}>
        <div className="spread">
          <Chip tone="accent" icon="parking">
            정차 중 · 덕평자연휴게소
          </Chip>
          <button className="icon-btn" aria-label="설정" onClick={() => go('/settings')}>
            <Icon name="gear" size={22} />
          </button>
        </div>
        <p className="kicker mt16">집결 1 · 덕평자연휴게소</p>
        <p className="t-num mt4">
          {trip.arrived.length}
          <small>/6</small>
          <span className="t-num-label">도착</span>
        </p>
        <div className="row mt12" style={{ gap: 6 }}>
          {all.map((m) => (
            <Avatar key={m.id} m={m} size={34} dashed={!trip.arrived.includes(m.id)} />
          ))}
        </div>
        <p className="small mt8">{everyone ? '모두 모였어요' : '지원님이 약 3분 뒤 도착해요'}</p>
        <section className="group mt24">
          <div className="row" style={{ gap: 16 }}>
            <span className="ring" style={{ ['--p' as string]: left / 20 }}>
              <span>{left}분</span>
            </span>
            <div>
              <span className="tiny">휴식 남은 시간</span>
              <p className="h3 num">08:35 출발 예정</p>
              <p className="tiny">모임장이 출발 안내를 보내면 소리로 알려 드려요</p>
            </div>
          </div>
        </section>
        <section className="group mt20">
          <p className="name">주유했다면 연료를 다시 골라 주세요</p>
          <div className="mt12">
            <Segmented
              value={fuel}
              onChange={(v) => {
                setProfile({ fuel: v })
                toast('바꾼 연료 상태를 다음 구간 추천에 반영했어요')
              }}
              options={[
                { value: 'plenty', label: '넉넉' },
                { value: 'normal', label: '보통' },
                { value: 'low', label: '부족' },
              ]}
            />
          </div>
          <p className="tiny mt8">바꾸면 다음 구간 추천에 바로 반영돼요 · 출처: 직접 입력</p>
        </section>
        <section className="group mt20">
          <div className="spread">
            <div className="grow">
              <span className="tiny">다음 구간</span>
              <p className="h3">{COURSE[2].name}</p>
              <p className="tiny">85km · 약 1시간 4분 · 09:39 도착</p>
            </div>
            <Btn kind="secondary" small icon="navigate" onClick={() => openSheet('navi')}>
              내비로 열기
            </Btn>
          </div>
        </section>
      </div>
      <Bottom>
        <Btn kind="soft" icon="volume" onClick={() => openSheet('status')}>
          상황 공유하기
        </Btn>
      </Bottom>
      <TripSheets />
    </div>
  )
}

// ---------------- M-11 여행 종료 ----------------

function EndView() {
  const trip = useApp((s) => s.trip)
  const stops = ['잠실', '덕평', '문막', '횡성', '강릉']
  return (
    <div className="screen">
      <div className="body" style={{ paddingTop: 24 }}>
        <p className="kicker">강릉 안목해변 도착 · 10:49</p>
        <h1 className="h1 mt8">
          모두 함께
          <br />
          도착했어요
        </h1>
        <section className="mt32">
          <div className="section-head">
            <h2 className="h3">함께 달린 기록</h2>
            <span className="tiny">{GROUP.date}</span>
          </div>
          <div className="course-dots mt16">
            {stops.map((s, i) => (
              <span key={s} className={`dot${i < stops.length - 1 ? ' done' : ' end'}`}>
                <i />
                <small>{s}</small>
              </span>
            ))}
          </div>
          <div className="stat-grid mt20">
            <div className="stat">
              <b>235km</b>
              <span>함께 달린 거리</span>
            </div>
            <div className="stat">
              <b>3시간 19분</b>
              <span>걸린 시간</span>
            </div>
            <div className="stat">
              <b>3번</b>
              <span>같이 쉰 횟수</span>
            </div>
            <div className="stat">
              <b>6대</b>
              <span>함께한 차</span>
            </div>
          </div>
          <div className="mt16">
            <AvatarRow size={28} />
          </div>
        </section>
        <section className="group mt24">
          <h2 className="h3">위치 공유를 멈췄어요</h2>
          <ul className="marks mt12">
            <li className="mark ok">
              <Icon name="check" size={16} stroke={2.2} />
              <span>모든 차·휴대폰의 위치 공유 중단</span>
            </li>
            <li className="mark ok">
              <Icon name="check" size={16} stroke={2.2} />
              <span>모임 중 위치는 모든 기기에서 삭제</span>
            </li>
            <li className="mark ok">
              <Icon name="check" size={16} stroke={2.2} />
              <span>초대 링크·모임 코드 만료</span>
            </li>
          </ul>
        </section>
        <p className="tiny mt24 row" style={{ gap: 8 }}>
          <span className="badge-later">출시 후</span> 이미지로 저장 · 코스 저장 · 다음 일정 투표
        </p>
      </div>
      <Bottom>
        <Btn
          onClick={() => {
            finishTrip(true)
            toast(`‘${trip.groupName}’ 기록을 남겼어요`)
            go('/home', true)
          }}
        >
          기록 남기기
        </Btn>
        <Btn
          kind="ghost"
          onClick={() => {
            finishTrip(false)
            go('/home', true)
          }}
        >
          남기지 않고 닫기
        </Btn>
      </Bottom>
    </div>
  )
}
