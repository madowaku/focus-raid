import { chromium } from '@playwright/test';
import { mkdir } from 'node:fs/promises';

const baseUrl = process.env.FOCUS_RAID_BASE_URL ?? 'http://127.0.0.1:4173';
const outDir = 'artifacts/visual';
await mkdir(outDir, { recursive: true });

const viewports = [
  { name: '360x800', width: 360, height: 800 },
  { name: '720x1280', width: 720, height: 1280 },
];

const browser = await chromium.launch({ headless: true });

for (const viewport of viewports) {
  const page = await browser.newPage({
    viewport: { width: viewport.width, height: viewport.height },
    deviceScaleFactor: 1,
  });

  // Keep visual evidence deterministic: a standard focus completion always yields a RARE drop.
  await page.addInitScript(() => {
    Math.random = () => 0.1;
  });
  await page.clock.install({ time: new Date('2026-09-01T21:00:00+09:00') });

  await page.goto(baseUrl, { waitUntil: 'networkidle' });
  await page.screenshot({ path: `${outDir}/${viewport.name}-home.png` });

  await page.getByRole('button', { name: /集中をはじめる/ }).click();
  await page.screenshot({ path: `${outDir}/${viewport.name}-focus.png` });

  // Run a real 25-minute session through the app's own interval logic without waiting in wall-clock time.
  await page.clock.runFor(25 * 60 * 1000);
  await page.locator('.result-shell').waitFor({ state: 'visible' });
  await page.screenshot({ path: `${outDir}/${viewport.name}-result.png` });

  await page.getByRole('button', { name: /HOMEへ戻る/ }).click();
  const navButtons = page.locator('.bottom-nav button');
  await navButtons.nth(1).click();
  await page.screenshot({ path: `${outDir}/${viewport.name}-world.png` });

  await page.close();
}

await browser.close();
