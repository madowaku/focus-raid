import http from 'node:http';
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { FieldValue, Timestamp, getFirestore } from 'firebase-admin/firestore';

initializeApp();

const db = getFirestore();
const auth = getAuth();
const port = Number(process.env.PORT ?? 8080);
const allowedOrigin = process.env.ALLOWED_ORIGIN ?? '*';

const lootTables = {
  COMMON: ['鉄鉱石', '旅人の短剣', '革の小盾', '古びた矢束'],
  RARE: ['白銀の槍', '氷晶の弓', '星灯りの盾', '古代の歯車'],
  EPIC: ['雷撃砲の心臓', '深層解析水晶', '月哭きの槍'],
  LEGENDARY: ['星喰らいの大剣', '黎明の世界鍵'],
};

const armoryPoints = {
  COMMON: 1,
  RARE: 2,
  EPIC: 5,
  LEGENDARY: 10,
};

function json(res, status, value) {
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'access-control-allow-origin': allowedOrigin,
    'access-control-allow-headers': 'authorization, content-type',
    'access-control-allow-methods': 'GET, POST, OPTIONS',
  });
  res.end(JSON.stringify(value));
}

async function readJson(req) {
  let body = '';
  for await (const chunk of req) {
    body += chunk;
    if (body.length > 32_768) throw new Error('Request body too large.');
  }
  return body ? JSON.parse(body) : {};
}

async function requireUser(req) {
  const header = req.headers.authorization ?? '';
  if (!header.startsWith('Bearer ')) throw Object.assign(new Error('Missing bearer token.'), { status: 401 });
  const token = header.slice('Bearer '.length);
  return auth.verifyIdToken(token);
}

function validateStart(payload) {
  const plannedMinutes = Number(payload.plannedMinutes);
  if (!Number.isInteger(plannedMinutes) || plannedMinutes < 1 || plannedMinutes > 180) {
    throw Object.assign(new Error('plannedMinutes must be an integer from 1 to 180.'), { status: 400 });
  }
  if (!['tower', 'abyss'].includes(payload.destination)) {
    throw Object.assign(new Error('destination must be tower or abyss.'), { status: 400 });
  }
  if (!['expedition', 'raid'].includes(payload.mode)) {
    throw Object.assign(new Error('mode must be expedition or raid.'), { status: 400 });
  }
  if (payload.mode === 'raid' && plannedMinutes !== 25) {
    throw Object.assign(new Error('World Raid sessions are fixed to 25 minutes.'), { status: 400 });
  }
  return {
    plannedMinutes,
    destination: payload.destination,
    mode: payload.mode,
    goalId: typeof payload.goalId === 'string' ? payload.goalId : null,
    raidId: typeof payload.raidId === 'string' ? payload.raidId : null,
  };
}

function rollDiscovery() {
  const roll = Math.random();
  const rarity = roll < 0.77 ? 'COMMON'
    : roll < 0.96 ? 'RARE'
      : roll < 0.995 ? 'EPIC'
        : 'LEGENDARY';
  const table = lootTables[rarity];
  const discovery = table[Math.floor(Math.random() * table.length)];
  return { rarity, discovery, armoryPoints: armoryPoints[rarity] };
}

async function startFocus(req, res) {
  const user = await requireUser(req);
  const payload = validateStart(await readJson(req));
  const startedAt = Timestamp.now();
  const sessionRef = db.collection('users').doc(user.uid).collection('sessions').doc();

  await sessionRef.set({
    ...payload,
    status: 'active',
    startedAt,
    createdAt: FieldValue.serverTimestamp(),
  });

  json(res, 201, {
    sessionId: sessionRef.id,
    startedAt: startedAt.toDate().toISOString(),
    plannedMinutes: payload.plannedMinutes,
  });
}

