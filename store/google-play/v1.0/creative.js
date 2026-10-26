/* Deterministic store layout. UI pixels come only from production Compose captures.
 * No painted UI, generated characters, invented activity counters or testimonials.
 * Coordinates are measured in the original 1080x2400 captures, never inferred at export.
 */
const ROOT = '../../..';
const PALETTE = { ink: '#120d20', paper: '#f6f0ff', gold: '#ffd36f', violet: '#b99aff', muted: '#cbbfda' };
const outputs = new Map();
const evidence = { font: 'Noto Sans JP', text: [], crops: [], errors: [] };
const pictures = {};
const font = '"StoreNoto", sans-serif';

function text(ctx, value, x, y, size, color = PALETTE.paper, weight = 800, align = 'left') {
  ctx.font = `${weight} ${size}px ${font}`;
  ctx.textAlign = align;
  ctx.textBaseline = 'alphabetic';
  ctx.fillStyle = color;
  const m = ctx.measureText(value);
  const left = align === 'center' ? x - m.width / 2 : align === 'right' ? x - m.width : x;
  const record = { asset: ctx.canvas.dataset.testid, value, size, left, right: left + m.width, top: y - m.actualBoundingBoxAscent, bottom: y + m.actualBoundingBoxDescent };
  evidence.text.push(record);
  if (record.left < 0 || record.right > ctx.canvas.width || record.top < 0 || record.bottom > ctx.canvas.height) evidence.errors.push(record);
  ctx.fillText(value, x, y);
}

function canvas(id, width = 1080, height = 1920, label = id) {
  const figure = document.createElement('figure');
  if (width > height) figure.className = 'wide';
  const el = document.createElement('canvas');
  el.width = width; el.height = height;
  el.dataset.testid = id;
  el.setAttribute('role', 'img');
  el.setAttribute('aria-label', label);
  const caption = document.createElement('figcaption'); caption.textContent = label;
  figure.append(el, caption); document.querySelector('#assets').append(figure);
  outputs.set(id, el);
  return el.getContext('2d');
}

function background(ctx, accent = '#513147') {
  const { width: w, height: h } = ctx.canvas;
  const gradient = ctx.createLinearGradient(0, 0, w * .7, h);
  gradient.addColorStop(0, '#100d20'); gradient.addColorStop(.62, '#21152f'); gradient.addColorStop(1, accent);
  ctx.fillStyle = gradient; ctx.fillRect(0, 0, w, h);
  const glow = ctx.createRadialGradient(w * .78, h * .22, 0, w * .78, h * .22, w * .8);
  glow.addColorStop(0, '#b38bff10'); glow.addColorStop(1, '#b38bff00');
  ctx.fillStyle = glow; ctx.fillRect(0, 0, w, h);
}

function crop(ctx, name, source, destination, radius = 0) {
  const image = pictures[name];
  const [sx, sy, sw, sh] = source;
  const [x, y, w, h] = destination;
  if (!image || sx < 0 || sy < 0 || sx + sw > image.width || sy + sh > image.height) throw new Error(`Invalid crop: ${name}`);
  if (Math.abs(sw / sh - w / h) > .015) throw new Error(`Distorted UI crop: ${name}`);
  evidence.crops.push({ asset: ctx.canvas.dataset.testid, source: `raw/${name}.png`, rect: source, destination });
  ctx.save();
  ctx.beginPath(); ctx.roundRect(x, y, w, h, radius); ctx.clip();
  ctx.drawImage(image, sx, sy, sw, sh, x, y, w, h);
  ctx.restore();
}

function header(ctx, lines, size, accent = PALETTE.gold) {
  text(ctx, 'Focus Raid', 64, 99, 34, PALETTE.paper, 800);
  text(ctx, '集中タイマー', 1016, 96, 28, PALETTE.muted, 500, 'right');
  ctx.fillStyle = accent; ctx.fillRect(64, 140, 72, 5);
  lines.forEach((line, i) => text(ctx, line, 64, 266 + i * (size * 1.26), size, i === lines.length - 1 ? accent : PALETTE.paper, 900));
}

