const { HttpsError } = require('firebase-functions/v2/https');
const KEYS = ['sessionId','generation','creditedMinutes','plannedMinutes','completedAtEpochMillis','startedAtEpochMillis'];

function validate(data, now) {
  if (!data || typeof data !== 'object' || Array.isArray(data) ||
      Object.keys(data).length !== KEYS.length || KEYS.some(k => !Object.hasOwn(data, k)) ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(data.sessionId) ||
      !(data.generation === null || (typeof data.generation === 'string' && /^[A-Za-z0-9_-]{1,80}$/.test(data.generation))) ||
      !Number.isInteger(data.plannedMinutes) || data.plannedMinutes < 5 || data.plannedMinutes > 180 ||
      data.creditedMinutes !== data.plannedMinutes ||
      !Number.isSafeInteger(data.startedAtEpochMillis) || data.startedAtEpochMillis <= 0 ||
      !Number.isSafeInteger(data.completedAtEpochMillis) || data.completedAtEpochMillis > now + 300000 ||
      data.completedAtEpochMillis - data.startedAtEpochMillis < data.creditedMinutes * 60000) {
    throw new HttpsError('invalid-argument', 'Invalid completed focus session');
  }
  return data;
}

async function contribute(db, uid, raw, now = Date.now()) {
  if (!uid) throw new HttpsError('unauthenticated', 'Sign in required');
  const data = validate(raw, now);
  const receiptRef = db.doc(`contributions/${data.sessionId}`);
  const worldRef = db.doc('world/current');
  const day = new Date(now).toISOString().slice(0, 10);
  const budgetRef = db.doc(`contributionBudgets/${uid}_${day}`);
  return db.runTransaction(async tx => {
    const existing = await tx.get(receiptRef);
    if (existing.exists) {
      const receipt = existing.data();
      if (receipt.uid !== uid || KEYS.some(k => receipt.request[k] !== data[k])) {
        return {status:'REJECTED', generation:data.generation, creditedMinutes:0, appliedDamage:0};
      }
      return {...receipt.result, status: receipt.result.status === 'ACCEPTED' ? 'ALREADY_COUNTED' : receipt.result.status};
    }
    const worldDoc = await tx.get(worldRef);
    if (!worldDoc.exists) throw new HttpsError('unavailable', 'Raid unavailable');
    const w = worldDoc.data();
    if (typeof w.generation !== 'string' || !/^[A-Za-z0-9_-]{1,80}$/.test(w.generation) || !['active','defeated'].includes(w.status) ||
        !Number.isSafeInteger(w.startsAtEpochMillis) || w.startsAtEpochMillis <= 0 ||
        !Number.isSafeInteger(w.bossMaxHp) || w.bossMaxHp <= 0 || w.bossMaxHp > 2147483647 ||
        !Number.isSafeInteger(w.bossHp) || w.bossHp < 0 || w.bossHp > w.bossMaxHp ||
        !Number.isSafeInteger(w.totalFocusMinutes) || w.totalFocusMinutes < 0 ||
        !Number.isSafeInteger(w.raidParticipants) || w.raidParticipants < 0) {
      throw new HttpsError('unavailable', 'Raid configuration unavailable');
    }
    let result = {status:'STALE', generation:data.generation, resolvedGeneration:w.generation, creditedMinutes:0, appliedDamage:0};
    if ((data.generation === null || data.generation === w.generation) &&
        data.startedAtEpochMillis >= w.startsAtEpochMillis && w.status === 'active' && w.bossHp > 0) {
      const participantRef = db.doc(`worldParticipants/${w.generation}/users/${uid}`);
      const [budget, participant] = await Promise.all([tx.get(budgetRef), tx.get(participantRef)]);
      const used = budget.exists ? budget.data().minutes : 0;
      if (!Number.isInteger(used) || used < 0) throw new HttpsError('unavailable', 'Budget unavailable');
      if (used + data.creditedMinutes > 1440) result = {...result, status:'REJECTED'};
      else {
        const total = w.totalFocusMinutes + data.creditedMinutes;
        const count = w.raidParticipants + (participant.exists ? 0 : 1);
        if (!Number.isSafeInteger(total) || count > 2147483647) throw new HttpsError('unavailable', 'Raid capacity reached');
        const damage = Math.min(w.bossHp, data.creditedMinutes);
        result = {...result, status:'ACCEPTED', creditedMinutes:data.creditedMinutes, appliedDamage:damage};
        tx.update(worldRef, {bossHp:w.bossHp-damage, totalFocusMinutes:total,
          raidParticipants:count, status: w.bossHp === damage ? 'defeated' : 'active'});
        tx.set(budgetRef, {minutes:used+data.creditedMinutes});
        if (!participant.exists) tx.create(participantRef, {firstSessionId:data.sessionId});
      }
    }
    tx.create(receiptRef, {uid, request:data, result, receivedAtEpochMillis:now});
    return result;
  });
}
module.exports = {validate, contribute};
