"""Read-only raster analysis: register display bounds; never edit generated PNG pixels.
Run with Python + Pillow after replacing an atlas. Outputs Kotlin frame metadata.
"""
from pathlib import Path
import hashlib
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RESOURCE = ROOT / "app/src/main/res/drawable-nodpi"
COMPANIONS = ["art_rag", "art_miko", "art_lune"]
SHEETS = COMPANIONS + ["art_bosses", "art_items_tower", "art_items_abyss", "art_items_star"]
frames = {}
manifest = {}
for name in SHEETS:
    image = Image.open(RESOURCE / f"{name}.png").convert("RGB")
    width, height = image.size
    if name in COMPANIONS:
        columns, rows = [0, .18, .36, .55, .745, 1], [0, 1 / 3, .64, 1]
    elif name == "art_bosses":
        columns, rows = [0, .34, .66, 1], [0, .355, .68, 1]
    else:
        columns, rows = [0, 1 / 3, 2 / 3, 1], [0, .258, .495, .729, 1]
    result = []
    for top, bottom in zip(rows, rows[1:]):
        for left, right in zip(columns, columns[1:]):
            cell = (round(left * width), round(top * height), round(right * width), round(bottom * height))
            # Midnight backplates stay below this brightness. The resulting bounds are padded
            # and visually reviewed to retain dark outlines, wing tips and celebration particles.
            points = [(x, y) for y in range(cell[1], cell[3]) for x in range(cell[0], cell[2]) if max(image.getpixel((x, y))) > 70]
            if not points:
                raise ValueError(f"Empty art slot: {name} {cell}")
            x0 = min(x for x, y in points); x1 = max(x for x, y in points) + 1
            y0 = min(y for x, y in points); y1 = max(y for x, y in points) + 1
            px = max(8, round((x1 - x0) * .09), (160 - (x1 - x0) + 1) // 2); py = max(8, round((y1 - y0) * .09), (160 - (y1 - y0) + 1) // 2)
            result.append([max(cell[0], x0 - px) / width, max(cell[1], y0 - py) / height,
                           min(cell[2], x1 + px) / width, min(cell[3], y1 + py) / height])
    frames[name] = result
    manifest[name] = {"size": [width, height], "sha256": hashlib.sha256((RESOURCE / f"{name}.png").read_bytes()).hexdigest(), "frames": result}
lines = ["package com.madowaku.focusraid.ui", "", "import com.madowaku.focusraid.R", "", "// Generated registration metadata; scripts/register-art.py only reads PNGs.", "private val registeredFrames by lazy { mapOf("]
for name, entries in frames.items():
    lines.append(f"    R.drawable.{name} to listOf(")
    for frame in entries:
        lines.append("        ArtworkFrame(" + ", ".join(f"{value:.7f}f" for value in frame) + "),")
    lines.append("    ),")
lines += [") }", "", "internal fun registeredArtworkFrame(resource: Int, slot: Int): ArtworkFrame = registeredFrames.getValue(resource)[slot]", ""]
(ROOT / "app/src/main/java/com/madowaku/focusraid/ui/ArtworkFrameRegistration.kt").write_text("\n".join(lines), encoding="utf-8")
(ROOT / "docs/art-registration.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
print(f"Registered {sum(map(len, frames.values()))} frames from {len(frames)} unchanged PNGs")
