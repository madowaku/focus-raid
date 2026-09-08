#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.madowaku.focusraid"
ACTIVITY="${PACKAGE}/.VisualQaActivity"
OUTPUT_DIR="artifacts/visual"

cleanup() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
  adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
}
trap cleanup EXIT

gradle installDebug --stacktrace
mkdir -p "$OUTPUT_DIR"
adb shell settings put system font_scale 1.0

capture() {
  local phase="$1"
  local width="$2"
  local height="$3"
  local output="$4"

  adb shell am force-stop "$PACKAGE"
  adb shell wm size "${width}x${height}"
  adb shell wm density 160
  adb shell am start -W -n "$ACTIVITY" --es phase "$phase" --ez concept_art "${CONCEPT_ART:-false}"
  sleep 2
  adb exec-out screencap -p > "${OUTPUT_DIR}/${output}.png"
}

capture READY 360 800 ready-360x800
capture STAR_READY 360 800 star-ready-360x800
capture CUSTOM 360 800 custom-360x800
capture SYSTEM_ACCESS 360 800 system-access-360x800
capture PAYWALL 360 800 paywall-360x800
capture RAID 360 800 raid-360x800
capture RAID_OVERVIEW 360 800 raid-overview-free-360x800
capture RAID_OVERVIEW_PRO 360 800 raid-overview-pro-360x800
capture STAR_ROUTE 360 800 star-route-360x800
capture PAUSED 360 800 paused-360x800
capture END_CONFIRM 360 800 end-confirm-360x800
capture ABORTED 360 800 aborted-360x800
capture VICTORY 360 800 victory-360x800
capture FOOTPRINT_LOADING 360 800 footprint-loading-360x800
capture FOOTPRINT_PRESENT 360 800 footprint-present-360x800
capture FOOTPRINT_POSTING 360 800 footprint-posting-360x800
capture FOOTPRINT_ERROR 360 800 footprint-error-360x800
capture FOOTPRINT_POSTED 360 800 footprint-posted-360x800
capture STAR_VICTORY 360 800 star-victory-360x800
capture STAR_CONTINUE 360 800 star-continue-360x800
capture EVOLUTION 360 800 evolution-360x800
capture COMPANION_EGG 360 800 companion-egg-360x800
capture COMPANION 360 800 companion-hatchling-360x800
capture COMPANION_FIRST 360 800 companion-first-360x800
capture COMPANION_SECOND 360 800 companion-second-360x800
capture COMPANION_MATURE 360 800 companion-mature-360x800
capture LOG 360 800 log-360x800
capture LOG_PRO 360 800 log-pro-360x800
capture READY 720 1280 ready-720x1280
capture STAR_READY 720 1280 star-ready-720x1280
capture CUSTOM 720 1280 custom-720x1280
capture SYSTEM_ACCESS 720 1280 system-access-720x1280
capture PAYWALL 720 1280 paywall-720x1280
capture RAID 720 1280 raid-720x1280
capture RAID_OVERVIEW 720 1280 raid-overview-free-720x1280
capture RAID_OVERVIEW_PRO 720 1280 raid-overview-pro-720x1280
capture STAR_ROUTE 720 1280 star-route-720x1280
capture PAUSED 720 1280 paused-720x1280
capture END_CONFIRM 720 1280 end-confirm-720x1280
capture ABORTED 720 1280 aborted-720x1280
capture VICTORY 720 1280 victory-720x1280
capture FOOTPRINT_PRESENT 720 1280 footprint-present-720x1280
capture FOOTPRINT_POSTED 720 1280 footprint-posted-720x1280
capture STAR_VICTORY 720 1280 star-victory-720x1280
capture STAR_CONTINUE 720 1280 star-continue-720x1280
capture EVOLUTION 720 1280 evolution-720x1280
capture COMPANION 720 1280 companion-hatchling-720x1280
capture LOG 720 1280 log-720x1280
capture LOG_PRO 720 1280 log-pro-720x1280

# Required accessibility matrix. Essential actions remain reachable by scrolling.
for scale in 1.3 1.5; do
  adb shell settings put system font_scale "$scale"
  for phase in READY TIMER_MAX CUSTOM_MAX RAID PAUSED END_CONFIRM VICTORY ABORTED PAYWALL PAYWALL_ERROR PAYWALL_RESTORING PAYWALL_PURCHASING FOOTPRINT_PRESENT FOOTPRINT_ERROR LOG_PRO; do
    capture "$phase" 360 800 "${phase,,}-360x800-font-${scale}"
    capture "$phase" 720 1280 "${phase,,}-720x1280-font-${scale}"
  done
done
adb shell settings put system font_scale 1.0
CONCEPT_ART=true capture COMPANION 360 800 concept-rag-360x800
CONCEPT_ART=true capture READY 360 800 concept-volga-360x800
CONCEPT_ART=true capture COMPANION_MIKO 360 800 concept-miko-360x800
CONCEPT_ART=true capture RAID_OVERVIEW 360 800 concept-mord-360x800

