#!/usr/bin/env bash
set -euo pipefail
export GCLOUD_PROJECT=demo-focus-raid
export FIRESTORE_EMULATOR_HOST=127.0.0.1:8080
node functions/seed.js
gradle connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.worldEmulator=true \
  -Pandroid.testInstrumentationRunnerArguments.class=com.madowaku.focusraid.WorldwideEmulatorTest \
  --stacktrace