function footer(ctx, number, demo = true) {
  ctx.fillStyle = '#ffffff25'; ctx.fillRect(64, 1816, 952, 1);
  text(ctx, demo ? '実際のアプリ画面 / 演出デモ' : '実際のアプリ画面 / はじまりの相棒', 64, 1873, 30, '#d9cde5', 500);
  text(ctx, number, 1016, 1873, 30, PALETTE.muted, 500, 'right');
}

function heroA() {
  const ctx = canvas('01-hero-a'); background(ctx);
  header(ctx, ['25分が、', '一撃になる。'], 118);
  text(ctx, 'みんなでレイドボスを倒す集中タイマー', 64, 486, 36, PALETTE.muted, 500);
  crop(ctx, 'first-run-strike', [60, 132, 960, 954], [48, 514, 984, 977.85], 48);
  text(ctx, '集中を完走すると、', 64, 1628, 48, PALETTE.paper, 700);
  text(ctx, 'その時間がレイドへ届く。', 64, 1700, 48, PALETTE.gold, 700);
  footer(ctx, '01');
}

function shared(id, number) {
  const ctx = canvas(id); background(ctx, '#283841');
  header(ctx, ['世界中の集中が、', '同じボスへ。'], 93, PALETTE.violet);
  text(ctx, 'みんなでレイドボスを倒す集中タイマー', 64, 454, 36, PALETTE.muted, 500);
  crop(ctx, 'shared-right', [60, 132, 960, 954], [48, 478, 984, 977.85], 48);
  ['shared-left', 'shared-right', 'shared-bottom'].forEach((name, i) => {
    crop(ctx, name, [60, 230, 960, 790], [48 + i * 336, 1476, 312, 256.75], 20);
  });
  text(ctx, '連続する灯の到着', 64, 1780, 30, PALETTE.muted, 500);
  text(ctx, 'あなたの25分も、誰かの25分も。', 1016, 1780, 30, PALETTE.paper, 600, 'right');
  footer(ctx, number);
}

function companion() {
  const ctx = canvas('03-companion'); background(ctx, '#584028');
  header(ctx, ['相棒も、', '一緒に育つ。'], 113);
  text(ctx, '集中した時間が、旅と成長に残る。', 64, 480, 38, PALETTE.muted, 500);
  crop(ctx, 'companion-egg', [60, 132, 960, 954], [48, 506, 984, 977.85], 48);
  // Only locked companions from the actual fresh-user roster. Future forms stay hidden.
  crop(ctx, 'companion-locks', [48, 1200, 984, 440], [48, 1490, 656, 293.33], 28);
  text(ctx, 'まだ見ぬ', 756, 1605, 35, PALETTE.gold, 700);
  text(ctx, '相棒たち。', 756, 1658, 35, PALETTE.gold, 700);
  footer(ctx, '03', false);
}

function reward() {
  const ctx = canvas('04-return-raid'); background(ctx);
  header(ctx, ['最後に、', 'あなたの集中が届く。'], 87);
  text(ctx, 'みんなの灯のあとに、あなたの金色の灯。', 64, 450, 35, PALETTE.muted, 500);
  // Two time slices from the same production Return Raid sequence.
  crop(ctx, 'return-other', [200, 250, 680, 520], [48, 474, 340, 260], 28);
  text(ctx, '誰かの灯から', 434, 585, 39, PALETTE.violet, 700);
  text(ctx, 'あなたの一撃へ。', 434, 645, 39, PALETTE.gold, 700);
  // The Pixel Return Raid frame already carries the after-strike HP bar and damage.
  crop(ctx, 'return-self', [48, 240, 984, 1212], [114, 752, 852, 1049.41], 32);
  footer(ctx, '04');
}

