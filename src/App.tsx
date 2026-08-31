import { useEffect, useMemo, useRef, useState } from 'react';
import { companionStage, resolveSession, type Expedition, type SessionReward } from './domain/focus';
import { expeditionCopy, mockWorld } from './data/mock';

const minuteOptions = [15, 25, 45, 60];

function formatClock(seconds: number) {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0');
  const s = (seconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

export default function App() {
  const [minutes, setMinutes] = useState(25);
  const [expedition, setExpedition] = useState<Expedition>('tower');
  const [remaining, setRemaining] = useState(minutes * 60);
  const [running, setRunning] = useState(false);
  const [reward, setReward] = useState<SessionReward | null>(null);
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

  function startSession() {
    const seconds = minutes * 60;
    startRemainingRef.current = seconds;
    setRemaining(seconds);
    setReward(null);
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
    return (
      <main className="app-shell timer-shell">
        <section className="timer-view" aria-live="polite">
          <div className="timer-companion" aria-hidden="true">🐲</div>
          <p className="eyebrow">{expeditionCopy[expedition].name}を探索中</p>
          <div className="timer-clock">{formatClock(remaining)}</div>
          <p className="timer-caption">あなたが頑張っているあいだ、モコも冒険しています。</p>
          <div className="focus-link"><span className="focus-dot" /> FOCUS LINK</div>
          <button className="secondary-button" onClick={finishSession}>今日はここまで・帰還</button>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="brand">FOCUS RAID</p>
          <p className="world-now">🌍 {mockWorld.focusNow.toLocaleString()}人が集中中</p>
        </div>
        <button className="icon-button" aria-label="設定">⚙</button>
      </header>

      <section className="hero-card">
        <div className="companion-orb" aria-label={`相棒モコ ${companionStage(totalFocusMinutes)}`}>🐲</div>
        <div className="hero-copy">
          <p className="eyebrow">モコと一緒に</p>
          <h1>{minutes}:00</h1>
          <p className="goal-line">🎯 資格試験遠征 <strong>{defeated}体討伐</strong></p>
        </div>

        <div className="minute-picker" aria-label="集中時間">
          {minuteOptions.map((option) => (
            <button key={option} className={minutes === option ? 'time-chip selected' : 'time-chip'} onClick={() => setMinutes(option)}>{option}</button>
          ))}
        </div>

        <button className="primary-button" onClick={startSession}>▶ 集中を始める</button>

        <div className="expedition-picker" aria-label="探索先">
          {(Object.keys(expeditionCopy) as Expedition[]).map((key) => {
            const item = expeditionCopy[key];
            return (
              <button key={key} className={expedition === key ? 'expedition selected' : 'expedition'} onClick={() => setExpedition(key)}>
                <span className="expedition-icon">{item.icon}</span>
                <span><strong>{item.name}</strong><small>{item.hint}</small></span>
              </button>
            );
          })}
        </div>
      </section>

      {reward && (
        <section className="result-card" aria-live="polite">
          <div>
            <p className="eyebrow">EXPLORATION COMPLETE</p>
            <h2>{reward.creditedMinutes}分、帰還しました。</h2>
          </div>
          <div className="result-grid">
            <span>🎯 {reward.defeated ? `個人ボス ${reward.defeated}体撃破` : '個人ボスへダメージ蓄積'}</span>
            <span>{expeditionCopy[expedition].icon} WORLD +{reward.worldEp} EP</span>
            <span>🎁 {reward.discovery ? `${reward.rarity} ${reward.discovery}` : '次の発見まで探索継続'}</span>
            <span>⚔️ ARMORY +{reward.armoryPoints}</span>
          </div>
          <button className="compact-button" onClick={startSession}>もう一本いく</button>
        </section>
      )}

      <section className="raid-card">
        <div className="raid-badge">🐉 NEXT WORLD RAID</div>
        <div className="raid-main">
          <div>
            <h2>{mockWorld.nextRaid.boss}</h2>
            <p>{mockWorld.nextRaid.time} RAID START · 👥 {mockWorld.nextRaid.participants.toLocaleString()}人参加予定</p>
          </div>
          <div className="raid-time">あと数時間</div>
        </div>
        <div className="progress"><span style={{ width: `${armoryReady}%` }} /></div>
        <p className="progress-copy">⚔️ WORLD ARMORY {armoryReady}% READY</p>
      </section>

      <section className="world-strip">
        <div><span>🗼</span><strong>{mockWorld.towerFloor.toLocaleString()}F</strong><small>天空塔 {mockWorld.towerProgress}%</small></div>
        <div><span>🕳️</span><strong>{mockWorld.abyssDepth.toLocaleString()}m</strong><small>深層迷宮 {mockWorld.abyssProgress}%</small></div>
        <div><span>🐲</span><strong>{Math.floor(totalFocusMinutes / 60)}h {totalFocusMinutes % 60}m</strong><small>モコと集中</small></div>
      </section>

      <nav className="bottom-nav" aria-label="メインナビゲーション">
        <button className="active">⏱<span>FOCUS</span></button>
        <button>🌍<span>WORLD</span></button>
        <button>📖<span>LOG</span></button>
      </nav>

      <p className="prototype-note">MVP prototype · elapsed {elapsedMinutes} min</p>
    </main>
  );
}