async function finishFocus(req, res) {
  const user = await requireUser(req);
  const { sessionId } = await readJson(req);
  if (typeof sessionId !== 'string' || !sessionId) {
    throw Object.assign(new Error('sessionId is required.'), { status: 400 });
  }

  const userRef = db.collection('users').doc(user.uid);
  const sessionRef = userRef.collection('sessions').doc(sessionId);
  const worldRef = db.collection('world').doc('current');

  const result = await db.runTransaction(async (tx) => {
    const [sessionSnap, userSnap, worldSnap] = await Promise.all([
      tx.get(sessionRef),
      tx.get(userRef),
      tx.get(worldRef),
    ]);

    if (!sessionSnap.exists) throw Object.assign(new Error('Focus session not found.'), { status: 404 });
    const session = sessionSnap.data();
    if (session.status === 'completed' && session.result) return session.result;
    if (session.status !== 'active') throw Object.assign(new Error('Focus session is not active.'), { status: 409 });

    const now = Timestamp.now();
    const elapsedMs = Math.max(0, now.toMillis() - session.startedAt.toMillis());
    const creditedMinutes = Math.min(session.plannedMinutes, Math.floor(elapsedMs / 60_000));
    const previous = userSnap.exists ? userSnap.data() : {};
    const previousTotal = Number(previous.totalFocusMinutes ?? 0);
    const previousRemainder = Number(previous.discoveryRemainderMinutes ?? 0);
    const previousPersonalCarry = Number(previous.personalDamageCarry ?? 0);

    const discoveryPool = previousRemainder + creditedMinutes;
    const discoveryRolls = session.mode === 'expedition' ? Math.floor(discoveryPool / 25) : 0;
    const discoveryRemainderMinutes = session.mode === 'expedition' ? discoveryPool % 25 : previousRemainder;
    const loot = discoveryRolls > 0 ? rollDiscovery() : null;
    const personalPool = previousPersonalCarry + creditedMinutes;
    const defeated = Math.floor(personalPool / 25);
    const personalDamageCarry = personalPool % 25;
    const totalFocusMinutes = previousTotal + creditedMinutes;
    const worldEp = session.mode === 'expedition' ? creditedMinutes : 0;
    const raidDamage = session.mode === 'raid' ? creditedMinutes : 0;
    const currentArmoryReady = Number(worldSnap.exists ? worldSnap.data().armoryReady ?? 0 : 0);
    const nextArmoryReady = Math.min(100, Number((currentArmoryReady + (loot?.armoryPoints ?? 0) * 0.02).toFixed(2)));

    const response = {
      sessionId,
      creditedMinutes,
      personalDamage: creditedMinutes,
      worldEp,
      defeated,
      rarity: loot?.rarity ?? null,
      discovery: loot?.discovery ?? null,
      armoryPoints: loot?.armoryPoints ?? 0,
      totalFocusMinutes,
      armoryReady: nextArmoryReady,
    };

    tx.set(userRef, {
      totalFocusMinutes,
      companionFocusMinutes: totalFocusMinutes,
      discoveryRemainderMinutes,
      personalDamageCarry,
      defeatedCount: FieldValue.increment(defeated),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    tx.set(sessionRef, {
      status: 'completed',
      finishedAt: now,
      creditedMinutes,
      result: response,
    }, { merge: true });

    const worldUpdate = {
      armoryReady: nextArmoryReady,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (session.destination === 'tower' && worldEp) worldUpdate.towerEp = FieldValue.increment(worldEp);
    if (session.destination === 'abyss' && worldEp) worldUpdate.abyssEp = FieldValue.increment(worldEp);
    if (raidDamage) worldUpdate.raidDamage = FieldValue.increment(raidDamage);
    tx.set(worldRef, worldUpdate, { merge: true });

    if (session.mode === 'raid' && session.raidId) {
      const entryRef = db.collection('raids').doc(session.raidId).collection('entries').doc(user.uid);
      tx.set(entryRef, {
        damage: raidDamage,
        focusMinutes: creditedMinutes,
        submittedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    }

    return response;
  });

  json(res, 200, result);
}

async function handle(req, res) {
  if (req.method === 'OPTIONS') return json(res, 204, {});
  if (req.method === 'GET' && req.url === '/health') return json(res, 200, { ok: true, service: 'focus-raid-api' });
  if (req.method === 'POST' && req.url === '/focus/start') return startFocus(req, res);
  if (req.method === 'POST' && req.url === '/focus/finish') return finishFocus(req, res);
  return json(res, 404, { error: 'Not found.' });
}

const server = http.createServer((req, res) => {
  handle(req, res).catch((error) => {
    console.error(error);
    json(res, Number(error.status ?? 500), { error: error.message ?? 'Internal server error.' });
  });
});

server.listen(port, '0.0.0.0', () => {
  console.log(`Focus Raid API listening on ${port}`);
});
