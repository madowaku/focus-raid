const {test} = require('node:test');
const assert = require('node:assert/strict');
const {randomUUID} = require('node:crypto');
const {validate} = require('../contribution');
const now = Date.now();
const valid = () => ({sessionId:randomUUID(),generation:null,creditedMinutes:25,plannedMinutes:25,
  completedAtEpochMillis:now,startedAtEpochMillis:now-1500000});
test('valid completed session includes safe unbound first offline participation',()=>assert.deepEqual(validate(valid(),now).generation,null));
test('rejects forged damage, bounds, fractions, bad times and generation paths',()=>{
  for(const change of [{damage:999},{plannedMinutes:181,creditedMinutes:181},{creditedMinutes:2},
    {plannedMinutes:5.5,creditedMinutes:5.5},{generation:'../other'},{sessionId:'bad'},
    {startedAtEpochMillis:now},{completedAtEpochMillis:now+300001}]) {
    assert.throws(()=>validate({...valid(),...change},now));
  }
});
