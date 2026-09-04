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
capture RAID 360 800 raid-360x800
capture VICTORY 360 800 victory-360x800
capture READY 720 1280 ready-720x1280
capture RAID 720 1280 raid-720x1280
capture VICTORY 720 1280 victory-720x1280

python3 - <<'PY'
from pathlib import Path
from PIL import Image

expected = {
    "ready-360x800.png": (360, 800),
    "raid-360x800.png": (360, 800),
    "victory-360x800.png": (360, 800),
    "ready-720x1280.png": (720, 1280),
    "raid-720x1280.png": (720, 1280),
    "victory-720x1280.png": (720, 1280),
}

root = Path("artifacts/visual")
for name, size in expected.items():
    path = root / name
    if not path.exists() or path.stat().st_size == 0:
        raise SystemExit(f"missing screenshot: {name}")
    actual = Image.open(path).size
    if actual != size:
        raise SystemExit(f"unexpected screenshot size for {name}: {actual} != {size}")

print("visual QA captures verified:", ", ".join(expected))
PY
