#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.madowaku.focusraid"
MAIN_ACTIVITY="${PACKAGE}/.MainActivity"
DEBUG_RECEIVER="${PACKAGE}/.DurabilityDebugReceiver"
ACTION_SEED="${PACKAGE}.debug.SEED_TIMER"
ACTION_PROBE="${PACKAGE}.debug.PROBE_TIMER"
ACTION_CLEAR="${PACKAGE}.debug.CLEAR_TIMER"
DELIVERY_PREFS="shared_prefs/focus_completion_delivery.xml"
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

kill_app_process() {
  # `am kill` refuses to kill foreground or temporarily allowlisted processes. AlarmManager grants
  # a short allowlist window after an allow-while-idle alarm fires, so retry the actual kill command
  # until that window expires instead of treating the still-live receiver process as a product bug.
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  adb shell input keyevent 82 >/dev/null 2>&1 || true
  adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
  sleep 0.5

  for _ in {1..24}; do
    adb shell am kill "$PACKAGE" >/dev/null 2>&1 || true
    sleep 0.5
    if [[ -z "$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')" ]]; then
      return 0
    fi
  done

  log "FAIL: app process could not be killed cleanly after the alarm allowlist window"
  adb shell dumpsys activity processes | grep -A 8 -B 4 "$PACKAGE" | tee -a "$REPORT_FILE" || true
  return 1
}

prepare_scenario() {
  clear_state
  kill_app_process
}

# Read the app's tiny completion-delivery marker directly from its debug data directory.
# `run-as` does not start the application process, so this remains a valid process-death test.
completion_marker_xml() {
  adb shell run-as "$PACKAGE" cat "$DELIVERY_PREFS" 2>/dev/null | tr -d '\r' || true
}

completion_marker_present() {
  completion_marker_xml | grep -q 'name="last_posted_at"'
}

assert_no_completion() {
  if completion_marker_present; then
    log "FAIL: completion delivery was recorded before the expected end"
    log "marker: $(completion_marker_xml)"
    return 1
  fi
}

wait_for_completion() {
  local scenario="$1"
  local timeout_seconds="$2"
  local started
  started="$(date +%s)"

  for ((i = 0; i < timeout_seconds; i++)); do
    if completion_marker_present; then
      local elapsed=$(( $(date +%s) - started ))
      log "PASS: ${scenario} recorded completion delivery (${elapsed}s polling latency)"
      log "marker: $(completion_marker_xml)"
      return 0
    fi
    sleep 1
  done

  log "FAIL: ${scenario} did not record completion delivery within ${timeout_seconds}s"
  probe | tee -a "$REPORT_FILE" || true
  adb shell dumpsys alarm | tail -n 140 | tee -a "$REPORT_FILE" || true
  adb shell dumpsys notification --noredact | tail -n 180 | tee -a "$REPORT_FILE" || true
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

  # Always exercise process-death restoration here. This prevents a ViewModel left alive by the
  # previous scenario from masking or blocking persisted-session reconciliation.
  kill_app_process
  launch_main

  for _ in {1..15}; do
    local output
    output="$(probe)"
    if grep -q "phase=READY" <<<"$output"; then
      log "PASS: ${scenario} reconciled expired persisted session to READY after fresh launch"
      adb shell input keyevent KEYCODE_HOME >/dev/null || true
      return 0
    fi
    sleep 1
  done

  log "FAIL: ${scenario} did not reconcile persisted state after fresh app launch"
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

# Notification permission is runtime-granted in CI so delivery can be observed.
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

# Doze runs first because Android rate-limits allow-while-idle alarms while already idle.
# Running it before any other Focus Raid alarm avoids a false failure from that OS quota.
log ""
log "SCENARIO 1/4: deep Doze"
prepare_scenario
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
assert_no_completion
wait_for_completion "deep-Doze" 25
adb shell dumpsys deviceidle unforce >/dev/null || true
adb shell dumpsys battery reset >/dev/null || true
adb shell input keyevent KEYCODE_WAKEUP >/dev/null || true
reconcile_after_expiry "deep-Doze"

log ""
log "SCENARIO 2/4: screen off"
prepare_scenario
seed_timer 12000
launch_main
adb shell input keyevent KEYCODE_SLEEP >/dev/null
sleep 3
assert_no_completion
wait_for_completion "screen-off" 20
adb shell input keyevent KEYCODE_WAKEUP >/dev/null || true
reconcile_after_expiry "screen-off"

log ""
log "SCENARIO 3/4: app process kill"
prepare_scenario
seed_timer 12000
launch_main
kill_app_process
log "PASS: app process was killed while the persisted timer remained scheduled"
sleep 2
assert_no_completion
wait_for_completion "process-kill" 20
reconcile_after_expiry "process-kill"

log ""
log "SCENARIO 4/4: device reboot"
prepare_scenario
seed_timer 12000
log "Rebooting emulator with an active persisted session..."
adb reboot
wait_for_boot

# Runtime permission normally survives reboot. Refresh it defensively for emulator images that
# reset transient grant state during boot. The persisted completion marker survives the reboot.
adb shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
wait_for_completion "device-reboot" 30
reconcile_after_expiry "device-reboot"

log ""
log "ALL PASS: deep-Doze / screen-off / process-kill / device-reboot"
log "Finished: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
