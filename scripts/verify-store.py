"""Validate Play outputs and source provenance. --normalize changes only PNG color mode."""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
STORE = ROOT / 'store/google-play/v1.0'
SHOTS = ['01-hero-a', '01-hero-b', '02-shared-raid', '03-companion', '04-return-raid', '05-everyday', '06-walking']


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--normalize', action='store_true')
    args = parser.parse_args()
    errors, records = [], []
    specs = {name: ((1080, 1920), 'RGB') for name in SHOTS}
    specs.update({'feature-graphic-1024x500': ((1024, 500), 'RGB'), 'icon-512': ((512, 512), 'RGBA')})
    for name, (size, mode) in specs.items():
        path = STORE / 'ja-JP' / f'{name}.png'
        with Image.open(path) as image:
            if args.normalize and image.mode != mode:
                image.convert(mode).save(path, optimize=True)
        with Image.open(path) as image:
            if image.size != size or image.mode != mode:
                errors.append(f'{name}: expected {size}/{mode}, got {image.size}/{image.mode}')
        if name == 'icon-512' and path.stat().st_size > 1024 * 1024:
            errors.append('Play icon exceeds 1024KB')
        records.append({'file': str(path.relative_to(STORE)).replace('\\', '/'), 'width': size[0], 'height': size[1], 'mode': mode, 'bytes': path.stat().st_size, 'sha256': digest(path)})

    en_feature = STORE / 'en-US' / 'feature-graphic-1024x500.png'
    with Image.open(en_feature) as image:
        if args.normalize and image.mode != 'RGB':
            image.convert('RGB').save(en_feature, optimize=True)
    with Image.open(en_feature) as image:
        if image.size != (1024, 500) or image.mode != 'RGB':
            errors.append(f'en-US feature graphic: expected (1024, 500)/RGB, got {image.size}/{image.mode}')
    records.append({'file': 'en-US/feature-graphic-1024x500.png', 'width': 1024, 'height': 500, 'mode': 'RGB', 'bytes': en_feature.stat().st_size, 'sha256': digest(en_feature)})

    copy = json.loads((STORE / 'listing-copy.json').read_text(encoding='utf-8'))
    copy_en = json.loads((STORE / 'listing-copy.en-US.json').read_text(encoding='utf-8'))
    assert len(copy['shortDescriptions']) == len(copy['longDescriptionOpenings']) == 3
    # Play limits: title 30, short description 80, full description 4000 characters.
    for listing in (copy, copy_en):
        locale = listing['locale']
        if not 1 <= len(listing['title']) <= 30:
            errors.append(f'{locale} title exceeds 30 characters')
        if not 1 <= len(listing['fullDescription']) <= 4000:
            errors.append(f'{locale} full description exceeds 4000 characters')
        for item in listing['shortDescriptions']:
            if not 1 <= len(item['text']) <= 80 or '\n' in item['text']:
                errors.append(f'{locale} short description {item["id"]} violates 80-character/single-line limit')
        for item in listing['assets']:
            if len(item['alt']) > 140:
                errors.append(f'{locale} alt text too long: {item["id"]}')

    evidence_path = STORE / 'review' / 'layout-evidence.json'
    if args.normalize:
        log = (ROOT / 'output/playwright/store-render.log').read_text(encoding='utf-8-sig')
        result = log.split('### Result\n', 1)[1].split('\n### ', 1)[0].strip()
        evidence = json.loads(result)
        evidence_path.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        evidence = json.loads(evidence_path.read_text(encoding='utf-8'))
    errors.extend(evidence['errors'])
    # Bounds alone miss collisions between headlines and subtitles. Check both.
    for i, a in enumerate(evidence['text']):
        for b in evidence['text'][i + 1:]:
            if a['asset'] != b['asset']:
                continue
            if min(a['right'], b['right']) - max(a['left'], b['left']) > 1 and min(a['bottom'], b['bottom']) - max(a['top'], b['top']) > 1:
                errors.append(f'Text overlap in {a["asset"]}: {a["value"]} / {b["value"]}')
    used_sources = sorted({entry['source'] for entry in evidence['crops']})
    sources = []
    for source in used_sources:
        path = STORE / source
        with Image.open(path) as image:
            if image.size != (1080, 2400):
                errors.append(f'Unexpected raw capture size: {source}')
        sources.append({'file': source, 'sha256': digest(path)})
    capture_path = STORE / 'raw' / 'capture-provenance.json'
    capture = json.loads(capture_path.read_text(encoding='utf-8-sig'))
    for source in sources:
        if capture['captures'].get(Path(source['file']).name) != source['sha256']:
            errors.append(f'Raw capture differs from provenance: {source["file"]}')
    for path in ['creative.js', 'creative.html', 'listing-copy.json', 'listing-copy.en-US.json']:
        sources.append({'file': path, 'sha256': digest(STORE / path)})
    for path in ['app/src/main/res/drawable-nodpi/ic_launcher_foreground.png', 'app/src/main/res/drawable-nodpi/ic_launcher_monochrome.png']:
        sources.append({'file': '../../../' + path, 'sha256': digest(ROOT / path)})
    summary = {
        'status': 'PASS' if not errors else 'FAIL',
        'capture': capture,
        'outputs': records,
        'sources': sources,
        'shortDescriptionCharacters': {item['id']: len(item['text']) for item in copy['shortDescriptions']},
        'shortDescriptionCharactersEnUS': {item['id']: len(item['text']) for item in copy_en['shortDescriptions']},
        'validation': ['PNG dimensions and color modes', '30/80/4000-character title and descriptions (ja-JP, en-US) and 140-character alt text', 'text bounds and overlaps', 'raw source hashes and dimensions'],
        'pending': ['3–5 independent five-second reviews', 'Play Console and actual launcher mask/themed-icon review', 'release-candidate production shared-raid verification'],
        'errors': errors,
    }
    manifest = STORE / 'review' / 'manifest.json'
    if args.normalize:
        manifest.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    elif json.loads(manifest.read_text(encoding='utf-8')) != summary:
        errors.append('Manifest does not match current assets or source. Render again.')
    if errors:
        raise SystemExit(json.dumps(errors, ensure_ascii=False, indent=2))
    print(f'PASS: {len(records)} Play assets, {len(used_sources)} raw sources; copy lengths {summary["shortDescriptionCharacters"]}')


if __name__ == '__main__':
    main()