function everyday() {
  const ctx = canvas('05-everyday'); background(ctx, '#293240');
  header(ctx, ['集中する。', '旅が進む。'], 114, PALETTE.violet);
  text(ctx, '25分から。勉強にも、仕事にも。', 64, 480, 38, PALETTE.muted, 500);
  // Large flush crops preserve the real CTA, timer, journey rail and pause action.
  crop(ctx, 'home', [36, 1660, 1008, 190.5], [64, 506, 952, 179.92], 38);
  crop(ctx, 'focusing', [48, 185, 984, 1413], [178, 748, 724, 1039.65], 36);
  footer(ctx, '05');
}

function walking() {
  const ctx = canvas('06-walking'); background(ctx, '#2c4034');
  header(ctx, ['歩いた日も、', '小さな冒険に。'], 108, '#9fe3b3');
  text(ctx, '歩数で、景色と記念品を発見。無料・任意。', 64, 470, 36, PALETTE.muted, 500);
  // Real home card and notebook; steps are a local demo fixture, never sent off the device.
  crop(ctx, 'walking-home', [0, 1680, 1080, 450], [48, 506, 984, 410], 36);
  crop(ctx, 'walking-notebook', [76, 326, 928, 1080], [170, 936, 740, 861.21], 36);
  footer(ctx, '06');
}

function feature() {
  const ctx = canvas('feature-graphic-1024x500', 1024, 500); background(ctx, '#593d30');
  // Native FIRST RUN crop. Text remains outside the captured UI.
  crop(ctx, 'first-run-strike', [245, 240, 590, 560], [565, 60, 410, 389.15], 80);
  text(ctx, 'Focus Raid', 80, 128, 32, PALETTE.paper, 800);
  text(ctx, '25分が、', 80, 224, 66, PALETTE.paper, 900);
  text(ctx, '一撃になる。', 80, 305, 66, PALETTE.gold, 900);
  text(ctx, 'みんなでボスを倒す集中タイマー', 80, 368, 24, PALETTE.muted, 500);
  text(ctx, '実画面 / 演出デモ', 80, 410, 18, PALETTE.muted, 500);
}

// English listing (Shipaton / US judges). Background is the in-app arena art, the boss is the
// native FIRST RUN crop; no new or painted UI.
function featureEn() {
  const ctx = canvas('en-US/feature-graphic-1024x500', 1024, 500, 'en-US feature graphic');
  const art = pictures.arena;
  ctx.drawImage(art, 0, art.height * .28, art.width, art.width * 500 / 1024, 0, 0, 1024, 500);
  const shade = ctx.createLinearGradient(0, 0, 1024, 0);
  shade.addColorStop(0, '#0d0a18f2'); shade.addColorStop(.55, '#0d0a18c8'); shade.addColorStop(1, '#0d0a1840');
  ctx.fillStyle = shade; ctx.fillRect(0, 0, 1024, 500);
  crop(ctx, 'first-run-strike', [245, 240, 590, 560], [600, 70, 380, 360.68], 60);
  text(ctx, 'Focus Raid', 72, 118, 32, PALETTE.paper, 800);
  text(ctx, 'Focus 25 minutes.', 72, 214, 54, PALETTE.paper, 900);
  text(ctx, 'Strike the boss together.', 72, 280, 40, PALETTE.gold, 900);
  text(ctx, 'A focus timer where everyone raids one boss', 72, 346, 23, PALETTE.muted, 500);
  text(ctx, 'Real app screen / effect demo', 72, 410, 18, PALETTE.muted, 500);
}

