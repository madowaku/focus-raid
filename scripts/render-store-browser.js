async (page) => {
  await page.goto('http://127.0.0.1:8731/store/google-play/v1.0/creative.html?export');
  await page.waitForFunction(() => window.storeReady || window.storeError);
  const error = await page.evaluate(() => window.storeError);
  if (error) throw new Error(error);
  const ids = await page.locator('canvas[data-testid]').evaluateAll(nodes => nodes.map(node => node.dataset.testid));
  const reviewIds = ['launcher-preview', 'sequence-180', 'hero-ab-360'];
  for (const id of ids) {
    // Canvas ids prefixed "en-US/" export into the English listing folder.
    const [directory, name] = reviewIds.includes(id) ? ['review', id]
      : id.startsWith('en-US/') ? ['en-US', id.slice('en-US/'.length)] : ['ja-JP', id];
    await page.getByTestId(id).screenshot({ path: `store/google-play/v1.0/${directory}/${name}.png` });
  }
  return await page.evaluate(() => window.storeEvidence);
}
