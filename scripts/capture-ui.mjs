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

  await page.goto(baseUrl, { waitUntil: 'networkidle' });
  await page.screenshot({ path: `${outDir}/${viewport.name}-home.png` });

  await page.getByRole('button', { name: /集中をはじめる/ }).click();
  await page.waitForTimeout(350);
  await page.screenshot({ path: `${outDir}/${viewport.name}-focus.png` });

  await page.getByRole('button', { name: /帰還する/ }).click();
  await page.waitForTimeout(200);
  await page.screenshot({ path: `${outDir}/${viewport.name}-result.png` });

  await page.getByRole('button', { name: /HOMEへ戻る/ }).click();
  const navButtons = page.locator('.bottom-nav button');
  await navButtons.nth(1).click();
  await page.waitForTimeout(200);
  await page.screenshot({ path: `${outDir}/${viewport.name}-world.png` });

  await page.close();
}

await browser.close();
