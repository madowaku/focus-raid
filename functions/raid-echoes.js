const { HttpsError } = require('firebase-functions/v2/https');

function validGeneration(value) {
  return typeof value === 'string' && /^[A-Za-z0-9_-]{1,80}$/.test(value);
}

async function recentEchoes(db, uid, raw = {}, now = Date.now()) {
  if (!uid) throw new HttpsError('unauthenticated', 'Sign in required');
  if (!raw || typeof raw !== 'object' || Array.isArray(raw) ||
      Object.keys(raw).some(key => key !== 'limit')) {
    throw new HttpsError('invalid-argument', 'Invalid echo request');
  }
  const limit = raw.limit === undefined ? 3 : raw.limit;
  if (!Number.isInteger(limit) || limit < 1 || limit > 3) {
    throw new HttpsError('invalid-argument', 'Invalid echo limit');
  }

  const worldDoc = await db.doc('world/current').get();
  if (!worldDoc.exists) throw new HttpsError('unavailable', 'Raid unavailable');
  const generation = worldDoc.data().generation;
  if (!validGeneration(generation)) throw new HttpsError('unavailable', 'Raid generation unavailable');

  // Scan beyond the visible limit so one very active person cannot occupy every
  // camp light. The client still receives at most `limit` anonymous echoes.
  const snapshot = await db.collection(`raidEchoes/${generation}/events`)
    .orderBy('receivedAtEpochMillis', 'desc')
    .limit(24)
    .get();
  const oldest = now - 24 * 60 * 60 * 1000;
  const echoes = [];
  const seenUids = new Set();
  for (const document of snapshot.docs) {
    const data = document.data();
    if (typeof data.uid !== 'string' || data.uid.length === 0 || data.uid === uid || seenUids.has(data.uid)) continue;
    if (!Number.isInteger(data.creditedMinutes) || data.creditedMinutes < 5 || data.creditedMinutes > 180 ||
        !Number.isInteger(data.appliedDamage) || data.appliedDamage < 0 || data.appliedDamage > data.creditedMinutes ||
        !Number.isSafeInteger(data.completedAtEpochMillis) ||
        !Number.isSafeInteger(data.receivedAtEpochMillis) || data.receivedAtEpochMillis < oldest) {
      continue;
    }
    seenUids.add(data.uid);
    echoes.push({
      focusMinutes: data.creditedMinutes,
      damage: data.appliedDamage,
      ageMinutes: Math.max(0, Math.floor((now - data.completedAtEpochMillis) / 60000)),
    });
    if (echoes.length >= limit) break;
  }
  return {generation, echoes};
}

module.exports = {recentEchoes};
