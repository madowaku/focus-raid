import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import { companionStage, resolveSession, type Expedition, type SessionReward } from './domain/focus';
import { expeditionCopy, mockWorld } from './data/mock';
import { PixelAbyss, PixelBoss, PixelRag, PixelTower } from './ui/pixel';

type MainTab = 'focus' | 'world' | 'log';

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

  function startSession() {
    const seconds = minutes * 60;
    startRemainingRef.current = seconds;
    setRemaining(seconds);
    setReward(null);
    setActiveTab('focus');
    setRunning(true);
  }

  function finishSession() {
    const credited = Math.max(0, Math.floor((startRemainingRef.current - remaining) / 60));
    const result = resolveSession(credited, expedition, totalFocusMinutes % 25);
    setRunning(false);
    setReward(result);
    setTotalFocusMinutes((v) => v + result.creditedMinutes);
    setDefeated((v) => v + result.defeated);
    if (result.armoryPoints) setArmoryReady((v) => Math.min(100, +(v + result.armoryPoints * 0.02).toFixed(2)));
  }

  if (running) {
    const ringStyle = { '--timer-progress': `${focusProgress * 360}deg` } as CSSProperties;
    return (
      <main className="app-shell timer-shell">
        <section className="focus-header">
          <button className="quiet-action" onClick={finishSession}>× 帰還する</button>
          <span className="pixel-label">FOCUS LINK ●</span>
        </section>

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

        <section className="timer-center">
          <div className="timer-ring" style={ringStyle}>
            <div className="timer-ring-inner">
              <div className="timer-clock">{formatClock(remaining)}</div>
              <p>/ {minutes}:00</p>
            </div>
          </div>
          <p className="timer-caption">現実ではあなたが集中。向こう側ではラグが冒険中。</p>
        </section>

        <section className="focus-task card-surface">
          <div>
            <span className="pixel-label">NOW FOCUSING</span>
            <strong>資格試験遠征</strong>
          </div>
          <div className="boss-hp-row">
            <span>PERSONAL BOSS</span><strong>{monsterHp} / 25 HP</strong>
          </div>
          <div className="hp-bar"><span style={{ width: `${(monsterHp / 25) * 100}%` }} /></div>
        </section>
      </main>
    );
  }

  if (reward) {
    return (
      <main className="app-shell result-shell">
        <header className="screen-heading">
          <span className="pixel-label">RETURN</span>
          <h1>おつかれさま！</h1>
          <p>{reward.creditedMinutes}分、集中できたよ。</p>
        </header>

        <section className="camp-scene card-surface">
          <div className="camp-moon" />
          <div className="camp-fire"><span>✦</span></div>
          <PixelRag className="camp-rag" />
          <p>ラグも帰還しました</p>
        </section>

        <section className="loot-card card-surface">
          <div className="loot-icon">⚔</div>
          <div>
            <span className={`rarity ${reward.rarity?.toLowerCase() ?? 'common'}`}>{reward.discovery ? reward.rarity : 'EXPEDITION'}</span>
            <h2>{reward.discovery ?? '探索は次回へ続く'}</h2>
            <p>{reward.discovery ? 'WORLD ARMORYへ納入しました。' : '集中時間はすべて記録されています。'}</p>
          </div>
        </section>

        <section className="result-stats card-surface">
          <div><span>FOCUS</span><strong>{reward.creditedMinutes}:00</strong></div>
          <div><span>PERSONAL</span><strong>{reward.defeated ? `${reward.defeated}体撃破` : `+${reward.creditedMinutes} DMG`}</strong></div>
          <div><span>WORLD</span><strong>+{reward.worldEp} EP</strong></div>
        </section>

        <section className="armory-result card-surface">
          <div className="section-row"><span>⚔ WORLD ARMORY</span><strong>{armoryReady}%</strong></div>
          <div className="progress teal"><span style={{ width: `${armoryReady}%` }} /></div>
        </section>

        <div className="result-actions">
          <button className="primary-button" onClick={startSession}>▶ もう{minutes}分</button>
          <button className="secondary-button" onClick={() => setReward(null)}>HOMEへ戻る</button>
        </div>
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

        <section className="raid-card raid-card-visual">
          <PixelBoss className="raid-boss-art" />
          <div className="raid-copy"><span className="pixel-label raid">NEXT WORLD RAID</span><h2>{mockWorld.nextRaid.boss}</h2><p>{mockWorld.nextRaid.time} · {mockWorld.nextRaid.participants.toLocaleString()}人参加予定</p></div>
          <span className="raid-arrow">›</span>
        </section>
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
        <button className="primary-button" onClick={startSession}>▶ 集中をはじめる</button>
        <div className="expedition-picker" aria-label="探索先">
          {(Object.keys(expeditionCopy) as Expedition[]).map((key) => {
            const item = expeditionCopy[key];
            return <button key={key} className={expedition === key ? 'expedition selected' : 'expedition'} onClick={() => setExpedition(key)}>{key === 'tower' ? <PixelTower /> : <PixelAbyss />}<span><strong>{item.name}</strong><small>{item.hint}</small></span><span className="selection-mark">{expedition === key ? '●' : '○'}</span></button>;
          })}
        </div>
      </section>

      <section className="raid-card raid-card-visual home-raid">
        <PixelBoss className="raid-boss-art" />
        <div className="raid-copy"><span className="pixel-label raid">NEXT WORLD RAID</span><h2>{mockWorld.nextRaid.boss}</h2><p>{mockWorld.nextRaid.time} · {mockWorld.nextRaid.participants.toLocaleString()}人参加予定</p></div>
        <span className="raid-arrow">›</span>
      </section>

      <section className="world-strip">
        <div><PixelTower /><strong>{mockWorld.towerFloor.toLocaleString()}F</strong><small>天空塔 {mockWorld.towerProgress}%</small></div>
        <div><PixelAbyss /><strong>{mockWorld.abyssDepth.toLocaleString()}m</strong><small>深層迷宮 {mockWorld.abyssProgress}%</small></div>
        <div><span className="armory-mini">⚔</span><strong>{armoryReady}%</strong><small>ARMORY READY</small></div>
      </section>

      <BottomNav active={activeTab} onChange={setActiveTab} />
    </main>
  );
}
