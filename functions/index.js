const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { onCall } = require('firebase-functions/v2/https');
const { contribute } = require('./contribution');
initializeApp();
exports.submitContribution = onCall({region:'us-central1', maxInstances:10,
  enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== 'true'},
  request => contribute(getFirestore(), request.auth?.uid, request.data));
