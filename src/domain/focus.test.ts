import { describe, expect, it } from 'vitest';
import { companionStage, rarityFromRoll, resolveSession } from './focus';

describe('Focus Raid domain rules', () => {
  it('maps rarity thresholds to the agreed distribution', () => {
    expect(rarityFromRoll(0.001)).toBe('LEGENDARY');
    expect(rarityFromRoll(0.01)).toBe('EPIC');
    expect(rarityFromRoll(0.1)).toBe('RARE');
    expect(rarityFromRoll(0.5)).toBe('COMMON');
  });

  it('turns credited minutes into personal damage and world EP', () => {
    const result = resolveSession(25, 'tower', 0, 0.5);
    expect(result.personalDamage).toBe(25);
    expect(result.worldEp).toBe(25);
    expect(result.defeated).toBe(1);
    expect(result.discovery).toBeTruthy();
  });

  it('keeps short sessions meaningful without inventing a discovery', () => {
    const result = resolveSession(10, 'abyss', 0, 0.5);
    expect(result.personalDamage).toBe(10);
    expect(result.defeated).toBe(0);
    expect(result.discovery).toBeNull();
  });

  it('uses cumulative focus time for companion growth', () => {
    expect(companionStage(74)).toContain('卵');
    expect(companionStage(75)).toContain('幼体');
    expect(companionStage(720)).toContain('第一成長');
  });
});
