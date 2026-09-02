import type { Expedition } from '../domain/focus';

export const mockWorld = {
  focusNow: 4218,
  towerFloor: 4281,
  towerProgress: 82,
  abyssDepth: 12481,
  abyssProgress: 51,
  armoryReady: 68,
  nextRaid: {
    boss: '灰燼竜ヴォルガ',
    time: '21:00',
    participants: 12481,
    hp: 428192,
  },
};

export const expeditionCopy: Record<Expedition, { icon: string; name: string; hint: string }> = {
  tower: { icon: '🗼', name: '天空塔', hint: '武器・防具を探す' },
  abyss: { icon: '🕳️', name: '深層迷宮', hint: '魔道具・未知を探す' },
};
