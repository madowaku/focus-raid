// Demo emulator only. Production generation creation must use trusted operator credentials.
if (process.env.GCLOUD_PROJECT !== 'demo-focus-raid' || !process.env.FIRESTORE_EMULATOR_HOST) {
  throw new Error('Refusing to seed outside demo-focus-raid emulator');
}
const {initializeApp} = require('firebase-admin/app');
const {getFirestore} = require('firebase-admin/firestore');
initializeApp({projectId:'demo-focus-raid'});
getFirestore().doc('world/current').set({generation:`qa-${Date.now()}`, status:'active',
  startsAtEpochMillis:Date.now()-86400000, bossName:'環焔竜ヴォルガ', bossHp:650000,
  bossMaxHp:650000,totalFocusMinutes:0,raidParticipants:0,focusNow:0,towerFloor:1,abyssDepth:0,armoryReady:0})
  .then(()=>process.exit(0)).catch(e=>{console.error(e);process.exit(1)});