function iconPath(ctx, shape, x, y, size) {
  ctx.beginPath();
  if (shape === 'circle') ctx.arc(x + size / 2, y + size / 2, size / 2, 0, Math.PI * 2);
  else if (shape === 'squircle') {
    for (let i = 0; i <= 160; i++) {
      const a = i / 160 * Math.PI * 2, c = Math.cos(a), s = Math.sin(a);
      const px = x + size / 2 + Math.sign(c) * Math.sqrt(Math.abs(c)) * size / 2;
      const py = y + size / 2 + Math.sign(s) * Math.sqrt(Math.abs(s)) * size / 2;
      if (i === 0) ctx.moveTo(px, py); else ctx.lineTo(px, py);
    }
    ctx.closePath();
  } else ctx.roundRect(x, y, size, size, size * .22);
}

function icon(ctx, x, y, size, shape = 'rounded', themed = false, adaptive = false, light = false) {
  ctx.save(); iconPath(ctx, shape, x, y, size); ctx.clip();
  ctx.fillStyle = themed ? (light ? '#ded5f4' : '#352548') : '#17102a';
  ctx.fillRect(x, y, size, size);
  // Adaptive layer: 108dp -> 72dp viewport (outer 18dp clipped on each side).
  // Store icon: separate high-resolution composition preserving the entire existing mark.
  // Launcher foreground uses a 21% inset (ic_launcher_*_safe.xml): 1.5 * 0.58 of the visible icon.
  const scale = adaptive ? 1.5 * (1 - 2 * .21) : .82;
  const d = size * scale, offset = (size - d) / 2;
  const img = pictures[themed ? 'monochrome' : 'launcher'];
  if (themed) {
    const tint = document.createElement('canvas'); tint.width = img.width; tint.height = img.height;
    const tc = tint.getContext('2d'); tc.drawImage(img, 0, 0);
    tc.globalCompositeOperation = 'source-in'; tc.fillStyle = light ? '#463458' : '#e5d8ff'; tc.fillRect(0, 0, tint.width, tint.height);
    ctx.drawImage(tint, x + offset, y + offset, d, d);
  } else ctx.drawImage(img, x + offset, y + offset, d, d);
  ctx.restore();
}

function icons() {
  const out = canvas('icon-512', 512, 512); out.fillStyle = '#17102a'; out.fillRect(0, 0, 512, 512);
  out.drawImage(pictures.launcher, 46.08, 46.08, 419.84, 419.84);
  const ctx = canvas('launcher-preview', 1440, 1060); ctx.fillStyle = '#f0edf5'; ctx.fillRect(0, 0, 1440, 1060);
  text(ctx, 'LAUNCHER / STORE ICON', 56, 78, 36, '#251b39');
  text(ctx, '現行のマークを保持。下段は108dpレイヤーの72dp表示領域を模擬。', 56, 124, 24, '#584b69', 500);
  const shapes = ['circle', 'squircle', 'rounded'];
  ['#f8f5fb', '#191122'].forEach((bg, row) => {
    const top = 161 + row * 215; ctx.fillStyle = bg; ctx.fillRect(36, top, 1368, 195);
    text(ctx, row ? 'DARK' : 'LIGHT', 62, top + 45, 22, row ? PALETTE.paper : '#342946');
    shapes.forEach((s, i) => { icon(ctx, 255 + i * 215, top + 53, 112, s, false, false); icon(ctx, 385 + i * 215, top + 102, 48, s); });
    icon(ctx, 972, top + 53, 112, 'circle', true, false, row === 0);
    icon(ctx, 1108, top + 102, 48, 'circle', true, false, row === 0);
    text(ctx, '48px', 1200, top + 135, 23, row ? PALETTE.muted : '#584b69', 500);
  });
  text(ctx, 'adaptive launcher・21% inset（シミュレーション）', 56, 657, 27, '#392c48');
  shapes.forEach((s, i) => { icon(ctx, 66 + i * 205, 698, 134, s, false, true); text(ctx, s, 66 + i * 205, 872, 21, '#584b69', 500); });
  icon(ctx, 702, 698, 134, 'circle', true, true, true);
  text(ctx, 'monochrome', 684, 872, 21, '#584b69', 500);
  text(ctx, 'inset後は輪と星が66dp安全域に収まる。', 917, 740, 24, '#7b372f', 700);
  text(ctx, '実機の各ランチャーで要確認。', 917, 780, 24, '#7b372f', 700);
  text(ctx, 'ストア用512pxは余白を調整済み。', 917, 821, 24, '#584b69', 500);
  text(ctx, 'Play Console・壁紙上・他アプリとの比較は未確認。自動合格として扱わない。', 56, 990, 25, '#584b69', 500);
}

