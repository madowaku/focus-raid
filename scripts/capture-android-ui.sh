#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.madowaku.focusraid"
ACTIVITY="${PACKAGE}/.VisualQaActivity"
OUTPUT_DIR="artifacts/visual"

cleanup() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
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
  adb shell am start -W -n "$ACTIVITY" --es phase "$phase"
  sleep 2
  adb exec-out screencap -p > "${OUTPUT_DIR}/${output}.png"
}

capture READY 360 800 ready-360x800
capture CUSTOM 360 800 custom-360x800
capture RAID 360 800 raid-360x800
capture PAUSED 360 800 paused-360x800
capture VICTORY 360 800 victory-360x800
capture READY 720 1280 ready-720x1280
capture CUSTOM 720 1280 custom-720x1280
capture RAID 720 1280 raid-720x1280
capture PAUSED 720 1280 paused-720x1280
capture VICTORY 720 1280 victory-720x1280

python3 - <<'PY'
from pathlib import Path
import struct

expected = {
    "ready-360x800.png": (360, 800),
    "custom-360x800.png": (360, 800),
    "raid-360x800.png": (360, 800),
    "paused-360x800.png": (360, 800),
    "victory-360x800.png": (360, 800),
    "ready-720x1280.png": (720, 1280),
    "custom-720x1280.png": (720, 1280),
    "raid-720x1280.png": (720, 1280),
    "paused-720x1280.png": (720, 1280),
    "victory-720x1280.png": (720, 1280),
}

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