# Production atlas registration and new companion: retained alongside all existing gates.
for phase in ART_RAG ART_MIKO ART_LUNE ART_BOSSES ART_ITEMS_TOWER ART_ITEMS_ABYSS ART_ITEMS_STAR COMPANION_LUNE; do
  capture "$phase" 360 800 "${phase,,}-360x800"
  capture "$phase" 720 1280 "${phase,,}-720x1280"
done

"${PYTHON:-python3}" - <<'PY'
from pathlib import Path
import struct

expected = {
    "ready-360x800.png": (360, 800),
    "star-ready-360x800.png": (360, 800),
    "custom-360x800.png": (360, 800),
    "system-access-360x800.png": (360, 800),
    "paywall-360x800.png": (360, 800),
    "raid-360x800.png": (360, 800),
    "raid-overview-free-360x800.png": (360, 800),
    "raid-overview-pro-360x800.png": (360, 800),
    "star-route-360x800.png": (360, 800),
    "paused-360x800.png": (360, 800),
    "end-confirm-360x800.png": (360, 800),
    "aborted-360x800.png": (360, 800),
    "victory-360x800.png": (360, 800),
    "footprint-loading-360x800.png": (360, 800),
    "footprint-present-360x800.png": (360, 800),
    "footprint-posting-360x800.png": (360, 800),
    "footprint-error-360x800.png": (360, 800),
    "footprint-posted-360x800.png": (360, 800),
    "star-victory-360x800.png": (360, 800),
    "star-continue-360x800.png": (360, 800),
    "evolution-360x800.png": (360, 800),
    "companion-egg-360x800.png": (360, 800),
    "companion-hatchling-360x800.png": (360, 800),
    "companion-first-360x800.png": (360, 800),
    "companion-second-360x800.png": (360, 800),
    "companion-mature-360x800.png": (360, 800),
    "log-360x800.png": (360, 800),
    "log-pro-360x800.png": (360, 800),
    "ready-720x1280.png": (720, 1280),
    "star-ready-720x1280.png": (720, 1280),
    "custom-720x1280.png": (720, 1280),
    "system-access-720x1280.png": (720, 1280),
    "paywall-720x1280.png": (720, 1280),
    "raid-720x1280.png": (720, 1280),
    "raid-overview-free-720x1280.png": (720, 1280),
    "raid-overview-pro-720x1280.png": (720, 1280),
    "star-route-720x1280.png": (720, 1280),
    "paused-720x1280.png": (720, 1280),
    "end-confirm-720x1280.png": (720, 1280),
    "aborted-720x1280.png": (720, 1280),
    "victory-720x1280.png": (720, 1280),
    "footprint-present-720x1280.png": (720, 1280),
    "footprint-posted-720x1280.png": (720, 1280),
    "star-victory-720x1280.png": (720, 1280),
    "star-continue-720x1280.png": (720, 1280),
    "evolution-720x1280.png": (720, 1280),
    "companion-hatchling-720x1280.png": (720, 1280),
    "log-720x1280.png": (720, 1280),
    "log-pro-720x1280.png": (720, 1280),
}

for scale in ("1.3", "1.5"):
    for phase in "READY TIMER_MAX CUSTOM_MAX RAID PAUSED END_CONFIRM VICTORY ABORTED PAYWALL PAYWALL_ERROR PAYWALL_RESTORING PAYWALL_PURCHASING FOOTPRINT_PRESENT FOOTPRINT_ERROR LOG_PRO".split():
        for width, height in ((360, 800), (720, 1280)):
            expected[f"{phase.lower()}-{width}x{height}-font-{scale}.png"] = (width, height)
expected["concept-miko-360x800.png"] = (360, 800)
expected["concept-mord-360x800.png"] = (360, 800)
expected["concept-rag-360x800.png"] = (360, 800)
expected["concept-volga-360x800.png"] = (360, 800)
for phase in "ART_RAG ART_MIKO ART_LUNE ART_BOSSES ART_ITEMS_TOWER ART_ITEMS_ABYSS ART_ITEMS_STAR COMPANION_LUNE".split():
    for width, height in ((360, 800), (720, 1280)):
        expected[f"{phase.lower()}-{width}x{height}.png"] = (width, height)
root = Path("artifacts/visual")
for name, expected_size in expected.items():
    path = root / name
    if not path.exists() or path.stat().st_size < 24:
        raise SystemExit(f"missing or invalid screenshot: {name}")
    with path.open("rb") as f:
        header = f.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise SystemExit(f"not a PNG screenshot: {name}")
    actual_size = struct.unpack(">II", header[16:24])
    if actual_size != expected_size:
        raise SystemExit(f"unexpected screenshot size for {name}: {actual_size} != {expected_size}")

print("visual QA captures verified:", ", ".join(expected))
PY