function contact(id, assetIds, width) {
  const margin = 20, gap = 16, h = width * 1920 / 1080;
  const ctx = canvas(id, margin * 2 + width * assetIds.length + gap * (assetIds.length - 1), h + 78);
  ctx.fillStyle = '#eeeaf4'; ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  assetIds.forEach((name, i) => {
    const x = margin + i * (width + gap);
    ctx.drawImage(outputs.get(name), x, 42, width, h);
    text(ctx, name, x, 27, 12, '#42334f', 600);
  });
}

async function run() {
  const face = new FontFace('StoreNoto', 'local("Noto Sans JP")', { weight: '100 900' });
  try { await face.load(); } catch { throw new Error('Install Noto Sans JP before exporting.'); }
  document.fonts.add(face);
  await document.fonts.load(`900 110px ${font}`, '25分が、一撃になる。');
  const names = ['first-run-strike', 'shared-left', 'shared-right', 'shared-bottom', 'shared-arrived', 'companion-egg', 'companion-locks', 'return-other', 'return-self', 'home', 'focusing', 'walking-home', 'walking-notebook'];
  await Promise.all([...names.map(name => [name, `raw/${name}.png`]), ['launcher', `${ROOT}/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`], ['monochrome', `${ROOT}/app/src/main/res/drawable-nodpi/ic_launcher_monochrome.png`], ['arena', `${ROOT}/app/src/main/res/drawable-nodpi/world_arena_v13.png`]].map(async ([name, path]) => {
    const image = new Image(); image.src = path; await image.decode(); pictures[name] = image;
  }));
  heroA(); shared('01-hero-b', '01'); shared('02-shared-raid', '02'); companion(); reward(); everyday(); walking(); feature(); featureEn(); icons();
  contact('sequence-180', ['01-hero-a', '02-shared-raid', '03-companion', '04-return-raid', '05-everyday', '06-walking'], 180);
  contact('hero-ab-360', ['01-hero-a', '01-hero-b'], 360);
  window.storeEvidence = evidence;
  if (evidence.errors.length) throw new Error(`Text outside canvas: ${JSON.stringify(evidence.errors)}`);
  window.storeReady = true;
  document.querySelector('#status').textContent = '描画完了 · 6枚＋A/B案、横長グラフィック、アイコン、縮小レビュー';
}

document.querySelector('#compact').onclick = () => document.querySelector('#assets').classList.toggle('compact');
const dialog = document.querySelector('#exposure');
let exposureTimer;
document.querySelector('#five-seconds').onclick = () => {
  const target = outputs.get(document.querySelector('#variant').value);
  if (!target) return;
  const copy = document.createElement('canvas'); copy.width = target.width; copy.height = target.height;
  copy.getContext('2d').drawImage(target, 0, 0);
  document.querySelector('#questions').hidden = true;
  document.querySelector('#exposure-image').replaceChildren(copy);
  dialog.showModal();
  exposureTimer = setTimeout(() => { document.querySelector('#exposure-image').replaceChildren(); document.querySelector('#questions').hidden = false; }, 5000);
};
dialog.onclose = () => clearTimeout(exposureTimer);
document.querySelector('#close-exposure').onclick = () => dialog.close();
if (new URLSearchParams(location.search).has('export')) document.body.classList.add('export');
run().catch(error => { document.querySelector('#status').textContent = error.message; window.storeError = error.message; console.error(error); });
