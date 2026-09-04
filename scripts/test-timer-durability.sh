#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.madowaku.focusraid"
MAIN_ACTIVITY="${PACKAGE}/.MainActivity"
DEBUG_RECEIVER="${PACKAGE}/.DurabilityDebugReceiver"
ACTION_SEED="${PACKAGE}.debug.SEED_TIMER"
ACTION_PROBE="${PACKAGE}.debug.PROBE_TIMER"
ACTION_CLEAR="${PACKAGE}.debug.CLEAR_TIMER"
REPORT_DIR="artifacts/timer-durability"
REPORT_FILE="${REPORT_DIR}/report.txt"

mkdir -p "$REPORT_DIR"
: > "$REPORT_FILE"

log() {
  printf '%s\n' "$*" | tee -a "$REPORT_FILE"
}

broadcast() {
  adb shell am broadcast -n "$DEBUG_RECEIVER" "$@" 2>&1 | tr -d '\r'
}

probe() {
  broadcast -a "$ACTION_PROBE"
}

clear_state() {
  broadcast -a "$ACTION_CLEAR" >/dev/null || true
}

notification_present() {
  adb shell dumpsys notification --noredact 2>/dev/null | tr -d '\r' | \
    grep -Fq "0|${PACKAGE}|2500|"
}

assert_no_notification() {
  if notification_present; then
    log "FAIL: completion notification fired before the expected end"
    return 1
  fi
}

wait_for_notification() {
  local scenario="$1"
  local timeout_seconds="$2"
  local started
  started="$(date +%s)"

  for ((i = 0; i < timeout_seconds; i++)); do
    if notification_present; then
      local elapsed=$(( $(date +%s) - started ))
      log "PASS: ${scenario} delivered completion notification (${elapsed}s polling latency)"
      return 0
    fi
    sleep 1
  done

  log "FAIL: ${scenario} did not deliver completion notification within ${timeout_seconds}s"
  adb shell dumpsys alarm | tail -n 120 | tee -a "$REPORT_FILE" || true
  adb shell dumpsys notification --noredact | tail -n 160 | tee -a "$REPORT_FILE" || true
  return 1
}

seed_timer() {
  local duration_ms="$1"
  local output
  output="$(broadcast -a "$ACTION_SEED" --el duration_ms "$duration_ms")"
  log "$output"
  grep -q "seeded=true" <<<"$output"
}

launch_main() {
  adb shell am start -W -n "$MAIN_ACTIVITY" >/dev/null
  sleep 1
}

reconcile_after_expiry() {
  local scenario="$1"
  launch_main

  for _ in {1..15}; do
    local output
    output="$(probe)"
    if grep -q "phase=READY" <<<"$output"; then
      log "PASS: ${scenario} reconciled expired persisted session to READY"
      adb shell input keyevent KEYCODE_HOME >/dev/null || true
      return 0
    fi
    sleep 1
  done

  log "FAIL: ${scenario} did not reconcile persisted state after app launch"
  probe | tee -a "$REPORT_FILE"
  return 1
}

wait_for_boot() {
  adb wait-for-device
  for _ in {1..120}; do
    if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
      adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
      adb shell input keyevent 82 >/dev/null 2>&1 || true
      sleep 2
      return 0
    fi
    sleep 1
  done
  log "FAIL: emulator did not finish booting within 120s"
  return 1
}

cleanup() {
  adb shell dumpsys deviceidle unforce >/dev/null 2>&1 || true
  adb shell dumpsys battery reset >/dev/null 2>&1 || true
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  clear_state
}
trap cleanup EXIT

log "Focus Raid timer durability suite"
log "Started: $(date -u +%Y-%m-%dT%H:%M:%SZ)"

gradle installDebug --stacktrace

# Notification permission is runtime-granted in CI so alarm delivery can be observed.
adb shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true

# Simulate the user granting exact-alarm special access. The product itself keeps this optional
# and falls back safely, but the Doze timing test needs deterministic exact delivery.
adb shell appops set "$PACKAGE" SCHEDULE_EXACT_ALARM allow >/dev/null 2>&1 || true
sleep 1

capabilities="$(probe)"
log "$capabilities"
if ! grep -q "exact=true" <<<"$capabilities"; then
  log "FAIL: emulator could not enable exact-alarm access for deterministic Doze testing"
  exit 1
fi

log ""
log "SCENARIO 1/4: screen off"
clear_state
seed_timer 12000
launch_main
adb shell input keyevent KEYCODE_SLEEP >/dev/null
sleep 3
assert_no_notification
wait_for_notification "screen-off" 20
adb shell input keyevent KEYCODE_WAKEUP >/dev/null || true
reconcile_after_expiry "screen-off"

log ""
log "SCENARIO 2/4: deep Doze"
clear_state
seed_timer 15000
launch_main
adb shell dumpsys battery unplug >/dev/null
adb shell input keyevent KEYCODE_SLEEP >/dev/null
sleep 1
idle_output="$(adb shell dumpsys deviceidle force-idle 2>&1 | tr -d '\r')"
log "deviceidle: ${idle_output}"
sleep 2
if ! adb shell dumpsys deviceidle | grep -q "mState=IDLE"; then
  log "FAIL: emulator did not enter deep idle"
  adb shell dumpsys deviceidle | tee -a "$REPORT_FILE"
  exit 1
fi
assert_no_notification
wait_for_notification "deep-Doze" 25
adb shell dumpsys deviceidle unforce >/dev/null || true
adb shell dumpsys battery reset >/dev/null || true
adb shell input keyevent KEYCODE_WAKEUP >/dev/null || true
reconcile_after_expiry "deep-Doze"

log ""
log "SCENARIO 3/4: app process kill"
clear_state
seed_timer 12000
launch_main
adb shell input keyevent KEYCODE_HOME >/dev/null
sleep 1
adb shell am kill "$PACKAGE" >/dev/null
sleep 1
if [[ -n "$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')" ]]; then
  log "FAIL: app process remained alive after am kill"
  exit 1
fi
log "PASS: app process was killed while the persisted timer remained scheduled"
sleep 2
assert_no_notification
wait_for_notification "process-kill" 20
reconcile_after_expiry "process-kill"

log ""
log "SCENARIO 4/4: device reboot"
clear_state
seed_timer 12000
log "Rebooting emulator with an active persisted session..."
adb reboot
wait_for_boot

# Runtime permission and special access normally survive reboot; refresh the runtime notification
# grant defensively for emulator images that reset transient app-op state during boot.
adb shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
wait_for_notification "device-reboot" 30
reconcile_after_expiry "device-reboot"

log ""
log "ALL PASS: screen-off / deep-Doze / process-kill / device-reboot"
log "Finished: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
