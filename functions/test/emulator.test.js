const {test,after} = require('node:test');
const assert = require('node:assert/strict');
const {randomUUID} = require('node:crypto');
const {initializeApp:adminApp} = require('firebase-admin/app');
const {getFirestore:adminDb} = require('firebase-admin/firestore');
const {initializeApp,deleteApp} = require('firebase/app');
const {getAuth,connectAuthEmulator,signInAnonymously} = require('firebase/auth');
const {getFirestore,connectFirestoreEmulator,doc,getDoc,setDoc} = require('firebase/firestore');
const {getFunctions,connectFunctionsEmulator,httpsCallable} = require('firebase/functions');
assert.equal(process.env.GCLOUD_PROJECT,'demo-focus-raid');
assert.ok(process.env.FIRESTORE_EMULATOR_HOST);
adminApp({projectId:'demo-focus-raid'});
const db=adminDb(), apps=[];
after(async()=>{await Promise.all(apps.map(deleteApp)); await db.terminate()});
async function client(signed=true){
  const app=initializeApp({projectId:'demo-focus-raid',apiKey:'fake-key',appId:'test'},randomUUID());apps.push(app);
  const auth=getAuth(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});
  const fs=getFirestore(app);connectFirestoreEmulator(fs,'127.0.0.1',8080);
  const fn=getFunctions(app);connectFunctionsEmulator(fn,'127.0.0.1',5001);
  if(signed) await signInAnonymously(auth);
  return {auth,fs,
    submit:async data=>(await httpsCallable(fn,'submitContribution')(data)).data,
    echoes:async data=>(await httpsCallable(fn,'getRecentRaidEchoes')(data)).data};
}
function request(generation='N', minutes=5){const now=Date.now();return {sessionId:randomUUID(),generation,
  creditedMinutes:minutes,plannedMinutes:minutes,startedAtEpochMillis:now-minutes*60000,completedAtEpochMillis:now}}
async function seed(generation='N', hp=10000, starts=Date.now()-86400000){
  await db.doc('world/current').set({generation,status:'active',startsAtEpochMillis:starts,bossHp:hp,
    bossMaxHp:hp,totalFocusMinutes:0,raidParticipants:0,bossName:'環焔竜ヴォルガ'});
}
async function world(c){return (await getDoc(doc(c.fs,'world/current'))).data()}
test('A/B/C real callable: two identities converge, response loss and concurrent retries count once',async()=>{
  await seed();const a=await client(),b=await client();const payload=request(null);
  const results=await Promise.all(Array.from({length:8},()=>a.submit(payload)));
  assert.equal(results.filter(x=>x.status==='ACCEPTED').length,1);
  assert.equal((await world(b)).totalFocusMinutes,5);
  assert.equal((await a.submit(payload)).status,'ALREADY_COUNTED');
  assert.equal((await b.submit(request())).status,'ACCEPTED');
  const wa=await world(a),wb=await world(b);assert.deepEqual(wa,wb);
  assert.equal(wa.totalFocusMinutes,10);assert.equal(wa.bossHp,9990);assert.equal(wa.raidParticipants,2);
  assert.equal((await b.submit(payload)).status,'REJECTED');
  assert.equal((await a.submit({...payload,generation:'other'})).status,'REJECTED');
  assert.equal((await world(a)).totalFocusMinutes,10);
});
test('recent raid echoes are anonymous, exclude caller and remain server-only',async()=>{
  await seed('echo');const a=await client(),b=await client();
  const aPayload=request('echo',25),bPayload=request('echo',50);
  assert.equal((await a.submit(aPayload)).status,'ACCEPTED');
  assert.equal((await b.submit(bPayload)).status,'ACCEPTED');
  const feed=await a.echoes({limit:3});
  assert.equal(feed.generation,'echo');
  assert.equal(feed.echoes.length,1);
  assert.deepEqual(Object.keys(feed.echoes[0]).sort(),['ageMinutes','damage','focusMinutes']);
  assert.equal(feed.echoes[0].focusMinutes,50);
  assert.equal(feed.echoes[0].damage,50);
  assert.ok(feed.echoes[0].ageMinutes>=0);
  await assert.rejects(getDoc(doc(a.fs,`raidEchoes/echo/events/${bPayload.sessionId}`)),e=>e.code==='permission-denied');
});
test('D old bound/unbound sessions never damage replacement; receipt survives rotation',async()=>{
  await seed('old');const c=await client();const accepted=request('old');await c.submit(accepted);
  const bound=request('old'),unbound=request(null);
  await seed('new',10000,Date.now());
  for(const payload of [bound,unbound]) {assert.equal((await c.submit(payload)).status,'STALE');assert.equal((await c.submit(payload)).status,'STALE')}
  assert.equal((await c.submit(accepted)).status,'ALREADY_COUNTED');assert.equal((await world(c)).totalFocusMinutes,0);
});
test('E rules deny direct world/receipt/budget writes, other receipts and unsigned reads',async()=>{
  await seed();const c=await client(),other=await client(),unsigned=await client(false);const payload=request();await c.submit(payload);
  for(const path of ['world/current',`contributions/${payload.sessionId}`,'contributionBudgets/forged']) {
    await assert.rejects(setDoc(doc(c.fs,path),{bossHp:0,minutes:0}),e=>e.code==='permission-denied');
  }
  await assert.rejects(getDoc(doc(other.fs,`contributions/${payload.sessionId}`)),e=>e.code==='permission-denied');
  await assert.rejects(getDoc(doc(unsigned.fs,'world/current')),e=>e.code==='permission-denied');
  assert.ok((await getDoc(doc(c.fs,`contributions/${payload.sessionId}`))).exists());
});
test('E/F validation, unauthenticated, budget and configuration failures never report success',async()=>{
  await seed();const c=await client(),unsigned=await client(false);
  await assert.rejects(unsigned.submit(request()),e=>e.code==='functions/unauthenticated');
  await assert.rejects(unsigned.echoes({limit:3}),e=>e.code==='functions/unauthenticated');
  await assert.rejects(c.echoes({limit:4}),e=>e.code==='functions/invalid-argument');
  for(const change of [{damage:1000000},{creditedMinutes:181,plannedMinutes:181},{creditedMinutes:4},{generation:'bad/path'}]) {
    await assert.rejects(c.submit({...request(),...change}),e=>e.code==='functions/invalid-argument');
  }
  for(let i=0;i<8;i++) assert.equal((await c.submit(request('N',180))).status,'ACCEPTED');
  const excess=request();assert.equal((await c.submit(excess)).status,'REJECTED');
  assert.equal((await c.submit(excess)).status,'REJECTED');
  assert.equal((await world(c)).totalFocusMinutes,1440);
  await db.doc('world/current').delete();
  await assert.rejects(c.submit(request()),e=>e.code==='functions/unavailable');
  await assert.rejects(c.echoes({limit:3}),e=>e.code==='functions/unavailable');
});
test('last hit clamps HP, retains full focus and ends raid',async()=>{
  await seed('last',3);const c=await client();const receipt=await c.submit(request('last'));
  assert.equal(receipt.appliedDamage,3);assert.equal((await world(c)).totalFocusMinutes,5);
  assert.equal((await world(c)).status,'defeated');
  assert.equal((await c.submit(request('last'))).status,'STALE');
});
