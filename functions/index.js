const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { onCall } = require('firebase-functions/v2/https');
const { contribute } = require('./contribution');
const { recentEchoes } = require('./raid-echoes');
initializeApp();
const callableOptions = {region:'us-central1', maxInstances:10,
  enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== 'true'};
exports.submitContribution = onCall(callableOptions,
  request => contribute(getFirestore(), request.auth?.uid, request.data));
exports.getRecentRaidEchoes = onCall(callableOptions,
  request => recentEchoes(getFirestore(), request.auth?.uid, request.data));
