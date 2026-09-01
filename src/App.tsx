import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import { companionStage, resolveSession, type Expedition, type SessionReward } from './domain/focus';
import { expeditionCopy, mockWorld } from './data/mock';
import { PixelAbyss, PixelBoss, PixelRag, PixelTower } from './ui/pixel';

type MainTab = 'focus' | 'world' | 'log';
type SessionMode = 'expedition' | 'raid';

const minuteOptions = [15, 25, 45, 60];

function formatClock(seconds: number) {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0');
  const s = (seconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

function BottomNav({ active, onChange }: { active: MainTab; onChange: (tab: MainTab) => void }) {
  return (
    <nav className="bottom-nav" aria-label="メインナビゲーション">
      <button className={active === 'focus' ? 'active' : ''} onClick={() => onChange('focus')}>⏱<span>FOCUS</span></button>
      <button className={active === 'world' ? 'active' : ''} onClick={() => onChange('world')}>🌍<span>WORLD</span></button>
      <button className={active === 'log' ? 'active' : ''} onClick={() => onChange('log')}>📖<span>LOG</span></button>
    </nav>
  );
}

export default function App() {
  const [minutes, setMinutes] = useState(25);
  const [sessionMinutes, setSessionMinutes] = useState(25);
  const [sessionMode, setSessionMode] = useState<SessionMode>('expedition');
  const [lastSessionMode, setLastSessionMode] = useState<SessionMode>('expedition');
  const [raidReady, setRaidReady] = useState(false);
  const [expedition, setExpedition] = useState<Expedition>('tower');
  const [remaining, setRemaining] = useState(minutes * 60);
  const [running, setRunning] = useState(false);
  const [reward, setReward] = useState<SessionReward | null>(null);
  const [activeTab, setActiveTab] = useState<MainTab>('focus');
  const [totalFocusMinutes, setTotalFocusMinutes] = useState(645);
  const [defeated, setDefeated] = useState(38);
  const [armoryReady, setArmoryReady] = useState(mockWorld.armoryReady);
  const startRemainingRef = useRef(minutes * 60);

  useEffect(() => {
    if (!running) setRemaining(minutes * 60);
  }, [minutes, running]);

  useEffect(() => {
    if (!running) return;
    const timer = window.setInterval(() => {
      setRemaining((value) => {
        if (value <= 1) {
          window.clearInterval(timer);
          return 0;
        }
        return value - 1;
      });
    }, 1000);
    return () => window.clearInterval(timer);
  }, [running]);

  useEffect(() => {
    if (running && remaining === 0) finishSession();
    // finishSession intentionally reads current state only when countdown reaches zero.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [remaining, running]);

  const elapsedMinutes = useMemo(
    () => Math.floor((startRemainingRef.current - remaining) / 60),
    [remaining],
  );

  const focusProgress = running
    ? Math.min(1, Math.max(0, 1 - remaining / startRemainingRef.current))
    : 0;
  const monsterHp = Math.max(0, 25 - (elapsedMinutes % 25));
  const currentExpedition = expeditionCopy[expedition];

  function startSession(mode: SessionMode = 'expedition') {
    const durationMinutes = mode === 'raid' ? 25 : minutes;
    const seconds = durationMinutes * 60;
    setSessionMode(mode);
    setSessionMinutes(durationMinutes);
    startRemainingRef.current = seconds;
    setRemaining(seconds);
    setReward(null);
    setRaidReady(false);
    setActiveTab('focus');
    setRunning(true);
  }

  function finishSession() {
    const credited = Math.max(0, Math.floor((startRemainingRef.current - remaining) / 60));
    const result = sessionMode === 'raid'
      ? {
          creditedMinutes: credited,
          personalDamage: credited,
          worldEp: 0,
          defeated: Math.floor(credited / 25),
          rarity: null,
          discovery: null,
          armoryPoints: 0,
        } satisfies SessionReward
      : resolveSession(credited, expedition, totalFocusMinutes % 25);
    setRunning(false);
    setReward(result);
    setLastSessionMode(sessionMode);
    setTotalFocusMinutes((v) => v + result.creditedMinutes);
    setDefeated((v) => v + result.defeated);
    if (result.armoryPoints) setArmoryReady((v) => Math.min(100, +(v + result.armoryPoints * 0.02).toFixed(2)));
  }

  if (running) {
    const ringStyle = { '--timer-progress': `${focusProgress * 360}deg` } as CSSProperties;
    const raidSession = sessionMode === 'raid';
    return (
      <main className="app-shell timer-shell">
        <section className="focus-header">
          <button className="quiet-action" onClick={finishSession}>× 帰還する</button>
          <span className={`pixel-label ${raidSession ? 'raid' : ''}`}>{raidSession ? 'WORLD RAID LINK ●' : 'FOCUS LINK ●'}</span>
        </section>

        {raidSession ? (
          <section className="focus-journey raid-focus-journey card-surface">
            <p className="eyebrow">WORLD RAID · {mockWorld.nextRaid.boss}</p>
            <div className="raid-focus-scene">
              <PixelBoss className="raid-focus-boss" />
              <PixelRag pose="raid" className="raid-rag raid-focus-rag" />
              <span className="raid-ground-glow" />
            </div>
            <div className="raid-focus-meta">
              <span>{mockWorld.nextRaid.participants.toLocaleString()}人と集中中</span>
              <strong>ARMORY {armoryReady}%</strong>
            </div>
          </section>
        ) : (
          <section className="focus-journey card-surface">
            <p className="eyebrow">{currentExpedition.name}を探索中</p>
            <div className={`pixel-scene ${expedition}`}>
              {expedition === 'tower' ? <PixelTower className="scene-landmark" /> : <PixelAbyss className="scene-landmark" />}
              <PixelRag className="scene-companion" />
              <span className="scene-spark spark-one" />
              <span className="scene-spark spark-two" />
            </div>
            <div className="scene-progress"><span style={{ width: `${Math.min(100, (expedition === 'tower' ? mockWorld.towerProgress : mockWorld.abyssProgress) + focusProgress * 2)}%` }} /></div>
          </section>
        )}

        <section className="timer-center">
          <div className={`timer-ring ${raidSession ? 'raid-timer-ring' : ''}`} style={ringStyle}>
            <div className="timer-ring-inner">
              <div className="timer-clock">{formatClock(remaining)}</div>
              <p>/ {sessionMinutes}:00</p>
            </div>
          </div>
          <p className="timer-caption">{raidSession ? '現実ではあなたが集中。向こう側ではラグがみんなとボスに挑戦中。' : '現実ではあなたが集中。向こう側ではラグが冒険中。'}</p>
        </section>

        <section className="focus-task card-surface">
          <div>
            <span className="pixel-label">NOW FOCUSING</span>
            <strong>{raidSession ? mockWorld.nextRaid.boss : '資格試験遠征'}</strong>
          </div>
          {raidSession ? (
            <>
              <div className="boss-hp-row"><span>YOUR RAID DAMAGE</span><strong>+{elapsedMinutes} DMG</strong></div>
              <div className="hp-bar raid-damage-bar"><span style={{ width: `${Math.min(100, (elapsedMinutes / 25) * 100)}%` }} /></div>
            </>
          ) : (
            <>
              <div className="boss-hp-row"><span>PERSONAL BOSS</span><strong>{monsterHp} / 25 HP</strong></div>
              <div className="hp-bar"><span style={{ width: `${(monsterHp / 25) * 100}%` }} /></div>
            </>
          )}
        </section>
      </main>
    );
  }

  if (reward) {
    const raidResult = lastSessionMode === 'raid';
    return (
      <main className="app-shell result-shell">
        <header className="screen-heading">
          <span className={`pixel-label ${raidResult ? 'raid' : ''}`}>{raidResult ? 'WORLD RAID RETURN' : 'RETURN'}</span>
          <h1>{raidResult ? 'レイドから帰還！' : 'おつかれさま！'}</h1>
          <p>{reward.creditedMinutes}分、集中できたよ。</p>
        </header>

        <section className={`camp-scene card-surface ${raidResult ? 'raid-return-scene' : ''}`}>
          <div className="camp-moon" />
          <div className="camp-fire"><span>✦</span></div>
          <PixelRag className="camp-rag" />
          <p>{raidResult ? 'ラグもレイドから帰還しました' : 'ラグも帰還しました'}</p>
        </section>

        {raidResult ? (
          <section className="raid-result-card card-surface">
            <PixelBoss className="raid-result-boss" />
            <div><span className="rarity epic">WORLD RAID</span><h2>+{reward.creditedMinutes} DAMAGE</h2><p>{mockWorld.nextRaid.boss}への攻撃として集計されます。</p></div>
          </section>
        ) : (
          <section className="loot-card card-surface">
            <div className="loot-icon">⚔</div>
            <div>
              <span className={`rarity ${reward.rarity?.toLowerCase() ?? 'common'}`}>{reward.discovery ? reward.rarity : 'EXPEDITION'}</span>
              <h2>{reward.discovery ?? '探索は次回へ続く'}</h2>
              <p>{reward.discovery ? 'WORLD ARMORYへ納入しました。' : '集中時間はすべて記録されています。'}</p>
            </div>
          </section>
        )}

        <section className="result-stats card-surface">
          <div><span>FOCUS</span><strong>{reward.creditedMinutes}:00</strong></div>
          <div><span>PERSONAL</span><strong>{reward.defeated ? `${reward.defeated}体撃破` : `+${reward.creditedMinutes} DMG`}</strong></div>
          <div><span>{raidResult ? 'WORLD RAID' : 'WORLD'}</span><strong>{raidResult ? `+${reward.creditedMinutes} DMG` : `+${reward.worldEp} EP`}</strong></div>
        </section>

        <section className="armory-result card-surface">
          <div className="section-row"><span>⚔ WORLD ARMORY</span><strong>{armoryReady}%</strong></div>
          <div className="progress teal"><span style={{ width: `${armoryReady}%` }} /></div>
        </section>

        <div className="result-actions">
          {raidResult ? (
            <button className="primary-button" onClick={() => setReward(null)}>HOMEへ戻る</button>
          ) : (
            <button className="primary-button" onClick={() => startSession('expedition')}>▶ もう{minutes}分</button>
          )}
          {!raidResult && <button className="secondary-button" onClick={() => setReward(null)}>HOMEへ戻る</button>}
        </div>
      </main>
    );
  }

  if (raidReady) {
    return (
      <main className="app-shell raid-ready-shell">
        <header className="focus-header raid-ready-header">
          <button className="quiet-action" onClick={() => setRaidReady(false)}>‹ 戻る</button>
          <span className="pixel-label raid">WORLD RAID READY</span>
        </header>

        <section className="raid-ready-hero card-surface">
          <div className="raid-ready-copy">
            <span className="pixel-label raid">NEXT WORLD RAID</span>
            <h1>{mockWorld.nextRaid.boss}</h1>
            <p>{mockWorld.nextRaid.time} 開始 · 25分集中</p>
          </div>
          <div className="raid-ready-stage">
            <PixelBoss className="raid-ready-boss" />
            <PixelRag pose="raid" className="raid-rag raid-ready-rag" />
            <span className="raid-ready-flare flare-one" />
            <span className="raid-ready-flare flare-two" />
          </div>
          <div className="raid-ready-numbers">
            <div><span>参加予定</span><strong>{mockWorld.nextRaid.participants.toLocaleString()}人</strong></div>
            <div><span>ARMORY</span><strong>{armoryReady}%</strong></div>
          </div>
        </section>

        <section className="raid-ready-message card-surface">
          <p>あなたが25分集中しているあいだ、ラグも世界のみんなと戦います。</p>
          <strong>1分 = 1 RAID DAMAGE</strong>
        </section>

        <button className="primary-button raid-start-button" onClick={() => startSession('raid')}>⚔ 25分レイド集中をはじめる</button>
        <button className="secondary-button raid-normal-button" onClick={() => { setRaidReady(false); setActiveTab('focus'); }}>今は通常集中する</button>
      </main>
    );
  }

  if (activeTab === 'world') {
    return (
      <main className="app-shell world-shell">
        <header className="topbar">
          <div><p className="brand">WORLD</p><p className="world-now">世界は、みんなの集中で進み続ける。</p></div>
          <button className="icon-button" aria-label="世界情報">ⓘ</button>
        </header>

        <section className="world-map card-surface">
          <div className="world-zone sky-zone">
            <div><span className="pixel-label">WORLD TOWER</span><h2>{mockWorld.towerFloor.toLocaleString()}F</h2><p>天空塔 · {mockWorld.towerProgress}%</p></div>
            <PixelTower className="world-landmark tower-art" />
            <div className="zone-progress"><span style={{ width: `${mockWorld.towerProgress}%` }} /></div>
          </div>

          <div className="world-town">
            <span className="town-lantern">◆</span><span>WORLD TOWN</span><span className="town-lantern">◆</span>
          </div>

          <div className="world-zone abyss-zone">
            <PixelAbyss className="world-landmark abyss-art" />
            <div><span className="pixel-label">WORLD ABYSS</span><h2>{mockWorld.abyssDepth.toLocaleString()}m</h2><p>深層迷宮 · {mockWorld.abyssProgress}%</p></div>
            <div className="zone-progress purple"><span style={{ width: `${mockWorld.abyssProgress}%` }} /></div>
          </div>
        </section>

        <section className="armory-card card-surface">
          <div className="section-row"><div><span className="pixel-label">WORLD ARMORY</span><h2>{armoryReady}% READY</h2></div><span className="armory-shield">⚔</span></div>
          <div className="progress teal"><span style={{ width: `${armoryReady}%` }} /></div>
        </section>

        <button className="raid-card raid-card-visual raid-card-button" onClick={() => setRaidReady(true)}>
          <PixelBoss className="raid-boss-art" />
          <span className="raid-copy"><span className="pixel-label raid">NEXT WORLD RAID</span><strong>{mockWorld.nextRaid.boss}</strong><small>{mockWorld.nextRaid.time} · {mockWorld.nextRaid.participants.toLocaleString()}人参加予定</small></span>
          <span className="raid-arrow">›</span>
        </button>
        <BottomNav active={activeTab} onChange={setActiveTab} />
      </main>
    );
  }

  if (activeTab === 'log') {
    return (
      <main className="app-shell log-shell">
        <header className="topbar"><div><p className="brand">ADVENTURE LOG</p><p className="world-now">ラグと積み重ねた時間。</p></div></header>
        <section className="log-hero card-surface"><PixelRag className="log-rag" /><div><span className="pixel-label">TOGETHER</span><h1>{Math.floor(totalFocusMinutes / 60)}h {totalFocusMinutes % 60}m</h1><p>{companionStage(totalFocusMinutes)} · PERSONAL {defeated}体討伐</p></div></section>
        <section className="log-grid"><div className="card-surface"><span>TOWER</span><strong>{mockWorld.towerFloor}F</strong></div><div className="card-surface"><span>ABYSS</span><strong>{mockWorld.abyssDepth}m</strong></div><div className="card-surface"><span>ARMORY</span><strong>{armoryReady}%</strong></div></section>
        <BottomNav active={activeTab} onChange={setActiveTab} />
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div><p className="brand">FOCUS RAID</p><p className="world-now"><span className="live-dot" /> {mockWorld.focusNow.toLocaleString()}人が集中中</p></div>
        <button className="icon-button" aria-label="設定">⚙</button>
      </header>

      <section className="home-hero card-surface">
        <div className="companion-stage">
          <div className="companion-halo" /><PixelRag className="home-rag" />
          <div className="companion-copy"><span>YOUR COMPANION</span><strong>ラグ</strong><small>{companionStage(totalFocusMinutes)}</small></div>
        </div>
        <div className="home-focus-copy">
          <p className="eyebrow">次の遠征</p>
          <h1>{minutes}:00</h1>
          <p className="goal-line">資格試験遠征 · <strong>{defeated}体討伐</strong></p>
        </div>
        <div className="minute-picker" aria-label="集中時間">
          {minuteOptions.map((option) => <button key={option} className={minutes === option ? 'time-chip selected' : 'time-chip'} onClick={() => setMinutes(option)}>{option}</button>)}
        </div>
        <button className="primary-button" onClick={() => startSession('expedition')}>▶ 集中をはじめる</button>
        <div className="expedition-picker" aria-label="探索先">
          {(Object.keys(expeditionCopy) as Expedition[]).map((key) => {
            const item = expeditionCopy[key];
            return <button key={key} className={expedition === key ? 'expedition selected' : 'expedition'} onClick={() => setExpedition(key)}>{key === 'tower' ? <PixelTower /> : <PixelAbyss />}<span><strong>{item.name}</strong><small>{item.hint}</small></span><span className="selection-mark">{expedition === key ? '●' : '○'}</span></button>;
          })}
        </div>
      </section>

      <button className="raid-card raid-card-visual home-raid raid-card-button" onClick={() => setRaidReady(true)}>
        <PixelBoss className="raid-boss-art" />
        <span className="raid-copy"><span className="pixel-label raid">NEXT WORLD RAID</span><strong>{mockWorld.nextRaid.boss}</strong><small>{mockWorld.nextRaid.time} · {mockWorld.nextRaid.participants.toLocaleString()}人参加予定</small></span>
        <span className="raid-arrow">›</span>
      </button>

      <section className="world-strip">
        <div><PixelTower /><strong>{mockWorld.towerFloor.toLocaleString()}F</strong><small>天空塔 {mockWorld.towerProgress}%</small></div>
        <div><PixelAbyss /><strong>{mockWorld.abyssDepth.toLocaleString()}m</strong><small>深層迷宮 {mockWorld.abyssProgress}%</small></div>
        <div><span className="armory-mini">⚔</span><strong>{armoryReady}%</strong><small>ARMORY READY</small></div>
      </section>

      <BottomNav active={activeTab} onChange={setActiveTab} />
    </main>
  );
}
