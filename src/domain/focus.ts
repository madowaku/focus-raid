export type Expedition = 'tower' | 'abyss';
export type Rarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';

export interface SessionReward {
  creditedMinutes: number;
  personalDamage: number;
  worldEp: number;
  defeated: number;
  rarity: Rarity | null;
  discovery: string | null;
  armoryPoints: number;
}

const drops: Record<Expedition, Record<Rarity, string[]>> = {
  tower: {
    COMMON: ['鉄の剣', '木の弓', '鉄鉱石', '旅人の盾'],
    RARE: ['白銀の槍', '氷晶の弓', '騎士の盾'],
    EPIC: ['雷撃砲の部品', '蒼天の大槍'],
    LEGENDARY: ['星喰らいの大剣'],
  },
  abyss: {
    COMMON: ['魔力石', '古い地図片', '薬草', '青晶石'],
    RARE: ['古代の鍵', '耐火の護符', '月影の水晶'],
    EPIC: ['共鳴結晶', '弱点解析器'],
    LEGENDARY: ['深淵の羅針盤'],
  },
};

export function rarityFromRoll(roll: number): Rarity {
  if (roll < 0.005) return 'LEGENDARY';
  if (roll < 0.04) return 'EPIC';
  if (roll < 0.23) return 'RARE';
  return 'COMMON';
}

export function resolveSession(
  creditedMinutes: number,
  expedition: Expedition,
  discoveryProgressMinutes: number,
  roll = Math.random(),
): SessionReward {
  const minutes = Math.max(0, Math.floor(creditedMinutes));
  const discoveries = Math.floor((discoveryProgressMinutes + minutes) / 25);
  const rarity = discoveries > 0 ? rarityFromRoll(roll) : null;
  const pool = rarity ? drops[expedition][rarity] : [];
  const discovery = pool.length ? pool[Math.floor(roll * 1000) % pool.length] : null;
  const armoryPoints = rarity ? { COMMON: 1, RARE: 2, EPIC: 5, LEGENDARY: 10 }[rarity] : 0;

  return {
    creditedMinutes: minutes,
    personalDamage: minutes,
    worldEp: minutes,
    defeated: Math.floor(minutes / 25),
    rarity,
    discovery,
    armoryPoints,
  };
}

export function companionStage(totalMinutes: number): string {
  if (totalMinutes < 75) return '🥚 卵';
  if (totalMinutes < 720) return '🐣 幼体';
  if (totalMinutes < 1800) return '🐲 第一成長';
  if (totalMinutes < 4500) return '🐲 第二成長';
  return '🐉 成熟';
}
